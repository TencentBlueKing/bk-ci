/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.quality.resources.v2

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.OPQualityMetadataResource
import com.tencent.devops.quality.api.v2.pojo.op.ElementNameData
import com.tencent.devops.quality.api.v2.pojo.op.QualityMetaData
import com.tencent.devops.quality.service.v2.QualityMetadataService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPQualityMetadataResourceImpl @Autowired constructor(
    private val qualityMetadataService: QualityMetadataService
) : OPQualityMetadataResource {

    override fun list(
        userId: String,
        elementName: String?,
        elementDetail: String?,
        searchString: String?,
        page: Int?,
        pageSize: Int?
    ): Result<Page<QualityMetaData>> {
        checkParams(userId)
        val result = qualityMetadataService.opList(elementName, elementDetail, searchString, page, pageSize)
        return Result(result)
    }

    override fun getElementNames(userId: String): Result<List<ElementNameData>> {
        checkParams(userId)
        return Result(qualityMetadataService.opGetElementNames())
    }

    override fun getElementDetails(userId: String): Result<List<String>> {
        checkParams(userId)
        val result = qualityMetadataService.opGetElementDetails()
        return Result(result)
    }

    override fun batchSaveMetadata(userId: String, metadataItemList: List<Map<String, String>>): Result<Boolean> {
        qualityMetadataService.batchSaveMetadata(userId, metadataItemList)
        return Result(true)
    }

    override fun deleteMetadata(metadataId: Long): Result<Boolean> {
        qualityMetadataService.deleteMetadata(metadataId)
        return Result(true)
    }

    private fun checkParams(userId: String) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
    }
}
