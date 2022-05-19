package com.tencent.bkrepo.nuget.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.util.NugetUtils

class NugetDownloadArtifactInfo(
    projectId: String,
    repoName: String,
    val packageName: String,
    val version: String = StringPool.EMPTY
) : NugetArtifactInfo(projectId, repoName, StringPool.EMPTY) {

    private val nugetFullPath = NugetUtils.getNupkgFullPath(packageName, version)

    override fun getArtifactFullPath(): String = nugetFullPath

    override fun getArtifactName(): String = packageName

    override fun getArtifactVersion(): String = version
}
