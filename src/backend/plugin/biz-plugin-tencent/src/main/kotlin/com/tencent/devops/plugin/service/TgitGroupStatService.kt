package com.tencent.devops.plugin.service

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.plugin.api.pojo.GitGroupStatRequest
import com.tencent.devops.plugin.dao.TgitGroupStatDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TgitGroupStatService @Autowired constructor(
    private val tgitGroupStatDao: TgitGroupStatDao,
    private val dslContext: DSLContext
) {
    companion object {
        private val logger = LoggerFactory.getLogger(TgitGroupStatService::class.java)
    }

    /**
     * 上报git项目组统计数据
     */
    fun reportGitGroupStat(
        group: String,
        gitGroupStatRequest: GitGroupStatRequest
    ): Result<Boolean> {
        logger.info("gitGroupStatRequest: $gitGroupStatRequest")
        val latestRecord = tgitGroupStatDao.getLatestRecord(dslContext, group, gitGroupStatRequest.statDate)
        logger.info("latestRecord: $latestRecord")
        if (latestRecord != null) {
            val latestCommitCount = latestRecord["COMMIT_COUNT"] as Int
            val latestCommitCountOpen = latestRecord["COMMIT_COUNT_OPEN"] as Int
            val latestProjectCount = latestRecord["PROJECT_COUNT"] as Int
            val latestProjectCountOpen = latestRecord["PROJECT_COUNT_OPEN"] as Int
            val latestUserCount = latestRecord["USER_COUNT"] as Int
            val latestUserCountOpen = latestRecord["USER_COUNT_OPEN"] as Int
            gitGroupStatRequest.commitIncre = gitGroupStatRequest.commitCount - latestCommitCount
            gitGroupStatRequest.commitIncreOpen = gitGroupStatRequest.commitCountOpen - latestCommitCountOpen
            gitGroupStatRequest.projectIncre = gitGroupStatRequest.projectCount - latestProjectCount
            gitGroupStatRequest.projectIncreOpen = gitGroupStatRequest.projectCountOpen - latestProjectCountOpen
            gitGroupStatRequest.userIncre = gitGroupStatRequest.userCount - latestUserCount
            gitGroupStatRequest.userIncreOpen = gitGroupStatRequest.userCountOpen - latestUserCountOpen
        } else {
            gitGroupStatRequest.commitIncre = 0
            gitGroupStatRequest.commitIncreOpen = 0
            gitGroupStatRequest.projectIncre = 0
            gitGroupStatRequest.projectIncreOpen = 0
            gitGroupStatRequest.userIncre = 0
            gitGroupStatRequest.userIncreOpen = 0
        }

        logger.info("gitGroupStat: $gitGroupStatRequest")
        tgitGroupStatDao.createOrUpdate(dslContext = dslContext, group = group, gitGroupStatRequest = gitGroupStatRequest)

        return Result(true)
    }
}