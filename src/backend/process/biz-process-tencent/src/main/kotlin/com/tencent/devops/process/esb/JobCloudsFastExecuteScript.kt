package com.tencent.devops.process.esb

import com.tencent.devops.common.api.exception.OperationException
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.util.Base64

@Component
class JobCloudsFastExecuteScript @Autowired constructor(rabbitTemplate: RabbitTemplate) :
    JobFastExecuteScript(rabbitTemplate) {

    companion object {
        private val logger = LoggerFactory.getLogger(JobCloudsFastExecuteScript::class.java)
    }

    @Value("\${clouds.esb.url}")
    private val cloudsEsbUrl = "http://api-t.o.bkclouds.cc/c/clouds/compapi/job/"

    fun cloudsFastExecuteScript(
        buildId: String,
        operator: String,
        content: String,
        scriptParam: String,
        scriptTimeout: Int,
        openstate: String,
        targetAppId: Int,
        elementId: String,
        containerHashId: String?,
        executeCount: Int,
        paramSensitive: Int = 0,
        type: Int = 1,
        account: String = System.getProperty("user.name")
    ): Long {
        checkParam(operator, targetAppId, content, account)

        val taskInstanceId = sendTaskRequest(
            buildId = buildId,
            operator = operator,
            appId = targetAppId,
            content = content,
            scriptParam = scriptParam,
            scriptTimeout = scriptTimeout,
            type = type,
            openstate = openstate,
            account = account,
            elementId = elementId,
            containerHashId = containerHashId,
            executeCount = executeCount
        )

        if (taskInstanceId <= 0) {
            // 失败处理
            logger.error("Job start execute script failed.")
            throw OperationException("Job执行脚本失败")
        }

        return taskInstanceId
    }

    private fun sendTaskRequest(
        buildId: String,
        operator: String,
        appId: Int,
        content: String,
        scriptParam: String,
        scriptTimeout: Int,
        type: Int,
        openstate: String,
        account: String,
        elementId: String,
        containerHashId: String?,
        executeCount: Int
    ): Long {
        val requestData = emptyMap<String, Any>().toMutableMap()
        requestData["app_code"] = appCode
        requestData["app_secret"] = appSecret
        requestData["app_id"] = appId
        requestData["content"] = content
        requestData["script_params"] = Base64.getEncoder().encodeToString(scriptParam.toByteArray())
        requestData["script_timeout"] = scriptTimeout
        requestData["type"] = type
        requestData["account"] = account
        requestData["openstate"] = openstate
        requestData["uin"] = operator
        val url = cloudsEsbUrl + "dev_ops_fast_execute_script"
        return doSendTaskRequest(url, requestData, buildId, elementId, containerHashId, executeCount)
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
