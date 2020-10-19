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

package com.tencent.bkrepo.repository.api

import com.tencent.bkrepo.common.api.pojo.Response
import com.tencent.bkrepo.repository.constant.SERVICE_NAME
import com.tencent.bkrepo.repository.pojo.download.DownloadStatisticsMetricResponse
import com.tencent.bkrepo.repository.pojo.download.DownloadStatisticsResponse
import com.tencent.bkrepo.repository.pojo.download.service.DownloadStatisticsAddRequest
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.context.annotation.Primary
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import java.time.LocalDate

@Api("构建下载量统计服务接口")
@Primary
@FeignClient(SERVICE_NAME, contextId = "DownloadStatisticsClient")
@RequestMapping("/service/download/statistics")
interface DownloadStatisticsClient {

    @ApiOperation("创建构建下载量")
    @PostMapping("/add")
    fun add(@RequestBody statisticsAddRequest: DownloadStatisticsAddRequest): Response<Void>

    @ApiOperation("查询构建下载量")
    @GetMapping("/query/{projectId}/{repoName}/{artifact}")
    fun query(
        @ApiParam("所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam("仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam("构建名称", required = true)
        @PathVariable artifact: String,
        @ApiParam("构建版本", required = false)
        @RequestParam version: String? = null,
        @ApiParam("开始日期", required = true)
        @RequestParam startDay: LocalDate,
        @ApiParam("结束日期", required = true)
        @RequestParam endDay: LocalDate
    ): Response<DownloadStatisticsResponse>

    @ApiOperation("查询构建在 日、周、月 的下载量")
    @GetMapping("/query/special/{projectId}/{repoName}/{artifact}")
    fun queryForSpecial(
        @ApiParam("所属项目", required = true)
        @PathVariable projectId: String,
        @ApiParam("仓库名称", required = true)
        @PathVariable repoName: String,
        @ApiParam("构建名称", required = true)
        @PathVariable artifact: String,
        @ApiParam("构建版本", required = false)
        @RequestParam version: String? = null
    ): Response<DownloadStatisticsMetricResponse>
}
