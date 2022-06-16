package com.tencent.bkrepo.common.api.collection

class LRULinkedHashMap<K, V>(private val capacity: Int) : LinkedHashMap<K, V>(capacity) {

    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean {
        return size > capacity
    }
}
