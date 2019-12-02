package com.tencent.devops.common.ci.task

import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.ci.CiBuildConfig
import com.tencent.devops.common.pipeline.pojo.element.market.MarketBuildAtomElement
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("CodeCC代码检查任务(客户端，通过容器方式)")
open class CodeCCScanInContainerTask(
    @ApiModelProperty("id", required = false)
    override var displayName: String,
    @ApiModelProperty("入参", required = true)
    override val inputs: CodeCCScanInContainerInput,
    @ApiModelProperty("执行条件", required = true)
    override val condition: String?
) : AbstractTask(displayName, inputs, condition) {
    override fun getTaskVersion() = taskVersion
    override fun getTaskType() = taskType

    companion object {
        const val taskType = "codeCCScanInDocker"
        const val taskVersion = "@latest"
        const val atomCode = "DockerRun"
    }

    override fun covertToElement(config: CiBuildConfig): MarketBuildAtomElement {
        val dockerRunInput = DockerRunInputParam(
                imageName = "", // TODO codecc_image
                ticketId = "",
                commandLine = createScanScript(config),
                env = emptyMap()
        )

        return MarketBuildAtomElement(
                "CodeCCScan",
                null,
                null,
                atomCode,
                "1.*",
                mapOf("input" to dockerRunInput)
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

        return if (inputs.scanType == 0) { // 全量
            val path = if (inputs.path == null) {
                "\${WORKSPACE} "
            } else {
                "\${WORKSPACE}/${inputs.path}"
            }
            "cd \${WORKSPACE} \r\n" +
                    "if [ -d \"/data/codecc_software\" ];then echo mount codecc software success ; else ln -s /tools/codecc_software/ /data/codecc_software;  fi \r\n" +
                    "export PATH=/data/codecc_software/python3.5/bin/:\$PATH \r\n" +
                    "echo $path > /tmp/scan_file_list.txt \r\n" +
                    "python ${config.codeCCSofwarePath} \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=\${WORKSPACE} \r\n"
        } else {
            "cd \${WORKSPACE} \r\n" +
                    "if [ -d \"/data/codecc_software\" ];then echo mount codecc software success ; else ln -s /tools/codecc_software/ /data/codecc_software;  fi \r\n" +
                    "export PATH=/data/codecc_software/python3.5/bin/:\$PATH \r\n" +
                    "if [ -f \"scan_file_list.txt\" ];then mv scan_file_list.txt /tmp/ ; else echo '\${WORKSPACE}' > /tmp/scan_file_list.txt ; fi \r\n" +
                    "python ${config.codeCCSofwarePath} \${pipeline.name} -DSCAN_TOOLS=$toolsStr -DSCAN_LIST_FILE=/tmp/scan_file_list.txt $ruleSetCmd $skipPath -DWORKSPACE_PATH=\${WORKSPACE} \r\n"
        }
    }
}

data class DockerRunInputParam(
    val imageName: String,
    val ticketId: String,
    val commandLine: String,
    val env: Map<String, String>
) : AbstractInput()

@ApiModel("CodeCC代码检查任务(客户端, 通过容器方式)")
open class CodeCCScanInContainerInput(
    @ApiModelProperty("扫描类型（0：全量, 1：增量）", required = false)
    open var scanType: Int? = 0,
    @ApiModelProperty("工具包,多个之间逗号分隔：ccn,dupc,sensitive,checkstyle,cpplint,detekt,eslint,goml,occheck,phpcs,pylint,styecop", required = true)
    var tools: String,
    @ApiModelProperty("要扫描的代码路径，默认为整个workspace", required = false)
    var path: String?,
    @ApiModelProperty("规则集,分隔", required = false)
    var rules: String?,
    @ApiModelProperty("排除的目录,分隔", required = false)
    var skipPath: String?
) : AbstractInput()