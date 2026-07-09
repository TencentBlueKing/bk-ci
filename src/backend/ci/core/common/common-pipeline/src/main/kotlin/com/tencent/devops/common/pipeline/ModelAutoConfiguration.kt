package com.tencent.devops.common.pipeline

import com.fasterxml.jackson.databind.module.SimpleModule
import com.tencent.devops.common.api.util.JsonUtil
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered

@Configuration
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
class ModelAutoConfiguration {

    companion object {
        private val logger = LoggerFactory.getLogger(ModelAutoConfiguration::class.java)

        // 定义模块唯一标识名称（常量）
        private const val PIPELINE_MODEL_MODULE_NAME = "pipeline-model-module"

        /**
         * 静态初始化块：在类加载时执行模块注册
         */
        init {
            logger.info("Registering pipeline-model-module to JsonUtil")
            val module = SimpleModule(PIPELINE_MODEL_MODULE_NAME).apply {
                setDeserializerModifier(ModelDeserializerModifier())
            }
            // 注册流水线模型模块到 JsonUtil 的 ObjectMapper
            JsonUtil.registerModule(module)
            logger.info("pipeline-model-module registered successfully")
        }
    }
}
