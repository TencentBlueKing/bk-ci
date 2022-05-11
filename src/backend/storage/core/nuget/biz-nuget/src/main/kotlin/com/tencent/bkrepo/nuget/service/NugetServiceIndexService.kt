package com.tencent.bkrepo.nuget.service

import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import org.springframework.http.ResponseEntity

interface NugetServiceIndexService {

    /**
     * 获取index.json内容
     */
    fun getFeed(artifactInfo: NugetArtifactInfo): ResponseEntity<Any>
}
