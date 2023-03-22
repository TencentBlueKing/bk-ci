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

package com.tencent.devops.process.service.pipelineExport

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.common.pipeline.container.NormalContainer
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.container.VMBuildContainer
import com.tencent.devops.common.pipeline.enums.DockerVersion
import com.tencent.devops.common.pipeline.type.StoreDispatchType
import com.tencent.devops.common.pipeline.type.docker.ImageType
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.process.constant.ProcessCode.BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE
import com.tencent.devops.process.constant.ProcessCode.BK_ENTER_URL_ADDRESS_IMAGE
import com.tencent.devops.process.constant.ProcessCode.BK_EXPORT_SYSTEM_CREDENTIALS
import com.tencent.devops.process.constant.ProcessCode.BK_EXPORT_TIME
import com.tencent.devops.process.constant.ProcessCode.BK_IDENTIFIED_SENSITIVE_INFORMATION
import com.tencent.devops.process.constant.ProcessCode.BK_NO_RIGHT_EXPORT_PIPELINE
import com.tencent.devops.process.constant.ProcessCode.BK_PARAMETERS_BE_EXPORTED
import com.tencent.devops.process.constant.ProcessCode.BK_PIPELINED_ID
import com.tencent.devops.process.constant.ProcessCode.BK_PIPELINE_NAME
import com.tencent.devops.process.constant.ProcessCode.BK_PROJECT_ID
import com.tencent.devops.process.constant.ProcessCode.BK_SENSITIVE_INFORMATION_IN_PARAMETERS
import com.tencent.devops.process.constant.ProcessCode.BK_STREAM_NOT_SUPPORT
import com.tencent.devops.process.constant.ProcessCode.BK_UNKNOWN_CONTEXT_EXISTS
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.engine.service.store.StoreImageHelper
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineExportContext
import com.tencent.devops.process.pojo.PipelineExportInfo
import com.tencent.devops.process.pojo.PipelineExportV2YamlConflictMapItem
import com.tencent.devops.process.pojo.PipelineExportV2YamlData
import com.tencent.devops.process.service.StageTagService
import com.tencent.devops.process.service.label.PipelineGroupService
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.process.yaml.v2.models.YAME_META_DATA_JSON_FILTER
import com.tencent.devops.process.yaml.v2.models.export.ExportPreScriptBuildYaml
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.time.LocalDateTime
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response
import javax.ws.rs.core.StreamingOutput

