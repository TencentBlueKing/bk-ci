package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.dao.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线yaml引用解析器,用于将yaml文件中的引用解析成具体的yaml版本
 */
@Service
class PipelineYamlRefResolver @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val client: Client,
    private val scmProxyService: ScmProxyService
) {
    /**
     * @param ref 引用分支,如果为空,则使用默认分支
     */
    fun resolvePipelineYamlVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String? = null
    ): PipelineYamlVersion {
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_YAML_REPOSITORY_NOT_FOUND
        )

        val authRepository = AuthRepository(repository)
        val serverRepository = scmProxyService.getServerRepository(
            projectId = projectId,
            authRepository = authRepository
        )
        if (serverRepository !is GitScmServerRepository) {
            throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_NOT_SUPPORT_REPOSITORY_TYPE_ENABLE_PAC
            )
        }
        val defaultBranch = serverRepository.defaultBranch!!
        val finalRef = ref ?: defaultBranch
        // 这里后续看是否可以改成从T_PIPELINE_YAML_BRANCH_FILE表中获取
        val fileContent = scmProxyService.getFileContent(
            projectId = projectId,
            path = filePath,
            ref = finalRef,
            authRepository = authRepository
        )
        return getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = finalRef,
            blobId = fileContent.blobId,
            defaultBranch = defaultBranch
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_YAML_VERSION_NOT_FOUND,
            params = arrayOf(finalRef!!, filePath)
        )
    }

    /**
     * 获取触发时版本
     *
     * 1. 查看触发分支有没有blobId对应的版本
     * 2. 如果触发分支没有,则查看默认分支
     * 3. 如果默认分支也没有,则查看其他分支是不是有对应的版本
     */
    private fun getPipelineYamlVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String
    ): PipelineYamlVersion? {
        val pipelineBranchVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = blobId,
            branchAction = BranchVersionAction.ACTIVE.name
        )
        return if (ref == defaultBranch) {
            pipelineBranchVersion
        } else {
            pipelineBranchVersion ?: pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                ref = defaultBranch,
                blobId = blobId,
                branchAction = BranchVersionAction.ACTIVE.name
            ) ?: pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId
            )
        }
    }
}
