package com.tencent.devops.process.esb

import com.tencent.devops.common.api.exception.OperationException
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JobCloudsFastPushFile @Autowired constructor(rabbitTemplate: RabbitTemplate) : JobFastPushFile(rabbitTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobCloudsFastPushFile::class.java)
        private const val cloudsBkAppId = 77770001
    }

    @Value("\${clouds.esb.url}")
    private val cloudsEsbUrl = "http://api-t.o.bkclouds.cc/c/clouds/compapi/job/"

    @Value("\${clouds.esb.proxyIp}")
    private val cloudStoneIps = ""

    fun cloudsFastPushFile(
        buildId: String,
        operator: String,
        sourceFileList: List<String>,
        targetPath: String,
        openstate: String,
        ipList: List<String>?,
        targetAppId: Int,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): Long {
        checkParam(operator, targetAppId, sourceFileList, targetPath)

        val taskInstanceId = sendTaskRequest(
            buildId = buildId,
            operator = operator,
            sourceFileList = sourceFileList,
            targetPath = targetPath,
            openstate = openstate,
            ipList = ipList,
            targetAppId = targetAppId,
            elementId = elementId,
            containerId = containerId,
            executeCount = executeCount
        )
        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("Job start push file failed.")
            throw OperationException("Job推文件失败")
        }
        return taskInstanceId
    }

    private fun sendTaskRequest(
        buildId: String,
        operator: String,
        sourceFileList: List<String>,
        targetPath: String,
        openstate: String,
        ipList: List<String>?,
        targetAppId: Int,
        elementId: String,
        containerId: String,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = cloudsBkAppId

        val fileSource = mutableListOf<Any>()
        val fileSourceItem = mutableMapOf<String, Any>()
        fileSourceItem["files"] = sourceFileList
        fileSourceItem["account"] = "root"
        fileSourceItem["ip_list"] = listOf(SourceIp(cloudStoneIps, 3))
        fileSourceItem["sourceAppId"] = 621
        fileSource.add(fileSourceItem)
        requestData["file_source"] = fileSource
        requestData["account"] = "root"
        requestData["file_target_path"] = targetPath
        requestData["uin"] = operator
        requestData["openstate"] = openstate
        if (ipList != null && ipList.isNotEmpty()) {
            val ips = mutableListOf<Map<String, String>>()
            ipList.filter { it.isNotBlank() }.forEach {
                val ip = mutableMapOf<String, String>()
                val sourceAndIp = it.split(":")
                when {
                    sourceAndIp.size == 1 -> {
                        ip["source"] = "1"
                        ip["ip"] = sourceAndIp[0]
                    }
                    sourceAndIp.size == 2 -> {
                        ip["source"] = sourceAndIp[0]
                        ip["ip"] = sourceAndIp[1]
                    }
                    else -> return@forEach
                }
                ips.add(ip)
            }

            if (ips.isNotEmpty()) {
                requestData["ip_list"] = ips
            }
        }
        requestData["target_app_id"] = targetAppId
        val url = cloudsEsbUrl + "dev_ops_fast_push_file"
        return doSendTaskRequest(url, requestData, buildId, elementId, containerId, executeCount)
    }

    override fun getTaskResult(appId: Int, taskInstanceId: Long, operator: String): TaskResult {
        val url = cloudsEsbUrl + "get_task_result"
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["task_instance_id"] = taskInstanceId
        requestData["uin"] = operator
        return doGetTaskResult(url, requestData, taskInstanceId)
    }
}
