/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.storage.innercos.cl5

import com.qq.l5.L5sys
import com.qq.l5.L5sys.QosRequest
import org.slf4j.Logger
import org.slf4j.LoggerFactory

object CL5Utils {

    private val logger: Logger = LoggerFactory.getLogger(CL5Utils::class.java)
    private var instance: L5sys? = null
    private const val CL5_SUCCESS = 0

    fun route(cl5Info: CL5Info): RouteInfo {
        val request = QosRequest().apply {
            modId = cl5Info.modId
            cmdId = cl5Info.cmdId
        }
        val retCode = cl5Instance.ApiGetRoute(request, cl5Info.timeout)
        check(retCode == CL5_SUCCESS) { "Failed to get cl5 route info[$cl5Info]，return code: $retCode" }
        val routeInfo = RouteInfo(request.hostIp, request.hostPort)
        if (logger.isDebugEnabled) {
            logger.debug("Success to get cl5 route info[$cl5Info]: $routeInfo")
        }
        return routeInfo
    }

    private val cl5Instance: L5sys
        get() {
            if (instance == null) {
                instance = L5sys()
            }
            return instance!!
        }
}
