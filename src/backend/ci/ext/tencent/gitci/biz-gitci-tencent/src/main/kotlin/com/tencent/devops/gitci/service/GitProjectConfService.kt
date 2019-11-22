package com.tencent.devops.gitci.service

import com.tencent.devops.common.api.exception.CustomException
import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.gitci.dao.GitProjectConfDao
import com.tencent.devops.gitci.pojo.GitProjectConf
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.ws.rs.core.Response

@Service
class GitProjectConfService @Autowired constructor(
    private val dslContext: DSLContext,
    private val gitProjectConfDao: GitProjectConfDao
) {
    companion object {
        private val logger = LoggerFactory.getLogger(GitProjectConfService::class.java)
    }

    fun create(gitProjectId: Long, name: String, url: String, enable: Boolean): Boolean {
        logger.info("Create git project, id: $gitProjectId, name: $name, url: $url, enable: $enable")
        val record = gitProjectConfDao.get(dslContext, gitProjectId)
        if (null != record) {
            throw CustomException(Response.Status.BAD_REQUEST, "项目已存在")
        }
        gitProjectConfDao.create(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun update(gitProjectId: Long, name: String?, url: String?, enable: Boolean?): Boolean {
        logger.info("update git project, id: $gitProjectId, name: $name, url: $url, enable: $enable")
        gitProjectConfDao.get(dslContext, gitProjectId) ?: throw CustomException(Response.Status.BAD_REQUEST, "项目不存在")
        gitProjectConfDao.update(dslContext, gitProjectId, name, url, enable)
        return true
    }

    fun delete(gitProjectId: Long): Boolean {
        logger.info("Delete git project, id: $gitProjectId")
        gitProjectConfDao.delete(dslContext, gitProjectId)
        return true
    }

    fun list(gitProjectId: Long?, name: String?, url: String?, page: Int, pageSize: Int): List<GitProjectConf> {
        return gitProjectConfDao.getList(dslContext, gitProjectId, name, url, page, pageSize).map {
            GitProjectConf(
                    it.id,
                    it.name,
                    it.url,
                    it.enable,
                    it.createTime.timestamp(),
                    it.updateTime.timestamp()
            )
        }
    }

    fun count(gitProjectId: Long?, name: String?, url: String?): Int {
        return gitProjectConfDao.count(dslContext, gitProjectId, name, url)
    }

    fun isEnable(gitProjectId: Long): Boolean {
        val record = gitProjectConfDao.get(dslContext, gitProjectId)
        if (null != record && record.enable) {
            return true
        }
        return false
    }
}