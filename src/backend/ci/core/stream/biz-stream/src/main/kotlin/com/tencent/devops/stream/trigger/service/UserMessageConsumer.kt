package com.tencent.devops.stream.trigger.service

import com.tencent.devops.common.api.util.JsonUtil
import com.tencent.devops.common.api.util.UUIDUtil
import com.tencent.devops.common.redis.RedisLock
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.BkTag
import com.tencent.devops.stream.dao.StreamUserMessageDao
import com.tencent.devops.stream.pojo.UserMessageData
import com.tencent.devops.stream.pojo.message.UserMessageType
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

@Component
class UserMessageConsumer @Autowired constructor(
    private val dslContext: DSLContext,
    private val userMessageDao: StreamUserMessageDao,
    @Qualifier("redisStringHashOperation")
    private val redisHashOperation: RedisOperation,
    private val bkTag: BkTag
) : ApplicationRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(UserMessageConsumer::class.java)
    }

    override fun run(args: ApplicationArguments) {
        Thread(UserMessageProcess(this)).start()
    }

    fun bufferKey() = "stream_user_message_consumer:${bkTag.getLocalTag()}:buffer"


    @Value("\${queue.max.size:10000}")
    private var maxSize: Int = 10000 // 默认值
    private var size = 0

    private class UserMessageProcess(
        private val consumer: UserMessageConsumer
    ) : Runnable {

        private val lock = "user_message_process_${consumer.bkTag}"
        private val key = consumer.bufferKey()

        companion object {
            const val SLEEP = 10000L
            const val CHUNKED = 200
        }

        override fun run() {
            logger.info("UserMessageProcess begin")
            while (true) {
                val redisLock = RedisLock(consumer.redisHashOperation, lock, 60L)
                try {
                    val lockSuccess = redisLock.tryLock()
                    if (lockSuccess) {
                        execute()
                    }
                } catch (e: Throwable) {
                    logger.error("UserMessageProcess failed ${e.message}", e)
                } finally {
                    Thread.sleep(SLEEP)
                    redisLock.unlock()
                }
            }
        }

        private fun execute() {
            val massages = consumer.redisHashOperation.hkeys(key)?.ifEmpty { null } ?: return
            consumer.size = massages.size
            val needDelete = mutableListOf<String>()
            massages.chunked(CHUNKED).forEach { keys ->
                val updateValues = consumer.redisHashOperation.hmGet(key, keys)
                    ?: return@forEach
                val removeDuplicates = mutableMapOf<String, UserMessageData>()
                keys.forEachIndexed { index, key ->
                    needDelete.add(key)
                    kotlin.runCatching {
                        val load = JsonUtil.to(updateValues[index], UserMessageData::class.java)
                        val loadKey = "${load.projectId}${load.userId}${load.messageId}"
                        if (removeDuplicates[loadKey] != null &&
                            removeDuplicates[loadKey]!!.messageType != UserMessageType.ONLY_SUCCESS.name
                        ) {
                            return@forEachIndexed
                        }
                        if (removeDuplicates[loadKey] != null &&
                            load.messageType == UserMessageType.ONLY_SUCCESS.name
                        ) {
                            return@forEachIndexed
                        }
                        removeDuplicates[loadKey] = load
                    }.onFailure {
                        logger.warn("UserMessageProcess failed ${it.message}", it)
                    }
                }
                removeDuplicates.forEach { (_, v) ->
                    consumer.writeData(
                        projectId = v.projectId,
                        userId = v.userId,
                        messageId = v.messageId,
                        messageType = UserMessageType.parse(v.messageType),
                        messageTitle = v.messageTitle,
                    )
                }
            }
            if (needDelete.isNotEmpty()) {
                logger.info("UserMessageProcess success write ${needDelete.size} messages")
                consumer.redisHashOperation.hdelete(key, needDelete.toTypedArray())
            }
        }
    }

    // 添加数据到队列
    fun addData(
        projectId: String,
        userId: String,
        messageId: String,
        messageType: UserMessageType,
        messageTitle: String
    ) {
        if (size < maxSize) {
            redisHashOperation.hset(
                bufferKey(),
                UUIDUtil.generate(),
                JsonUtil.toJson(
                    UserMessageData(
                        projectId = projectId,
                        userId = userId,
                        messageId = messageId,
                        messageType = messageType.name,
                        messageTitle = messageTitle
                    )
                )
            )
        } else {
            logger.error("Queue is full. Cannot add data.")
        }
    }

    fun writeData(
        projectId: String,
        userId: String,
        messageId: String,
        messageType: UserMessageType,
        messageTitle: String
    ) {
        val exist = userMessageDao.getMessageExist(dslContext, projectId, userId, messageId)
        if (exist == null) {
            userMessageDao.save(
                dslContext = dslContext,
                projectId = projectId,
                userId = userId,
                messageType = messageType,
                messageId = messageId,
                messageTitle = messageTitle
            )
        } else {
            if (exist.messageType == messageType.name || exist.messageType == UserMessageType.REQUEST.name) {
                return
            }
            userMessageDao.updateMessageType(
                dslContext = dslContext,
                projectId = projectId,
                userId = userId,
                messageId = messageId,
                messageType = messageType
            )
        }
    }
}
