package com.tencent.devops.remotedev.service.transfer

import com.github.benmanes.caffeine.cache.Caffeine
import com.github.benmanes.caffeine.cache.LoadingCache
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.dao.WorkspaceDao
import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.service.GitTransferService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.TimeUnit

@Service
class RemoteDevGitTransfer @Autowired constructor(
    private val tGitTransferService: TGitTransferService,
    private val githubTransferService: GithubTransferService,
    private val dslContext: DSLContext,
    private val workspaceDao: WorkspaceDao
) {
    companion object {
        val logger = LoggerFactory.getLogger(RemoteDevGitTransfer::class.java)!!
    }

    private val urlCache: LoadingCache<String, RemoteDevGitType?> = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterAccess(30, TimeUnit.MINUTES)
        .build { workspaceName ->
            workspaceDao.fetchAnyWorkspace(dslContext, null, workspaceName)?.url?.let {
                RemoteDevGitType.load4Url(it)
            }
        }

    fun load(
        gitType: RemoteDevGitType
    ): GitTransferService {
        return when (gitType) {
            RemoteDevGitType.GIT -> tGitTransferService
            RemoteDevGitType.GITHUB -> githubTransferService
        }
    }

    fun loadByWorkspace(
        workspaceName: String
    ): GitTransferService {
        return when (urlCache.get(workspaceName)) {
            RemoteDevGitType.GIT -> tGitTransferService
            RemoteDevGitType.GITHUB -> githubTransferService
            null -> {
                logger.info("workspace $workspaceName not find")
                throw ErrorCodeException(
                    errorCode = ErrorCodeEnum.WORKSPACE_NOT_FIND.errorCode,
                    defaultMessage = ErrorCodeEnum.WORKSPACE_NOT_FIND.formatErrorMessage.format(workspaceName)
                )
            }
        }
    }

    fun loadByGitUrl(
        url: String
    ): GitTransferService {
        return when (RemoteDevGitType.load4Url(url)) {
            RemoteDevGitType.GIT -> tGitTransferService
            RemoteDevGitType.GITHUB -> githubTransferService
        }
    }
}
