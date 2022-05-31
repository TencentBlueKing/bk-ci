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

package com.tencent.bkrepo.npm.resource

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.npm.api.NpmFixToolResource
import com.tencent.bkrepo.npm.artifact.NpmArtifactInfo
import com.tencent.bkrepo.npm.pojo.fixtool.DateTimeFormatResponse
import com.tencent.bkrepo.npm.pojo.fixtool.PackageMetadataFixResponse
import com.tencent.bkrepo.npm.pojo.fixtool.PackageManagerResponse
import com.tencent.bkrepo.npm.service.NpmFixToolService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class NpmFixToolResourceImpl @Autowired constructor(
    private val npmFixToolService: NpmFixToolService
) : NpmFixToolResource {

    override fun fixDateFormat(artifactInfo: NpmArtifactInfo, pkgName: String): DateTimeFormatResponse {
        return npmFixToolService.fixDateFormat(artifactInfo, pkgName)
    }

    override fun fixPackageSizeField(artifactInfo: NpmArtifactInfo): PackageMetadataFixResponse {
        return npmFixToolService.fixPackageSizeField(artifactInfo)
    }

    override fun fixPackageManager(): List<PackageManagerResponse> {
        return npmFixToolService.fixPackageManager()
    }

    override fun inconsistentCorrectionData(artifactInfo: NpmArtifactInfo, name: String): Response<Any> {
        npmFixToolService.inconsistentCorrectionData(artifactInfo, name)
        return ResponseBuilder.success()
    }
}
