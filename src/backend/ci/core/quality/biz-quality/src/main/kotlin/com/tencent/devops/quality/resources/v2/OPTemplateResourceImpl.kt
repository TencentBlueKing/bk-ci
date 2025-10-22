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
import com.tencent.devops.quality.api.v2.OPTemplateResource
import com.tencent.devops.quality.api.v2.pojo.op.TemplateData
import com.tencent.devops.quality.api.v2.pojo.op.TemplateUpdateData
import com.tencent.devops.quality.service.v2.QualityTemplateService
import org.springframework.beans.factory.annotation.Autowired

/**
 * @author eltons,  Date on 2019-03-01.
 */
@RestResource
class OPTemplateResourceImpl @Autowired constructor(
    private val qualityTemplateService: QualityTemplateService
) : OPTemplateResource {
    override fun list(userId: String, page: Int?, pageSize: Int?): Result<Page<TemplateData>> {
        checkParams(userId)
        val result = qualityTemplateService.opList(userId, page, pageSize)
        return Result(result)
    }

    override fun add(userId: String, templateUpdateData: TemplateUpdateData): Result<Boolean> {
        checkParams(userId)
        val result = qualityTemplateService.opCreate(userId, templateUpdateData)
        return Result(result)
    }

    override fun delete(userId: String, id: Long): Result<Boolean> {
        checkParams(userId, id)
        val result = qualityTemplateService.opDelete(userId, id)
        return Result(result)
    }

    override fun update(userId: String, id: Long, templateUpdateData: TemplateUpdateData): Result<Boolean> {
        checkParams(userId, id)
        val result = qualityTemplateService.opUpdate(userId, id, templateUpdateData)
        return Result(result)
    }

    private fun checkParams(userId: String, id: Long = 1) {
        if (userId.isBlank()) throw ParamBlankException("Invalid userId")
        if (id <= 0L) throw ParamBlankException("Invalid id")
    }
}
