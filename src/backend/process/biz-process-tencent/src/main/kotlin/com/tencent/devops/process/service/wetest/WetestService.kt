package com.tencent.devops.process.service.wetest

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.devops.process.dao.third.ThirdJobDao
import com.tencent.devops.process.pojo.third.enum.JobType
import com.tencent.devops.process.pojo.third.wetest.WetestCallback
import com.tencent.devops.process.pojo.third.wetest.WetestResponse
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class WetestService @Autowired constructor(
    private val objectMapper: ObjectMapper,
    private val dslContext: DSLContext,
    private val thirdJobDao: ThirdJobDao
) {

    companion object {
        private val logger = LoggerFactory.getLogger(WetestService::class.java)
    }

    fun saveCallback(callback: WetestCallback) {
        val str = objectMapper.writeValueAsString(callback)
        logger.info("JinGangAppCallback: $str")
        thirdJobDao.saveCallback(dslContext, callback.taskID, JobType.WETEST, str)
    }

    fun saveResponse(response: WetestResponse, projectId: String, pipelineId: String, buildId: String) {
        val str = objectMapper.writeValueAsString(response)
        if (response.taskId.isNullOrBlank()) throw RuntimeException("task id is null")
        thirdJobDao.saveResponse(dslContext, response.taskId!!, str, JobType.WETEST, projectId, pipelineId, buildId)
    }
}
