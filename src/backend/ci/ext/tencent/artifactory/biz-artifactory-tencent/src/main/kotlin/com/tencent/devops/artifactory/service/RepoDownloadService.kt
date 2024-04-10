/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.artifactory.service

import com.tencent.devops.artifactory.pojo.Url
import com.tencent.devops.artifactory.pojo.enums.ArtifactoryType
import com.tencent.devops.common.pipeline.enums.ChannelCode

@Suppress("LongParameterList")
interface RepoDownloadService {

    /**
     * 外网BKRepo下载地址(鉴权根据token)
     */
    fun outerDownloadUrlByToken(
        creatorId: String?,
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        path: String,
        ttl: Int
    ): Url

    /**
     * 外网BKRepo下载地址(鉴权根据token)
     */
    fun innerDownloadUrlByToken(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int
    ): Url

    /**
     * 内网下载地址(鉴权根据用户态)
     */
    fun innerDownloadUrlByUser(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        channelCode: ChannelCode? = ChannelCode.BS,
        fullUrl: Boolean = true /*是否返回全路径（包含域名）*/
    ): Url

    /**
     * 外网Html地址(可跳转到下载链接)
     */
    fun outerHtmlUrl4Download(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String
    ): Url

    /**
     * 发送内部下载地址给相关人员
     */
    fun sendNotifyWithInnerUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        downloadUsers: String
    )

    /**
     * 内部跨项目下载
     */
    fun innerCrossDownloadUrl(
        projectId: String,
        pipelineId: String,
        buildId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int?,
        crossProjectId: String?,
        crossPipineId: String?,
        crossBuildNo: String?,
        region: String? = null,
        userId: String? = null
    ): List<String>

    /**
     * 外网使用的IPA的Plist的内容
     */
    fun outerPlistContent(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int,
        experienceHashId: String?,
        organization: String?
    ): String

    /**
     * 外网使用的IPA的Plist的地址
     */
    fun outerPlistUrl(
        userId: String,
        projectId: String,
        artifactoryType: ArtifactoryType,
        argPath: String,
        ttl: Int
    ): Url
}
