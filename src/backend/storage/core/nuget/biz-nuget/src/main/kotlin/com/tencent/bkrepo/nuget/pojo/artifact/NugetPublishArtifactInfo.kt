package com.tencent.bkrepo.nuget.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactFile
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.pojo.nuspec.NuspecPackage
import com.tencent.bkrepo.nuget.util.NugetUtils

/**
 * nuget publish信息
 */
class NugetPublishArtifactInfo(
    projectId: String,
    repoName: String,
    val packageName: String,
    val version: String,
    val nuspecPackage: NuspecPackage,
    val size: Long
) : NugetArtifactInfo(projectId, repoName, StringPool.EMPTY) {
    lateinit var artifactFile: ArtifactFile

    override fun getArtifactFullPath(): String = NugetUtils.getNupkgFullPath(packageName, version)
}
