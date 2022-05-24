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

package com.tencent.bkrepo.docker.artifact

import com.tencent.bkrepo.common.artifact.util.PackageKeys
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import com.tencent.bkrepo.docker.context.RequestContext
import com.tencent.bkrepo.repository.api.OperateLogClient
import com.tencent.bkrepo.repository.api.PackageClient
import com.tencent.bkrepo.repository.api.PackageDownloadsClient
import com.tencent.bkrepo.repository.pojo.download.PackageDownloadRecord
import com.tencent.bkrepo.repository.pojo.packages.PackageVersion
import com.tencent.bkrepo.repository.pojo.packages.request.PackagePopulateRequest
import com.tencent.bkrepo.repository.pojo.packages.request.PackageVersionCreateRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class DockerPackageRepo @Autowired constructor(
    private val packageClient: PackageClient,
    private val packageDownloadsClient: PackageDownloadsClient,
    private val operateLogClient: OperateLogClient
) {

    /**
     * create version
     * @param request the request to create version
     * @return Boolean is the package version create success
     */
    fun createVersion(request: PackageVersionCreateRequest): Boolean {
        return packageClient.createVersion(request, HttpContextHolder.getClientAddress()).isOk()
    }

    /**
     * populate package
     * @param request the request to populate version
     * @return Boolean is the package version create success
     */
    fun populatePackage(request: PackagePopulateRequest): Boolean {
        return packageClient.populatePackage(request).isOk()
    }

    /**
     * delete package
     * @param context the request context
     * @return Boolean is the package version delete success
     */
    fun deletePackage(context: RequestContext): Boolean {
        with(context) {
            return packageClient.deletePackage(
                projectId,
                repoName,
                PackageKeys.ofDocker(artifactName),
                HttpContextHolder.getClientAddress()
            ).isOk()
        }
    }

    /**
     * delete package version
     * @param context the request context
     * @param version package version
     * @return Boolean is the package version exist
     */
    fun deletePackageVersion(context: RequestContext, version: String): Boolean {
        with(context) {
            return packageClient.deleteVersion(
                projectId,
                repoName,
                PackageKeys.ofDocker(artifactName),
                version,
                HttpContextHolder.getClientAddress()
            ).isOk()
        }
    }

    /**
     * get package version
     * @param context the request context
     * @param version package version
     * @return PackageVersion the package version detail
     */
    fun getPackageVersion(context: RequestContext, version: String): PackageVersion? {
        with(context) {
            return packageClient.findVersionByName(projectId, repoName, PackageKeys.ofDocker(artifactName), version)
                .data
        }
    }

    /**
     * add download statics
     * @param context the request context
     * @param version package version
     * @return Boolean is add download static success
     */
    fun addDownloadStatic(context: RequestContext, version: String): Boolean {
        with(context) {
            val request = PackageDownloadRecord(
                projectId,
                repoName,
                PackageKeys.ofDocker(artifactName),
                version
            )
            return packageDownloadsClient.record(request).isOk()
        }
    }
}
