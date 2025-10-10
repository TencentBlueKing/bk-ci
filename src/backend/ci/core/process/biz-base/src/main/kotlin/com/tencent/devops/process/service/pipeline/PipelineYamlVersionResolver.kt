package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.dao.yaml.PipelineYamlDependencyDao
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 流水线yaml引用解析器,用于将yaml文件中的引用解析成具体的yaml版本
 */
@Service
class PipelineYamlVersionResolver @Autowired constructor(
    private val dslContext: DSLContext,
    private val pipelineYamlVersionDao: PipelineYamlVersionDao,
    private val pipelineYamlDependencyDao: PipelineYamlDependencyDao,
    private val client: Client,
    private val scmProxyService: ScmProxyService
) {
    /**
     * 获取模版ref对应的模版版本
     *
     * @param ref 引用分支,如果为空,则使用默认分支
     */
    fun resolveTemplateRefVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String? = null
    ): PipelineYamlVersion {
        logger.info("resolve pipeline yaml version|$projectId|$repoHashId|$filePath|$ref")
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
        val finalRef = ref?.let { trimRef(it) } ?: defaultBranch
        // 这里后续看是否可以改成从T_PIPELINE_YAML_BRANCH_FILE表中获取
        val fileContent = try {
            scmProxyService.getFileContent(
                projectId = projectId,
                path = filePath,
                ref = finalRef,
                authRepository = authRepository
            )
        } catch (exception: RemoteServiceException) {
            if (exception.httpStatus == HttpStatus.NOT_FOUND.value) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_TEMPLATE_YAML_FILE_NOT_FOUND,
                    params = arrayOf(filePath, finalRef)
                )
            }
            throw exception
        }

        return getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = finalRef,
            blobId = fileContent.blobId,
            defaultBranch = defaultBranch
        ) ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_TEMPLATE_YAML_VERSION_NOT_FOUND,
            params = arrayOf(finalRef, filePath)
        )
    }

    /**
     * 获取触发时版本
     *
     * 1. 如果是默认分支,则查找当前文件blob_id在默认分支对应的版本
     * 2. 如果不是默认分支
     *      - 查找blob_id在当前分支是否存在对应的版本
     *      - 如果当前分支不存在,则查找是否在默认分支存在
     *      - 如果默认分支也不存在,则查找所有的blob_id对应的版本,
     *           这种情况出现在分支a,修改了文件,分支b从分支a拉出,后面分支a又做了修改,分支a合入默认分支后
     */
    fun getPipelineYamlVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String,
        dependentFilePath: String? = null,
        dependentBlobId: String? = null
    ): PipelineYamlVersion? {
        logger.info("get pipeline yaml version|$projectId|$repoHashId|$filePath|$ref|$blobId|$defaultBranch")
        val pipelineBranchVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = blobId,
            branchAction = BranchVersionAction.ACTIVE.name,
            dependentFilePath = dependentFilePath,
            dependentBlobId = dependentBlobId
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
                branchAction = BranchVersionAction.ACTIVE.name,
                dependentFilePath = dependentFilePath,
                dependentBlobId = dependentBlobId
            ) ?: pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId,
                dependentFilePath = dependentFilePath,
                dependentBlobId = dependentBlobId
            )
        }
    }

    /**
     * 获取触发时版本
     */
    fun getTriggerVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String,
        blobId: String,
        defaultBranch: String,
        authRepository: AuthRepository
    ): PipelineYamlVersion? {
        val dependency = pipelineYamlDependencyDao.getDependency(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = blobId
        )
        val dependentBlobId = if (dependency != null) {
            // 依赖的分支为空,则是用当前默认的分支
            val finalRef = if (dependency.dependentRef == DEFAULT_DEPENDENT_REF) {
                ref
            } else {
                dependency.dependentRef
            }
            // 这里后续看是否可以改成从T_PIPELINE_YAML_BRANCH_FILE表中获取
            scmProxyService.getFileContent(
                projectId = projectId,
                path = dependency.dependentFilePath,
                ref = finalRef,
                authRepository = authRepository
            ).blobId
        } else {
            null
        }
        return getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = blobId,
            defaultBranch = defaultBranch,
            dependentFilePath = dependency?.dependentFilePath,
            dependentBlobId = dependentBlobId
        )
    }

    private fun trimRef(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlVersionResolver::class.java)
        private const val DEFAULT_DEPENDENT_REF = "*"
    }
}
