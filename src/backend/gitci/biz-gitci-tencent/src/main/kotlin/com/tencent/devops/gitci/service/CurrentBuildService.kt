package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.pojo.GitCIModelDetail
import com.tencent.devops.process.api.service.ServiceBuildResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class CurrentBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(CurrentBuildService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getLatestBuildDetail(userId: String, gitProjectId: Long): GitCIModelDetail? {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")
        val eventBuildRecord = gitRequestEventBuildDao.getLatestBuild(dslContext, gitProjectId) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(userId, conf.projectCode!!, eventBuildRecord.pipelineId, eventBuildRecord.buildId, channelCode).data!!

        return GitCIModelDetail(eventRecord!!, modelDetail)
    }

    fun getBuildDetail(userId: String, gitProjectId: Long, buildId: String): GitCIModelDetail? {
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")
        val eventBuildRecord = gitRequestEventBuildDao.getByBuildId(dslContext, buildId) ?: return null
        val eventRecord = gitRequestEventDao.get(dslContext, eventBuildRecord.eventId)
        val modelDetail = client.get(ServiceBuildResource::class).getBuildDetail(userId, conf.projectCode!!, eventBuildRecord.pipelineId, buildId, channelCode).data!!

        return GitCIModelDetail(eventRecord!!, modelDetail)
    }
}
