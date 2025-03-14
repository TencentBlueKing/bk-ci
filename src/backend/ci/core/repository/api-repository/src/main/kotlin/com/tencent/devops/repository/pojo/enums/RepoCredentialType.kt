package com.tencent.devops.repository.pojo.enums

import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "代码库授权凭证类型")
// 字段值需要与CredentialType中的值名字一致
enum class RepoCredentialType(val authType: RepoAuthType) {
    OAUTH(RepoAuthType.OAUTH),
    PASSWORD(RepoAuthType.HTTPS),
    ACCESSTOKEN(RepoAuthType.HTTPS),
    USERNAME_PASSWORD(RepoAuthType.HTTPS),
    SSH_PRIVATEKEY(RepoAuthType.SSH),
    TOKEN_SSH_PRIVATEKEY(RepoAuthType.SSH),
    TOKEN_USERNAME_PASSWORD((RepoAuthType.HTTPS));
}
