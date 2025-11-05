package com.tencent.devops.store.pojo.event.conditions

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.event.enums.ConditionOperator
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "触发条件-基础输入框")
data class InputCondition(
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
    override val required: Boolean? = false,
    @get:Schema(title = "描述")
    override val desc: String?,
    override val component: AtomFormComponentType = AtomFormComponentType.VUE_INPUT
) : TriggerCondition