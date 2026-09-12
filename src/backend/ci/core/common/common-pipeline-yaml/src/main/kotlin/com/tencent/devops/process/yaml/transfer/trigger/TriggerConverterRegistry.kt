package com.tencent.devops.process.yaml.transfer.trigger

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.yaml.v3.models.TriggerType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 触发器转换器注册中心。
 *
 * Spring 自动收集所有 [TriggerConverter] Bean，按触发器类型与 Element 双向索引，
 * 供 YAML <-> Model 转换时"注册表优先，存量回落"路由使用。
 */
@Service
class TriggerConverterRegistry @Autowired(required = false) constructor(
    converters: List<TriggerConverter> = emptyList()
) {
    private val byType: Map<TriggerType, TriggerConverter> = converters.associateBy { it.triggerType }

    /**
     * 按触发器类型查找转换器。
     */
    fun byType(type: TriggerType): TriggerConverter? = byType[type]

    /**
     * 按 Element 查找归属的转换器（Model -> YAML 归组）。
     */
    fun byElement(element: Element): TriggerConverter? =
        byType.values.firstOrNull { it.support(element) }

    /**
     * 当前已注册的全部触发器类型（顺序稳定，按 [TriggerType] 声明序）。
     */
    fun supportedTypes(): List<TriggerType> = TriggerType.values().filter { byType.containsKey(it) }
}
