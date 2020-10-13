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

package com.tencent.bkrepo.pypi.api

import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo.Companion.PYPI_MIGRATE_RESULT
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo.Companion.PYPI_MIGRATE_URL
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo.Companion.PYPI_PACKAGES_MAPPING_URI
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo.Companion.PYPI_ROOT_POST_URI
import com.tencent.bkrepo.pypi.artifact.PypiArtifactInfo.Companion.PYPI_SIMPLE_MAPPING_INSTALL_URI
import com.tencent.bkrepo.pypi.pojo.PypiMigrateResponse
import io.swagger.annotations.ApiOperation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody

interface PypiResource {

    /**
     * pypi upload 接口
     */
    @PostMapping(PYPI_ROOT_POST_URI)
    fun upload(
        @ArtifactPathVariable
        pypiArtifactInfo: PypiArtifactInfo,
        artifactFileMap: ArtifactFileMap
    )

    /**
     * pypi search 接口
     */
    @PostMapping(
        PYPI_ROOT_POST_URI,
        consumes = [MediaType.TEXT_XML_VALUE],
        produces = [MediaType.TEXT_XML_VALUE]
    )
    fun search(
        @ArtifactPathVariable
        pypiArtifactInfo: PypiArtifactInfo,
        @RequestBody xmlString: String
    )

    /**
     * pypi simple/{package} 接口
     */
    @GetMapping(PYPI_SIMPLE_MAPPING_INSTALL_URI)
    fun simple(@ArtifactPathVariable artifactInfo: PypiArtifactInfo)

    /**
     * pypi install 接口
     * packages/{package}/{version}/{filename}#md5={md5}
     */
    @GetMapping(PYPI_PACKAGES_MAPPING_URI)
    fun packages(@ArtifactPathVariable artifactInfo: PypiArtifactInfo)

    @ApiOperation("数据迁移接口")
    @GetMapping(PYPI_MIGRATE_URL, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun migrateByUrl(@ArtifactPathVariable pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String>

    /**
     * 数据迁移结果查询接口
     */
    @ApiOperation("数据迁移结果查询接口")
    @GetMapping(PYPI_MIGRATE_RESULT, produces = [MediaType.APPLICATION_JSON_VALUE])
    fun migrateResult(@ArtifactPathVariable pypiArtifactInfo: PypiArtifactInfo): PypiMigrateResponse<String>
}
