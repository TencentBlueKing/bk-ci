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

package com.tencent.bkrepo.common.artifact.api

import com.tencent.bkrepo.common.api.constant.CharPool.AT
import com.tencent.bkrepo.common.api.constant.CharPool.SLASH
import com.tencent.bkrepo.common.artifact.path.PathUtils

/**
 * 构件信息
 *
 * [projectId]为项目名，[repoName]为仓库名，[artifactUri]为构件完整uri
 */
open class ArtifactInfo(
    /**
     * 项目名称
     */
    val projectId: String,
    /**
     * 仓库名称
     */
    val repoName: String,
    /**
     * 构件完整uri，如/archive/file/tmp.data
     */
    private val artifactUri: String
) {

    private val normalizedUri = PathUtils.normalizeFullPath(artifactUri)

    /**
     * 构件名称，不同依赖源解析规则不一样，可以override
     *
     * 默认使用传入的artifactUri作为名称
     */
    open fun getArtifactName(): String = normalizedUri

    /**
     * 构件版本
     *
     */
    open fun getArtifactVersion(): String? = null

    /**
     * 构件对应的节点完整路径，不同依赖源解析规则不一样，可以override
     *
     * 默认使用传入的artifactUri作为名称
     */
    open fun getArtifactFullPath(): String = normalizedUri

    /**
     * 构件下载显示名称，不同依赖源解析规则不一样，可以override
     *
     * 默认使用传入的artifactUri作为名称
     */
    open fun getResponseName(): String = PathUtils.resolveName(getArtifactFullPath())

    /**
     * 获取仓库唯一名, 格式 /{projectId}/{repoName}
     */
    open fun getRepoIdentify(): String {
        val builder = StringBuilder()
        builder.append(SLASH)
            .append(projectId)
            .append(SLASH)
            .append(repoName)
        return builder.toString()
    }

    /**
     *  /{projectId}/{repoName}/xxx
     */
    override fun toString(): String {
        val builder = StringBuilder()
        builder.append(getRepoIdentify())
        val artifactName = getArtifactName()
        if (!artifactName.startsWith(SLASH)) {
            builder.append(SLASH)
        }
        builder.append(artifactName)
        getArtifactVersion()?.let { builder.append(AT).append(it) }
        return builder.toString()
    }
}
