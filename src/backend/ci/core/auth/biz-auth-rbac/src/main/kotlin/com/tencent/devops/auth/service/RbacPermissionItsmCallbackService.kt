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
 *
 */

package com.tencent.devops.auth.service

import com.tencent.devops.auth.pojo.ItsmCallBackInfo
import com.tencent.devops.auth.pojo.enums.AuthItsmApprovalType
import com.tencent.devops.auth.pojo.event.AuthItsmCallbackEvent
import com.tencent.devops.auth.service.iam.PermissionItsmCallbackService
import com.tencent.devops.common.event.dispatcher.trace.TraceEventDispatcher
import org.slf4j.LoggerFactory

class RbacPermissionItsmCallbackService constructor(
    private val traceEventDispatcher: TraceEventDispatcher,
    private val itsmService: ItsmService
) : PermissionItsmCallbackService {

    companion object {
        private val logger = LoggerFactory.getLogger(RbacPermissionItsmCallbackService::class.java)
        private const val CANCEL_ITSM_APPLICATION_STATUS = "REVOKED"
    }

    override fun createProjectCallBack(itsmCallBackInfo: ItsmCallBackInfo) {
        logger.info("auth itsm create project callback info:$itsmCallBackInfo")
        // 校验itsm回调token
        itsmService.verifyItsmToken(itsmCallBackInfo.token)
        // 当用户主动取消创建时,会调itsm单取消创建,这时itsm也会发送回调,不处理
        if (itsmCallBackInfo.currentStatus == CANCEL_ITSM_APPLICATION_STATUS) {
            return
        }
        traceEventDispatcher.dispatch(
            AuthItsmCallbackEvent(
                approveType = AuthItsmApprovalType.CREATE.name,
                itsmCallBackInfo = itsmCallBackInfo
            )
        )
    }

    override fun updateProjectCallback(itsmCallBackInfo: ItsmCallBackInfo) {
        logger.info("auth itsm update callback info:$itsmCallBackInfo")
        // 校验itsm回调token
        itsmService.verifyItsmToken(itsmCallBackInfo.token)
        // 当用户主动取消创建时,会调itsm单取消创建,这时itsm也会发送回调,不处理
        if (itsmCallBackInfo.currentStatus == CANCEL_ITSM_APPLICATION_STATUS) {
            return
        }
        traceEventDispatcher.dispatch(
            AuthItsmCallbackEvent(
                approveType = AuthItsmApprovalType.UPDATE.name,
                itsmCallBackInfo = itsmCallBackInfo
            )
        )
    }
}
