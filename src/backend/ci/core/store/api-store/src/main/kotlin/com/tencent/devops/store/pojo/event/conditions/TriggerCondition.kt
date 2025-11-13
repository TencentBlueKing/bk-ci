package com.tencent.devops.store.pojo.event.conditions

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.common.pipeline.pojo.element.EmptyElement
import com.tencent.devops.store.pojo.event.enums.ConditionOperator

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
    val default: Any?
    // 映射字段
    val refField: String
    // 操作符
    val operator: ConditionOperator
    // 类型
    val component: AtomFormComponentType
    // 是否必填
    val required: Boolean?
    // 描述
    val desc: String?

    fun key() = "$refField@$operator"

    fun defaultPreview(): String = default as String? ?: ""
}
