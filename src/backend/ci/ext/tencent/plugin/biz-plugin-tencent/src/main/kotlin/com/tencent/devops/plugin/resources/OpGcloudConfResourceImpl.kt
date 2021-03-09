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

package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.OpGcloudConfResource
import com.tencent.devops.plugin.pojo.GcloudConfReq
import com.tencent.devops.plugin.pojo.GcloudConfResponse
import com.tencent.devops.plugin.service.gcloud.GcloudConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGcloudConfResourceImpl @Autowired constructor(
    private val gcloudConfService: GcloudConfService
) : OpGcloudConfResource {
    override fun create(userId: String, gcloudConfReq: GcloudConfReq): Result<Map<String, Int>> {
        with(gcloudConfReq) {
            return Result(gcloudConfService.createGcloudConf(region, address, fileAddress, userId, remark))
        }
    }

    override fun edit(userId: String, gcloudConfReq: GcloudConfReq): Result<Int> {
        with(gcloudConfReq) {
            return Result(gcloudConfService.updateGcloudConf(id, region, address, fileAddress, userId, remark))
        }
    }

    override fun delete(userId: String, confId: Int): Result<Int> {
        return Result(gcloudConfService.deleteGcloudConf(confId))
    }

    override fun getList(page: Int?, pageSize: Int?): Result<GcloudConfResponse> {
        val resultList = gcloudConfService.getList(page ?: 1, pageSize ?: 10)
        val resultCount = gcloudConfService.getCount()
        return Result(data = GcloudConfResponse(
                count = resultCount.toString(),
                page = page ?: 1,
                pageSize = pageSize ?: 10,
                totalPages = resultCount / (pageSize ?: 10) + 1,
                records = resultList
        ))
    }
}
