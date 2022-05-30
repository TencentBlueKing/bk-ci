package com.tencent.bkrepo.git.constant

import com.tencent.bkrepo.common.api.message.MessageCode

enum class GitMessageCode(private val key: String) : MessageCode {
    GIT_URL_NOT_CONFIG("git.url.not.config"),
    GIT_REPO_NOT_SYNC("git.repo.not.sync"),
    GIT_ORIGINAL_FILE_MISS("git.original.file.miss"),
    GIT_REF_NOT_FOUND("git.ref.not.found"),
    GIT_PATH_NOT_FOUND("git.path.not.found"),
    GIT_HUB_TYPE_NOT_SUPPORT("git.hub.type.not.support"),
    GIT_REMOTE_REPO_PUSH_NOT_SUPPORT("git.remote.repo.push.not.support")
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 13
}
