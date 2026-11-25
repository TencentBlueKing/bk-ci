package com.tencent.devops.process.yaml.transfer.trigger

import com.tencent.devops.common.pipeline.pojo.element.Element
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 触发器转换器注册中心。
 *
 * Spring 自动收集所有 [TriggerConverter] Bean，按触发器类型名与 Element 双向索引，
 * 供 YAML <-> Model 转换时"注册表优先，存量回落"路由使用。
 */
@Service
class TriggerConverterRegistry @Autowired(required = false) constructor(
    converters: List<TriggerConverter> = emptyList()
) {
    // LinkedHashMap 保持注入顺序，保证 Model -> YAML 输出顺序稳定
    private val byType: Map<String, TriggerConverter> = converters.associateBy { it.type }

    /**
     * 按触发器类型名查找转换器。
     */
    fun byType(type: String?): TriggerConverter? = type?.let { byType[it] }

    /**
     * 按 Element 查找归属的转换器（Model -> YAML 归组）。
     */
    fun byElement(element: Element): TriggerConverter? =
        byType.values.firstOrNull { it.support(element) }

    /**
     * 当前已注册的全部转换器（顺序稳定，按注入顺序）。
     */
    fun converters(): Collection<TriggerConverter> = byType.values
}
