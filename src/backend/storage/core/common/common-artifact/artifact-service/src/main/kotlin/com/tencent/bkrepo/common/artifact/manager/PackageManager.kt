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

package com.tencent.bkrepo.common.artifact.manager

import com.tencent.bkrepo.common.api.exception.NotFoundException
import com.tencent.bkrepo.common.artifact.message.ArtifactMessageCode
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.pojo.packages.PackageSummary
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion

/**
 * 包版本相关Manager
 */
class PackageManager(
    private val packageClient: PackageClient
) {

    fun findPackageByKey(projectId: String, repoName: String, packageKey: String): PackageSummary {
        return packageClient.findPackageByKey(projectId, repoName, packageKey).data
            ?: throw NotFoundException(ArtifactMessageCode.PACKAGE_NOT_FOUND, packageKey)
    }

    fun findVersionByName(
        projectId: String,
        repoName: String,
        packageKey: String,
        version: String
    ): PackageVersion {
        return packageClient.findVersionByName(projectId, repoName, packageKey, version).data
            ?: throw NotFoundException(ArtifactMessageCode.VERSION_NOT_FOUND, version)
    }

    fun findVersionNameByTag(
        projectId: String,
        repoName: String,
        packageKey: String,
        tag: String
    ): String {
        return packageClient.findVersionNameByTag(projectId, repoName, packageKey, tag).data
            ?: throw NotFoundException(ArtifactMessageCode.VERSION_NOT_FOUND, tag)
    }
}
