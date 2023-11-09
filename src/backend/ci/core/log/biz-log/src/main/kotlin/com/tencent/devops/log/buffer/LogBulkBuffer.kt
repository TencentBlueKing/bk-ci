package com.tencent.devops.log.buffer

import com.tencent.devops.common.log.pojo.message.LogMessageToBulk
import java.util.LinkedList
import java.util.concurrent.locks.ReentrantLock

class LogBulkBuffer(private val maxSize: Int) {

    private val storageQueue: LinkedList<LogMessageToBulk> = LinkedList()
    private val lock = ReentrantLock(false)

    fun <T> enqueue(item: List<LogMessageToBulk>, bulkAction: () -> T) {
        lock.lock()
        try {
            if (storageQueue.size >= maxSize) {
                flushBuffer(bulkAction)
            }
            storageQueue.addAll(item)
        } finally {
            lock.unlock()
        }
    }

    private fun <T> flushBuffer(bulkAction: () -> T) {
        lock.lock()
        try {
            if (storageQueue.isEmpty()) return
            bulkAction()
            storageQueue.clear()
        } finally {
            lock.unlock()
        }
    }
}
