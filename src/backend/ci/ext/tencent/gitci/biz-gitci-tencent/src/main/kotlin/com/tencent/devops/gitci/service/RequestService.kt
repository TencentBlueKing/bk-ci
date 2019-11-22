package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.pojo.BuildHistoryPage
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.dao.GitRequestEventBuildDao
import com.tencent.devops.gitci.dao.GitRequestEventDao
import com.tencent.devops.gitci.dao.GitRequestEventNotBuildDao
import com.tencent.devops.gitci.pojo.GitCIBuildHistory
import com.tencent.devops.gitci.pojo.enums.TriggerReason
import com.tencent.devops.process.api.service.ServiceBuildResource
import com.tencent.devops.process.pojo.BuildHistory
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class RequestService @Autowired constructor(
    private val client: Client,
    private val dslContext: DSLContext,
    private val gitCISettingDao: GitCISettingDao,
    private val gitRequestEventDao: GitRequestEventDao,
    private val gitRequestEventBuildDao: GitRequestEventBuildDao,
    private val gitRequestEventNotBuildDao: GitRequestEventNotBuildDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RequestService::class.java)
    }

    private val channelCode = ChannelCode.GIT

    fun getRequestList(userId: String, gitProjectId: Long, page: Int?, pageSize: Int?): BuildHistoryPage<GitCIBuildHistory> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 10
        logger.info("get request list, gitProjectId: $gitProjectId")
        val gitProjectConf = gitCISettingDao.getSetting(dslContext, gitProjectId)
                ?: throw CustomException(Response.Status.FORBIDDEN, "项目未开启工蜂CI，无法查询")

        val count = gitRequestEventDao.getRequestCount(dslContext, gitProjectId)
        val gitRequestList = gitRequestEventDao.getRequestList(dslContext, gitProjectId, pageNotNull, pageSizeNotNull)
        if (count == 0L || gitRequestList.isEmpty()) {
            logger.info("no record, gitProjectId: $gitProjectId")
            return BuildHistoryPage(
                    pageNotNull,
                    pageSizeNotNull,
                    0,
                    emptyList(),
                    false,
                    0
            )
        }

        val eventIds = gitRequestList.map { it.id!! }.toSet()
        val gitRequestBuildList = gitRequestEventBuildDao.getByEventIds(dslContext, eventIds)
        if (gitRequestBuildList.isEmpty()) {
            logger.info("no build record, gitProjectId: $gitProjectId")
            val records = mutableListOf<GitCIBuildHistory>()
            gitRequestList.forEach {
                val notBuildRecord = gitRequestEventNotBuildDao.getByEventId(dslContext, it.id!!)
                it.description = notBuildRecord?.reason
                records.add(GitCIBuildHistory(it, null))
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

        val builds = gitRequestBuildList.map { it.buildId }.toSet()
        logger.info("get history build list, build ids: $builds")
        val buildHistoryList = client.get(ServiceBuildResource::class).getBatchBuildStatus(gitProjectConf.projectCode!!, builds, channelCode).data
        if (null == buildHistoryList) {
            logger.info("Get branch build history list return empty, gitProjectId: $gitProjectId")
            val records = mutableListOf<GitCIBuildHistory>()
            gitRequestList.forEach {
                val notBuildRecord = gitRequestEventNotBuildDao.getByEventId(dslContext, it.id!!)
                it.description = notBuildRecord?.reason
                records.add(GitCIBuildHistory(it, null))
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

        val records = mutableListOf<GitCIBuildHistory>()
        gitRequestList.forEach {
            val gitRequestEventBuild = gitRequestEventBuildDao.getByEventId(dslContext, it.id!!)
            if (null == gitRequestEventBuild) {
                val notBuildRecord = gitRequestEventNotBuildDao.getByEventId(dslContext, it.id!!)
                it.description = notBuildRecord?.reason
                records.add(GitCIBuildHistory(it, null))
            } else {
                val buildHistory = getBuildHistory(gitRequestEventBuild.buildId, buildHistoryList)
                it.description = TriggerReason.TRIGGER_SUCCESS.name
                records.add(GitCIBuildHistory(it, buildHistory))
            }
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
