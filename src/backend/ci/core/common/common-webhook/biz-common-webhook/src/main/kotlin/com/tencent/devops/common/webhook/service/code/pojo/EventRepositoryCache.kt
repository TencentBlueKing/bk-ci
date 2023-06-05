package com.tencent.devops.common.webhook.service.code.pojo

import com.tencent.devops.scm.code.p4.api.P4ServerInfo
import com.tencent.devops.scm.pojo.GitCommit
import com.tencent.devops.scm.pojo.GitMrInfo
import com.tencent.devops.scm.pojo.GitMrReviewInfo

/**
 * 事件触发仓库级缓存
 */
data class EventRepositoryCache(
    var gitMrReviewInfo: GitMrReviewInfo? = null,
    var gitMrInfo: GitMrInfo? = null,
    var gitMrChangeFiles: Set<String>? = null,
    var gitCompareChangeFiles: Set<String>? = null,
    var gitDefaultBranchLatestCommitInfo: Pair<String?, GitCommit?>? = null,
    var repoAuthUser: String? = null,
    var p4ChangeFiles: List<String>? = null,
    val serverInfo: P4ServerInfo? = null
)
