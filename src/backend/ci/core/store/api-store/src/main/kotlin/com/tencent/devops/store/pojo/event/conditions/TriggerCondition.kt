package com.tencent.devops.store.pojo.event.conditions

import com.tencent.devops.common.pipeline.pojo.atom.form.enums.AtomFormComponentType
import com.tencent.devops.store.pojo.event.enums.ConditionOperator

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
}