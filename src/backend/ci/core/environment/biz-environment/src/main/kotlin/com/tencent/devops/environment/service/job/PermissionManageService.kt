package com.tencent.devops.environment.service.job

import com.tencent.devops.environment.dao.job.JobDao
import com.tencent.devops.environment.pojo.job.req.Host
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("PermissionManageService")
class PermissionManageService @Autowired constructor(
    private val dslContext: DSLContext,
    private val jobDao: JobDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(PermissionManageService::class.java)
    }

    fun isJobInsBelongToProj(projectId: String, jobInstanceId: Long): Boolean {
        val jobProjRecord = jobDao.getProjIdFromJobInsIdList(dslContext, projectId, jobInstanceId)
        if (logger.isDebugEnabled) logger.debug("[getProjIdFromJob] jobProjRecord: $jobProjRecord")

        return if (!jobProjRecord.isEmpty()) {
            val projectIdFromTable = jobProjRecord.map { it.projectId }
            projectId == projectIdFromTable[0]
        } else {
            if (logger.isDebugEnabled) logger.debug("[getProjIdFromJob] no record.")
            false
        }
    }

    fun recordJobInsToProj(projectId: String, jobInstanceId: Long, createUser: String) {
        val jobProjRecord = jobDao.addJobProjRecord(dslContext, projectId, jobInstanceId, createUser)
        if (logger.isDebugEnabled) logger.debug("[recordJobInsToProj] jobProjRecord: $jobProjRecord")
    }
}