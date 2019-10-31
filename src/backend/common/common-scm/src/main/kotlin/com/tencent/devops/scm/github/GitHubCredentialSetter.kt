package com.tencent.devops.scm.github

import com.tencent.devops.scm.code.git.api.GitCredentialSetter
import org.eclipse.jgit.api.TransportCommand
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

class GitHubCredentialSetter constructor(private val token: String?) : GitCredentialSetter {
    override fun setGitCredential(command: TransportCommand<*, *>) {
        if (token.isNullOrBlank()) { // 兼容Github App实现，不用Private token凭证的方式
            return
        }
        command.setCredentialsProvider(UsernamePasswordCredentialsProvider("PRIVATE-TOKEN", token))
    }
}
