package com.tencent.devops.common.expression.expression.sdk

class ResultMemory {
    /**
     * 只有同时满足以下两个条件时才设置非空值：
     * 1) 结果是一个复杂的对象。换句话说，结果是
     * 不是简单类型：字符串、布尔值、数字或空值。
     * 2) 结果是一个新创建的对象。
     *
     * 例如，考虑一个接受字符串参数的函数 jsonParse()，
     * 并返回一个 Json 对象。 Json 对象是新创建的，并且是一个粗略的
     * 测量应该返回它在内存中消耗的字节数。
     *
     * 再举一个例子，考虑一个从
     * 复杂的参数值。从单个功能的角度来看，
     * 复杂参数值的大小未知。在这种情况下，设置
     * 值到 IntPtr.Size。
     *
     * 不确定时，将值设置为null。 Null 表示 a 的开销
     * 应该考虑新指针。
     */
    var bytes: Int? = null

    /**
     * 表示 <c ref="Bytes" /> 是否代表结果的总大小。
     * True表示可以丢弃下游参数的accounting-overhead。
     *
     * 对于 <c ref="EvaluationOptions.Converters" />，此值当前被忽略。
     *
     * 例如，考虑一个接受字符串参数的函数 jsonParse()，
     * 并返回一个 Json 对象。 Json 对象是新创建的，并且是一个粗略的
     * 测量值应该返回它在内存中消耗的字节数。
     * 将 <c ref="IsTotal" /> 设置为 true，因为新对象不包含引用
     * 到之前分配的内存。
     *
     * 再举一个例子，考虑一个包装复杂参数结果的函数。
     * <c ref="Bytes" /> 应该设置为新分配的内存量。
     * 但是由于对象引用了之前分配的内存，设置 <c ref="IsTotal" />
     * 为假。
     */
    var isTotal: Boolean = false
}
