package com.tencent.devops.project.service.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.gray.Gray
import com.tencent.devops.project.dao.ProjectDao
import com.tencent.devops.project.jmx.api.ProjectJmxApi
import com.tencent.devops.project.pojo.*
import com.tencent.devops.project.pojo.enums.ProjectValidateType
import com.tencent.devops.project.service.ProjectPermissionService
import org.glassfish.jersey.media.multipart.FormDataContentDisposition
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class ProjectServiceImpl @Autowired constructor(
        private val projectPermissionService: ProjectPermissionService,
        private val dslContext: DSLContext,
        private val projectDao: ProjectDao,
        private val projectJmxApi: ProjectJmxApi,
        private val redisOperation: RedisOperation,
        private val gray: Gray,
        private val client: Client
): AbsProjectServiceImpl(projectPermissionService, dslContext, projectDao, projectJmxApi, redisOperation, gray, client){
    override fun validate(validateType: ProjectValidateType, name: String, projectId: String?) {
        super.validate(validateType, name, projectId)
    }

    override fun create(userId: String, projectCreateInfo: ProjectCreateInfo): String {
        return super.create(userId, projectCreateInfo)
    }

    override fun getByEnglishName(englishName: String): ProjectVO? {
        return super.getByEnglishName(englishName)
    }

    override fun update(userId: String, projectId: String, projectUpdateInfo: ProjectUpdateInfo): Boolean {
        return super.update(userId, projectId, projectUpdateInfo)
    }

    override fun list(userId: String): List<ProjectVO> {
        return super.list(userId)
    }

    override fun list(projectCodes: Set<String>): List<ProjectVO> {
        return super.list(projectCodes)
    }

    override fun getAllProject(): List<ProjectVO> {
        return super.getAllProject()
    }

    override fun getProjectByUser(userName: String): List<ProjectVO> {
        return super.getProjectByUser(userName)
    }

    override fun getNameByCode(projectCodes: String): HashMap<String, String> {
        return super.getNameByCode(projectCodes)
    }

    override fun grayProjectSet(): Set<String> {
        return super.grayProjectSet()
    }

    override fun updateLogo(userId: String, projectId: String, inputStream: InputStream, disposition: FormDataContentDisposition): Result<Boolean> {
        return super.updateLogo(userId, projectId, inputStream, disposition)
    }


    override fun updateUsableStatus(userId: String, projectId: String, enabled: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun createGitCIProject(userId: String, gitProjectId: Long): ProjectVO {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}