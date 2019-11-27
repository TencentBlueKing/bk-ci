package com.tencent.devops.common.ci.task

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.enums.BuildScriptType
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxScriptElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(客户端)")
open class CodeCCScanClientTask(
    @ApiModelProperty("id", required = false)
    override var displayName: String?,
    @ApiModelProperty("入参", required = true)
    override val inputs: CodeCCScanClientInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    override fun getTaskVersion() = taskVersion
    override fun getTaskType() = taskType

    companion object {
        const val taskType = "codeCCScanClient"
        const val taskVersion = "@latest"
    }

    override fun covertToElement(config: CiBuildConfig): LinuxScriptElement {
        return LinuxScriptElement(
                displayName ?: "codecc",
                null,
                null,
                BuildScriptType.SHELL,
                createScanScript(config),
                false
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
            "echo /data/landun/workspace > /tmp/scan_file_list.txt \r\n" +
            "docker run -it -v /tmp/scan_file_list.txt:/tmp/scan_file_list.txt -v \${WORKSPACE}:/data/landun/workspace ${config.codeCCSofwareClientImage} /bin/sh -c 'python /data/codecc_software/scan_local_prod/bin/build.py \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=/data/landun/workspace' \r\n"
        } else {
            "echo /data/landun/workspace > /tmp/scan_file_list.txt \r\n" +
            "docker run -it -v /tmp/scan_file_list.txt:/tmp/scan_file_list.txt -v \${WORKSPACE}:/data/landun/workspace ${config.codeCCSofwareClientImage} /bin/sh -c 'python /data/codecc_software/scan_local_prod/bin/build.py \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=/data/landun/workspace' \r\n"
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