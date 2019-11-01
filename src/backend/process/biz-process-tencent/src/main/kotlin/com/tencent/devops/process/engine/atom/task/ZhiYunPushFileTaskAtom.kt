package com.tencent.devops.process.engine.atom.task

import com.tencent.devops.common.api.constant.PIPELINE_MATERIAL_URL
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.archive.pojo.ArtifactorySearchParam
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.element.ZhiyunPushFileElement
import com.tencent.devops.common.pipeline.enums.BuildStatus
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.plugin.api.ServiceZhiyunResource
import com.tencent.devops.plugin.pojo.zhiyun.ZhiyunUploadParam
import com.tencent.devops.process.engine.atom.AtomResponse
import com.tencent.devops.process.engine.atom.IAtomTask
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import org.apache.poi.util.StringUtil
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.config.ConfigurableBeanFactory
import org.springframework.context.annotation.Scope
import org.springframework.stereotype.Component
import java.net.URLEncoder

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
class ZhiYunPushFileTaskAtom @Autowired constructor(
    private val client: Client,
    private val rabbitTemplate: RabbitTemplate
) : IAtomTask<ZhiyunPushFileElement> {
    override fun getParamElement(task: PipelineBuildTask): ZhiyunPushFileElement {
        return JsonUtil.mapTo(task.taskParams, ZhiyunPushFileElement::class.java)
    }

    override fun execute(task: PipelineBuildTask, param: ZhiyunPushFileElement, runVariables: Map<String, String>): AtomResponse {
        val product = parseVariable(param.product, runVariables)
        val packageName = parseVariable(param.packageName, runVariables)
        val description = parseVariable(param.description, runVariables)
        val clean = param.clean
        val fileSource = parseVariable(param.fileSource, runVariables)
        val filePath = parseVariable(param.filePath, runVariables)

        val projectId = task.projectId
        val buildId = task.buildId
        val pipelineId = task.pipelineId
        val userId = task.starter
        val codeRepoUrl = getCodeRepoUrl(runVariables)
        logger.info("codeRepoUrl is $codeRepoUrl")
        val uploadParams = ZhiyunUploadParam(
                userId,
                ZhiyunUploadParam.CommonParam(
                        product,
                        packageName,
                        userId,
                        description,
                        clean.toString(),
                        buildId,
                        codeRepoUrl
                ),
                ArtifactorySearchParam(
                        projectId,
                        pipelineId,
                        buildId,
                        filePath,
                        fileSource == "CUSTOMIZE",
                        task.executeCount ?: 1,
                        task.taskId
                )
        )

        LogUtils.addLine(rabbitTemplate, buildId, "开始上传对应文件到织云...【<a target='_blank' href='http://ccc.oa.com/package/versions?innerurl=${URLEncoder.encode("http://yun.ccc.oa.com/index.php/package/versions?product=$product&package=$packageName","UTF-8")}'>查看详情</a>】", task.taskId, task.executeCount ?: 1)
        LogUtils.addLine(rabbitTemplate, buildId, "匹配文件中: ${uploadParams.fileParams.regexPath}($fileSource)", task.taskId, task.executeCount ?: 1)
        val versions = client.getWithoutRetry(ServiceZhiyunResource::class).pushFile(uploadParams).data ?: throw RuntimeException("0 file send to zhiyun")
        val ver = versions.lastOrNull() ?: throw RuntimeException("0 file send to zhiyun")
        LogUtils.addLine(rabbitTemplate, buildId, "上传对应文件到织云成功!", task.taskId, task.executeCount ?: 1)
        return AtomResponse(BuildStatus.SUCCEED, mapOf("bk_zhiyun_version_$packageName" to ver))
    }

    private fun getCodeRepoUrl(runVariables: Map<String, String>): String? {
        val codeRepoUrlMap = runVariables.filter { it.key.startsWith(PIPELINE_MATERIAL_URL) }.toMap()
        if (codeRepoUrlMap.isEmpty()) {
            logger.info("No code repo url in variables")
            return ""
        }
        return StringUtil.join(codeRepoUrlMap.values.toTypedArray(), ";")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ZhiYunPushFileTaskAtom::class.java)
    }
}
