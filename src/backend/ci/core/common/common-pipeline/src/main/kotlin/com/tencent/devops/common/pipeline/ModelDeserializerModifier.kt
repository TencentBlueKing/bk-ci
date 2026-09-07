package com.tencent.devops.common.pipeline

import com.fasterxml.jackson.databind.BeanDescription
import com.fasterxml.jackson.databind.DeserializationConfig
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier

/**
 * 为 [Model] 的反序列化器包裹一层 [ModelDeserializer]（委托默认反序列化器 + 公共变量展开后处理）。
 * 其余类型保持默认反序列化器不变。
 */
class ModelDeserializerModifier : BeanDeserializerModifier() {

    override fun modifyDeserializer(
        config: DeserializationConfig,
        beanDesc: BeanDescription,
        deserializer: JsonDeserializer<*>
    ): JsonDeserializer<*> {
        if (beanDesc.beanClass != Model::class.java) {
            return deserializer
        }
        // 避免重复包裹（理论上不会发生，作为防御）
        if (deserializer is ModelDeserializer) {
            return deserializer
        }
        return ModelDeserializer(deserializer)
    }
}
