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

package com.tencent.devops.common.ci.task

import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(V3插件)")
open class CodeCCScanInContainerTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: CodeCCScanInContainerInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {

    companion object {
        const val taskType = "codeCCScan"
        const val taskVersion = "@latest"
        const val atomCode = "CodeccCheckAtomDebug"
    }

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        return MarketBuildAtomElement(
            name = displayName ?: "CodeCC扫描",
            id = null,
            status = null,
            atomCode = atomCode,
            version = "4.*",
            data = mapOf("input" to inputs)
        )
    }
}

@ApiModel("CodeCC代码检查任务(V3插件)")
data class CodeCCScanInContainerInput(
    @ApiModelProperty("语言", required = true)
    val languages: List<String>? = null, // ["PYTHON", "KOTLIN"]
    @ApiModelProperty("工具", required = true)
    val tools: List<String>? = null, // ["PYTHON", "KOTLIN"]
    @ApiModelProperty("白名单", required = false)
    var path: List<String>? = null,
    @ApiModelProperty("编译脚本", required = false)
    val script: String? = null,
    @ApiModelProperty("规则集", required = true)
    val languageRuleSetMap: Map<String, List<String>?>? = emptyMap(),
    @ApiModelProperty("全量还是增量, 1：增量；0：全量", required = false)
    val toolScanType: String? = null, // 对应接口的scanType, 1：增量；0：全量 2: diff模式
    @ApiModelProperty("黑名单，添加后的代码路径将不会产生告警", required = false)
    val customPath: String? = null, // 黑名单，添加后的代码路径将不会产生告警
    @ApiModelProperty("Python版本", required = false)
    val pyVersion: String? = null
) : AbstractInput()
