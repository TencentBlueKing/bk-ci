package com.tencent.devops.store.pojo.trigger.conditions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.store.pojo.trigger.enums.ConditionOperatorEnum

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "@type",
    defaultImpl = EmptyElement::class
)
@JsonSubTypes(
    JsonSubTypes.Type(value = CheckboxListCondition::class, name = CheckboxListCondition.classType),
    JsonSubTypes.Type(value = EnumInputCondition::class, name = EnumInputCondition.classType),
    JsonSubTypes.Type(value = InputCondition::class, name = InputCondition.classType),
    JsonSubTypes.Type(value = SelectCondition::class, name = SelectCondition.classType)
)
interface TriggerCondition {
    // 字段名
    val label: String
    // 字段分组
    val group: String?
    // 默认值
    val defaultValue: Any?
    // 映射字段
    val targetField: String
    // 操作符
    val operator: ConditionOperatorEnum
    // 类型
    val component: AtomFormComponentType
    // 是否必填
    val required: Boolean?
    // 描述
    val desc: String?

    fun key() = "$targetField@$operator"

    fun defaultPreview(): String = defaultValue as String? ?: ""
}
