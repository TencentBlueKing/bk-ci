package com.tencent.bkrepo.common.artifact.metrics

import io.micrometer.core.instrument.Meter.Id
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.config.MeterFilter
import io.micrometer.core.instrument.config.MeterFilterReply

/**
 * 达到最大限制时，根据LRU策略逐出meter
 * */
class LruMeterFilter(
    private val meterNamePrefix: String,
    private val registry: MeterRegistry,
    private val capacity: Int
) : MeterFilter {

    private val lruSet = LRUSet()
    override fun accept(id: Id): MeterFilterReply {
        if (matchName(id)) {
            synchronized(lruSet) {
                lruSet.add(id)
            }
        }
        return MeterFilterReply.NEUTRAL
    }

    fun access(value: Id) {
        synchronized(lruSet) {
            lruSet[value]
        }
    }

    inner class LRUSet : LinkedHashMap<Id, Any>(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, true) {

        private val dummy = Any()
        fun add(value: Id) {
            super.put(value, dummy)
        }

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Id, Any>): Boolean {
            if (capacity == -1) {
                return false
            }
            val removed = size > capacity
            if (removed) {
                registry.remove(eldest.key)
            }
            return removed
        }
    }

    private fun matchName(id: Id): Boolean {
        return id.name.startsWith(meterNamePrefix)
    }

    companion object {
        private const val DEFAULT_INITIAL_CAPACITY = 16
        private const val DEFAULT_LOAD_FACTOR = 0.75f
    }
}
