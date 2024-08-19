package com.tencent.devops.remotedev.service.transfer

import com.tencent.devops.remotedev.pojo.RemoteDevGitType
import com.tencent.devops.remotedev.service.IGitTransferService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class RemoteDevGitTransfer @Autowired constructor(
    private val gitTransferService: GitTransferService,
    private val githubTransferService: GithubTransferService,
    private val tGitTransferService: TGitTransferService
) {
    companion object {
        val logger = LoggerFactory.getLogger(RemoteDevGitTransfer::class.java)!!
    }

    fun load(
        gitType: RemoteDevGitType
    ): IGitTransferService {
        return when (gitType) {
            RemoteDevGitType.GIT -> gitTransferService
            RemoteDevGitType.GITHUB -> githubTransferService
            RemoteDevGitType.T_GIT -> tGitTransferService
        }
    }
}
