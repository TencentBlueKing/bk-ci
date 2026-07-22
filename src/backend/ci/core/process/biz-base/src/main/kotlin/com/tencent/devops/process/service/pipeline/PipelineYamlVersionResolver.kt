package com.tencent.devops.process.service.pipeline

import com.tencent.devops.common.api.constant.HTTP_404
import com.tencent.devops.common.api.constant.HttpStatus
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.enums.BranchVersionAction
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.constant.ProcessMessageCode.ERROR_NOT_FOUND_PIPELINE_VERSION_EXISTS_BY_BRANCH
import com.tencent.devops.process.dao.yaml.PipelineYamlVersionDao
import com.tencent.devops.process.pojo.pipeline.PipelineYamlVersion
import com.tencent.devops.process.service.scm.ScmProxyService
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.credential.AuthRepository
import com.tencent.devops.scm.api.pojo.repository.git.GitScmServerRepository
import com.tencent.devops.scm.utils.code.git.GitUtils
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
            ) ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_TEMPLATE_YAML_FILE_NOT_FOUND,
                params = arrayOf(filePath, finalRef)
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
     *    （不按 ACTIVE 过滤：默认分支删除再创建会把历史版本置为 INACTIVE，过滤后无法触发）
     * 2. 如果不是默认分支
     *      - 仅查找当前分支 ACTIVE 版本；已合入默认分支的应回退到默认分支查找
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
        defaultBranch: String
    ): PipelineYamlVersion? {
        logger.info("get pipeline yaml version|$projectId|$repoHashId|$filePath|$ref|$blobId|$defaultBranch")
        // 默认分支不过滤 ACTIVE,因为默认分支可能存在先删除再创建的场景,删建后历史版本被标 INACTIVE 导致触发失败
        // 非默认分支过滤 ACTIVE,因为非默认分支合入默认分支后,应该触发默认分支对应的版本
        val branchAction = BranchVersionAction.ACTIVE.name.takeIf { ref != defaultBranch }
        val pipelineBranchVersion = pipelineYamlVersionDao.getPipelineYamlVersion(
            dslContext = dslContext,
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = blobId,
            branchAction = branchAction
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
                blobId = blobId
            ) ?: pipelineYamlVersionDao.getPipelineYamlVersion(
                dslContext = dslContext,
                projectId = projectId,
                repoHashId = repoHashId,
                filePath = filePath,
                blobId = blobId
            )
        }
    }

    /**
     * 获取流水线yaml版本
     */
    fun resolvePipelineRefVersion(
        projectId: String,
        repoHashId: String,
        filePath: String,
        ref: String
    ): Int {
        logger.info("resolve pipeline yaml version|$projectId|$repoHashId|$filePath|$ref")
        val repository = client.get(ServiceRepositoryResource::class).get(
            projectId = projectId,
            repositoryId = repoHashId,
            repositoryType = RepositoryType.ID
        ).data ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.GIT_NOT_FOUND,
            params = arrayOf(repoHashId)
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
        val ciDir = filePath.let { it.substring(0, it.indexOfLast { c -> c == '/' }) }
        val repoFileUrl = repoFileUrl(repository.url, ref, ciDir)
        // 这里后续看是否可以改成从T_PIPELINE_YAML_BRANCH_FILE表中获取
        val fileContent = try {
            scmProxyService.getFileContent(
                projectId = projectId,
                ref = ref,
                path = filePath,
                authRepository = AuthRepository(repository)
            )?.takeIf { it.blobId.isNotBlank() }
        } catch (ignored: RemoteServiceException) {
            if (ignored.errorCode == HTTP_404) {
                throw ErrorCodeException(
                    errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_YAML_FILE_NOT_FOUND,
                    params = arrayOf(ref, ciDir, repoFileUrl)
                )
            } else {
                logger.warn(
                    "fail to get file content|$projectId|$repoHashId|$ref|$filePath", ignored
                )
                null
            }
        } ?: throw ErrorCodeException(
            errorCode = ProcessMessageCode.ERROR_PIPELINE_REF_YAML_FILE_NOT_FOUND,
            params = arrayOf(ref, ciDir, repoFileUrl)
        )

        return getPipelineYamlVersion(
            projectId = projectId,
            repoHashId = repoHashId,
            filePath = filePath,
            ref = ref,
            blobId = fileContent.blobId,
            defaultBranch = defaultBranch
        )?.version ?: throw ErrorCodeException(
            errorCode = ERROR_NOT_FOUND_PIPELINE_VERSION_EXISTS_BY_BRANCH,
            params = arrayOf(ref)
        )
    }

    private fun trimRef(branch: String): String {
        return when {
            branch.startsWith("refs/heads/") -> branch.removePrefix("refs/heads/")
            branch.startsWith("refs/tags/") -> branch.removePrefix("refs/tags/")
            else -> branch
        }
    }

    /**
     * 代码源仓库文件链接
     */
    private fun repoFileUrl(
        repoUrl: String,
        branch: String,
        filePath: String
    ): String {
        val (domain, repoName) = GitUtils.getDomainAndRepoName(repoUrl)
        return "https://$domain/$repoName/tree/$branch/$filePath"
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PipelineYamlVersionResolver::class.java)
    }
}
