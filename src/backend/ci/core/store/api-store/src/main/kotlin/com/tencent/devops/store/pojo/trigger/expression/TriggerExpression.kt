package com.tencent.devops.store.pojo.trigger.expression

/**
 * 触发条件表达式
 */
interface TriggerExpression {

    /**
     * 判断事件值是否满足输入值
     *
     * @param eventValue 事件值
     * @param inputValue 输入值
     */
    fun evaluate(eventValue: Any?, inputValue: Any?): Boolean
}
