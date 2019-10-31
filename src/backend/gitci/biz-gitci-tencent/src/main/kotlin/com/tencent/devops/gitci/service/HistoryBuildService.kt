package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class HistoryBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitCISettingDao: GitCISettingDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(HistoryBuildService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getHistoryBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): BuildHistoryPage<GitCIBuildHistory> {
        logger.info("get history build list, gitProjectId: $gitProjectId")
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")

        val count = gitRequestEventBuildDao.getRequestEventBuildCount(dslContext, gitProjectId)
        val gitRequestBuildList = gitRequestEventBuildDao.getRequestEventBuildList(dslContext, gitProjectId, pageNotNull, pageSizeNotNull)
        val builds = gitRequestBuildList.map { it.buildId }.toSet()
        logger.info("get history build list, build ids: $builds")
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(conf.projectCode!!, builds, channelCode).data
        if (null == buildHistoryList) {
            logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            return BuildHistoryPage(
                    pageNotNull,
                    pageSizeNotNull,
                    0,
                    emptyList(),
                    false,
                    0
            )
        }

        val records = mutableListOf<GitCIBuildHistory>()
        gitRequestBuildList.forEach {
            val gitRequestEvent = gitRequestEventDao.get(dslContext, it.eventId)
            val buildHistory = getBuildHistory(it.buildId, buildHistoryList)
            records.add(GitCIBuildHistory(gitRequestEvent!!, buildHistory))
        }

        return BuildHistoryPage(
                pageNotNull,
                pageSizeNotNull,
                count,
                records,
                false,
                0
        )
    }

    private fun getBuildHistory(buildId: String, buildHistoryList: List<BuildHistory>): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildId) {
                return it
            }
        }
        return null
    }
}
