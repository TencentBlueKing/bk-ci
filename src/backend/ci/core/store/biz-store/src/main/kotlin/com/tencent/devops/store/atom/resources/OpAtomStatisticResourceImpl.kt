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

package com.tencent.devops.store.atom.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.OpAtomStatisticResource
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.atom.service.MarketAtomStatisticService
import org.springframework.beans.factory.annotation.Autowired
import java.text.SimpleDateFormat
import java.util.Calendar

@RestResource
class OpAtomStatisticResourceImpl @Autowired constructor(
    private val marketAtomStatisticService: MarketAtomStatisticService
) : OpAtomStatisticResource {

    override fun asyncUpdateStorePipelineNum(): Result<Boolean> {
        return Result(marketAtomStatisticService.asyncUpdateStorePipelineNum())
    }

    override fun asyncUpdateDailyInfo(date: String): Result<Boolean> {
        val format = DateTimeUtil.YYYY_MM_DD
        val startTime = DateTimeUtil.convertDateToLocalDateTime(SimpleDateFormat(format).parse(date))
        val endTime = DateTimeUtil.convertDateToFormatLocalDateTime(
            date = DateTimeUtil.getFutureDate(startTime, Calendar.DAY_OF_MONTH, 1),
            format = format
        )
        return Result(
            marketAtomStatisticService.asyncAtomDailyStatisticInfo(
                storeType = StoreTypeEnum.ATOM.type.toByte(),
                startTime = startTime,
                endTime = endTime
            )
        )
    }
}
