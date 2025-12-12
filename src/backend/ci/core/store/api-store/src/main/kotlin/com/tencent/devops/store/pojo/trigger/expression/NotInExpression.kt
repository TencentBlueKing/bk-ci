package com.tencent.devops.store.pojo.trigger.expression

class NotInExpression : TriggerExpression {
    override fun evaluate(eventValue: Any?, inputValue: Any?): Boolean {
        return when {
            // 只要输入框为空，那么都算通过
            inputValue == null -> true
            // 事件没有值,用户输入有值,那么通过
            eventValue == null -> true
            // 用户输入是集合，才比较,都不相等,那么通过
            inputValue is Collection<*> -> inputValue.all {
                it != null && ComparisonUtils.compare(eventValue, it) != 0
            }

            else -> false
        }
    }
}
