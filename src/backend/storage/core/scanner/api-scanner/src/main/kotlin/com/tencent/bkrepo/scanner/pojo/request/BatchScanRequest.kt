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

import com.fasterxml.jackson.annotation.JsonAlias
import com.tencent.bkrepo.scanner.pojo.ScanTriggerType
import com.tencent.bkrepo.scanner.pojo.rule.ArtifactRule
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("方案批量扫描请求")
data class BatchScanRequest(
    @ApiModelProperty("项目ID")
    val projectId: String,
    @ApiModelProperty("方案ID")
    @JsonAlias("id")
    val planId: String,
    @ApiModelProperty("触发方式")
    @JsonAlias("triggerMethod")
    val triggerType: String = ScanTriggerType.MANUAL.name,
    /**
     * 指定扫描的仓库列表，未指定时使用扫描方案指定的仓库，都不存在时扫描全部仓库
     */
    @ApiModelProperty("仓库名")
    @JsonAlias("repoNameList")
    val repoNames: List<String> = emptyList(),
    /**
     * 指定扫描的制品名字与制品版本规则，未指定时使用扫描方案指定的规则，都不存在时扫描全部文件
     */
    @ApiModelProperty("制品规则")
    val artifactRules: List<ArtifactRule> = emptyList()
)
