package com.tencent.bk.codecc.quartz.job

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.task.pojo.ActiveProjModel
import com.tencent.bk.codecc.task.pojo.ActiveProjParseModel
import com.tencent.devops.common.api.util.timestampmilli
import com.tencent.devops.common.util.OkhttpUtils
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_ACTIVE_PROJECT
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.time.LocalDateTime

class ActiveGongfengProjScheduleTask @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(ActiveGongfengProjScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {
        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("job custom param is null!")
            return
        }
        val gitCodePath = jobCustomParam["gitCodePath"] as String
        val gitProjectId = jobCustomParam["gitProjectId"] as String
        val gitPrivateToken = jobCustomParam["gitPrivateToken"] as String

        val url = "$gitCodePath/api/v3/projects/$gitProjectId/repository/tree?ref=master"
        // 从工蜂拉取项目文件列表
        val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken))
        if (result.isBlank()) {
            logger.info("null returned from api")
            return
        }
        val projectFileList: List<ActiveProjModel> = objectMapper.readValue(result,
            object : TypeReference<List<ActiveProjModel>>() {})
        logger.info("file list size is : ${projectFileList.size}")

        // 对文件进行正则匹配,取日期最近的一个时间
        val personalRegex = Regex("""personal_projects_([0-9]+).json""")
        val personalDate = projectFileList.filter { personalRegex.matches(it.name) }
            .map { personalRegex.find(it.name)!!.groupValues[1] }
            .minByOrNull { LocalDateTime.now().timestampmilli() - SimpleDateFormat("yyyyMMdd").parse(it).time }
        val personalFileName = "personal_projects_$personalDate.json"
        val personalUrl =
            "$gitCodePath/api/v3/projects/$gitProjectId/repository/blobs/master?filepath=$personalFileName"
        val personalRequest = Request.Builder()
            .url(personalUrl)
            .get()
            .header("PRIVATE-TOKEN", gitPrivateToken)
            .build()
        var projectJsonList = mutableListOf<ActiveProjParseModel>()
        OkhttpUtils.doHttp(personalRequest).use {
            val data = it.body()!!.bytes().toString(Charset.forName("GBK"))
            if (!it.isSuccessful) throw RuntimeException("fail to get git file content with: $url($data)")
            projectJsonList.addAll(
                objectMapper.readValue(
                    data,
                    object : TypeReference<List<ActiveProjParseModel>>() {})
            )
        }
        logger.info("personal project size is: ${projectJsonList.size}")

        val teamRegex = Regex("""team_projects_([0-9]+).json""")
        val teamDate = projectFileList.filter { teamRegex.matches(it.name) }
            .map { teamRegex.find(it.name)!!.groupValues[1] }
            .minByOrNull { LocalDateTime.now().timestampmilli() - SimpleDateFormat("yyyyMMdd").parse(it).time }
        val teamFileName = "team_projects_$teamDate.json"
        val teamUrl = "$gitCodePath/api/v3/projects/$gitProjectId/repository/blobs/master?filepath=$teamFileName"
        val teamRequest = Request.Builder()
            .url(teamUrl)
            .get()
            .header("PRIVATE-TOKEN", gitPrivateToken)
            .build()
        OkhttpUtils.doHttp(teamRequest).use {
            val data = it.body()!!.bytes().toString(Charset.forName("GBK"))
            if (!it.isSuccessful) throw RuntimeException("fail to get git file content with: $url($data)")
            projectJsonList.addAll(
                objectMapper.readValue(
                    data,
                    object : TypeReference<List<ActiveProjParseModel>>() {})
            )
        }
        logger.info("total project size is: ${projectJsonList.size}")

        projectJsonList.forEach {
            rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_CODECC_SCAN, ROUTE_GONGFENG_ACTIVE_PROJECT, it)
            Thread.sleep(200)
        }
    }
}
