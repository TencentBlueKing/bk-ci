/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.  
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.helm.resource

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.common.service.util.ResponseBuilder
import com.tencent.bkrepo.helm.api.ChartRepositoryResource
import com.tencent.bkrepo.helm.artifact.HelmArtifactInfo
import com.tencent.bkrepo.helm.service.ChartRepositoryService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

@RestController
class ChartRepositoryResourceImpl : ChartRepositoryResource {

    @Autowired
    private lateinit var chartRepositoryService: ChartRepositoryService

    override fun getIndexYaml(artifactInfo: HelmArtifactInfo) {
        chartRepositoryService.getIndexYaml(artifactInfo)
    }

    override fun installTgz(artifactInfo: HelmArtifactInfo) {
        chartRepositoryService.installTgz(artifactInfo)
    }

    override fun installProv(artifactInfo: HelmArtifactInfo) {
        chartRepositoryService.installTgz(artifactInfo)
    }

    override fun regenerateIndexYaml(artifactInfo: HelmArtifactInfo): Response<Void> {
        chartRepositoryService.regenerateIndexYaml(artifactInfo)
        return ResponseBuilder.success()
    }

    override fun batchInstallTgz(artifactInfo: HelmArtifactInfo, startTime: LocalDateTime) {
        chartRepositoryService.batchInstallTgz(artifactInfo, startTime)
    }
}
