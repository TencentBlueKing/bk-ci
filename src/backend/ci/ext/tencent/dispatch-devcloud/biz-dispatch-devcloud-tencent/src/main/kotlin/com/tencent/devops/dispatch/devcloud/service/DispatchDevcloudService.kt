package com.tencent.devops.dispatch.devcloud.service

import com.tencent.devops.dispatch.devcloud.dao.BuildContainerPoolNoDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DispatchDevcloudService @Autowired constructor(
    private val dslContext: DSLContext,
    private val buildContainerPoolNoDao: BuildContainerPoolNoDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(DispatchDevcloudService::class.java)
    }

    fun needRetry(
        buildId: String,
        vmSeqId: String,
        executeCount: Int?
    ): Boolean {
        // 当前流水线job还在调度中时，收到相同的启动事件，放入延迟队列重新消费
        val buildContainerPoolNo = buildContainerPoolNoDao.getDevCloudBuildLastPoolNo(
            dslContext,
            buildId,
            vmSeqId,
            executeCount ?: 1
        )
        logger.info("$buildId|$vmSeqId|$executeCount buildContainerPoolNo: $buildContainerPoolNo")
        return buildContainerPoolNo.isNotEmpty() && buildContainerPoolNo[0].second != null
    }
}
