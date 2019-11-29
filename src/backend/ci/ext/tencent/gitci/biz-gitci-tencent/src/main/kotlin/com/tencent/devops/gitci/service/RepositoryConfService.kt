package com.tencent.devops.gitci.service

import com.tencent.devops.common.client.Client
import com.tencent.devops.gitci.dao.GitCISettingDao
import com.tencent.devops.gitci.pojo.GitRepositoryConf
import com.tencent.devops.project.api.service.service.ServiceTxProjectResource
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val client: Client,
    private val gitCISettingDao: GitCISettingDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryConfService::class.java)
    }

    fun enableGitCI(gitProjectId: Long): Boolean {
        if (gitCISettingDao.getSetting(dslContext, gitProjectId) == null) {
            logger.info("git repo not exists.")
            return false
        }

        gitCISettingDao.enableGitCI(dslContext, gitProjectId, true)
        return true
    }

    fun disableGitCI(gitProjectId: Long): Boolean {
        if (gitCISettingDao.getSetting(dslContext, gitProjectId) == null) {
            logger.info("git repo not exists.")
            return false
        }
        gitCISettingDao.enableGitCI(dslContext, gitProjectId, false)
        return true
    }

    fun getGitCIConf(gitProjectId: Long): GitRepositoryConf? {
        return gitCISettingDao.getSetting(dslContext, gitProjectId)
    }

    fun saveGitCIConf(userId: String, repositoryConf: GitRepositoryConf): Boolean {
        logger.info("save git ci conf, repositoryConf: $repositoryConf")
        val gitRepoConf = gitCISettingDao.getSetting(dslContext, repositoryConf.gitProjectId)
        val projectCode = if (gitRepoConf?.projectCode == null) {
            val projectResult = client.get(ServiceTxProjectResource::class).createGitCIProject(repositoryConf.gitProjectId, userId)
            if (projectResult.isNotOk()) {
                throw RuntimeException("Create git ci project in devops failed, msg: ${projectResult.message}")
            }
            projectResult.data!!.projectCode
        } else {
            gitRepoConf.projectCode
        }

        gitCISettingDao.saveSetting(dslContext, repositoryConf, projectCode!!)
        return true
    }
}