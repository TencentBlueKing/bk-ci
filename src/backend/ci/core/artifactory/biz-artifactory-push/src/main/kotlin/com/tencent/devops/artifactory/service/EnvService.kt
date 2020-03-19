package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.EnvSet
import com.tencent.devops.artifactory.pojo.RemoteResourceInfo

interface EnvService {
    fun parsingAndValidateEnv(remoteResourceInfo: RemoteResourceInfo, user: String) : EnvSet
}