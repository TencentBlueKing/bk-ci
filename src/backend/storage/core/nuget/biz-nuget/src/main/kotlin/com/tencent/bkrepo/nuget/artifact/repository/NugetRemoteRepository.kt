/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.nuget.artifact.repository

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactQueryContext
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactUploadContext
import com.tencent.bkrepo.common.artifact.repository.remote.RemoteRepository
import com.tencent.bkrepo.nuget.artifact.NugetArtifactInfo
import com.tencent.bkrepo.nuget.common.NugetRemoteAndVirtualCommon
import com.tencent.bkrepo.nuget.constant.REMOTE_URL
import com.tencent.bkrepo.nuget.exception.NugetFeedNofFoundException
import com.tencent.bkrepo.nuget.pojo.artifact.NugetRegistrationArtifactInfo
import com.tencent.bkrepo.nuget.pojo.v3.metadata.feed.Feed
import com.tencent.bkrepo.nuget.pojo.v3.metadata.feed.Resource
import com.tencent.bkrepo.nuget.util.NugetUtils
import com.tencent.bkrepo.nuget.util.NugetV3RemoteRepositoryUtils
import com.tencent.bkrepo.nuget.util.NugetV3RemoteRepositoryUtils.convertOriginalToBkrepoResource
import okhttp3.Response
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import java.io.InputStream
import java.util.Objects
import java.util.stream.Collectors

@Component
class NugetRemoteRepository(
    private val commonUtils: NugetRemoteAndVirtualCommon
) : RemoteRepository(), NugetRepository {

    @Suppress("UNCHECKED_CAST")
    override fun feed(artifactInfo: NugetArtifactInfo): ResponseEntity<Any> {
        // 1、请求远程索引文件
        // 2、将resource里面的内容进行更改
        // 先使用type进行匹配筛选，然后在进行id的替换
        val feed = commonUtils.downloadRemoteFeed()
        val v2BaseUrl = NugetUtils.getV2Url(artifactInfo)
        val v3BaseUrl = NugetUtils.getV3Url(artifactInfo)
        val convertResource = feed.resources.stream()
//            .filter(NugetRemoteAndVirtualCommon.distinctByKey { Resource::type.name })
            .map { resource -> convertOriginalToBkrepoResource(resource, v2BaseUrl, v3BaseUrl) }
            .filter(Objects::nonNull)
            .collect(Collectors.toList())
        return ResponseEntity.ok(Feed(feed.version, convertResource as List<Resource>))
    }

    override fun onQueryResponse(context: ArtifactQueryContext, response: Response): InputStream {
        val artifactFile = createTempFile(response.body()!!)
        return artifactFile.getInputStream()
    }

    override fun createRemoteDownloadUrl(context: ArtifactContext): String {
        return context.getStringAttribute(REMOTE_URL).orEmpty()
    }

    override fun registrationIndex(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        // 1、先根据请求URL匹配对应的远程URL地址的type，在根据type去找到对应的key
        // 2、根据匹配到的URL去添加请求packageId之后去请求远程索引文件
        // 3、缓存索引文件，然后将文件中的URL改成对应的仓库URL进行返回
        val v2BaseUrl = NugetUtils.getV2Url(artifactInfo)
        val v3BaseUrl = NugetUtils.getV3Url(artifactInfo)
        val registrationIndex = commonUtils.downloadRemoteRegistrationIndex(
            artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl
        )
        val rewriteRegistrationIndex = registrationIndex?.let {
            NugetV3RemoteRepositoryUtils.rewriteRegistrationIndexUrls(
                registrationIndex, artifactInfo, v2BaseUrl, v3BaseUrl, registrationPath
            )
        } ?: throw NugetFeedNofFoundException(
            "Failed to parse registration json for package: [${artifactInfo.packageName}]," +
                " in repo: [${artifactInfo.getRepoIdentify()}]"
        )
        return ResponseEntity.ok(rewriteRegistrationIndex)
    }

    override fun registrationPage(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val v2BaseUrl = NugetUtils.getV2Url(artifactInfo)
        val v3BaseUrl = NugetUtils.getV3Url(artifactInfo)
        val registrationPage = commonUtils.downloadRemoteRegistrationPage(
            artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl
        )
        val rewriteRegistrationPage = NugetV3RemoteRepositoryUtils.rewriteRegistrationPageUrls(
            registrationPage, artifactInfo, v2BaseUrl, v3BaseUrl, registrationPath
        )
        return ResponseEntity.ok(rewriteRegistrationPage)
    }

    override fun registrationLeaf(
        artifactInfo: NugetRegistrationArtifactInfo,
        registrationPath: String,
        isSemver2Endpoint: Boolean
    ): ResponseEntity<Any> {
        val v2BaseUrl = NugetUtils.getV2Url(artifactInfo)
        val v3BaseUrl = NugetUtils.getV3Url(artifactInfo)
        val registrationLeaf = commonUtils.downloadRemoteRegistrationLeaf(
            artifactInfo, registrationPath, v2BaseUrl, v3BaseUrl
        )
        val rewriteRegistrationLeaf = NugetV3RemoteRepositoryUtils.rewriteRegistrationLeafUrls(
            registrationLeaf, artifactInfo, v2BaseUrl, v3BaseUrl, registrationPath
        )
        return ResponseEntity.ok(rewriteRegistrationLeaf)
    }

    override fun upload(context: ArtifactUploadContext) {
        with(context) {
            val message = "Unable to upload nuget package into a remote repository [$projectId/$repoName]"
            logger.warn(message)
            // return 400 bad request
            response.status = HttpStatus.BAD_REQUEST.value
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(NugetRemoteRepository::class.java)
    }
}
