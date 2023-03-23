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
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.quality.api.v2.OPIndicatorResource
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorData
import com.tencent.devops.quality.api.v2.pojo.op.IndicatorUpdate
import com.tencent.devops.quality.constant.QualityMessageCode.INDEX_ENGLISH_NAME_CANNOT_EMPTY
import com.tencent.devops.quality.service.v2.QualityIndicatorService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPIndicatorResourceImpl @Autowired constructor(
    private val indicatorService: QualityIndicatorService
) : OPIndicatorResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<Page<IndicatorData>> {
        checkParams(userId)
        val result = indicatorService.opList(userId, page, pageSize)
        return Result(result)
    }

    override fun getByIds(userId: String, ids: String): Result<List<IndicatorData>> {
        checkParams(userId)
        return Result(indicatorService.opListByIds(userId, ids))
    }

    override fun add(userId: String, indicatorUpdate: IndicatorUpdate): Result<Boolean> {
        checkParams(userId)
        if (indicatorUpdate.enName.isNullOrBlank()) {
            return Result(
                -1,
                MessageUtil.getMessageByLocale(INDEX_ENGLISH_NAME_CANNOT_EMPTY, I18nUtil.getLanguage(userId)),
                false
            )
        }
        val result = indicatorService.opCreate(userId, indicatorUpdate)
        return Result(result.code, result.msg, result.flag)
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        checkParams(userId, id)
        val result = indicatorService.opDelete(userId, id)
        return Result(result)
    }

    override fun update(userId: String, id: Long, indicatorUpdate: IndicatorUpdate): Result<Boolean> {
        checkParams(userId, id)
        val result = indicatorService.opUpdate(userId, id, indicatorUpdate)
        return Result(result.code, result.msg, result.flag)
    }

    private fun checkParams(userId: String, id: Long = 1) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        if (id <= 0L) throw ParamBlankException("Invalid id")
    }
}
