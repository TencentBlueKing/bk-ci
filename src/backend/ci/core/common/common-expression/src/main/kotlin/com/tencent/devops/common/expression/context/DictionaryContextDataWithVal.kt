package com.tencent.devops.common.expression.context

import com.tencent.devops.common.expression.utils.ExpressionJsonUtil
import java.util.TreeMap

/**
 * 包含用户原始值的类型，用来兼容老数据中 a.b.c 已经存在一个固定值的情况，优先兼容老的 json 存储
 * @param oriValue 用户原始值
 */
class DictionaryContextDataWithVal(
    private val oriValue: String
) : AbsDictionaryContextData() {
    override var mIndexLookup: TreeMap<String, Int>? = null
    override var mList: MutableList<DictionaryContextDataPair> = mutableListOf()

    override fun clone(): PipelineContextData {
        val result = DictionaryContextDataWithVal(oriValue)

        if (mList.isNotEmpty()) {
            result.mList = mutableListOf()
            mList.forEach {
                result.mList.add(DictionaryContextDataPair(it.key, it.value?.clone()))
            }
        }

        return result
    }

    override fun fetchValue(): Any {
        return try {
            ExpressionJsonUtil.getObjectMapper().readTree(oriValue)
        } catch (ignore: Exception) {
            return oriValue
        }
    }
}