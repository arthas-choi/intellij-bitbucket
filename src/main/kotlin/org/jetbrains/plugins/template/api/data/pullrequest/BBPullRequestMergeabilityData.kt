// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data.pullrequest

import org.jetbrains.plugins.template.api.data.BBNodes

class BBPullRequestMergeabilityData(val mergeable: BBPullRequestMergeableState,
                                    val canBeRebased: Boolean,
                                    val mergeStateStatus: BBPullRequestMergeStateStatus,
                                    val commits: BBNodes<BBPullRequestCommitWithCheckStatuses>)
