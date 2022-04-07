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

package com.tencent.devops.plugin.codecc.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class CodeccConfig {

    /**
     * 代码检查网关地址
     */
    @Value("\${codeccGateway.gateway:}")
    val codeccApiGateWay: String = ""

    @Value("\${codeccGateway.proxy:}")
    val codeccApiProxyGateWay: String = ""

    @Value("\${codeccGateway.api.createTask:/ms/task/api/service/task}")
    val createPath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.updateTask:/ms/task/api/service/task}")
    val updatePath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.checkTaskExists:/ms/task/api/service/task/exists}")
    val existPath = "/ms/task/api/service/task/exists"

    @Value("\${codeccGateway.api.deleteTask:/ms/task/api/service/task}")
    val deletePath = "/ms/task/api/service/task"

    @Value("\${codeccGateway.api.codeCheckReport:/api}")
    val report = ""

    @Value("\${codeccGateway.api.getRuleSets:/blueShield/getRuleSetsPath}")
    val getRuleSetsPath = ""

    /**
     *  代码检查插件维度详情
     */
    @Value("\${quality.codecc.compileTool:}")
    val compileTool = ""

    @Value("\${quality.codecc.ccnDupcTool:}")
    val ccnDupcTool = ""

    @Value("\${quality.codecc.dimension:}")
    val dimension = ""

    @Value("\${quality.codecc.compileUrl:}")
    val compileUrl = ""

    @Value("\${quality.codecc.ccnDupcUrl:}")
    val ccnDupcUrl = ""

    @Value("\${quality.codecc.lintUrl:}")
    val lintUrl = ""

    @Value("\${quality.codecc.dimensionUrl:}")
    val dimensionUrl = ""

    fun getCodeccDetailUrl(detail: String?): String {
        val compileTool = compileTool.split(",")
        val ccnDupcTool = ccnDupcTool.split(",")
        val dimension = dimension.split(",")
        return when (detail) {
            in compileTool -> compileUrl
            in ccnDupcTool -> ccnDupcUrl
            in dimension -> dimensionUrl.replace("##dimension##", detail?.toUpperCase() ?: "")
            else -> lintUrl
        }
    }
}
