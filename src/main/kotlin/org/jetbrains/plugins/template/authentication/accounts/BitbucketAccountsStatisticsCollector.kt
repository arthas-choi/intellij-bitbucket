// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.authentication.accounts

import com.intellij.internal.statistic.beans.MetricEvent
import com.intellij.internal.statistic.beans.newMetric
import com.intellij.internal.statistic.eventLog.FeatureUsageData
import com.intellij.internal.statistic.service.fus.collectors.ApplicationUsagesCollector
import com.intellij.openapi.components.service
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.plugins.template.api.BitbucketServerPath


internal class BitbucketAccountsStatisticsCollector : ApplicationUsagesCollector() {

  override fun getVersion() = 2

  override fun getMetrics(): Set<MetricEvent> {
    val accountManager = service<BitbucketAccountManager>()
    val hasAccountsWithNonDefaultHost = accountManager.accounts.any {
      !StringUtil.equalsIgnoreCase(it.server.host, BitbucketServerPath.DEFAULT_HOST)
    }

    return setOf(
      newMetric("accounts", FeatureUsageData()
        .addCount(accountManager.accounts.size)
        .addData("has_enterprise", hasAccountsWithNonDefaultHost)))
  }

  override fun getGroupId(): String = "vcs.github"
}