package com.tencent.devops.store.pojo.trigger.expression

import com.tencent.devops.common.api.util.ReflectUtil

class InExpression : TriggerExpression {
    override fun evaluate(eventValue: Any?, inputValue: Any?): Boolean {
        return when {
            // 只要输入框为空，那么都算通过
            inputValue == null -> true
            // 事件没有值,用户输入有值,那么不通过
            eventValue == null -> false
            // 用户输入是集合，才比较,只要在其中一个匹配上，那么就通过
            inputValue is Collection<*> -> inputValue.any {
                it != null && ComparisonUtils.compare(eventValue, it) == 0
            }
            // 兼容处理，如果用户输入是单个元素，则转成集合
            ReflectUtil.isNativeType(inputValue) || inputValue is String -> listOf("$inputValue").all {
                ComparisonUtils.compare(eventValue, it) == 0
            }

            else -> false
        }
    }
}
