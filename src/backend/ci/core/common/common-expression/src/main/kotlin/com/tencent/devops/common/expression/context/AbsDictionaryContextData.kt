package com.tencent.devops.common.expression.context

import com.fasterxml.jackson.databind.JsonNode
import com.tencent.devops.common.expression.expression.sdk.CollectionPipelineResult
import com.tencent.devops.common.expression.expression.sdk.IReadOnlyObject
import com.tencent.devops.common.expression.utils.ExpressionJsonUtil
import java.util.TreeMap

/**
 * dict 的抽象类，总结公共方法
 */
abstract class AbsDictionaryContextData : PipelineContextData(PipelineContextDataType.DICTIONARY), IReadOnlyObject {
    protected open var mIndexLookup: TreeMap<String, Int>? = null
    protected open var mList: MutableList<DictionaryContextDataPair> = mutableListOf()

    override val values: Iterable<Any?>
        get() {
            if (mList.isNotEmpty()) {
                return mList.map { it.value }
            }
            return emptyList()
        }

    protected val indexLookup: MutableMap<String, Int>
        get() {
            if (mIndexLookup == null) {
                mIndexLookup = TreeMap<String, Int>()
                if (mList.isNotEmpty()) {
                    mList.forEachIndexed { index, pair ->
                        mIndexLookup!![pair.key] = index
                    }
                }
            }

            return mIndexLookup!!
        }

    private val list: MutableList<DictionaryContextDataPair>
        get() {
            return mList
        }

    override operator fun get(key: String): PipelineContextData? {
        val index = indexLookup[key] ?: return null
        return list[index].value
    }

    override fun getRes(key: String): CollectionPipelineResult {
        return if (containsKey(key)) {
            CollectionPipelineResult(list.getOrNull(indexLookup[key]!!)?.value)
        } else {
            CollectionPipelineResult.noKey()
        }
    }

    override fun toJson(): JsonNode {
        val json = ExpressionJsonUtil.createObjectNode()
        if (mList.isNotEmpty()) {
            mList.forEach {
                json.set<JsonNode>(it.key, it.value?.toJson())
            }
        }
        return json
    }

    operator fun set(k: String, value: PipelineContextData?) {
        // Existing
        val index = indexLookup[k]
        if (index != null) {
            val key = mList[index].key // preserve casing
            mList[index] = DictionaryContextDataPair(key, value)
        }
        // New
        else {
            add(k, value)
        }
    }

    fun add(pairs: Iterable<Pair<String, PipelineContextData>>) {
        pairs.forEach { pair ->
            add(pair.first, pair.second)
        }
    }

    fun add(
        key: String,
        value: PipelineContextData?
    ) {
        indexLookup[key] = mList.count()
        list.add(DictionaryContextDataPair(key, value))
    }

    fun containsKey(key: String): Boolean {
        return mList.isNotEmpty() && indexLookup.containsKey(key)
    }

    protected data class DictionaryContextDataPair(
        val key: String,
        val value: PipelineContextData?
    )
}
