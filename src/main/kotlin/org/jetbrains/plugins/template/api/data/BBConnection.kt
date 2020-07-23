// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.template.api.data

import org.jetbrains.plugins.template.api.data.graphql.BBGQLPageInfo
import org.jetbrains.plugins.template.api.data.graphql.BBGQLPagedRequestResponse


open class BBConnection<out T>(override val pageInfo: BBGQLPageInfo, nodes: List<T>)
  : BBNodes<T>(nodes), BBGQLPagedRequestResponse<T>