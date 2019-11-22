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
class MergeBuildService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventDao: GitRequestEventDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(MergeBuildService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getMergeBuildList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): BuildHistoryPage<GitCIBuildHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get merge build list, gitProjectId: $gitProjectId")
        val conf = gitCISettingDao.getSetting(dslContext, gitProjectId) ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")

        val count = gitRequestEventBuildDao.getMergeRequestBuildCount(dslContext, gitProjectId)
        val mergeBuildsList = gitRequestEventBuildDao.getMergeRequestBuildList(dslContext, gitProjectId, pageNotNull, pageSizeNotNull)
        if (mergeBuildsList.isEmpty() || count == 0L) {
            logger.info("Get branch build list return empty, gitProjectId: $gitProjectId")
            return BuildHistoryPage(
                    pageNotNull,
                    pageSizeNotNull,
                    0,
                    emptyList(),
                    false,
                    0
            )
        }
        logger.info("Get merge build list mergeBuildsList: $mergeBuildsList, gitProjectId: $gitProjectId")
        val builds = mergeBuildsList.map { it.buildId }.toSet()
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
        logger.info("Get merge build history list buildHistoryList: $buildHistoryList, gitProjectId: $gitProjectId")
        val records = mutableListOf<GitCIBuildHistory>()
        mergeBuildsList.forEach {
            val history = getBuildHistory(buildHistoryList, it.buildId)
            val gitRequestBuildEvent = gitRequestEventBuildDao.getByBuildId(dslContext, it.buildId)
            val gitRequestEvent = gitRequestEventDao.get(dslContext, gitRequestBuildEvent!!.eventId)

            records.add(GitCIBuildHistory(gitRequestEvent!!, history))
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

    private fun getBuildHistory(buildHistoryList: List<BuildHistory>, buildIdIt: String): BuildHistory? {
        buildHistoryList.forEach {
            if (it.id == buildIdIt) {
                return it
            }
        }
        return null
    }
}
