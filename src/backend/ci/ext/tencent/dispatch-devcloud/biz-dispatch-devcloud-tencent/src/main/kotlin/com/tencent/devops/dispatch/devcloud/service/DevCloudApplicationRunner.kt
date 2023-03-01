package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.client.DispatchDevCloudClient
import com.tencent.devops.dispatch.devcloud.pojo.Action
import com.tencent.devops.dispatch.devcloud.utils.RedisUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner

@Component
class DevCloudApplicationRunner constructor(
    private val redisUtils: RedisUtils,
    private val dispatchDevCloudClient: DispatchDevCloudClient
) : ApplicationRunner {

    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudApplicationRunner::class.java)
    }

    override fun run(args: ApplicationArguments) {
        logger.info("Start first, clear untracked containers")
/*        val creatingContainers = redisUtils.getAndRemoveCreatingContainer()
        val startingContainers = redisUtils.getAndRemoveStartingContainers()

        if (null != creatingContainers && creatingContainers.isNotEmpty()) {
            creatingContainers.forEach {
                val userContainer = it.split("#")
                clearUntrackedContainer(userContainer[0], userContainer[1], Action.DELETE)
            }
        }

        if (null != startingContainers && startingContainers.isNotEmpty()) {
            startingContainers.forEach {
                val userContainer = it.split("#")
                clearUntrackedContainer(userContainer[0], userContainer[1], Action.STOP)
            }
        }*/
    }

    private fun clearUntrackedContainer(userId: String, containerName: String, action: Action) {
        try {
            logger.info("Delete or stop container, userId: $userId, containerName: $containerName, action: ${action.getValue()}")
            // dispatchDevCloudClient.operateContainer("", "", userId, containerName, action)
        } catch (e: Exception) {
            logger.error("delete or stop container failed", e)
        }
    }
}
