package com.tencent.bkrepo.oci.pojo.artifact

class OciDeleteArtifactInfo(
    projectId: String,
    repoName: String,
    packageName: String,
    version: String,
) : OciArtifactInfo(projectId, repoName, packageName, version)
