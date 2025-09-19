package com.tencent.devops.repository.service.github

import org.springframework.stereotype.Service

@Service
class GithubExtService : IGithubExtService {
    override fun webhookCommit(
        event: String,
        guid: String,
        signature: String,
        body: String
    ) = Unit
}
