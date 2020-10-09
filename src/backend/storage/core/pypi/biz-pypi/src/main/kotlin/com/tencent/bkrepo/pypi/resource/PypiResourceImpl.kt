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

package com.tencent.bkrepo.pypi.resource

import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.pypi.api.PypiResource
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import com.tencent.bkrepo.pypi.pojo.PypiMigrateResponse
import com.tencent.bkrepo.pypi.service.PypiService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseBody

/**
 * pypi服务接口实现类
 */
@Controller
class PypiResourceImpl : PypiResource {

    @Autowired
    private lateinit var pypiService: PypiService

    @ResponseBody
    override fun upload(
        pypiArtifactInfo: PypiArtifactInfo,
        artifactFileMap: ArtifactFileMap

    ) {
        pypiService.upload(pypiArtifactInfo, artifactFileMap)
    }

    @ResponseBody
    override fun search(
        pypiArtifactInfo: PypiArtifactInfo,
        @RequestBody xmlString: String
    ) {
        pypiService.search(pypiArtifactInfo, xmlString)
    }

    @ResponseBody
    override fun simple(artifactInfo: PypiArtifactInfo) {
        pypiService.simple(artifactInfo)
    }

    @ResponseBody
    override fun packages(artifactInfo: PypiArtifactInfo) {
        pypiService.packages(artifactInfo)
    }

    @ResponseBody
    override fun migrateByUrl(pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String> {
        return pypiService.migrate(pypiArtifactInfo)
    }

    /**
     * 数据迁移结果查询接口
     */
    @ResponseBody
    override fun migrateResult(pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String> {
        return pypiService.migrateResult(pypiArtifactInfo)
    }
}
