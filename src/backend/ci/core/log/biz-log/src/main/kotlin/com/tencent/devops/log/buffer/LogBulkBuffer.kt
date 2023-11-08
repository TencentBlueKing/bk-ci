package com.tencent.devops.log.buffer

import com.tencent.devops.common.log.pojo.message.LogMessageToBulk
import java.util.LinkedList

class LogBulkBuffer(private val maxSize: Int) {

    private val storageQueue: LinkedList<LogMessageToBulk> = LinkedList()

    @Synchronized
    fun <T> enqueue(item: List<LogMessageToBulk>, bulkAction: () -> T) {
        if (storageQueue.size >= maxSize) {
            flushBuffer(bulkAction)
        }
        storageQueue.addAll(item)
    }

    @Synchronized
    fun <T> flushBuffer(bulkAction: () -> T) {
        if (storageQueue.isEmpty()) return
        bulkAction()
        storageQueue.clear()
    }
}
