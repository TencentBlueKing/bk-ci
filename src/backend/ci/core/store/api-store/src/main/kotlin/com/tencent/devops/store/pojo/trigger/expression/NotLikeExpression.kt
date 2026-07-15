package com.tencent.devops.store.pojo.trigger.expression

import com.tencent.devops.store.pojo.trigger.expression.ComparisonUtils.compare

class NotLikeExpression : TriggerExpression {
    override fun evaluate(eventValue: Any?, inputValue: Any?): Boolean {
        return when {
            // 只要输入框为空，那么都算通过
            inputValue == null -> true
            // 事件没有值,用户输入有值,那么通过
            eventValue == null -> true
            // 事件和用户输入都有值,那么比较
            else -> compare(eventValue, inputValue) != 0
        }
    }
}
