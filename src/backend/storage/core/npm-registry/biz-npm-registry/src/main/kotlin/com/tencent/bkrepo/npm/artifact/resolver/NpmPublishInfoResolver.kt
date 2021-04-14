/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.npm.artifact.resolver

import com.tencent.bkrepo.common.api.constant.HttpStatus
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.api.exception.ParameterInvalidException
import com.tencent.bkrepo.common.api.message.CommonMessageCode
import com.tencent.bkrepo.common.api.util.Preconditions
import com.tencent.bkrepo.common.api.util.readJsonString
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.resolve.path.ArtifactInfoResolver
import com.tencent.bkrepo.common.artifact.resolve.path.Resolver
import com.tencent.bkrepo.common.artifact.util.version.SemVersion
import com.tencent.bkrepo.npm.constant.NAME
import com.tencent.bkrepo.npm.constant.NpmMessageCode
import com.tencent.bkrepo.npm.constant.SCOPE
import com.tencent.bkrepo.npm.constant.VERSION
import com.tencent.bkrepo.npm.pojo.artifact.NpmPublishInfo
import com.tencent.bkrepo.npm.pojo.metadata.NpmPackageMetadata
import com.tencent.bkrepo.npm.pojo.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.util.NpmUtils
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerMapping
import java.util.Base64
import javax.servlet.http.HttpServletRequest

@Component
@Resolver(NpmPublishInfo::class)
class NpmPublishInfoResolver : ArtifactInfoResolver {
    override fun resolve(
        projectId: String,
        repoName: String,
        artifactUri: String,
        request: HttpServletRequest
    ): ArtifactInfo {
        val attributes = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) as Map<*, *>
        val name = attributes[NAME].toString().trim()
        val scope = attributes[SCOPE]?.toString()?.trim()
        Preconditions.checkNotBlank(name, NAME)
        scope?.let { Preconditions.checkNotBlank(scope, SCOPE) }
        val packageName = NpmUtils.formatPackageName(name, scope)
        val packageMetadata = request.inputStream.readJsonString<NpmPackageMetadata>()
        val version = packageMetadata.versions.keys.firstOrNull().orEmpty()
        // attachment 数据解析与校验
        val isDeprecated = validate(packageName, packageMetadata)
        val publishInfo = NpmPublishInfo(projectId, repoName, packageName, version, packageMetadata, isDeprecated)
        if (!isDeprecated) {
            publishInfo.tarball = decodeTarballContent(publishInfo)
        }
        return publishInfo
    }

    /**
     * 验证构件信息格式是否正确
     *
     * @return 是否为deprecate 请求
     */
    private fun validate(packageName: String, packageMetadata: NpmPackageMetadata): Boolean {
        // package name 格式校验
        // Preconditions.checkArgument(StringUtils.isAllLowerCase(name), "name")
        Preconditions.checkArgument(packageName.length <= NAME_MAX_LENGTH, NAME)
        // package.json 和 url 中的 @scope/name 一致
        Preconditions.checkArgument(packageMetadata.name == packageName, "package.name")
        Preconditions.checkArgument(packageMetadata.id == packageName, "package.name")
        // versions不能为空
        Preconditions.checkNotBlank(packageMetadata.versions, "package.versions")
        // version 符合语义化版本
        val version = packageMetadata.versions.keys.firstOrNull().orEmpty()
        Preconditions.checkArgument(SemVersion.validate(version), VERSION)
        // 不是deprecate请求则_attachments不能为空
        // npm deprecate: attachments 为 {}
        // npm star: attachments 为 null
        // npm publish: attachments 包含一条记录
        if (packageMetadata.attachments.isEmpty()) {
            if (containsDeprecatedVersion(packageMetadata.versions)) {
                return true
            }
            throw ErrorCodeException(CommonMessageCode.PARAMETER_EMPTY, "package._attachments")
        }
        // maintainers, 为空且登录状态，将当前操作用户填入
        // val versionPackage = getVersionPackage()
        // if (versionPackage.maintainers.isEmpty()) {
        //     if (SecurityUtils.isAnonymous()) {
        //         throw ErrorCodeException(CommonMessageCode.PARAMETER_EMPTY, "package.maintainers")
        //     }
        //     versionPackage.maintainers.add(NpmAuthor(name = SecurityUtils.getUserId()))
        // }
        // dist-tags校验，不能为空
        Preconditions.checkNotBlank(packageMetadata.distTags, "package.dist-tags")
        packageMetadata.distTags.forEach { (tag, version) ->
            Preconditions.checkNotBlank(tag, "package.dist-tags")
            Preconditions.checkNotBlank(version, "package.dist-tags")
        }
        return false
    }

    /**
     * 检查是否包含deprecated类型的version
     */
    private fun containsDeprecatedVersion(versions: Map<String, NpmVersionMetadata>): Boolean {
        versions.forEach { (_, version) ->
            if (version.deprecated != null) {
                return true
            }
        }
        return false
    }

    /**
     * 解码tarball内容，经base64编码
     */
    private fun decodeTarballContent(npmPublishInfo: NpmPublishInfo): ByteArray {
        val attachment = npmPublishInfo.getAttachment()
        try {
            val content = Base64.getDecoder().decode(attachment.data)
            if (content.size != attachment.length) {
                throw ErrorCodeException(
                    status = HttpStatus.FORBIDDEN,
                    messageCode = NpmMessageCode.ATTACHMENT_SIZE_INVALID,
                    params = arrayOf(attachment.length, content.size)
                )
            }
            // 补齐packageSize数据，tgz
            npmPublishInfo.getVersionPackage().dist.packageSize = content.size
            return content
        } catch (exception: IllegalArgumentException) {
            logger.warn("Failed to decode base64 tarball content")
            throw ParameterInvalidException("package._attachments")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(NpmPublishInfoResolver::class.java)
        private const val NAME_MAX_LENGTH = 214
    }
}
