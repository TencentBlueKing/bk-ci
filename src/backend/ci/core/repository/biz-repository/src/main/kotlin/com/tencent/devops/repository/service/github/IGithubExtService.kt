package com.tencent.devops.repository.service.github

/**
 * github扩展服务
 */
interface IGithubExtService {
    fun webhookCommit(event: String, guid: String, signature: String, body: String)
}
