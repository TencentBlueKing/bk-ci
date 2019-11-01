package com.tencent.devops.common.auth.code

import com.tencent.devops.common.auth.api.BkAuthServiceCode

class BSArtifactoryAuthServiceCode : ArtifactoryAuthServiceCode {
    override fun id() = BkAuthServiceCode.ARTIFACTORY.value
}