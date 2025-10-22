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

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.quality.api.v2.ServiceQualityIndicatorMarketResource
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceQualityIndicatorMarketResourceImpl @Autowired constructor(
    private val qualityIndicatorService: QualityIndicatorService
) : ServiceQualityIndicatorMarketResource {
    override fun setTestIndicator(
        userId: String,
        atomCode: String,
        tag: String,
        indicatorUpdateList: Collection<IndicatorUpdate>
    ): Result<Int> {
        return Result(qualityIndicatorService.setTestIndicator(
            userId = userId,
            elementType = atomCode,
            tag = tag,
            indicatorUpdateList = indicatorUpdateList
        ))
    }

    override fun refreshIndicator(elementType: String, metadataMap: Map<String, String>): Result<Int> {
        return Result(qualityIndicatorService.serviceRefreshIndicator(elementType, metadataMap))
    }

    override fun deleteTestIndicator(elementType: String, extra: String): Result<Int> {
        return Result(qualityIndicatorService.serviceDeleteTestIndicator(elementType, extra))
    }
}
