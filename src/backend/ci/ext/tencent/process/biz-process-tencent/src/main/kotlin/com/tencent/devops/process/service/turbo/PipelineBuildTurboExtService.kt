package com.tencent.devops.process.service.turbo

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.notify.enums.NotifyType
import com.tencent.devops.common.service.utils.LogUtils
import com.tencent.devops.common.websocket.pojo.BuildPageInfo
import com.tencent.devops.notify.api.service.ServiceNotifyMessageTemplateResource
import com.tencent.devops.notify.pojo.SendNotifyMessageTemplateRequest
import com.tencent.devops.process.engine.dao.PipelineInfoDao
import com.tencent.devops.process.engine.pojo.PipelineBuildTask
import com.tencent.devops.process.engine.service.PipelineBuildExtService
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.service.ProjectNameService
import com.tencent.devops.process.util.ServiceHomeUrlUtils
import com.tencent.devops.process.utils.PIPELINE_TURBO_TASK_ID
import com.tencent.devops.process.websocket.page.DetailPageBuild
import com.tencent.devops.store.pojo.common.PIPELINE_TASK_PAUSE_NOTIFY
import okhttp3.Request
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient
import org.springframework.stereotype.Service
import java.util.Random

@Service
class PipelineBuildTurboExtService @Autowired constructor(
    private val consulClient: ConsulDiscoveryClient?,
    private val dslContext: DSLContext,
    private val pipelineInfoDao: PipelineInfoDao,
    private val projectNameService: ProjectNameService,
    private val client: Client,
    private val pipelineRuntimeService: PipelineRuntimeService
) : PipelineBuildExtService {

    override fun buildExt(task: PipelineBuildTask): Map<String, String> {
        val taskType = task.taskType
        val extMap = mutableMapOf<String, String>()
        if (taskType.contains("linuxPaasCodeCCScript") || taskType.contains("linuxScript")) {
            logger.info("task need turbo, ${task.buildId}, ${task.taskName}, ${task.taskType}")
            val turboTaskId = getTurboTask(task.pipelineId, task.taskId)
            extMap[PIPELINE_TURBO_TASK_ID] = turboTaskId
        }
        return extMap
    }

    override fun sendPauseNotify(buildId: String, buildTask: PipelineBuildTask) {
        try {
            // 发送消息给相关关注人
            val sendUser = buildTask.additionalOptions!!.subscriptionPauseUser
            val subscriptionPauseUser = mutableSetOf<String>()
            if (!sendUser.isNullOrEmpty()) {
                val sendUsers = sendUser!!.split(",").toSet()
                subscriptionPauseUser.addAll(sendUsers)
            }
            sendPauseNotify(
                buildId = buildId,
                taskName = buildTask.taskName,
                pipelineId = buildTask.pipelineId,
                receivers = subscriptionPauseUser
            )
            logger.info("|$buildId| next task |$buildTask| need pause, send End status to Vm agent")
        } catch (e: Exception) {
            logger.warn("pause atom send notify fail", e)
        }
    }

    private fun sendPauseNotify(
        buildId: String,
        taskName: String,
        pipelineId: String,
        receivers: Set<String>?
    ) {
        val pipelineRecord = pipelineInfoDao.getPipelineInfo(dslContext, pipelineId)
        if (pipelineRecord == null) {
            logger.warn("sendPauseNotify pipeline[$pipelineId] is empty record")
            return
        }

        val buildRecord = pipelineRuntimeService.getBuildInfo(buildId)
        val pipelineName = (pipelineRecord?.pipelineName ?: "")
        val buildNum = buildRecord?.buildNum.toString()
        val projectName = projectNameService.getProjectName(pipelineRecord.projectId) ?: ""
        val host = ServiceHomeUrlUtils.server()
        val url = host + DetailPageBuild().buildPage(
            buildPageInfo = BuildPageInfo(
                buildId = buildId,
                pipelineId = pipelineId,
                projectId = pipelineRecord.projectId,
                atomId = null
            )
        )
        // 指定通过rtx发送
        val notifyType = mutableSetOf<String>()
        notifyType.add(NotifyType.RTX.name)

        // 若没有配置订阅人，则将暂停消息发送给发起人
        val receiver = mutableSetOf<String>()
        if (receivers == null || receivers.isEmpty()) {
            receiver.add(buildRecord!!.startUser)
            receiver.add(pipelineRecord.lastModifyUser)
        } else {
            receiver.addAll(receivers)
        }
        logger.info("sean pause notify: $buildId| $taskName| $receiver")

        val msg = SendNotifyMessageTemplateRequest(
            templateCode = PIPELINE_TASK_PAUSE_NOTIFY,
            titleParams = mapOf(
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum
            ),
            notifyType = notifyType,
            bodyParams = mapOf(
                "BK_CI_PROJECT_NAME_CN" to projectName,
                "BK_CI_PIPELINE_NAME" to pipelineName,
                "BK_CI_BUILD_NUM" to buildNum,
                "taskName" to taskName,
                "BK_CI_START_USER_ID" to (buildRecord?.startUser ?: ""),
                "url" to url
            ),
            receivers = receiver
        )
        logger.info("sendPauseNotify|$buildId| $pipelineId| $msg")
        client.get(ServiceNotifyMessageTemplateResource::class)
            .sendNotifyMessageByTemplate(msg)
    }

    fun getTurboTask(pipelineId: String, elementId: String): String {
        try {
            val instances = consulClient!!.getInstances("turbo")
                    ?: return ""
            if (instances.isEmpty()) {
                return ""
            }
            val instance = loadBalance(instances)
            val url = "${if (instance.isSecure) "https" else
                "http"}://${instance.host}:${instance.port}/api/service/turbo/task/pipeline/$pipelineId/$elementId"

            logger.info("Get turbo task info, request url: $url")
            val startTime = System.currentTimeMillis()
            val request = Request.Builder().url(url).get().build()
            OkhttpUtils.doHttp(request).use { response ->
                val data = response.body()?.string() ?: return ""
                logger.info("Get turbo task info, response: $data")
                LogUtils.costTime("call turbo ", startTime)
                if (!response.isSuccessful) {
                    throw RemoteServiceException(data)
                }
                val responseData: Map<String, Any> = jacksonObjectMapper().readValue(data)
                val code = responseData["status"] as Int
                if (0 == code) {
                    val dataMap = responseData["data"] as Map<String, Any>
                    return dataMap["taskId"] as String? ?: ""
                } else {
                    throw RemoteServiceException(data)
                }
            }
        } catch (e: Throwable) {
            logger.warn("Get turbo task info failed, $e")
            return ""
        }
    }

    fun loadBalance(instances: List<ServiceInstance>): ServiceInstance {
        val random = Random()
        val index = random.nextInt(instances.size)
        return instances[index]
    }

    companion object {
        val logger = LoggerFactory.getLogger(this :: class.java)
    }
}
