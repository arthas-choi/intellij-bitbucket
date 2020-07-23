// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

open class BBCommitWithCheckStatuses(id: String, oid: String, abbreviatedOid: String,
                                     val status: Status?,
                                     val checkSuites: BBNodes<BBCommitCheckSuiteStatus>)
  : BBCommitHash(id, oid, abbreviatedOid) {

  class Status(val contexts: List<BBCommitStatusContextStatus>)
}