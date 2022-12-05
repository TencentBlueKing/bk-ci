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

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.NpmDomainInfo
import com.tencent.bkrepo.npm.pojo.user.request.PackageDeleteRequest
import com.tencent.bkrepo.npm.pojo.user.request.PackageVersionDeleteRequest
import com.tencent.bkrepo.npm.pojo.user.PackageVersionInfo

interface NpmWebService {

    /**
     * 查询版本信息
     */
    fun detailVersion(artifactInfo: NpmArtifactInfo, packageKey: String, version: String): PackageVersionInfo

    /**
     * 删除包
     */
    fun deletePackage(artifactInfo: NpmArtifactInfo, deleteRequest: PackageDeleteRequest)

    /**
     * 删除包版本
     */
    fun deleteVersion(artifactInfo: NpmArtifactInfo, deleteRequest: PackageVersionDeleteRequest)

    /**
     * 获取npm域名信息
     */
    fun getRegistryDomain(): NpmDomainInfo
}
