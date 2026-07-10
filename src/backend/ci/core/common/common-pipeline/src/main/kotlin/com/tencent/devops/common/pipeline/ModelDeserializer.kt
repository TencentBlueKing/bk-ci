package com.tencent.devops.common.pipeline

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.enums.PublicVarGroupReferenceTypeEnum
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.common.pipeline.service.ServiceModelHandleResource
import com.tencent.devops.common.service.utils.BkServiceUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory

/**
 * Model 对象的 JSON 反序列化器。
 *
 * 采用委托（Delegating）模式：对象本身的构建完全交给 Jackson 默认反序列化器，
 * 本类只在构建完成后做"公共变量组展开"的后处理。相比重新调用 [DeserializationContext.readValue]
 * 的实现方式，委托模式：
 * - 天然不产生递归，无需依赖 ThreadLocal 判定"内外部"；
 * - 对顶层 Model、嵌套 Model、集合元素等场景表现一致，不会被 Jackson 的反序列化器缓存旁路；
 * - 可正常参与缓存与上下文化（Contextual/Resolvable），性能更优。
 */
class ModelDeserializer(
    delegate: JsonDeserializer<*>
) : DelegatingDeserializer(delegate) {

    override fun newDelegatingInstance(newDelegatee: JsonDeserializer<*>): JsonDeserializer<*> =
        ModelDeserializer(newDelegatee)

    // 与被委托的默认反序列化器保持一致的缓存能力，避免每次重复构建
    override fun isCachable(): Boolean = _delegatee.isCachable

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Any? {
        val value = super.deserialize(p, ctxt)
        if (value is Model) {
            expandPublicVarsIfNeeded(value)
        }
        return value
    }

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext, intoValue: Any): Any {
        val value = super.deserialize(p, ctxt, intoValue)
        if (value is Model) {
            expandPublicVarsIfNeeded(value)
        }
        return value
    }

    /**
     * 按需展开公共变量组。以下情况直接跳过：
     * - 业务方主动关闭展开（[ModelPublicVarExpansion.withoutExpansion]）；
     * - 当前正处于展开处理过程中（防止处理逻辑内部再次反序列化 Model 时重复展开、重入调用）；
     * - Model 未引用公共变量组或缺少 projectId。
     *
     * 后处理失败仅降级为原始参数，绝不影响反序列化本身，保证历史逻辑不被破坏。
     */
    private fun expandPublicVarsIfNeeded(model: Model) {
        if (ModelPublicVarExpansion.isDisabled() || ModelPublicVarExpansion.isProcessing()) {
            return
        }
        if (model.publicVarGroups.isNullOrEmpty() || model.projectId.isNullOrBlank()) {
            return
        }
        ModelPublicVarExpansion.runProcessing {
            try {
                processTriggerContainers(model)
            } catch (ignored: Throwable) {
                logger.warn("Expand publicVarGroups failed, fallback to raw model params", ignored)
            }
        }
    }

    /**
     * 查找触发器容器并处理其公共变量参数。
     */
    private fun processTriggerContainers(model: Model) {
        val triggerContainer = model.stages.asSequence()
            .flatMap { it.containers.asSequence() }
            .filterIsInstance<TriggerContainer>()
            .firstOrNull() ?: return

        val serviceName = BkServiceUtil.findServiceName()
        val modelHandleService = if (serviceName in PROCESS_ENGINE_SERVICES) {
            try {
                SpringContextUtil.getBean(ModelHandleService::class.java)
            } catch (ignored: Throwable) {
                logger.warn("Get ModelHandleService bean failed, will fallback", ignored)
                null
            }
        } else {
            null
        }
        processSingleTriggerContainer(model, triggerContainer, modelHandleService)
    }

    private fun processSingleTriggerContainer(
        model: Model,
        triggerContainer: TriggerContainer,
        modelHandleService: ModelHandleService?
    ) {
        val projectId = model.projectId
        if (projectId.isNullOrBlank()) {
            return
        }
        val pipelineId = model.pipelineId
        val templateId = model.templateId
        val (referId, referType) = when {
            !pipelineId.isNullOrBlank() -> pipelineId to PublicVarGroupReferenceTypeEnum.PIPELINE
            !templateId.isNullOrBlank() -> templateId to PublicVarGroupReferenceTypeEnum.TEMPLATE
            else -> {
                logger.warn("No valid reference ID found for TriggerContainer, projectId=$projectId")
                return
            }
        }

        val context = ModelPublicVarHandleContext(
            referId = referId,
            referType = referType,
            referVersion = model.latestVersion,
            params = triggerContainer.params,
            publicVarGroups = model.publicVarGroups ?: emptyList()
        )
        triggerContainer.params = resolveParams(projectId, context, modelHandleService)
    }

    /**
     * 解析展开后的参数。本地存在 [ModelHandleService] 时走本地，否则回退到远程调用；
     * 任一路径失败均回退到原始参数，保证反序列化结果可用。
     */
    private fun resolveParams(
        projectId: String,
        context: ModelPublicVarHandleContext,
        modelHandleService: ModelHandleService?
    ): MutableList<BuildFormProperty> {
        return try {
            if (modelHandleService != null) {
                modelHandleService.handleModelParams(
                    projectId = projectId,
                    modelPublicVarHandleContext = context
                ).toMutableList()
            } else {
                val client = SpringContextUtil.getBean(Client::class.java)
                client.get(ServiceModelHandleResource::class).handlePipelineModelParams(
                    projectId = projectId,
                    modelPublicVarHandleContext = context
                ).data?.toMutableList() ?: context.params.toMutableList()
            }
        } catch (ignored: Throwable) {
            logger.warn("Handle publicVarGroups params failed, fallback to original params", ignored)
            context.params.toMutableList()
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ModelDeserializer::class.java)
        private val PROCESS_ENGINE_SERVICES = setOf("process", "engine")
    }
}
