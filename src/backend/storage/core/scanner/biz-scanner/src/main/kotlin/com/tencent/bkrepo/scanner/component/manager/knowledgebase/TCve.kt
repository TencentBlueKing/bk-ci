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

package com.tencent.bkrepo.scanner.component.manager.knowledgebase

import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CvssV2
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CvssV3
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.index.CompoundIndexes
import org.springframework.data.mongodb.core.mapping.Document
import java.time.LocalDateTime

/**
 * Cve信息
 */
@Document("cve")
@CompoundIndexes(
    CompoundIndex(name = "pocId_idx", def = "{'pocId': 1}", unique = true),
    CompoundIndex(name = "cveId_idx", def = "{'cveId': 1}")
)
data class TCve(
    val id: String? = null,
    val createdBy: String,
    val createdDate: LocalDateTime,
    val lastModifiedBy: String,
    val lastModifiedDate: LocalDateTime,

    /**
     * 组件名
     */
    var component: String,

    /**
     * 漏洞影响版本
     */
    val versionEffected: String? = null,

    /**
     * 漏洞修复版本
     */
    val versionFixed: String? = null,

    /**
     * 漏洞名
     */
    val name: String,

    /**
     * 漏洞利用类型
     */
    val category: String? = null,

    /**
     * 漏洞类型
     */
    val categoryType: String? = null,

    /**
     * 漏洞描述
     */
    val description: String,

    /**
     * 官方解决方案
     */
    val officialSolution: String? = null,

    /**
     * 解决方案
     */
    val defenseSolution: String? = null,

    /**
     * 相关链接
     */
    val references: List<String> = emptyList(),

    /**
     * 漏洞年份
     */
    val cveYear: String,

    /**
     * poc id
     */
    val pocId: String,

    /**
     * cve id
     */
    val cveId: String? = null,

    /**
     * cnvd id
     */
    val cnvdId: String? = null,

    /**
     * cnnvd id
     */
    val cnnvdId: String? = null,

    /**
     * cwe id
     */
    val cweId: String? = null,

    /**
     * cvss等级
     * CRITICAL,HIGH,MEDIUM,LOW
     */
    val cvssRank: String,

    /**
     * cvss 评分
     */
    val cvss: Double,

    /**
     * cvss V3 漏洞影响评价
     */
    val cvssV3: CvssV3? = null,

    /**
     * cvss V2 漏洞影响评价
     */
    val cvssV2: CvssV2? = null
)
