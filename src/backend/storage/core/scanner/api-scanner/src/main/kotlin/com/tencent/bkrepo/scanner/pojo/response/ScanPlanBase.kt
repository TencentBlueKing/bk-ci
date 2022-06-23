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

package com.tencent.bkrepo.scanner.pojo.response

import com.tencent.bkrepo.common.query.model.Rule
import com.tencent.bkrepo.scanner.pojo.rule.ArtifactRule
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("扫描方案基础信息")
@Deprecated(
    "仅用于兼容旧接口",
    replaceWith = ReplaceWith(
        expression = "ScanPlan",
        imports = ["com.tencent.bkrepo.scanner.pojo.ScanPlan"]
    )
)
data class ScanPlanBase(
    @ApiModelProperty("方案id")
    val id: String,
    @ApiModelProperty("方案名")
    val name: String?,
    @ApiModelProperty("方案类型")
    val type: String,
    @ApiModelProperty("使用的扫描器")
    val scanner: String,
    @ApiModelProperty("描述")
    val description: String? = "",
    @ApiModelProperty("projectId")
    val projectId: String,
    @ApiModelProperty("是否开启自动扫描")
    @Deprecated("仅用于兼容旧接口", ReplaceWith("scanOnNewArtifact"))
    val autoScan: Boolean,
    @ApiModelProperty("是否有新制品上传时自动扫描")
    val scanOnNewArtifact: Boolean? = null,
    @ApiModelProperty("自动扫描仓库")
    @Deprecated("仅用于兼容旧接口", ReplaceWith("repoNames"))
    val repoNameList: List<String> = emptyList(),
    @ApiModelProperty("自动扫描仓库")
    val repoNames: List<String> = emptyList(),
    @ApiModelProperty("自动扫描规则")
    @Deprecated("仅用于兼容旧接口", ReplaceWith("rule"))
    val artifactRules: List<ArtifactRule> = emptyList(),
    @ApiModelProperty("自动扫描规则")
    val rule: Rule? = null,

    @ApiModelProperty("创建者")
    val createdBy: String,
    @ApiModelProperty("创建时间")
    val createdDate: String,
    @ApiModelProperty("修改者")
    val lastModifiedBy: String,
    @ApiModelProperty("修改时间")
    val lastModifiedDate: String
)
