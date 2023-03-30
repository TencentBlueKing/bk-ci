package com.tencent.devops.repository.service.scm

import com.tencent.devops.common.api.constant.RepositoryMessageCode
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.utils.RepositoryConfigUtils
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.service.CredentialService
import com.tencent.devops.repository.service.RepositoryService
import com.tencent.devops.scm.api.ServiceP4Resource
import com.tencent.devops.scm.code.p4.api.P4FileSpec
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service
import java.net.URLDecoder
import java.net.URLEncoder

@Service
@Primary
class TencentP4Service(
    private val repositoryService: RepositoryService,
    private val credentialService: CredentialService,
    private val client: Client
) : Ip4Service {
    override fun getChangelistFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<P4FileSpec> {
        val (repository, username, password) = getRepositoryInfo(projectId, repositoryId, repositoryType)
        return client.getScm(ServiceP4Resource::class).getChangelistFiles(
            p4Port = repository.url,
            username = username,
            password = URLEncoder.encode(password, "UTF-8"),
            change = change
        ).data!!
    }

    override fun getShelvedFiles(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        change: Int
    ): List<P4FileSpec> {
        val (repository, username, password) = getRepositoryInfo(projectId, repositoryId, repositoryType)
        return client.getScm(ServiceP4Resource::class).getShelvedFiles(
            p4Port = repository.url,
            username = username,
            password = URLEncoder.encode(password, "UTF-8"),
            change = change
        ).data!!
    }

    override fun getFileContent(
        p4Port: String,
        filePath: String,
        reversion: Int,
        username: String,
        password: String
    ): String {
        return client.getScm(ServiceP4Resource::class).getFileContent(
            p4Port = p4Port,
            username = username,
            password = URLEncoder.encode(password, "UTF-8"),
            filePath = filePath,
            reversion = reversion
        ).data!!
    }

    @SuppressWarnings("ThrowsCount")
    private fun getRepositoryInfo(
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?
    ): Triple<Repository, String, String> {
        if (projectId.isBlank()) {
            throw ParamBlankException("Invalid projectId")
        }
        if (repositoryId.isBlank()) {
            throw ParamBlankException("Invalid repositoryHashId")
        }
        val repository = repositoryService.serviceGet(
            projectId = projectId,
            repositoryConfig =
            RepositoryConfigUtils.buildConfig(URLDecoder.decode(repositoryId, "UTF-8"), repositoryType)
        )
        val credentials = credentialService.getCredential(
            projectId = projectId,
            repository = repository
        )
        val username = credentials[0]
        if (username.isEmpty()) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(messageCode = RepositoryMessageCode.USER_NAME_EMPTY,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
            )
        }
        if (credentials.size < 2) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(messageCode = RepositoryMessageCode.PWD_EMPTY,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId())
                )
            )
        }
        val password = credentials[1]
        if (password.isEmpty()) {
            throw OperationException(
                message = I18nUtil.getCodeLanMessage(messageCode = RepositoryMessageCode.PWD_EMPTY,
                    language = I18nUtil.getLanguage(I18nUtil.getRequestUserId()))
            )
        }
        return Triple(repository, username, password)
    }
}
