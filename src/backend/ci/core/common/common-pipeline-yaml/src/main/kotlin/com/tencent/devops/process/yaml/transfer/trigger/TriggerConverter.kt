package com.tencent.devops.process.yaml.transfer.trigger

import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.process.yaml.transfer.aspect.PipelineTransferAspectWrapper
import com.tencent.devops.process.yaml.v3.models.TriggerType
import com.tencent.devops.process.yaml.v3.models.on.TriggerOn

/**
 * 触发器转换器统一 SPI。
 *
 * 目标：新增触发器时只需实现一个 [TriggerConverter] 并注册为 Spring Bean，
 * 无需再改动 [com.tencent.devops.process.yaml.transfer.ElementTransfer.yaml2Triggers]
 * 的分支与 [com.tencent.devops.process.yaml.transfer.ModelTransfer] 的拼接逻辑。
 *
 * 触发器在 YAML 中遵循「触发器 -> 事件类型」的统一结构：
 * - 单触发器：`on.{triggerType}.{eventType}`（嵌套形态）
 * - 多触发器：`on[].type = {triggerType}` + `{eventType}`（列表形态）
 */
interface TriggerConverter {

    /**
     * 该转换器负责的触发器类型。
     */
    val triggerType: TriggerType

    /**
     * YAML -> Model：将单个触发器节点（已归一化为 [TriggerOn]）转换为流水线 [Element]，
     * 追加到 [elements] 队列。
     */
    fun yaml2Elements(triggerOn: TriggerOn, elements: MutableList<Element>)

    /**
     * Model -> YAML：从全部触发器 [Element] 中筛选归属本转换器的元素，
     * 聚合为一到多个 [TriggerOn] 节点（顶层不含 type，type 由调用方按 [triggerType] 回填）。
     */
    fun elements2Yaml(
        elements: List<Element>,
        aspectWrapper: PipelineTransferAspectWrapper
    ): List<TriggerOn>

    /**
     * 判定某个 [Element] 是否归属本转换器（用于 Model -> YAML 的归组）。
     */
    fun support(element: Element): Boolean
}
