package com.tencent.devops.store.pojo.trigger.expression

import com.tencent.devops.store.pojo.trigger.expression.ComparisonUtils.compare

class EqualsExpression : TriggerExpression {
    override fun evaluate(eventValue: Any?, inputValue: Any?): Boolean {
        return when {
            // 只要输入框为空，那么都算通过
            inputValue == null -> true
            // 事件没有值,用户输入有值,那么不通过
            eventValue == null -> false
            // 用户输入是集合，才比较,只要在其中一个匹配上，那么就通过
            inputValue is Collection<*> -> inputValue.any {
                it != null && compare(eventValue, it) == 0
            }
            // 事件和用户输入都有值,那么比较
            else -> compare(eventValue, inputValue) == 0
        }
    }
}
