package com.tencent.bkrepo.common.api.collection.concurrent

import java.util.AbstractSet
import java.util.concurrent.ConcurrentHashMap

class ConcurrentHashSet<T : Any> : AbstractSet<T>() {
    private val delegate = ConcurrentHashMap<T, Boolean>()

    override fun iterator(): MutableIterator<T> {
        return delegate.keys.iterator()
    }

    override val size: Int get() = delegate.size

    override fun add(element: T): Boolean {
        return delegate.put(element, true) == null
    }

    override fun remove(element: T): Boolean {
        return delegate.remove(element) != null
    }
}
