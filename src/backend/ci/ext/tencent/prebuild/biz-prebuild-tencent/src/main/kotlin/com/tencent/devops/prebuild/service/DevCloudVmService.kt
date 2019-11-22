package com.tencent.devops.prebuild.service

import com.tencent.devops.common.environment.agent.pojo.devcloud.DevCloudContainer
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskStatus
import com.tencent.devops.common.environment.agent.pojo.devcloud.ContainerType
import com.tencent.devops.common.environment.agent.pojo.devcloud.Registry
import com.tencent.devops.prebuild.client.PrebuildDevCloudClient
import org.json.JSONObject
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Random

@Component
class DevCloudVmService @Autowired constructor(
    private val devCloudClient: PrebuildDevCloudClient
) {

    @Value("\${registry.host}")
    val registryHost: String? = null

    @Value("\${registry.userName}")
    val registryUser: String? = null

    @Value("\${registry.password}")
    val registryPwd: String? = null

    private val cpu = 32
    private val memory = "65535M"
    private val disk = "500G"

//    private val cpu = 8
//    private val memory = "8192M"
//    private val disk = "100G"

    /**
     * return
     * first: vmIP
     * second: password
     * third: containerName
     */
    fun createVm(userId: String, image: String): Triple<String, String, String> {
        val vmPwd = generatePwd()
        val devCloudTaskId = devCloudClient.createContainer(userId, DevCloudContainer(
                "$userId-${System.currentTimeMillis()}",
                ContainerType.DEV.getValue(),
                "devcloud/$image",
                Registry(registryHost!!, registryUser!!, registryPwd!!),
                cpu,
                memory,
                disk,
                1,
                emptyList(),
                vmPwd,
                null))
        logger.info("createContainer, taskId:($devCloudTaskId)")
        val createResult = waitTaskFinish(userId, devCloudTaskId)
        if (createResult.first == TaskStatus.SUCCEEDED) {
            // 得到本次任务的实例的信息
            val containerName = createResult.second
            val containerInstanceInfo = devCloudClient.getContainerInstance(userId, containerName)
            val actionCode = containerInstanceInfo.optInt("actionCode")
            if (actionCode != 200) {
                val actionMessage = containerInstanceInfo.optString("actionMessage")
                logger.error("Get container instance failed, msg: $actionMessage")
                throw Exception("create vm failed")
            }
            // 启动成功
            logger.info("start dev cloud vm success")
            val item = containerInstanceInfo.optJSONArray("data")[0] as JSONObject
            return Triple(item.optString("ip"), vmPwd, containerName)
        } else {
            // 创建失败，记录日志，告警，通知
            logger.error("create dev cloud vm failed, msg: ${createResult.second}")
            throw Exception("create vm failed")
        }
    }

    fun executeContainerCommand(userId: String, containerName: String, command: List<String>) =
            devCloudClient.executeContainerCommand(userId, containerName, command)

    private fun waitTaskFinish(userId: String, taskId: String): Pair<TaskStatus, String> {
        logger.info("waiting for dev cloud task finish")
        val startTime = System.currentTimeMillis()
        loop@ while (true) {
            if (System.currentTimeMillis() - startTime > 5 * 60 * 1000) {
                logger.error("dev cloud task timeout")
                return Pair(TaskStatus.TIMEOUT, "")
            }
            Thread.sleep(10 * 1000)
            val (isFinish, success, msg) = getTaskResult(userId, taskId)
            return when {
                !isFinish -> continue@loop
                !success -> {
                    logger.error("execute job failed, msg: $msg")
                    Pair(TaskStatus.FAILED, msg)
                }
                else -> Pair(TaskStatus.SUCCEEDED, msg)
            }
        }
    }

    private fun getTaskResult(userId: String, taskId: String): TaskResult {
        try {
            val taskStatus = devCloudClient.getTasks(userId, taskId)
            val actionCode = taskStatus.optString("actionCode")
            return if ("200" != actionCode) {
                // 创建失败
                val msg = taskStatus.optString("actionMessage")
                logger.error("Execute  task failed, actionCode is $actionCode, msg: $msg")
                TaskResult(true, false, msg)
            } else {
                val status = taskStatus.optJSONObject("data").optString("status")
                when (status) {
                    "succeeded" -> {
                        val containerName = taskStatus.optJSONObject("data").optString("name")
                        logger.info("Task success, containerName: $containerName")
                        TaskResult(true, true, containerName)
                    }
                    "failed" -> {
                        val resultDisplay = taskStatus.optJSONObject("data").optString("result")
                        logger.error("Task failed")
                        TaskResult(true, false, resultDisplay)
                    }
                    else -> TaskResult(false, false, "")
                }
            }
        } catch (e: Exception) {
            logger.error("Get dev cloud task error", e)
            return TaskResult(true, false, "创建失败，异常信息:${e.message}")
        }
    }

    private fun generatePwd(): String {
        val secretSeed = arrayOf("ABCDEFGHIJKLMNOPQRSTUVWXYZ", "abcdefghijklmnopqrstuvwxyz", "0123456789", "[()~!@#%&-+=_")

        val random = Random()
        val buf = StringBuffer()
        for (i in 0 until 15) {
            val num = random.nextInt(secretSeed[i / 4].length)
            buf.append(secretSeed[i / 4][num])
        }
        return buf.toString()
    }

    data class TaskResult(val isFinish: Boolean, val success: Boolean, val msg: String)
    companion object {
        private val logger = LoggerFactory.getLogger(DevCloudVmService::class.java)
    }
}