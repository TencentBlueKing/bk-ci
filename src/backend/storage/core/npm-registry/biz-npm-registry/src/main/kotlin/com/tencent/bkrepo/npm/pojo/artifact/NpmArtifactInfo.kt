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

package com.tencent.bkrepo.npm.pojo.artifact

import com.tencent.bkrepo.common.api.constant.StringPool
import com.tencent.bkrepo.common.artifact.api.ArtifactInfo
import com.tencent.bkrepo.npm.util.NpmUtils

/**
 * npm 构件基本信息
 * 其余场景的ArtifactInfo 可以继承该类，如[NpmPublishInfo]
 */
open class NpmArtifactInfo(
    projectId: String,
    repoName: String,
    val packageName: String,
    val version: String = StringPool.EMPTY,
    private val delimiter: String = StringPool.DASH,
    private val write: Boolean = true // meaning the request using for write, don't sync
) : ArtifactInfo(projectId, repoName, StringPool.EMPTY) {

    /**
     * 1. without scope: /test/-/test-1.0.0.tgz
     * 2. with scope: /@scope/test/-/@scope/test-1.0.0.tgz
     */
    private val tarballFullPath: String = NpmUtils.formatTarballPath(packageName, version, delimiter)

    override fun getArtifactFullPath() = tarballFullPath

    override fun getArtifactVersion() = version

    override fun getArtifactName() = packageName
}
