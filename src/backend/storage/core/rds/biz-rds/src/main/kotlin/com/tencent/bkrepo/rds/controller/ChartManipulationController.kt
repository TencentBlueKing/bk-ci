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

package com.tencent.bkrepo.rds.controller

import com.tencent.bkrepo.common.artifact.api.ArtifactFileMap
import com.tencent.bkrepo.common.artifact.api.ArtifactPathVariable
import com.tencent.bkrepo.rds.pojo.RdsSuccessResponse
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo.Companion.CHART_DELETE_VERSION_URL
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo.Companion.RDS_PUSH_PLUGIN_URL
import com.tencent.bkrepo.rds.pojo.artifact.RdsArtifactInfo.Companion.RDS_PUSH_URL
import com.tencent.bkrepo.rds.pojo.artifact.RdsDeleteArtifactInfo
import com.tencent.bkrepo.rds.service.ChartManipulationService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestAttribute
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Suppress("MVCPathVariableInspection")
@RestController
class ChartManipulationController(
    private val chartManipulationService: ChartManipulationService
) {
    /**
     * rds push
     */
    @PostMapping(RDS_PUSH_URL, RDS_PUSH_PLUGIN_URL)
    @ResponseStatus(HttpStatus.CREATED)
    fun upload(
        @ArtifactPathVariable artifactInfo: RdsArtifactInfo,
        artifactFileMap: ArtifactFileMap
    ): RdsSuccessResponse {
        chartManipulationService.upload(artifactInfo, artifactFileMap)
        return RdsSuccessResponse.pushSuccess()
    }

    /**
     * delete chart version
     */
    @DeleteMapping(CHART_DELETE_VERSION_URL)
    fun deleteVersion(
        @RequestAttribute userId: String,
        @ArtifactPathVariable artifactInfo: RdsDeleteArtifactInfo
    ): RdsSuccessResponse {
        chartManipulationService.deleteVersion(userId, artifactInfo)
        return RdsSuccessResponse.deleteSuccess()
    }
}
