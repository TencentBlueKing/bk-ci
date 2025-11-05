package com.tencent.devops.store.pojo.event.conditions

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.event.enums.ConditionOperator
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发条件-多选框")
data class CheckboxListCondition(
    @get:Schema(title = "条件字段名称")
    override val label: String,
    @get:Schema(title = "分组名称")
    override val group: String? = null,
    @get:Schema(title = "默认值")
    override val default: String?,
    @get:Schema(title = "条件字段")
    override val refField: String,
    @get:Schema(title = "逻辑操作")
    override val operator: ConditionOperator,
    @get:Schema(title = "是否必填")
    override val required: Boolean?,
    @get:Schema(title = "描述")
    override val desc: String?,
    override val component: AtomFormComponentType = AtomFormComponentType.ATOM_CHECKBOX_LIST,
    @get:Schema(title = "多选选项")
    val options: List<ConditionOption>? = listOf()
) : TriggerCondition

data class ConditionOption(
    @get:Schema(description = "选项名称")
    val label: String,
    @get:Schema(description = "选项值")
    val value: String
)
