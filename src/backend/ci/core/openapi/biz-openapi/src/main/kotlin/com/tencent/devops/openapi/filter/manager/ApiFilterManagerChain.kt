/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.openapi.filter.manager

import javax.ws.rs.container.ContainerRequestContext
import javax.ws.rs.core.Response

interface ApiFilterManagerChain {

    fun doFilterCheck(
        requestContext: FilterContext,
        chain: Iterator<ApiFilterManager>
    ) {
        if (chain.hasNext()) {
            val next = chain.next()
            if (next.canExecute(requestContext)) {
                requestContext.setFlowState(next.verify(requestContext))
            }
            doFilterCheck(requestContext, chain)
            return
        }
        // 只有通过BREAK退出才有效，剩余情况为没有匹配到合适的
        if (requestContext.needCheckPermissions && requestContext.flowState != ApiFilterFlowState.BREAK) {
            requestContext.requestContext.abortWith(
                Response.status(Response.Status.BAD_REQUEST)
                    .entity("You do not have permission to access")
                    .build()
            )
        }
    }

    fun FilterContext.setFlowState(state: ApiFilterFlowState) {
        /*BREAK 为最终态，不允许状态流转变更*/
        if (flowState != ApiFilterFlowState.BREAK) {
            flowState = state
        }
    }

    fun doFilterCheck(requestContext: ContainerRequestContext)
}
