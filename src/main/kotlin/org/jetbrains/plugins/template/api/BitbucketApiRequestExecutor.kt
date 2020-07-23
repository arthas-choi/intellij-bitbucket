// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.EmptyProgressIndicator
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.EventDispatcher
import com.intellij.util.ThrowableConvertor
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpSecurityUtil
import com.intellij.util.io.RequestBuilder
import org.jetbrains.annotations.CalledInAny
import org.jetbrains.annotations.CalledInBackground
import org.jetbrains.annotations.TestOnly
import org.jetbrains.plugins.template.api.data.BitbucketErrorMessage
import org.jetbrains.plugins.template.exceptions.*
import org.jetbrains.plugins.template.util.BitbucketSettings


import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.net.HttpURLConnection
import java.util.*
import java.util.function.Supplier
import java.util.zip.GZIPInputStream

/**
 * Executes API requests taking care of authentication, headers, proxies, timeouts, etc.
 */
sealed class BitbucketApiRequestExecutor {

  protected val authDataChangedEventDispatcher = EventDispatcher.create(AuthDataChangeListener::class.java)

  @CalledInBackground
  @Throws(IOException::class, ProcessCanceledException::class)
  abstract fun <T> execute(indicator: ProgressIndicator, request: BitbucketApiRequest<T>): T

  @TestOnly
  @CalledInBackground
  @Throws(IOException::class, ProcessCanceledException::class)
  fun <T> execute(request: BitbucketApiRequest<T>): T = execute(EmptyProgressIndicator(), request)

  fun addListener(listener: AuthDataChangeListener, disposable: Disposable) =
    authDataChangedEventDispatcher.addListener(listener, disposable)

  fun addListener(disposable: Disposable, listener: () -> Unit) =
    authDataChangedEventDispatcher.addListener(object : AuthDataChangeListener {
      override fun authDataChanged() {
        listener()
      }
    }, disposable)

