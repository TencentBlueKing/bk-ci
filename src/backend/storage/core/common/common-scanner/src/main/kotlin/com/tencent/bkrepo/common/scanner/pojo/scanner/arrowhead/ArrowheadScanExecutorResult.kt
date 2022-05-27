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

package com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead

import com.tencent.bkrepo.common.scanner.pojo.scanner.ScanExecutorResult
import io.swagger.annotations.ApiModel
import io.swagger.annotations.ApiModelProperty

@ApiModel("arrowhead扫描器扫描结果")
data class ArrowheadScanExecutorResult(
    override val scanStatus: String,
    override val overview: Map<String, Any?>,
    @ApiModelProperty("安全审计结果")
    val checkSecItems: List<CheckSecItem>,
    @ApiModelProperty("License审计结果")
    val applicationItems: List<ApplicationItem>,
    @ApiModelProperty("敏感信息审计结果")
    val sensitiveItems: List<SensitiveItem>,
    @ApiModelProperty("cve审计结果")
    val cveSecItems: List<CveSecItem>
) : ScanExecutorResult(scanStatus, overview, ArrowheadScanner.TYPE) {
    companion object {

        fun overviewKeyOfSensitive(type: String): String {
            return "sensitive${type.capitalize()}Count"
        }

        fun overviewKeyOfCve(level: String): String {
            return "cve${level.capitalize()}Count"
        }

        fun overviewKeyOfLicenseRisk(riskLevel: String): String {
            val level = if (riskLevel.isEmpty()) {
                // 扫描器尚未支持的证书类型数量KEY
                "notAvailable"
            } else {
                riskLevel
            }
            return "licenseRisk${level.capitalize()}Count"
        }
    }
}