@Suppress("ALL")
@Service("TXPipelineExportService")
class TXPipelineExportService @Autowired constructor(
    private val stageTagService: StageTagService,
    private val pipelineGroupService: PipelineGroupService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService,
    private val storeImageHelper: StoreImageHelper,
    private val scmProxyService: ScmProxyService,
    private val client: Client
) {

    companion object {
        private val logger = LoggerFactory.getLogger(TXPipelineExportService::class.java)
        private val yamlObjectMapper = ObjectMapper(YAMLFactory().enable(YAMLGenerator.Feature.LITERAL_BLOCK_STYLE))
            .apply {
                registerKotlinModule().setFilterProvider(
                    SimpleFilterProvider().addFilter(
                        YAME_META_DATA_JSON_FILTER,
                        SimpleBeanPropertyFilter.serializeAllExcept(YAME_META_DATA_JSON_FILTER)
                    )
                )
            }
    }

    // 导出工蜂CI-2.0的yml
    fun exportV2Yaml(userId: String, projectId: String, pipelineId: String, isGitCI: Boolean = false): Response {
        val pair = generateV2Yaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isGitCI = isGitCI,
            exportFile = true
        )
        return exportToFile(pair.first, pair.second.name)
    }

    fun exportV2YamlStr(
        userId: String,
        projectId: String,
        pipelineId: String,
        isGitCI: Boolean = false
    ): PipelineExportV2YamlData {
        val pair = generateV2Yaml(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            isGitCI = isGitCI
        )
        return PipelineExportV2YamlData(pair.first, pair.third)
    }

    fun generateV2Yaml(
        userId: String,
        projectId: String,
        pipelineId: String,
        isGitCI: Boolean = false,
        exportFile: Boolean = false
    ): Triple<String, Model, Map<String, List<List<PipelineExportV2YamlConflictMapItem>>>> {
        pipelinePermissionService.validPipelinePermission(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            permission = AuthPermission.EDIT,
            message = MessageUtil.getMessageByLocale(
                messageCode = BK_NO_RIGHT_EXPORT_PIPELINE,
                language = I18nUtil.getLanguage(userId),
                params = arrayOf(userId, projectId)
            )
        )
        val pipelineInfo = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)
            ?: throw ErrorCodeException(
                statusCode = Response.Status.NOT_FOUND.statusCode,
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在",
                params = arrayOf(pipelineId)
            )

        val baseModel = pipelineRepositoryService.getModel(projectId, pipelineId) ?: throw ErrorCodeException(
            statusCode = Response.Status.BAD_REQUEST.statusCode,
            errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
            defaultMessage = "流水线已不存在，请检查"
        )

        val pipelineGroupsMap = mutableMapOf<String, String>()
        pipelineGroupService.getGroups(userId, pipelineInfo.projectId).forEach {
            it.labels.forEach { label ->
                pipelineGroupsMap[label.id] = label.name
            }
        }

        // 获取流水线labels
        val groups = pipelineGroupService.getGroups(
            userId = userId,
            projectId = pipelineInfo.projectId,
            pipelineId = pipelineInfo.pipelineId
        )
        val labels = mutableListOf<String>()
        groups.forEach {
            labels.addAll(it.labels)
        }

        return doParseModel(
            PipelineExportInfo(
                userId = userId,
                pipelineInfo = pipelineInfo,
                model = baseModel,
                stageTags = stageTagService.getAllStageTag().data,
                labels = labels.map { pipelineGroupsMap[it] ?: "" },
                isGitCI = isGitCI,
                exportFile = exportFile,
                getImageNameAndCredentials = this::getImageNameAndCredentials,
                getAtomRely = this::getAtomRely,
                getRepoInfo = this::getRepoInfo
            ),
            PipelineExportContext()
        )
    }

    fun getAtomRely(elementInfo: GetRelyAtom): Map<String, Map<String, Any>>? {
        return try {
            client.get(ServiceMarketAtomResource::class).getAtomRely(elementInfo).data
        } catch (e: Exception) {
            logger.error("get Atom Rely error.", e)
            null
        }
    }

    fun getRepoInfo(projectId: String, repositoryConfig: RepositoryConfig): Repository? {
        return kotlin.runCatching { scmProxyService.getRepo(projectId, repositoryConfig) }.getOrDefault(null)
    }

    fun doParseModel(
        allInfo: PipelineExportInfo,
        context: PipelineExportContext
    ): Triple<String, Model, Map<String, List<List<PipelineExportV2YamlConflictMapItem>>>> {
        // 将所有插件ID按编排顺序刷新
        var stepCount = 1
        allInfo.model.stages.forEach { s ->
            s.containers.forEach { c ->
                c.elements.forEach { e ->
                    e.id = "step_$stepCount"
                    stepCount++
                }
            }
        }

        // 过滤出enable == false 的stage/job/step
        val filterStage = allInfo.model.stages.filter { it.stageControlOption?.enable != false }
        val enableStages: MutableList<Stage> = mutableListOf()
        filterStage.forEach { stageIt ->
            val filterContainer = stageIt.containers.filter { fit ->
                when (fit) {
                    is NormalContainer -> {
                        fit.jobControlOption?.enable != false
                    }
                    is VMBuildContainer -> {
                        fit.jobControlOption?.enable != false
                    }
                    is TriggerContainer -> true
                    else -> true
                }
            }
            filterContainer.forEach { elementIt ->
                elementIt.elements = elementIt.elements.filter { fit ->
                    fit.additionalOptions?.enable != false
                }
            }
            enableStages.add(stageIt.copy(containers = filterContainer))
        }
        val model = allInfo.model.copy(stages = enableStages)
        context.yamlSb = getYamlStringBuilder(
            projectId = allInfo.pipelineInfo.projectId,
            pipelineId = allInfo.pipelineInfo.pipelineId,
            model = model,
            isGitCI = allInfo.isGitCI
        )

        model.labels = allInfo.labels

        context.output2Elements = mutableMapOf()
        context.outputConflictMap = mutableMapOf()
        context.variables = getVariableFromModel(model) ?: emptyMap()
        val yamlObj = try {
            ExportPreScriptBuildYaml(
                version = "v2.0",
                name = model.name,
                label = model.labels,
                triggerOn = null,
                variables = context.variables,
                stages = ExportStage.getV2StageFromModel(
                    allInfo = allInfo,
                    context = context
                ),
                extends = null,
                resources = null,
                notices = null,
                finally = ExportStage.getV2FinalFromStage(
                    allInfo = allInfo,
                    context = context,
                    stage = model.stages.last()
                )
            )
        } catch (t: Throwable) {
            logger.error("Export v2 yaml with error, return blank yml", t)
            if (t is ErrorCodeException) throw t
            ExportPreScriptBuildYaml(
                version = "v2.0",
                name = model.name,
                label = model.labels.ifEmpty { null },
                triggerOn = null,
                variables = null,
                stages = null,
                extends = null,
                resources = null,
                notices = null,
                finally = null
            )
        }
        val modelYaml = toYamlStr(yamlObj)
        context.yamlSb.append(modelYaml)
        return Triple(context.yamlSb.toString(), model, context.outputConflictMap)
    }

    private fun getYamlStringBuilder(
        projectId: String,
        pipelineId: String,
        model: Model,
        isGitCI: Boolean
    ): StringBuilder {

        val yamlSb = StringBuilder()
        yamlSb.append(
            "############################################################################" +
                "#########################################\n"
        )
        yamlSb.append(
            MessageUtil.getMessageByLocale(
            messageCode = BK_PROJECT_ID,
            language = I18nUtil.getLanguage()
        ) + " $projectId \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
            messageCode = BK_PIPELINED_ID,
            language = I18nUtil.getLanguage()
        ) + " $pipelineId \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PIPELINE_NAME,
                language = I18nUtil.getLanguage()
            ) + " ${model.name} \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_EXPORT_TIME,
                language = I18nUtil.getLanguage()
            ) + " ${DateTimeUtil.toDateTime(LocalDateTime.now())} \n")
        yamlSb.append("# \n")
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_EXPORT_SYSTEM_CREDENTIALS,
                language = I18nUtil.getLanguage()
            )
        )
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_SENSITIVE_INFORMATION_IN_PARAMETERS,
                language = I18nUtil.getLanguage()
            )
        )
        if (isGitCI) {
            yamlSb.append(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_STREAM_NOT_SUPPORT,
                    language = I18nUtil.getLanguage()
                )
            )
        }
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_PARAMETERS_BE_EXPORTED,
                language = I18nUtil.getLanguage()
            )
        )
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_IDENTIFIED_SENSITIVE_INFORMATION,
                language = I18nUtil.getLanguage()
            )
        )
        yamlSb.append(
            MessageUtil.getMessageByLocale(
                messageCode = BK_UNKNOWN_CONTEXT_EXISTS,
                language = I18nUtil.getLanguage()
            )
        )
        yamlSb.append(
            "########################################################" +
                "#############################################################\n\n"
        )
        return yamlSb
    }

    private fun exportToFile(yaml: String, pipelineName: String): Response {
        // 流式下载
        val fileStream = StreamingOutput { output ->
            val sb = StringBuilder()
            sb.append(yaml)
            output.write(sb.toString().toByteArray())
            output.flush()
        }
        val fileName = URLEncoder.encode("$pipelineName.yml", "UTF-8")
        return Response
            .ok(fileStream, MediaType.APPLICATION_OCTET_STREAM_TYPE)
            .header("content-disposition", "attachment; filename = $fileName")
            .header("Cache-Control", "no-cache")
            .build()
    }

    private fun getVariableFromModel(model: Model): Map<String, String>? {
        val result = mutableMapOf<String, String>()
        (model.stages[0].containers[0] as TriggerContainer).params.forEach {
            result[it.id] = it.defaultValue.toString()
        }
        return if (result.isEmpty()) {
            null
        } else {
            result
        }
    }

    private fun toYamlStr(bean: Any?): String {
        return bean?.let {
            yamlObjectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
                .writeValueAsString(it)!!
        } ?: ""
    }

    fun getImageNameAndCredentials(
        userId: String,
        projectId: String,
        pipelineId: String,
        dispatchType: StoreDispatchType
    ): Pair<String, String?> {
        try {
            when (dispatchType.imageType) {
                ImageType.BKSTORE -> {
                    val imageRepoInfo = storeImageHelper.getImageRepoInfo(
                        userId = userId,
                        projectId = projectId,
                        pipelineId = pipelineId,
                        buildId = "",
                        imageCode = dispatchType.imageCode,
                        imageVersion = dispatchType.imageVersion,
                        defaultPrefix = null
                    )
                    val completeImageName = if (ImageType.BKDEVOPS == imageRepoInfo.sourceType) {
                        // 蓝盾项目源镜像
                        "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                    } else {
                        // 第三方源镜像
                        // dockerhub镜像名称不带斜杠前缀
                        if (imageRepoInfo.repoUrl.isBlank()) {
                            imageRepoInfo.repoName
                        } else {
                            "${imageRepoInfo.repoUrl}/${imageRepoInfo.repoName}"
                        }
                    } + ":" + imageRepoInfo.repoTag
                    return if (imageRepoInfo.publicFlag) {
                        Pair(completeImageName, null)
                    } else Pair(
                        completeImageName, imageRepoInfo.ticketId
                    )
                }
                ImageType.BKDEVOPS -> {
                    // 针对非商店的旧数据处理
                    return if (dispatchType.value != DockerVersion.TLINUX1_2.value &&
                        dispatchType.value != DockerVersion.TLINUX2_2.value
                    ) {
                        dispatchType.dockerBuildVersion = "bkdevops/" + dispatchType.value
                        Pair("bkdevops/" + dispatchType.value, null)
                    } else {
                        Pair(
                            MessageUtil.getMessageByLocale(
                                messageCode = BK_AUTOMATIC_EXPORT_NOT_SUPPORTED_IMAGE,
                                language = I18nUtil.getLanguage(userId)
                            ), null)
                    }
                }
                else -> {
                    return if (dispatchType.credentialId.isNullOrBlank()) {
                        Pair(dispatchType.value, null)
                    } else Pair(
                        dispatchType.value, dispatchType.credentialId
                    )
                }
            }
        } catch (e: Exception) {
            return Pair(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_ENTER_URL_ADDRESS_IMAGE,
                    language = I18nUtil.getLanguage(userId)
                ), null)
        }
    }
}
