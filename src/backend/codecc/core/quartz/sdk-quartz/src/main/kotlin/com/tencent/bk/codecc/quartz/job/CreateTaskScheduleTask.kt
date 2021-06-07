package com.tencent.bk.codecc.quartz.job

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.codecc.quartz.pojo.QuartzJobContext
import com.tencent.bk.codecc.task.pojo.GongfengProjPageModel
import com.tencent.bk.codecc.task.pojo.GongfengPublicProjModel
import com.tencent.devops.common.web.mq.EXCHANGE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.web.mq.ROUTE_GONGFENG_CODECC_SCAN
import com.tencent.devops.common.util.OkhttpUtils
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired

class CreateTaskScheduleTask @Autowired constructor(
    private val rabbitTemplate: RabbitTemplate,
    private val objectMapper: ObjectMapper
) : IScheduleTask {

    companion object {
        private val logger = LoggerFactory.getLogger(CreateTaskScheduleTask::class.java)
    }

    override fun executeTask(quartzJobContext: QuartzJobContext) {

        val jobCustomParam = quartzJobContext.jobCustomParam
        if (null == jobCustomParam) {
            logger.info("job custom param is null!")
            return
        }
        val gitCodePath = jobCustomParam["gitCodePath"] as String
        val gitPrivateToken = jobCustomParam["gitPrivateToken"] as String
        val startPage = jobCustomParam["startPage"] as Int
        val endPage = jobCustomParam["endPage"] as Int
        var page = startPage
        var dataSize: Int
        do {
            val url = "$gitCodePath/api/v3/projects/public?per_page=100&page=$page"

            //从工蜂拉取信息，并按分页下发
            val result = OkhttpUtils.doGet(url, mapOf("PRIVATE-TOKEN" to gitPrivateToken))
            if (result.isBlank()) {
                logger.info("null returned from api")
                return
            }
            val gongfengModelList: List<GongfengPublicProjModel> =
                objectMapper.readValue(result, object : TypeReference<List<GongfengPublicProjModel>>() {})
            logger.info("size of json array is: ${gongfengModelList.size}")
            dataSize = gongfengModelList.size
            val gongfengPageModel = GongfengProjPageModel(page, gongfengModelList)
            rabbitTemplate.convertAndSend(EXCHANGE_GONGFENG_CODECC_SCAN, ROUTE_GONGFENG_CODECC_SCAN, gongfengPageModel)
            //每一次线程休息5秒，确保负载正常
            Thread.sleep(3000)
            page++
        } while (dataSize >= 100 && page <= endPage)
    }
}
