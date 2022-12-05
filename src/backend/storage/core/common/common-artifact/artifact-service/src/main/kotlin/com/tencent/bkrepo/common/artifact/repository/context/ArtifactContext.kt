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

package com.tencent.bkrepo.common.artifact.repository.context

import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.common.artifact.constant.ARTIFACT_INFO_KEY
import com.tencent.bkrepo.common.artifact.constant.REPO_KEY
import com.tencent.bkrepo.common.artifact.pojo.RepositoryCategory
import com.tencent.bkrepo.common.artifact.pojo.configuration.composite.CompositeConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.local.LocalConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.remote.RemoteConfiguration
import com.tencent.bkrepo.common.artifact.pojo.configuration.virtual.VirtualConfiguration
import com.tencent.bkrepo.common.security.util.SecurityUtils
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.common.storage.credentials.StorageCredentials
import com.tencent.bkrepo.repository.pojo.repo.RepositoryDetail
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * 构件上下文
 */
@Suppress("UNCHECKED_CAST")
open class ArtifactContext(
    repo: RepositoryDetail? = null,
    artifact: ArtifactInfo? = null,
    userId: String = SecurityUtils.getUserId()
) {
    val request: HttpServletRequest = HttpContextHolder.getRequest()
    val response: HttpServletResponse = HttpContextHolder.getResponse()
    val userId: String = userId
    val artifactInfo: ArtifactInfo = artifact ?: request.getAttribute(ARTIFACT_INFO_KEY) as ArtifactInfo
    var repositoryDetail: RepositoryDetail = repo ?: request.getAttribute(REPO_KEY) as RepositoryDetail
    val storageCredentials: StorageCredentials? = repositoryDetail.storageCredentials
    val projectId: String = repositoryDetail.projectId
    val repoName: String = repositoryDetail.name

    private var contextAttributes: MutableMap<String, Any> = mutableMapOf()

    /**
     * 使用当前实例的属性，拷贝出一个新的[ArtifactContext]实例
     * 传入的[repositoryDetail]会替换当前实例的仓库信息
     */
    fun copy(repositoryDetail: RepositoryDetail): ArtifactContext {
        val context = this.javaClass.newInstance()
        context.repositoryDetail = repositoryDetail
        context.contextAttributes = this.contextAttributes
        return context
    }

    /**
     * 获取context Attributes map
     */
    fun getAttributes(): Map<String, Any> = contextAttributes

    /**
     * 添加自定义context属性
     */
    fun putAttribute(key: String, value: Any) {
        this.contextAttributes[key] = value
    }

    /**
     * 根据属性名[key]获取自定义context属性
     */
    inline fun <reified T> getAttribute(key: String): T? {
        return getAttributes()[key] as T?
    }

    /**
     * 根据属性名[key]获取字符串类型属性
     */
    fun getStringAttribute(key: String): String? {
        return this.contextAttributes[key]?.toString()
    }

    /**
     * 根据属性名[key]获取Long类型属性
     */
    fun getLongAttribute(key: String): Long? {
        return this.contextAttributes[key]?.toString()?.toLong()
    }

    /**
     * 根据属性名[key]获取Boolean类型属性
     */
    fun getBooleanAttribute(key: String): Boolean? {
        return this.contextAttributes[key]?.toString()?.toBoolean()
    }

    /**
     * 根据属性名[key]获取整数类型属性
     */
    fun getIntegerAttribute(key: String): Int? {
        return this.contextAttributes[key]?.toString()?.toInt()
    }

    /**
     * 获取本地仓库配置
     *
     * 当仓库类型和配置类型不符时抛[IllegalArgumentException]异常
     */
    @Throws(IllegalArgumentException::class)
    fun getLocalConfiguration(): LocalConfiguration {
        require(this.repositoryDetail.category == RepositoryCategory.LOCAL)
        return this.repositoryDetail.configuration as LocalConfiguration
    }

    /**
     * 获取远程仓库配置
     *
     * 当仓库类型和配置类型不符时抛[IllegalArgumentException]异常
     */
    @Throws(IllegalArgumentException::class)
    fun getRemoteConfiguration(): RemoteConfiguration {
        require(this.repositoryDetail.category == RepositoryCategory.REMOTE)
        return this.repositoryDetail.configuration as RemoteConfiguration
    }

    /**
     * 获取远程仓库配置
     *
     * 当仓库类型和配置类型不符时抛[IllegalArgumentException]异常
     */
    @Throws(IllegalArgumentException::class)
    fun getVirtualConfiguration(): VirtualConfiguration {
        require(this.repositoryDetail.category == RepositoryCategory.VIRTUAL)
        return this.repositoryDetail.configuration as VirtualConfiguration
    }

    /**
     * 获取组合仓库配置
     *
     * 当仓库类型和配置类型不符时抛[IllegalArgumentException]异常
     */
    @Throws(IllegalArgumentException::class)
    fun getCompositeConfiguration(): CompositeConfiguration {
        require(this.repositoryDetail.category == RepositoryCategory.COMPOSITE)
        return this.repositoryDetail.configuration as CompositeConfiguration
    }
}
