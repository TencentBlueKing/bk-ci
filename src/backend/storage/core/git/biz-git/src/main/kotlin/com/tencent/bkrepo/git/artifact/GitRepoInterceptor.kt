package com.tencent.bkrepo.git.artifact

import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.artifact.constant.PROJECT_ID
import com.tencent.bkrepo.common.artifact.constant.REPO_NAME
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.RepositoryType
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.git.config.GitProperties
import com.tencent.bkrepo.git.constant.GitMessageCode
import com.tencent.bkrepo.git.constant.HubType
import com.tencent.bkrepo.git.constant.PARAMETER_HUBTYPE
import com.tencent.bkrepo.git.constant.PARAMETER_OWNER
import com.tencent.bkrepo.git.constant.DOT_GIT
import com.tencent.bkrepo.git.constant.PATH_SYNC
import com.tencent.bkrepo.git.constant.X_DEVOPS_BUILD_ID
import com.tencent.bkrepo.git.constant.X_DEVOPS_PIPELINE_ID
import com.tencent.bkrepo.repository.api.RepositoryClient
import com.tencent.bkrepo.repository.pojo.repo.RepoCreateRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.HandlerMapping
import java.lang.Exception
import java.lang.IllegalArgumentException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class GitRepoInterceptor : HandlerInterceptor {

    @Autowired
    private lateinit var repositoryClient: RepositoryClient

    @Autowired
    private lateinit var properties: GitProperties

    private val logger = LoggerFactory.getLogger(GitRepoInterceptor::class.java)
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val type = request.getParameter(PARAMETER_HUBTYPE)
        type?.let {
            try {
                val domain = properties.getDomain(HubType.valueOf(type.toUpperCase())) ?: let {
                    throw ErrorCodeException(GitMessageCode.GIT_HUB_TYPE_NOT_SUPPORT, type)
                }
                val owner = request.getParameter(PARAMETER_OWNER)
                val uriAttribute = request.getAttribute(
                    HandlerMapping
                        .URI_TEMPLATE_VARIABLES_ATTRIBUTE
                ) as MutableMap<Any, Any>
                val repoName = uriAttribute[REPO_NAME].toString()
                val realRepoName = "${type}_${owner}_$repoName"
                val projectId = uriAttribute[PROJECT_ID].toString()
                val uri = "$domain$owner/$repoName$DOT_GIT"
                uriAttribute[REPO_NAME] = realRepoName

                if (!request.requestURI.endsWith(PATH_SYNC)) return true
                logger.info(
                    "receive $X_DEVOPS_BUILD_ID:${request.getHeader(X_DEVOPS_BUILD_ID)} " +
                        "$X_DEVOPS_PIPELINE_ID:${request.getHeader(X_DEVOPS_PIPELINE_ID)} " +
                        "sync request $uri"
                )
                repositoryClient.getRepoDetail(projectId, realRepoName).data ?: let {
                    val req = RepoCreateRequest(
                        projectId = projectId,
                        name = realRepoName,
                        category = RepositoryCategory.REMOTE,
                        type = RepositoryType.GIT,
                        public = false,
                        storageCredentialsKey = properties.storageCredentialsKey,
                        configuration = RemoteConfiguration(
                            url = uri
                        )
                    )
                    repositoryClient.createRepo(req)
                    logger.info("create projectId $projectId repo $realRepoName")
                }
            } catch (e: IllegalArgumentException) {
                throw ErrorCodeException(GitMessageCode.GIT_HUB_TYPE_NOT_SUPPORT, type)
            } catch (e: Exception) {
                logger.error("pre handle error", e)
                return false
            }
        }
        return true
    }
}
