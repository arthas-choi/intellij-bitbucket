// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api

import com.fasterxml.jackson.databind.JsonNode
import com.intellij.util.ThrowableConvertor
import org.jetbrains.plugins.template.api.data.BitbucketResponsePage
import org.jetbrains.plugins.template.api.data.BitbucketSearchResult
import org.jetbrains.plugins.template.api.data.graphql.BBGQLQueryRequest
import org.jetbrains.plugins.template.api.data.graphql.BBGQLSyntaxError
import org.jetbrains.plugins.template.api.data.graphql.BBGQLResponse
import org.jetbrains.plugins.template.exceptions.BitbucketAuthenticationException
import org.jetbrains.plugins.template.exceptions.BitbucketConfusingException
import org.jetbrains.plugins.template.exceptions.BitbucketJsonException


import java.io.IOException

/**
 * Represents an API request with strictly defined response type
 */
sealed class BitbucketApiRequest<out T>(val url: String) {
  var operationName: String? = null
  abstract val acceptMimeType: String?

  open val tokenHeaderType = BitbucketApiRequestExecutor.TokenHeaderType.TOKEN

  protected val headers = mutableMapOf<String, String>()
  val additionalHeaders: Map<String, String>
    get() = headers

  @Throws(IOException::class)
  abstract fun extractResult(response: BitbucketApiResponse): T

  fun withOperationName(name: String): BitbucketApiRequest<T> {
    operationName = name
    return this
  }

  abstract class Get<T> @JvmOverloads constructor(url: String,
                                                  override val acceptMimeType: String? = null) : BitbucketApiRequest<T>(url) {
    abstract class Optional<T> @JvmOverloads constructor(url: String,
                                                         acceptMimeType: String? = null) : Get<T?>(url, acceptMimeType) {
      companion object {
        inline fun <reified T> json(url: String, acceptMimeType: String? = null): Optional<T> =
          Json(url, T::class.java, acceptMimeType)
      }

      open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
        : Optional<T>(url, acceptMimeType) {

        override fun extractResult(response: BitbucketApiResponse): T = parseJsonObject(response, clazz)
      }
    }

    companion object {
      inline fun <reified T> json(url: String, acceptMimeType: String? = null): Get<T> =
        Json(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonPage(url: String, acceptMimeType: String? = null): Get<BitbucketResponsePage<T>> =
        JsonPage(url, T::class.java, acceptMimeType)

      inline fun <reified T> jsonSearchPage(url: String, acceptMimeType: String? = null): Get<BitbucketResponsePage<T>> =
        JsonSearchPage(url, T::class.java, acceptMimeType)
    }

    open class Json<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<T>(url, acceptMimeType) {

      override fun extractResult(response: BitbucketApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<List<T>>(url, acceptMimeType) {

      override fun extractResult(response: BitbucketApiResponse): List<T> = parseJsonList(response, clazz)
    }

    open class JsonPage<T>(url: String, private val clazz: Class<T>, acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<BitbucketResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: BitbucketApiResponse): BitbucketResponsePage<T> {
        return BitbucketResponsePage.parseFromHeader(parseJsonList(response, clazz),
                                                  response.findHeader(BitbucketResponsePage.HEADER_NAME))
      }
    }

    open class JsonSearchPage<T>(url: String,
                                 private val clazz: Class<T>,
                                 acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
      : Get<BitbucketResponsePage<T>>(url, acceptMimeType) {

      override fun extractResult(response: BitbucketApiResponse): BitbucketResponsePage<T> {
        return BitbucketResponsePage.parseFromHeader(parseJsonSearchPage(response, clazz).items,
                                                  response.findHeader(BitbucketResponsePage.HEADER_NAME))
      }
    }
  }

  abstract class Head<T> @JvmOverloads constructor(url: String,
                                                   override val acceptMimeType: String? = null) : BitbucketApiRequest<T>(url)

  abstract class WithBody<out T>(url: String) : BitbucketApiRequest<T>(url) {
    abstract val body: String?
    abstract val bodyMimeType: String
  }

  abstract class Post<out T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                       url: String,
                                                       override var acceptMimeType: String? = null) : BitbucketApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any, acceptMimeType: String? = null): Post<T> =
        Json(url, body, T::class.java, acceptMimeType)
    }

    open class Json<T>(url: String, private val bodyObject: Any, private val clazz: Class<T>,
                       acceptMimeType: String? = BitbucketApiContentHelper.V3_JSON_MIME_TYPE)
      : Post<T>(BitbucketApiContentHelper.JSON_MIME_TYPE, url, acceptMimeType) {

      override val body: String
        get() = BitbucketApiContentHelper.toJson(bodyObject)

      override fun extractResult(response: BitbucketApiResponse): T = parseJsonObject(response, clazz)
    }

