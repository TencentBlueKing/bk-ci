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

package com.tencent.bkrepo.oci.artifact.resolver

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.oci.pojo.artifact.OciBlobArtifactInfo
import io.undertow.servlet.spec.HttpServletRequestImpl
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(OciBlobArtifactInfo::class)
class OciBlobArtifactInfoResolver : ArtifactInfoResolver {

    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val requestUrl = ArtifactContextHolder.getUrlPath(this.javaClass.name)!!
        val packageName = requestUrl.replaceAfterLast("/blobs", StringPool.EMPTY).removeSuffix("/blobs")
            .removePrefix("/v2/$projectId/$repoName/")
        validate(packageName)
        val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
        // 解析digest
        val digest = attributes["digest"]?.toString()?.trim() ?: run {
            request.getParameter("digest")?.toString()?.trim()
        }
        // 解析UUID
        val uuid = attributes["uuid"]?.toString()?.trim()
        val params = (request as HttpServletRequestImpl).queryParameters
        // 解析mount
        val mount = params?.get("mount")?.first
        val from = params?.get("from")?.first
        return OciBlobArtifactInfo(projectId, repoName, packageName, "", digest, uuid, mount, from)
    }

    private fun validate(packageName: String) {
        // packageName格式校验
        Preconditions.checkNotBlank(packageName, "packageName")
        Preconditions.matchPattern(packageName, PACKAGE_NAME_PATTERN, "package name [$packageName] invalid")
    }

    companion object {
        const val PACKAGE_NAME_PATTERN = "[a-z0-9]+([._-][a-z0-9]+)*(/[a-z0-9]+([._-][a-z0-9]+)*)*"
    }
}
