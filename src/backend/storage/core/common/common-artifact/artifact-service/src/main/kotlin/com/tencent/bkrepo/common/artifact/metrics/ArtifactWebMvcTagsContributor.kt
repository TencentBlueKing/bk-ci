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

package com.tencent.bkrepo.common.artifact.metrics

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import io.micrometer.core.instrument.Tag
import org.springframework.boot.actuate.metrics.web.servlet.WebMvcTagsContributor
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 为请求增加额外的tag
 * */
class ArtifactWebMvcTagsContributor(private val artifactMetricsProperties: ArtifactMetricsProperties) :
    WebMvcTagsContributor {
    override fun getTags(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any?,
        exception: Throwable?
    ): Iterable<Tag> {
        // 添加project,添加repo
        val artifactInfo = ArtifactContextHolder.getArtifactInfo(request) ?: return TagUtils.tagOfProjectAndRepo(
            StringPool.UNKNOWN,
            StringPool.UNKNOWN
        )
        return TagUtils.tagOfProjectAndRepo(
            artifactInfo.projectId,
            artifactInfo.repoName,
            artifactMetricsProperties.includeRepositories
        )
    }

    override fun getLongRequestTags(request: HttpServletRequest, handler: Any): Iterable<Tag> {
        return emptyList()
    }
}
