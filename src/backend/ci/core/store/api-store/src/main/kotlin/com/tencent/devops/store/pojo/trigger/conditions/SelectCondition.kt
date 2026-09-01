package com.tencent.devops.store.pojo.trigger.conditions

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperatorEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发条件-下拉选")
data class SelectCondition(
    @get:Schema(title = "条件字段名称")
    override val label: String,
    @get:Schema(title = "分组名称")
    override val group: String? = null,
    @get:Schema(title = "默认值")
    override val defaultValue: String?,
    @get:Schema(title = "条件字段")
    override val targetField: String,
    @get:Schema(title = "逻辑操作")
    override val operator: ConditionOperatorEnum,
    @get:Schema(title = "是否必填")
    override val required: Boolean? = false,
    @get:Schema(title = "描述")
    override val desc: String?,
    override val component: AtomFormComponentType = AtomFormComponentType.SELECT_INPUT,
    @get:Schema(title = "是否可多选")
    val multiple: Boolean = false,
    @get:Schema(title = "下拉选项")
    val options: List<ConditionOption>? = listOf()
) : TriggerCondition {
    companion object {
        const val classType = "selectInput"
    }
}
