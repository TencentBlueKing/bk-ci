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

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(客户端)")
open class CodeCCScanClientTask(
    @ApiModelProperty("displayName", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: CodeCCScanClientInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {

    companion object {
        const val taskType = "codeCCScanClient"
        const val taskVersion = "@latest"
    }

    override fun covertToElement(config: CiBuildConfig): LinuxScriptElement {
        return LinuxScriptElement(
            name = displayName ?: "codecc",
            id = null,
            status = null,
            scriptType = BuildScriptType.SHELL,
            script = createScanScript(config),
            continueNoneZero = false
        )
    }

    private val scanTools = listOf("ccn", "dupc", "sensitive", "checkstyle", "cpplint", "detekt", "eslint", "goml", "occheck", "phpcs", "pylint", "styecop")

    private fun createScanScript(config: CiBuildConfig): String {
        val tools = inputs.tools.split(",").map { it.trim() }.filter { scanTools.contains(it) }
        if (tools.isEmpty()) {
            throw OperationException("工具不合法")
        }
        val toolsStr = tools.joinToString(",")
        val ruleSetCmd = if (inputs.rules.isNullOrBlank()) {
            " "
        } else {
            " -DRULE_SET_IDS=${inputs.rules!!.trim()} "
        }
        val skipPath = if (inputs.skipPath.isNullOrBlank()) {
            " "
        } else {
            " -DSKIP_PATHS=${inputs.skipPath!!.trim()} "
        }

        val path = if (inputs.path.isNullOrBlank()) {
            "\${WORKSPACE} "
        } else {
            "\${WORKSPACE}/${inputs.path!!.removePrefix("/")}"
        }
        return if (inputs.scanType == "all") { // 全量
            "echo \${WORKSPACE} > /tmp/scan_file_list.txt \r\n" +
            "docker run -t --rm -v /tmp:/tmp -v \${WORKSPACE}:\${WORKSPACE} ${config.codeCCSofwareClientImage} /bin/sh -c 'python /data/codecc_software/scan_local_prod/bin/build.py \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=\${WORKSPACE}' \r\n"
        } else {
            "echo $path > /tmp/scan_file_list.txt \r\n" +
            "docker run -t --rm -v /tmp:/tmp -v \${WORKSPACE}:\${WORKSPACE} ${config.codeCCSofwareClientImage} /bin/sh -c 'python /data/codecc_software/scan_local_prod/bin/build.py \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=\${WORKSPACE}' \r\n"
        }
    }
}

@ApiModel("CodeCC代码检查任务(客户端)")
open class CodeCCScanClientInput(
    @ApiModelProperty("扫描类型（all：全量, updated：增量）", required = false)
    open var scanType: String? = "all",
    @ApiModelProperty("工具包,多个之间逗号分隔：ccn,dupc,sensitive,checkstyle,cpplint,detekt,eslint,goml,occheck,phpcs,pylint,styecop", required = true)
    var tools: String,
    @ApiModelProperty("要扫描的代码路径，默认为整个workspace", required = false)
    var path: String?,
    @ApiModelProperty("规则集,逗号分隔", required = false)
    var rules: String?,
    @ApiModelProperty("排除的目录,逗号分隔", required = false)
    var skipPath: String?
) : AbstractInput()
