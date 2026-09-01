package com.tencent.devops.store.pojo.trigger.conditions

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperatorEnum
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发条件-多选框")
data class CheckboxListCondition(
    @get:Schema(title = "条件字段名称")
    override val label: String,
    @get:Schema(title = "分组名称")
    override val group: String? = null,
    @get:Schema(title = "默认值")
    override val defaultValue: List<String>,
    @get:Schema(title = "条件字段")
    override val targetField: String,
    @get:Schema(title = "逻辑操作")
    override val operator: ConditionOperatorEnum = ConditionOperatorEnum.IN,
    @get:Schema(title = "是否必填")
    override val required: Boolean?,
    @get:Schema(title = "描述")
    override val desc: String?,
    override val component: AtomFormComponentType = AtomFormComponentType.ATOM_CHECKBOX_LIST,
    @get:Schema(title = "多选选项")
    val options: List<ConditionOption>? = listOf()
) : TriggerCondition {

    override fun defaultPreview(): String {
        return defaultValue.joinToString(
            separator = ", ",
            prefix = "(",
            postfix = ")"
        )
    }

    companion object {
        const val classType = "checkboxList"
    }
}

data class ConditionOption(
    @get:Schema(description = "选项名称")
    val label: String,
    @get:Schema(description = "选项值")
    val value: String
)
