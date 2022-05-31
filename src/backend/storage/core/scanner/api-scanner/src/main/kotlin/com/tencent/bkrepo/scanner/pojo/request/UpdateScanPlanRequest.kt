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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.tencent.bkrepo.scanner.pojo.rule.ArtifactRule
import io.swagger.annotations.ApiModelProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class UpdateScanPlanRequest(
    @ApiModelProperty("方案ID")
    val id: String? = null,
    @ApiModelProperty("项目ID")
    val projectId: String? = null,
    @ApiModelProperty("方案名称")
    val name: String? = null,
    @ApiModelProperty("使用的扫描器")
    val scanner: String? = null,
    @ApiModelProperty("描述")
    val description: String? = null,
    @ApiModelProperty("是否自动扫描")
    val autoScan: Boolean? = null,
    @ApiModelProperty("自动扫描仓库")
    val repoNameList: List<String>? = null,
    @ApiModelProperty("自动扫描规则")
    val artifactRules: List<ArtifactRule>? = null
)
