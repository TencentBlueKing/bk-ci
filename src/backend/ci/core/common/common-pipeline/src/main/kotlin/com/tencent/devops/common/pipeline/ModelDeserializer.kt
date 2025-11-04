package com.tencent.devops.common.pipeline

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.service.utils.SpringContextUtil
import org.slf4j.LoggerFactory

/**
 * Model对象的JSON反序列化器
 * 负责将JSON数据反序列化为Model对象，并处理相关的触发器容器逻辑
 * 主要用于CI/CD流水线配置的反序列化处理
 */
class ModelDeserializer : JsonDeserializer<Model>() {
    companion object {
        private val logger = LoggerFactory.getLogger(ModelDeserializer::class.java)
    }

    /**
     * 反序列化JSON数据为Model对象
     * @param p JSON解析器，用于读取JSON输入流
     * @param ctxt 反序列化上下文，提供反序列化过程中的上下文信息
     * @return 反序列化后的Model对象
     * @throws IllegalStateException 当反序列化返回null时抛出
     * @throws Throwable 反序列化过程中可能出现的任何异常
     * 方法执行流程：
     * 1. 标记反序列化上下文，防止循环引用
     * 2. 转换为Model对象
     * 3. 处理模型中的触发器容器
     * 4. 清理标记并返回结果
     */
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): Model {
        // 标记反序列化上下文，用于标识当前正在反序列化过程中(防止在反序列化过程中出现循环引用问题)
        ModelDeserializeMarker.markInside()
        logger.info("Start deserializing Model (thread: ${Thread.currentThread().name})")
        return try {
            // 转换为具体的Model对象
            val model = ctxt.readValue(p, Model::class.java)
                ?: throw IllegalStateException("Model deserialization returned null")
            // 处理模型中的触发器容器，设置相关参数
            processTriggerContainers(model)
            // 返回反序列化完成的Model对象
            model
        } catch (ignored: Throwable) {
            // 反序列化失败时的错误处理
            logger.error("Failed to deserialize Model", ignored)
            throw ignored
        } finally {
            // 清理反序列化标记，确保标记状态正确重置
            ModelDeserializeMarker.clearMark()
            logger.info("Model deserialization completed (thread: ${Thread.currentThread().name})")
        }
    }

    /**
     * 处理模型中的所有触发器容器
     * @param model 需要处理的Model对象
     * 处理逻辑：
     * - 检查模型是否包含公共变量组和项目ID
     * - 遍历所有阶段(stages)中的容器(containers)
     * - 对每个TriggerContainer类型的容器进行处理
     */
    private fun processTriggerContainers(model: Model) {
        // 快速检查：如果没有公共变量组或项目ID，直接返回
        if (model.publicVarGroups.isNullOrEmpty() || model.projectId == null) {
            return
        }
        // 获取ModelHandleService服务实例，用于处理模型参数
        val modelHandleService = SpringContextUtil.getBean(ModelHandleService::class.java)
        // 遍历所有阶段和容器，查找并处理TriggerContainer
        model.stages.forEach { stage ->
            stage.containers.forEach { container ->
                if (container is TriggerContainer) {
                    processSingleTriggerContainer(
                        model = model,
                        modelHandleService = modelHandleService
                    )
                }
            }
        }
    }

    /**
     * 处理单个触发器容器
     * @param model 包含触发器容器的Model对象
     * @param modelHandleService 模型处理服务，用于处理模型参数
     * 处理逻辑：
     * - 根据pipelineId或templateId确定引用类型
     * - 调用ModelHandleService处理模型参数
     * - 记录警告信息当没有有效的引用ID时
     */
    private fun processSingleTriggerContainer(
        model: Model,
        modelHandleService: ModelHandleService
    ) {
        val projectId = model.projectId!!
        val pipelineId = model.pipelineId
        val templateId = model.templateId

        when {
            // 处理流水线引用类型的触发器容器
            !pipelineId.isNullOrBlank() -> {
                modelHandleService.handleModelParams(
                    projectId = projectId,
                    model = model,
                    referId = pipelineId,
                    referType = "PIPELINE",
                    referVersion = model.latestVersion
                )
            }
            // 处理模板引用类型的触发器容器
            !templateId.isNullOrBlank() -> {
                modelHandleService.handleModelParams(
                    projectId = projectId,
                    model = model,
                    referId = templateId,
                    referType = "TEMPLATE",
                    referVersion = model.latestVersion
                )
            }
            // 没有有效引用ID时的处理
            else -> {
                logger.warn("No valid reference ID found for TriggerContainer")
            }
        }
    }
}
