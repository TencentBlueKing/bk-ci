package com.tencent.devops.process.yaml.actions.tgit

import com.tencent.devops.process.yaml.actions.GitBaseAction
import com.tencent.devops.process.yaml.actions.data.ActionData
import com.tencent.devops.process.yaml.git.pojo.tgit.TGitCred
import com.tencent.devops.process.yaml.git.service.TGitApiService

/**
 * 对于stream 平台级功能的具体实现，不需要下放到具体的event
 * 对于只有一两个事件实现的，也可在平台级实现一个通用的，一两个再自己重写
 */
abstract class TGitActionGit(
    override val api: TGitApiService
) : GitBaseAction {
    override lateinit var data: ActionData

    /**
     * 提供拿取gitProjectId的公共方法
     * 因为会存在跨库触发导致的event的gitProjectId和触发的不一致的问题
     * 所以会优先拿取pipeline的gitProjectId
     */
    override fun getGitProjectIdOrName(gitProjectId: String?) =
        gitProjectId ?: data.eventCommon.gitProjectId

    override fun getGitCred(personToken: String?): TGitCred {
        if (personToken != null) {
            return TGitCred(
                userId = null,
                accessToken = personToken,
                useAccessToken = false
            )
        }
        return TGitCred(data.setting.enableUser)
    }

    override fun getChangeSet(): Set<String>? = null
}
