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
package com.tencent.devops.notify.pojo

import com.tencent.devops.common.api.util.AESUtil
import org.slf4j.LoggerFactory

/**
 * TOF4秘钥信息
 */
class ExtTOF4SecurityInfo {
    var enable: Boolean = false
    var token: String = ""
    var passId: String = ""

    companion object {
        private val logger = LoggerFactory.getLogger(ExtTOF4SecurityInfo::class.java)

        fun get(message: BaseMessage, encryptKey: String?): ExtTOF4SecurityInfo {
            if (message.v2ExtInfo.isBlank()) {
                return ExtTOF4SecurityInfo()
            }

            if (encryptKey.isNullOrBlank()) {
                logger.error("TOF error, decrypt notify v2 extension, encrypt key can not be empty")
                return ExtTOF4SecurityInfo()
            }

            return try {
                val securityArr = AESUtil.decrypt(encryptKey, message.v2ExtInfo).split(":")
                ExtTOF4SecurityInfo().apply {
                    enable = true
                    passId = securityArr[0]
                    token = securityArr[1]
                }
            } catch (e: Exception) {
                logger.error("TOF error, decrypt notify v2 extension info fail", e)
                ExtTOF4SecurityInfo()
            }
        }
    }
}