    abstract class GQLQuery<out T>(url: String,
                                   private val queryName: String,
                                   private val variablesObject: Any)
      : Post<T>(BitbucketApiContentHelper.JSON_MIME_TYPE, url) {

      override val tokenHeaderType = BitbucketApiRequestExecutor.TokenHeaderType.BEARER

      override val body: String
        get() {
          val query = BBGQLQueryLoader.loadQuery(queryName)
          val request = BBGQLQueryRequest(query, variablesObject)
          return BitbucketApiContentHelper.toJson(request, true)
        }

      protected fun throwException(errors: List<BBGQLSyntaxError>): Nothing {
        if (errors.any { it.type.equals("INSUFFICIENT_SCOPES", true) })
          throw BitbucketAuthenticationException("Access token has not been granted the required scopes.")

        throw BitbucketConfusingException(errors.toString())
      }

      class Parsed<out T>(url: String,
                          requestFilePath: String,
                          variablesObject: Any,
                          private val clazz: Class<T>)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: BitbucketApiResponse): T {
          val result: BBGQLResponse<out T> = parseGQLResponse(response, clazz)
          val data = result.data
          if (data != null) return data

          val errors = result.errors
          if (errors == null) error("Undefined request state - both result and errors are null")
          else throwException(errors)
        }
      }

      class TraversedParsed<out T : Any>(url: String,
                                         requestFilePath: String,
                                         variablesObject: Any,
                                         private val clazz: Class<out T>,
                                         private vararg val pathFromData: String)
        : GQLQuery<T>(url, requestFilePath, variablesObject) {

        override fun extractResult(response: BitbucketApiResponse): T {
          return parseResponse(response, clazz, pathFromData)
                 ?: throw BitbucketJsonException("Non-nullable entity is null or entity path is invalid")
        }
      }

      class OptionalTraversedParsed<T>(url: String,
                                       requestFilePath: String,
                                       variablesObject: Any,
                                       private val clazz: Class<T>,
                                       private vararg val pathFromData: String)
        : GQLQuery<T?>(url, requestFilePath, variablesObject) {
        override fun extractResult(response: BitbucketApiResponse): T? {
          return parseResponse(response, clazz, pathFromData)
        }
      }

      internal fun <T> parseResponse(response: BitbucketApiResponse,
                                     clazz: Class<T>,
                                     pathFromData: Array<out String>): T? {
        val result: BBGQLResponse<out JsonNode> = parseGQLResponse(response, JsonNode::class.java)
        val data = result.data
        if (data != null && !data.isNull) {
          var node: JsonNode = data
          for (path in pathFromData) {
            node = node[path] ?: break
          }
          if (!node.isNull) return BitbucketApiContentHelper.fromJson(node.toString(), clazz, true)
        }
        val errors = result.errors
        if (errors == null) return null
        else throwException(errors)
      }
    }
  }

  abstract class Put<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                  url: String,
                                                  override val acceptMimeType: String? = null) : BitbucketApiRequest.WithBody<T>(url) {
    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Put<T> = Json(url, body, T::class.java)

      inline fun <reified T> jsonList(url: String, body: Any): Put<List<T>> = JsonList(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<T>(BitbucketApiContentHelper.JSON_MIME_TYPE, url, BitbucketApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { BitbucketApiContentHelper.toJson(it) }

      override fun extractResult(response: BitbucketApiResponse): T = parseJsonObject(response, clazz)
    }

    open class JsonList<T>(url: String, private val bodyObject: Any?, private val clazz: Class<T>)
      : Put<List<T>>(BitbucketApiContentHelper.JSON_MIME_TYPE, url, BitbucketApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { BitbucketApiContentHelper.toJson(it) }

      override fun extractResult(response: BitbucketApiResponse): List<T> = parseJsonList(response, clazz)
    }
  }

  abstract class Patch<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                    url: String,
                                                    override var acceptMimeType: String? = null) : Post<T>(bodyMimeType,
                                                                                                           url,
                                                                                                           acceptMimeType) {
    companion object {
      inline fun <reified T> json(url: String, body: Any): Post<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, bodyObject: Any, clazz: Class<T>) : Post.Json<T>(url, bodyObject, clazz)
  }

  abstract class Delete<T> @JvmOverloads constructor(override val bodyMimeType: String,
                                                     url: String,
                                                     override val acceptMimeType: String? = null) : BitbucketApiRequest.WithBody<T>(url) {

    companion object {
      inline fun <reified T> json(url: String, body: Any? = null): Delete<T> = Json(url, body, T::class.java)
    }

    open class Json<T>(url: String, private val bodyObject: Any? = null, private val clazz: Class<T>)
      : Delete<T>(BitbucketApiContentHelper.JSON_MIME_TYPE, url, BitbucketApiContentHelper.V3_JSON_MIME_TYPE) {
      init {
        if (bodyObject == null) headers["Content-Length"] = "0"
      }

      override val body: String?
        get() = bodyObject?.let { BitbucketApiContentHelper.toJson(it) }

      override fun extractResult(response: BitbucketApiResponse): T = parseJsonObject(response, clazz)
    }
  }

  companion object {
    private fun <T> parseJsonObject(response: BitbucketApiResponse, clazz: Class<T>): T {
      return response.readBody(ThrowableConvertor { BitbucketApiContentHelper.readJsonObject(it, clazz) })
    }

    private fun <T> parseJsonList(response: BitbucketApiResponse, clazz: Class<T>): List<T> {
      return response.readBody(ThrowableConvertor { BitbucketApiContentHelper.readJsonList(it, clazz) })
    }

    private fun <T> parseJsonSearchPage(response: BitbucketApiResponse, clazz: Class<T>): BitbucketSearchResult<T> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        BitbucketApiContentHelper.readJsonObject(it, BitbucketSearchResult::class.java, clazz) as BitbucketSearchResult<T>
      })
    }

    private fun <T> parseGQLResponse(response: BitbucketApiResponse, clazz: Class<out T>): BBGQLResponse<out T> {
      return response.readBody(ThrowableConvertor {
        @Suppress("UNCHECKED_CAST")
        BitbucketApiContentHelper.readJsonObject(it, BBGQLResponse::class.java, clazz, gqlNaming = true) as BBGQLResponse<T>
      })
    }
  }
}
