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
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.repository.context.ArtifactContextHolder
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.oci.artifact.OciRegistryArtifactConfigurer
import com.tencent.bkrepo.oci.constant.NAME
import com.tencent.bkrepo.oci.constant.PACKAGE_KEY
import com.tencent.bkrepo.oci.constant.VERSION
import com.tencent.bkrepo.oci.pojo.artifact.OciDeleteArtifactInfo
import com.tencent.bkrepo.oci.util.OciUtils
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(OciDeleteArtifactInfo::class)
class OciDeleteArtifactInfoResolver(
    private val artifactConfigurerSupport: OciRegistryArtifactConfigurer
) : ArtifactInfoResolver {

    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        // 判断是客户端的请求还是页面发送的请求分别进行处理
        val requestURL = ArtifactContextHolder.getUrlPath(this.javaClass.name)!!
        return when {
            // 页面删除包请求
            requestURL.contains(PACKAGE_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                val packageName = OciUtils.getPackageNameFormPackageKey(
                    packageKey = packageKey,
                    defaultType = artifactConfigurerSupport.getRepositoryType(),
                    extraTypes = artifactConfigurerSupport.getRepositoryTypes()
                )
                OciDeleteArtifactInfo(projectId, repoName, packageName, StringPool.EMPTY)
            }
            // 页面删除包版本请求
            requestURL.contains(PACKAGE_VERSION_DELETE_PREFIX) -> {
                val packageKey = request.getParameter(PACKAGE_KEY)
                val packageName = OciUtils.getPackageNameFormPackageKey(
                    packageKey = packageKey,
                    defaultType = artifactConfigurerSupport.getRepositoryType(),
                    extraTypes = artifactConfigurerSupport.getRepositoryTypes()
                )
                val version = request.getParameter(VERSION)
                OciDeleteArtifactInfo(projectId, repoName, packageName, version)
            }
            else -> {
                // 客户端请求删除版本
                val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
                val id = attributes[NAME].toString().trim()
                val version = attributes[VERSION].toString().trim()
                OciDeleteArtifactInfo(projectId, repoName, id, version)
            }
        }
    }

    companion object {
        private const val PACKAGE_DELETE_PREFIX = "/ext/package/delete/"
        private const val PACKAGE_VERSION_DELETE_PREFIX = "/ext/version/delete/"
    }
}
