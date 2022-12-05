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
import com.tencent.bkrepo.oci.constant.OCI_TAG
import com.tencent.bkrepo.oci.constant.USER_API_PREFIX
import com.tencent.bkrepo.oci.pojo.artifact.OciTagArtifactInfo
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(OciTagArtifactInfo::class)
class OciTagArtifactInfoResolver : ArtifactInfoResolver {

    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val requestURL = ArtifactContextHolder.getUrlPath(this.javaClass.name)!!
        return when {
            requestURL.contains(TAG_PREFIX) -> {
                val requestUrl = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString()
                val packageName = requestUrl.removePrefix("$USER_API_PREFIX/tag/$projectId/$repoName/")
                validate(packageName)
                val tag = request.getParameter(OCI_TAG) ?: StringPool.EMPTY
                OciTagArtifactInfo(projectId, repoName, packageName, tag)
            }
            else -> {
                val requestUrl = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE).toString()
                val packageName = requestUrl.replaceAfterLast("/tags", StringPool.EMPTY).removeSuffix("/tags")
                    .removePrefix("/v2/$projectId/$repoName/")
                validate(packageName)
                OciTagArtifactInfo(projectId, repoName, packageName, StringPool.EMPTY)
            }
        }
    }

    private fun validate(packageName: String) {
        // packageName格式校验
        Preconditions.checkNotBlank(packageName, "packageName")
        Preconditions.matchPattern(packageName, PACKAGE_NAME_PATTERN, "package name [$packageName] invalid")
    }

    companion object {
        const val PACKAGE_NAME_PATTERN = "[a-z0-9]+([._-][a-z0-9]+)*(/[a-z0-9]+([._-][a-z0-9]+)*)*"
        const val TAG_PREFIX = "/ext/tag/"
    }
}
