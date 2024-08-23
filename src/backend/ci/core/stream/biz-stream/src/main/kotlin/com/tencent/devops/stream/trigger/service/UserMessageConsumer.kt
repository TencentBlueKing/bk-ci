package com.tencent.devops.stream.trigger.service

import com.tencent.devops.common.api.util.JsonUtil
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
            const val SLEEP = 5000L
            const val CHUNKED = 100
        }

        override fun run() {
            logger.info("UserMessageProcess begin")
            while (true) {
                val redisLock = RedisLock(consumer.redisHashOperation, lock, 60L)
                try {
                    val lockSuccess = redisLock.tryLock()
                    if (lockSuccess) {
                        logger.info("UserMessageProcess get lock.")
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
                keys.forEachIndexed { index, key ->
                    kotlin.runCatching {
                        val load = JsonUtil.to(updateValues[index], UserMessageData::class.java)
                        val split = key.split("@@")
                        consumer.writeData(
                            projectId = split.elementAt(0),
                            userId = split.elementAt(1),
                            messageId = split.elementAt(2),
                            messageType = UserMessageType.valueOf(load.messageType),
                            messageTitle = load.messageTitle,
                        )
                    }.onFailure {
                        logger.warn("UserMessageProcess failed ${it.message}", it)
                    }
                    needDelete.add(key)
                }
            }
            if (needDelete.isNotEmpty()) {
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
                "$projectId@@$userId@@$messageId",
                JsonUtil.toJson(UserMessageData(messageType.name, messageTitle))
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
            if (exist.messageType == messageType.name) {
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
