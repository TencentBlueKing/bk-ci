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

package com.tencent.devops.support.resources.external

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.external.ExternalWechatWorkResource
import com.tencent.devops.support.robot.RobotService
import com.tencent.devops.support.services.WechatWorkCallbackService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalWechatWorkResourceImpl @Autowired constructor(
    private val wechatWorkCallbackService: WechatWorkCallbackService,
    private val robotService: RobotService
) : ExternalWechatWorkResource {
    private val logger = LoggerFactory.getLogger(ExternalWechatWorkResourceImpl::class.java)

    override fun callback(
        signature: String,
        timestamp: Long,
        nonce: String,
        echoStr: String,
        reqData: String?
    ): Result<String> {

        val sMsg = wechatWorkCallbackService.callbackGet(signature, timestamp, nonce, echoStr)

        logger.info(sMsg)
        logger.info(signature)
        logger.info(timestamp.toString())
        logger.info(nonce)
        logger.info(echoStr)
        logger.info(reqData)
        return Result(data = sMsg)
    }

    override fun callback(
        signature: String,
        timestamp: Long,
        nonce: String,
        reqData: String?
    ): Result<Boolean> {

        val xmlDocument = wechatWorkCallbackService.callbackPost(signature, timestamp, nonce, reqData)
        return Result(data = xmlDocument)
    }

    override fun robotCallback(
        signature: String,
        timestamp: Long,
        nonce: String,
        reqData: String?
    ): Result<Boolean> {
        return Result(robotService.robotCallbackPost(signature, timestamp, nonce, reqData))
    }

    override fun robotCallback(
        signature: String,
        timestamp: Long,
        nonce: String,
        echoStr: String,
        reqData: String?
    ): String {
        val sMsg = robotService.robotVerifyURL(signature, timestamp, nonce, echoStr)
        logger.info(sMsg)
        return sMsg
    }
}
