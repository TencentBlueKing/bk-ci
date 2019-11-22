package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.gitci.dao.GitCIServicesConfDao
import com.tencent.devops.gitci.pojo.GitCIServicesConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter
import javax.ws.rs.core.Response

@Service
class GitCIServicesConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitCIServicesConfDao: GitCIServicesConfDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitCIServicesConfService::class.java)
    }

    fun create(userId: String, gitCIServicesConf: GitCIServicesConf): Boolean {
        logger.info("Create git service, user: $userId, gitCIServicesConf: $gitCIServicesConf")
        try {
            gitCIServicesConfDao.create(
                dslContext,
                gitCIServicesConf.imageName,
                gitCIServicesConf.imageTag,
                gitCIServicesConf.repoUrl,
                gitCIServicesConf.repoUsername,
                gitCIServicesConf.repoPwd,
                gitCIServicesConf.enable,
                gitCIServicesConf.env,
                gitCIServicesConf.createUser,
                gitCIServicesConf.updateUser)
            return true
        } catch (e: Exception) {
            logger.error("Create git service failed. ${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Create git service failed.")
        }
    }

    fun update(userId: String, id: Long, enable: Boolean?): Boolean {
        logger.info("update git ci service, user: $userId, id: $id")
        try {
            gitCIServicesConfDao.update(dslContext, id, userId, enable)
            return true
        } catch (e: Exception) {
            logger.error("update git service failed. ${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "update git service failed.")
        }
    }

    fun delete(userId: String, id: Long): Boolean {
        logger.info("Delete git ci service, user: $userId, id: $id")
        try {
            gitCIServicesConfDao.delete(dslContext, id)
            return true
        } catch (e: Exception) {
            logger.error("Delete git service failed. ${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "Delete git service failed.")
        }
    }

    fun list(userId: String): List<GitCIServicesConf> {
        try {
            val list = gitCIServicesConfDao.list(dslContext)
            val resultList = mutableListOf<GitCIServicesConf>()
            list.forEach {
                resultList.add(
                    GitCIServicesConf(
                        it.id,
                        it.imageName,
                        it.imageTag,
                        it.repoUrl,
                        it.repoUsername,
                        it.repoPwd,
                        it.enable,
                        it.env,
                        it.createUser,
                        it.updateUser,
                        it.gmtCreate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                        it.gmtModified.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    )
                )
            }
            return resultList
        } catch (e: Exception) {
            logger.error("List git service failed. ${e.message}")
            throw CustomException(Response.Status.INTERNAL_SERVER_ERROR, "List git service failed.")
        }
    }
}