  class WithTokenAuth internal constructor(bitbucketSettings: BitbucketSettings,
                                           token: String,
                                           private val useProxy: Boolean) : Base(bitbucketSettings) {
    @Volatile
    internal var token: String = token
      set(value) {
        field = value
        authDataChangedEventDispatcher.multicaster.authDataChanged()
      }

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: BitbucketApiRequest<T>): T {
      indicator.checkCanceled()
      return createRequestBuilder(request)
        .tuner { connection ->
          request.additionalHeaders.forEach(connection::addRequestProperty)
          connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "${request.tokenHeaderType} $token")
        }
        .useProxy(useProxy)
        .execute(request, indicator)
    }
  }

  class WithBasicAuth internal constructor(bitbucketSettings: BitbucketSettings,
                                           private val login: String,
                                           private val password: CharArray,
                                           private val twoFactorCodeSupplier: Supplier<String?>) : Base(bitbucketSettings) {
    private var twoFactorCode: String? = null

    @Throws(IOException::class, ProcessCanceledException::class)
    override fun <T> execute(indicator: ProgressIndicator, request: BitbucketApiRequest<T>): T {
      indicator.checkCanceled()
      val basicHeaderValue = HttpSecurityUtil.createBasicAuthHeaderValue(login, password)
      return executeWithBasicHeader(indicator, request, basicHeaderValue)
    }

    private fun <T> executeWithBasicHeader(indicator: ProgressIndicator, request: BitbucketApiRequest<T>, header: String): T {
      indicator.checkCanceled()
      return try {
        createRequestBuilder(request)
          .tuner { connection ->
            request.additionalHeaders.forEach(connection::addRequestProperty)
            connection.addRequestProperty(HttpSecurityUtil.AUTHORIZATION_HEADER_NAME, "Basic $header")
            twoFactorCode?.let { connection.addRequestProperty(OTP_HEADER_NAME, it) }
          }
          .execute(request, indicator)
      }
      catch (e: BitbucketTwoFactorAuthenticationException) {
        twoFactorCode = twoFactorCodeSupplier.get() ?: throw e
        executeWithBasicHeader(indicator, request, header)
      }
    }
  }

  abstract class Base(private val bitbucketSettings: BitbucketSettings) : BitbucketApiRequestExecutor() {
    protected fun <T> RequestBuilder.execute(request: BitbucketApiRequest<T>, indicator: ProgressIndicator): T {
      indicator.checkCanceled()
      try {
        LOG.debug("Request: ${request.url} ${request.operationName} : Connecting")
        return connect {
          val connection = it.connection as HttpURLConnection
          if (request is BitbucketApiRequest.WithBody) {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} with body:\n${request.body} : Connected")
            request.body?.let { body -> it.write(body) }
          }
          else {
            LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Connected")
          }
          checkResponseCode(connection)
          indicator.checkCanceled()
          val result = request.extractResult(createResponse(it, indicator))
          LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Result extracted")
          result
        }
      }
      catch (e: BitbucketStatusCodeException) {
        @Suppress("UNCHECKED_CAST")
        if (request is BitbucketApiRequest.Get.Optional<*> && e.statusCode == HttpURLConnection.HTTP_NOT_FOUND) return null as T else throw e
      }
      catch (e: BitbucketConfusingException) {
        if (request.operationName != null) {
          val errorText = "Can't ${request.operationName}"
          e.setDetails(errorText)
          LOG.debug(errorText, e)
        }
        throw e
      }
    }

    protected fun createRequestBuilder(request: BitbucketApiRequest<*>): RequestBuilder {
      return when (request) {
        is BitbucketApiRequest.Get -> HttpRequests.request(request.url)
        is BitbucketApiRequest.Post -> HttpRequests.post(request.url, request.bodyMimeType)
        is BitbucketApiRequest.Put -> HttpRequests.put(request.url, request.bodyMimeType)
        is BitbucketApiRequest.Patch -> HttpRequests.patch(request.url, request.bodyMimeType)
        is BitbucketApiRequest.Head -> HttpRequests.head(request.url)
        is BitbucketApiRequest.Delete -> {
          if (request.body == null) HttpRequests.delete(request.url) else HttpRequests.delete(request.url, request.bodyMimeType)
        }
        else -> throw UnsupportedOperationException("${request.javaClass} is not supported")
      }
        .connectTimeout(bitbucketSettings.connectionTimeout)
        .userAgent("Intellij IDEA Bitbucket Plugin")
        .throwStatusCodeException(false)
        .forceHttps(false)
        .accept(request.acceptMimeType)
    }

    @Throws(IOException::class)
    private fun checkResponseCode(connection: HttpURLConnection) {
      if (connection.responseCode < 400) return
      val statusLine = "${connection.responseCode} ${connection.responseMessage}"
      val errorText = getErrorText(connection)
      LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Error ${statusLine} body:\n${errorText}")

      val jsonError = errorText?.let { getJsonError(connection, it) }
      jsonError ?: LOG.debug("Request: ${connection.requestMethod} ${connection.url} : Unable to parse JSON error")

      throw when (connection.responseCode) {
        HttpURLConnection.HTTP_UNAUTHORIZED,
        HttpURLConnection.HTTP_PAYMENT_REQUIRED,
        HttpURLConnection.HTTP_FORBIDDEN -> {
          val otpHeader = connection.getHeaderField(OTP_HEADER_NAME)
          if (otpHeader != null && otpHeader.contains("required", true)) {
            BitbucketTwoFactorAuthenticationException(jsonError?.presentableError ?: errorText)
          }
          else if (jsonError?.containsReasonMessage("API rate limit exceeded") == true) {
            BitbucketRateLimitExceededException(jsonError.presentableError)
          }
          else BitbucketAuthenticationException("Request response: " + (jsonError?.presentableError ?: errorText ?: statusLine))
        }
        else -> {
          if (jsonError != null) {
            BitbucketStatusCodeException("$statusLine - ${jsonError.presentableError}", jsonError, connection.responseCode)
          }
          else {
            BitbucketStatusCodeException("$statusLine - ${errorText}", connection.responseCode)
          }
        }
      }
    }

    private fun getErrorText(connection: HttpURLConnection): String? {
      val errorStream = connection.errorStream ?: return null
      val stream = if (connection.contentEncoding == "gzip") GZIPInputStream(errorStream) else errorStream
      return InputStreamReader(stream, Charsets.UTF_8).use { it.readText() }
    }

    private fun getJsonError(connection: HttpURLConnection, errorText: String): BitbucketErrorMessage? {
      if (!connection.contentType.startsWith(BitbucketApiContentHelper.JSON_MIME_TYPE)) return null
      return try {
        return BitbucketApiContentHelper.fromJson(errorText)
      }
      catch (jse: BitbucketJsonException) {
        null
      }
    }

    private fun createResponse(request: HttpRequests.Request, indicator: ProgressIndicator): BitbucketApiResponse {
      return object : BitbucketApiResponse {
        override fun findHeader(headerName: String): String? = request.connection.getHeaderField(headerName)

        override fun <T> readBody(converter: ThrowableConvertor<Reader, T, IOException>): T = request.getReader(indicator).use {
          converter.convert(it)
        }

        override fun <T> handleBody(converter: ThrowableConvertor<InputStream, T, IOException>): T = request.inputStream.use {
          converter.convert(it)
        }
      }
    }
  }

  class Factory {
    @CalledInAny
    fun create(token: String): WithTokenAuth {
      return create(token, true)
    }

    @CalledInAny
    fun create(token: String, useProxy: Boolean = true): WithTokenAuth {
      return WithTokenAuth(BitbucketSettings.getInstance(), token, useProxy)
    }

    @CalledInAny
    internal fun create(login: String, password: CharArray, twoFactorCodeSupplier: Supplier<String?>): WithBasicAuth {
      return WithBasicAuth(BitbucketSettings.getInstance(), login, password, twoFactorCodeSupplier)
    }

    companion object {
      @JvmStatic
      fun getInstance(): Factory = service()
    }
  }

  companion object {
    private val LOG = logger<BitbucketApiRequestExecutor>()

    private const val OTP_HEADER_NAME = "X-Bitbucket-OTP"
  }

  interface AuthDataChangeListener : EventListener {
    fun authDataChanged()
  }

  enum class TokenHeaderType {
    TOKEN, BEARER
  }
}