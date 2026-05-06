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

package com.tencent.devops.ai.agent.template

import com.fasterxml.jackson.core.type.TypeReference
import com.tencent.devops.ai.agent.BaseTools
import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.pipeline.pojo.TemplateInstanceCreateRequest
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.api.service.ServicePipelineVersionResource
import com.tencent.devops.process.api.template.v2.ServicePipelineTemplateV2Resource
import com.tencent.devops.process.pojo.template.TemplateInstanceCreate
import com.tencent.devops.process.pojo.template.TemplateInstanceUpdate
import io.agentscope.core.tool.Tool
import io.agentscope.core.tool.ToolParam
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.function.Supplier

/**
 * 流水线模板管理工具集，提供模板查询、实例管理、模板删除等能力。
 */
@Suppress("TooManyFunctions")
class TemplateTools(
    client: Client,
    userIdSupplier: Supplier<String>
) : BaseTools(client, userIdSupplier) {

    override val logger: Logger = LoggerFactory.getLogger(TemplateTools::class.java)

    private fun templateV2Resource() = service(ServicePipelineTemplateV2Resource::class)
    private fun pipelineResource() = service(ServicePipelineResource::class)
    private fun buildResource() = service(ServiceBuildResource::class)
    private fun versionResource() = service(ServicePipelineVersionResource::class)

    // ── 模板查询 ──
    @Tool(
        name = "获取模板列表",
        description = "获取项目下的模板管理列表，返回模板名称、ID、类型、版本、是否有待升级实例等。" +
            "支持分页查询。"
    )
    fun listTemplates(
        @ToolParam(name = "projectId", description = "项目ID（英文标识）")
        projectId: String,
        @ToolParam(name = "page", description = "页码，默认1", required = false)
        page: Int? = null,
        @ToolParam(name = "pageSize", description = "每页条数，默认10", required = false)
        pageSize: Int? = null
    ): String {
        return safeQuery("TemplateTool", "listTemplates") {
            val actualPageSize = (pageSize ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
            val result = templateV2Resource().listTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateType = null,
                storeFlag = null,
                orderBy = null,
                sort = null,
                page = page ?: 1,
                pageSize = actualPageSize
            )
            val data = result.data ?: return@safeQuery "获取模板列表失败"
            toJson(data)
        }
    }

    @Tool(
        name = "获取模板详情",
        description = "获取模板的详细信息，包括版本列表、参数列表（BuildFormProperty）、编排信息（Model）。" +
            "可指定版本号或版本名称查询特定版本，不传则返回最新版本。"
    )
    fun getTemplateDetail(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "version", description = "模板版本号（数字，可选）", required = false)
        version: Long? = null,
        @ToolParam(name = "versionName", description = "模板版本名称（如 v1.0，可选）", required = false)
        versionName: String? = null
    ): String {
        return safeQuery("TemplateTool", "getTemplateDetail") {
            val result = templateV2Resource().getTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                version = version,
                versionName = versionName
            )
            val detail = result.data ?: return@safeQuery "未找到模板 $templateId"
            toJson(detail)
        }
    }

    @Tool(
        name = "获取所有可用模板",
        description = "获取项目下所有可用模板列表，包括自定义模板和商店模板。" +
            "适用于创建流水线时选择模板的场景。"
    )
    fun listAllTemplates(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String
    ): String {
        return safeQuery("TemplateTool", "listAllTemplates") {
            val result = templateV2Resource().listAllTemplate(
                userId = getOperatorUserId(),
                projectId = projectId
            )
            val data = result.data ?: return@safeQuery "获取可用模板列表失败"
            toJson(data)
        }
    }

    // ── 实例管理 ──

    @Tool(
        name = "查看模板实例列表",
        description = "查询模板关联的流水线实例列表，返回流水线名称、ID、版本、更新状态等。" +
            "支持分页和按名称搜索。"
    )
    fun listTemplateInstances(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "page", description = "页码（可选）", required = false)
        page: Int? = null,
        @ToolParam(name = "pageSize", description = "每页条数（可选）", required = false)
        pageSize: Int? = null,
        @ToolParam(name = "searchKey", description = "按流水线名称搜索（可选）", required = false)
        searchKey: String? = null
    ): String {
        return safeQuery("TemplateTool", "listTemplateInstances") {
            val result = templateV2Resource().listTemplateInstances(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                page = page,
                pageSize = pageSize,
                searchKey = searchKey,
                sortType = null,
                desc = null
            )
            val data = result.data ?: return@safeQuery "获取模板实例列表失败"
            toJson(data)
        }
    }

    @Tool(
        name = "批量实例化模板",
        description = "从模板批量创建流水线实例。这是写操作，执行前必须向用户确认。" +
            "instancesJson 为 JSON 数组字符串，每个元素包含 pipelineName（流水线名称，必填）、" +
            "buildNo（构建号，可选，传 null）、param（变量列表，可选，传 null 使用模板默认值）。" +
            "示例：[{\"pipelineName\":\"业务A\",\"buildNo\":null,\"param\":null}]"
    )
    fun createTemplateInstances(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "version", description = "模板版本号")
        version: Long,
        @ToolParam(
            name = "instancesJson",
            description = "实例列表JSON数组，每个元素含 pipelineName/buildNo/param 字段"
        )
        instancesJson: String
    ): String {
        return safeOperate("TemplateTool", "createTemplateInstances", mapOf(
            "projectId" to projectId,
            "templateId" to templateId,
            "version" to version,
            "instancesJson" to instancesJson
        )) {
            val instances: List<TemplateInstanceCreate> = try {
                JsonUtil.to(instancesJson, object : TypeReference<List<TemplateInstanceCreate>>() {})
            } catch (e: Exception) {
                return@safeOperate "实例参数格式错误，请使用 JSON 数组格式: ${e.message}"
            }
            if (instances.isEmpty()) {
                return@safeOperate "实例列表不能为空"
            }
            val result = templateV2Resource().createTemplateInstances(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                version = version,
                useTemplateSettings = true,
                instances = instances
            )
            val data = result.data ?: return@safeOperate "批量实例化失败: ${result.message}"
            toJson(data)
        }
    }

    @Tool(
        name = "批量升级模板实例",
        description = "将模板关联的流水线实例批量升级到指定版本。这是写操作，执行前必须向用户确认。" +
            "instancesJson 为 JSON 数组字符串，每个元素必须包含 pipelineId（流水线ID，必填）、" +
            "pipelineName（流水线名称，必填）、buildNo（可选）、param（可选）。" +
            "示例：[{\"pipelineId\":\"p-xxx\",\"pipelineName\":\"业务A\",\"buildNo\":null,\"param\":null}]"
    )
    fun updateTemplateInstances(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "version", description = "目标模板版本号")
        version: Long,
        @ToolParam(
            name = "instancesJson",
            description = "实例列表JSON数组，每个元素含 pipelineId/pipelineName/buildNo/param 字段"
        )
        instancesJson: String
    ): String {
        return safeOperate("TemplateTool", "updateTemplateInstances", mapOf(
            "projectId" to projectId,
            "templateId" to templateId,
            "version" to version,
            "instancesJson" to instancesJson
        )) {
            val instances: List<TemplateInstanceUpdate> = try {
                JsonUtil.to(instancesJson, object : TypeReference<List<TemplateInstanceUpdate>>() {})
            } catch (e: Exception) {
                return@safeOperate "实例参数格式错误，请使用 JSON 数组格式: ${e.message}"
            }
            if (instances.isEmpty()) {
                return@safeOperate "实例列表不能为空"
            }
            val result = templateV2Resource().updateTemplateInstances(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                version = version,
                useTemplateSettings = true,
                instances = instances
            )
            val data = result.data ?: return@safeOperate "批量升级失败: ${result.message}"
            toJson(data)
        }
    }

    // ── 模板删除 ──

    @Tool(
        name = "删除模板",
        description = "删除整个模板。这是写操作，执行前必须向用户确认。" +
            "删除前建议先查看模板实例列表，确认没有关联的流水线实例。"
    )
    fun deleteTemplate(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String
    ): String {
        return safeOperate("TemplateTool", "deleteTemplate", mapOf(
            "projectId" to projectId,
            "templateId" to templateId
        )) {
            val result = templateV2Resource().deleteTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId
            )
            if (result.data == true) {
                "模板 $templateId 已删除"
            } else {
                "删除模板失败: ${result.message}"
            }
        }
    }

    @Tool(
        name = "删除模板版本",
        description = "删除模板的指定版本。这是写操作，执行前必须向用户确认。" +
            "注意：如果该版本是模板的唯一版本，删除后模板将无可用版本。"
    )
    fun deleteTemplateVersion(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "version", description = "要删除的版本号")
        version: Long
    ): String {
        return safeOperate("TemplateTool", "deleteTemplateVersion", mapOf(
            "projectId" to projectId,
            "templateId" to templateId,
            "version" to version
        )) {
            val result = templateV2Resource().deleteTemplateVersion(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                version = version
            )
            if (result.data == true) {
                "模板 $templateId 的版本 $version 已删除"
            } else {
                "删除模板版本失败: ${result.message}"
            }
        }
    }

    // ── 模板创建与更新 ──

    @Tool(
        name = "基于模板创建新模板",
        description = "基于已有模板创建（复制）一个新模板。这是写操作，执行前必须向用户确认。" +
            "会获取源模板最新版本的编排(Model)，然后以此创建新模板。" +
            "新模板创建后是独立的，和源模板无关联。"
    )
    fun createTemplateByCopy(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "sourceTemplateId", description = "源模板ID，将基于此模板复制")
        sourceTemplateId: String,
        @ToolParam(name = "newTemplateName", description = "新模板的名称")
        newTemplateName: String
    ): String {
        return safeOperate("TemplateTool", "createTemplateByCopy", mapOf(
            "projectId" to projectId,
            "sourceTemplateId" to sourceTemplateId,
            "newTemplateName" to newTemplateName
        )) {
            // 1. 获取源模板的编排 Model
            val detailResult = templateV2Resource().getTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = sourceTemplateId,
                version = null,
                versionName = null
            )
            val detail = detailResult.data ?: return@safeOperate "未找到源模板 $sourceTemplateId"
            val model = detail.template

            // 2. 修改 Model 名称为新模板名
            model.name = newTemplateName

            // 3. 创建新模板
            val createResult = templateV2Resource().createTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                template = model
            )
            val newTemplateId = createResult.data
                ?: return@safeOperate "创建模板失败: ${createResult.message}"
            "模板创建成功，新模板ID: $newTemplateId，名称: $newTemplateName"
        }
    }

    @Tool(
        name = "更新模板",
        description = "更新已有模板的编排内容，会创建一个新版本。这是写操作，执行前必须向用户确认。" +
            "modelJson 为模板编排的 JSON 字符串（Model 格式）。" +
            "通常的做法是先通过「获取模板详情」拿到当前编排，修改后传入。"
    )
    fun updateTemplate(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "versionName", description = "新版本名称（如 v2.0）")
        versionName: String,
        @ToolParam(name = "modelJson", description = "模板编排的 JSON 字符串（Model 格式）")
        modelJson: String
    ): String {
        return safeOperate("TemplateTool", "updateTemplate", mapOf(
            "projectId" to projectId,
            "templateId" to templateId,
            "versionName" to versionName,
            "modelJson" to modelJson
        )) {
            val model = try {
                JsonUtil.to(modelJson, com.tencent.devops.common.pipeline.Model::class.java)
            } catch (e: Exception) {
                return@safeOperate "模板编排JSON格式错误: ${e.message}"
            }
            val result = templateV2Resource().updateTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                templateId = templateId,
                versionName = versionName,
                template = model
            )
            val newVersion = result.data
                ?: return@safeOperate "更新模板失败: ${result.message}"
            "模板 $templateId 已更新，新版本号: $newVersion，版本名称: $versionName"
        }
    }

    // ── 从模板创建流水线 ──

    @Tool(
        name = "从模板创建独立流水线",
        description = "从指定模板创建一条独立流水线（非实例化，创建后与模板无关联）。" +
            "这是写操作，执行前必须向用户确认。"
    )
    fun createPipelineFromTemplate(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "templateId", description = "模板ID")
        templateId: String,
        @ToolParam(name = "pipelineName", description = "新流水线名称")
        pipelineName: String,
        @ToolParam(
            name = "templateVersion",
            description = "模板版本号（数字），不传则使用最新版本",
            required = false
        )
        templateVersion: Long? = null
    ): String {
        return safeOperate("TemplateTool", "createPipelineFromTemplate", mapOf(
            "projectId" to projectId,
            "templateId" to templateId,
            "pipelineName" to pipelineName,
            "templateVersion" to templateVersion
        )) {
            val request = TemplateInstanceCreateRequest(
                templateId = templateId,
                templateVersion = templateVersion,
                pipelineName = pipelineName,
                useSubscriptionSettings = true,
                useLabelSettings = true,
                useConcurrencyGroup = true
            )
            val result = versionResource().createPipelineFromTemplate(
                userId = getOperatorUserId(),
                projectId = projectId,
                request = request
            )
            val data = result.data
                ?: return@safeOperate "从模板创建流水线失败: ${result.message}"
            toJson(data)
        }
    }

    // ── 流水线查询（辅助工具） ──

    @Tool(
        name = "获取流水线编排",
        description = "获取指定流水线的完整编排信息（Model），包括阶段、任务、参数等。" +
            "可用于查看流水线的参数配置，在实例化模板时作为参考。" +
            "返回内容较大时会自动截断。"
    )
    fun getPipelineModel(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String
    ): String {
        return safeQuery("TemplateTool", "getPipelineModel") {
            val result = pipelineResource().get(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS
            )
            val model = result.data ?: return@safeQuery "未找到流水线 $pipelineId"
            toJson(model)
        }
    }

    @Tool(
        name = "获取流水线启动参数",
        description = "获取指定流水线的手动启动参数信息，包括参数名称、类型、默认值、" +
            "可选值列表等。适用于查看其他流水线的参数配置作为参考，" +
            "帮助用户决定模板实例化时如何填写参数。"
    )
    fun getPipelineStartupInfo(
        @ToolParam(name = "projectId", description = "项目ID")
        projectId: String,
        @ToolParam(name = "pipelineId", description = "流水线ID")
        pipelineId: String
    ): String {
        return safeQuery("TemplateTool", "getPipelineStartupInfo") {
            val result = buildResource().manualStartupInfo(
                userId = getOperatorUserId(),
                projectId = projectId,
                pipelineId = pipelineId,
                channelCode = ChannelCode.BS
            )
            val info = result.data ?: return@safeQuery "未找到流水线 $pipelineId 的启动参数"
            toJson(info)
        }
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 10
        private const val MAX_PAGE_SIZE = 50
    }
}
