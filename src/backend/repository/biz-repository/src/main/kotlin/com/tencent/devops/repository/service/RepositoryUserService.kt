package com.tencent.devops.repository.service

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.repository.dao.RepositoryCodeGitDao
import com.tencent.devops.repository.dao.RepositoryCodeGitLabDao
import com.tencent.devops.repository.dao.RepositoryCodeSvnDao
import com.tencent.devops.repository.dao.RepositoryDao
import com.tencent.devops.repository.dao.RepositoryGithubDao
import com.tencent.devops.repository.pojo.UpdateRepositoryInfoRequest
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RepositoryUserService @Autowired constructor(
    private val repositoryDao: RepositoryDao,
    private val repositoryCodeSvnDao: RepositoryCodeSvnDao,
    private val repositoryCodeGitDao: RepositoryCodeGitDao,
    private val repositoryCodeGitLabDao: RepositoryCodeGitLabDao,
    private val repositoryGithubDao: RepositoryGithubDao,
    private val dslContext: DSLContext
) {

    companion object {
        private val logger = LoggerFactory.getLogger(RepositoryUserService::class.java)
    }

    /**
     * 更改代码库的用户信息
     * @param userId 用户ID
     * @param projectCode 项目代码
     * @param repositoryHashId 代码库HashId
     */
    fun updateRepositoryUserInfo(userId: String, projectCode: String, repositoryHashId: String): Result<Boolean> {
        logger.info("updateRepositoryUserInfo userId is:$userId,projectCode is:$projectCode,repositoryHashId is:$repositoryHashId")
        val repositoryId = HashUtil.decodeOtherIdToLong(repositoryHashId)
        val repositoryRecord = repositoryDao.get(dslContext, repositoryId, projectCode)
        logger.info("updateRepositoryUserInfo repositoryRecord is:$repositoryRecord")
        when (repositoryRecord.type) {
            ScmType.CODE_SVN.name -> {
                repositoryCodeSvnDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_GIT.name -> {
                repositoryCodeGitDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_TGIT.name -> {
                repositoryCodeGitDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.CODE_GITLAB.name -> {
                repositoryCodeGitLabDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            ScmType.GITHUB.name -> {
                repositoryGithubDao.updateRepositoryInfo(dslContext, repositoryId, UpdateRepositoryInfoRequest(userId))
            }
            else -> {
            }
        }
        return Result(true)
    }
}