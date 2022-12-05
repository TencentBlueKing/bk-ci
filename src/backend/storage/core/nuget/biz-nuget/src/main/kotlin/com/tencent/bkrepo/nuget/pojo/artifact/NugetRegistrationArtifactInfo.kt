package com.tencent.bkrepo.nuget.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo

/**
 * registration artifact info
 */
class NugetRegistrationArtifactInfo(
    projectId: String,
    repoName: String,
    val packageName: String,
    val version: String = StringPool.EMPTY,
    val lowerVersion: String = StringPool.EMPTY,
    val upperVersion: String = StringPool.EMPTY
) : NugetArtifactInfo(projectId, repoName, StringPool.EMPTY)
