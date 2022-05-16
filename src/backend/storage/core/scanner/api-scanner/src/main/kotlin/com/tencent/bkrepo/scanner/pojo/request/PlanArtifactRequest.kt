/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2022 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bkrepo.scanner.pojo.request

import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_NUMBER
import com.tencent.bkrepo.common.api.constant.DEFAULT_PAGE_SIZE
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty
import java.time.Instant
import java.time.LocalDateTime

@ApiModel("请求指定扫描方案扫描过的制品扫描结果信息")
data class PlanArtifactRequest(
    @ApiModelProperty("扫描方案所属项目id", required = true)
    val projectId: String,
    @ApiModelProperty("扫描方案id", required = true)
    val id: String,
    @ApiModelProperty("扫描任务id，默认为扫描方案最新一次的扫描任务")
    var parentScanTaskId: String? = null,
    @ApiModelProperty("制品名关键字，只要制品名包含该关键字则匹配")
    val name: String? = null,
    /**
     * [com.tencent.bkrepo.scanner.pojo.LeakType]
     */
    @ApiModelProperty("制品最高等级漏洞")
    var highestLeakLevel: String? = null,
    /**
     * [com.tencent.bkrepo.common.artifact.pojo.RepositoryType]
     */
    @ApiModelProperty("制品所属仓库类型")
    val repoType: String? = null,
    @ApiModelProperty("制品所属仓库名")
    val repoName: String? = null,
    /**
     * [com.tencent.bkrepo.scanner.pojo.ScanStatus]
     */
    @ApiModelProperty("制品扫描状态")
    @Deprecated("仅用于兼容旧接口", ReplaceWith("subScanTaskStatus"))
    val status: String? = null,
    @ApiModelProperty("制品扫描状态")
    var subScanTaskStatus: List<String>? = null,
    @ApiModelProperty("制品开始扫描时间")
    val startTime: Instant? = null,
    @ApiModelProperty("制品开始扫描时间")
    var startDateTime: LocalDateTime? = null,
    @ApiModelProperty("制品扫描结束时间")
    val endTime: Instant? = null,
    @ApiModelProperty("制品扫描结束时间")
    var finishedDateTime: LocalDateTime? = null,
    @ApiModelProperty("页码")
    val pageNumber: Int = DEFAULT_PAGE_NUMBER,
    @ApiModelProperty("页大小")
    val pageSize: Int = DEFAULT_PAGE_SIZE
)
