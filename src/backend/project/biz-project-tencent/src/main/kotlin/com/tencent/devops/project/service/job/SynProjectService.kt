package com.tencent.devops.project.service.job

import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.service.ProjectService
import com.tencent.devops.project.service.tof.TOFService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SynProjectService @Autowired constructor(
        private val projectDao: ProjectDao,
        private val tofService: TOFService,
        private val dslContext: DSLContext
) {

    fun syncCCAppName(): Int {
        logger.info("Sync all the cc app names")
        val projects = projectDao.listCCProjects(dslContext)
        if (projects.isEmpty()) {
            logger.info("All projects are not link to cc")
            return 0
        }

        val ccIds = projects.filter { it.ccAppId > 0 }.map { it.ccAppId }.toSet()
        logger.info("Get the cc ids - ($ccIds)")
        val id2Names = HashMap<Long/*ccAppId*/, String/*ccAppName*/>()
        ccIds.forEach {
            try {
                id2Names[it] = tofService.getCCAppName(it)
            } catch (t: Throwable) {
                logger.warn("Fail to get the cc app name of $it", t)
            }
        }

        val need2UpdateProjects = HashMap<String/*projectId*/, String/*ccAppId*/>()
        projects.forEach {
            val ccAppName = id2Names[it.ccAppId] ?: return@forEach
            if (ccAppName != it.ccAppName) {
                logger.info("The project cc app name need to change from ${it.ccAppName} to $ccAppName")
                need2UpdateProjects[it.projectId] = ccAppName
            }
        }
        if (need2UpdateProjects.isEmpty()) {
            logger.info("All project cc app name is latest, don't need to update")
            return 0
        }
        return projectDao.batchUpdateAppName(dslContext, need2UpdateProjects)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ProjectService::class.java)
        const val ENGLISH_NAME_PATTERN = "[a-z][a-zA-Z0-9-]+"
    }
}