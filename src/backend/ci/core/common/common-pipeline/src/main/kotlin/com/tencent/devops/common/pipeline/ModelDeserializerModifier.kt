package com.tencent.devops.common.pipeline

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier
import org.slf4j.LoggerFactory

class ModelDeserializerModifier : BeanDeserializerModifier() {
    private val logger = LoggerFactory.getLogger(ModelDeserializerModifier::class.java)

    /**
     * 修改反序列化器的主要入口方法
     *
     * @param config Jackson 反序列化配置
     * @param beanDesc Bean 描述信息
     * @param deserializer 默认的反序列化器
     * @return 调整后的反序列化器
     */
    override fun modifyDeserializer(
        config: DeserializationConfig, beanDesc: BeanDescription, deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        // 如果不是 Model 类，立即返回默认反序列化器
        if (beanDesc.beanClass != Model::class.java) {
            return deserializer
        }
        return if (ModelDeserializeMarker.isInside()) {
            // 打印默认反序列化器类型，方便排查
            logger.info("Detect inside ModelDeserializer,use default deserializer:${deserializer::class.java.name}")
            deserializer
        } else {
            logger.info("Registering ModelDeserializer for Model class (external call)")
            ModelDeserializer()
        }
    }
}
