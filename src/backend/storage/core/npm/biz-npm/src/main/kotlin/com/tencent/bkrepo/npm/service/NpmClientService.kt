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
import com.tencent.bkrepo.npm.model.metadata.NpmVersionMetadata
import com.tencent.bkrepo.npm.model.metadata.NpmPackageMetaData
import com.tencent.bkrepo.npm.pojo.NpmSearchResponse
import com.tencent.bkrepo.npm.pojo.NpmSuccessResponse
import com.tencent.bkrepo.npm.pojo.metadata.MetadataSearchRequest
import com.tencent.bkrepo.npm.pojo.metadata.disttags.DistTags

interface NpmClientService {
    /**
     * npm publish or update package
     */
    fun publishOrUpdatePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String): NpmSuccessResponse

    /**
     * 查询npm包的package.json元数据信息
     */
    fun packageInfo(artifactInfo: NpmArtifactInfo, name: String): NpmPackageMetaData

    /**
     * 查询npm包的版本对应元数据信息
     */
    fun packageVersionInfo(artifactInfo: NpmArtifactInfo, name: String, version: String): NpmVersionMetadata

    /**
     * download tgz file
     */
    fun download(artifactInfo: NpmArtifactInfo)

    /**
     * npm search
     */
    fun search(artifactInfo: NpmArtifactInfo, searchRequest: MetadataSearchRequest): NpmSearchResponse

    /**
     * get dist tags
     */
    fun getDistTags(artifactInfo: NpmArtifactInfo, name: String): DistTags

    /**
     * add dist tags
     */
    fun addDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String)

    /**
     * delete dist tags
     */
    fun deleteDistTags(userId: String, artifactInfo: NpmArtifactInfo, name: String, tag: String)

    /**
     * update packages
     */
    fun updatePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String)

    /**
     * delete package version
     */
    fun deleteVersion(artifactInfo: NpmArtifactInfo, name: String, version: String, tgzPath: String)

    /**
     * delete package
     */
    fun deletePackage(userId: String, artifactInfo: NpmArtifactInfo, name: String)
}
