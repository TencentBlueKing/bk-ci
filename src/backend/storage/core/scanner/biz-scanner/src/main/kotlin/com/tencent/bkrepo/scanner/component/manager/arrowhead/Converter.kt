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

package com.tencent.bkrepo.scanner.component.manager.arrowhead

import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.ApplicationItem
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.CveSecItem
import com.tencent.bkrepo.common.scanner.pojo.scanner.arrowhead.License
import com.tencent.bkrepo.repository.constant.SYSTEM_USER
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TApplicationItem
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TApplicationItemData
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TCveSecItem
import com.tencent.bkrepo.scanner.component.manager.arrowhead.model.TCveSecItemData
import com.tencent.bkrepo.scanner.component.manager.knowledgebase.TCve
import com.tencent.bkrepo.scanner.component.manager.knowledgebase.TLicense
import java.time.LocalDateTime

object Converter {
    fun convertToLicense(license: License, now: LocalDateTime = LocalDateTime.now()) = TLicense(
        createdBy = SYSTEM_USER,
        createdDate = now,
        lastModifiedBy = SYSTEM_USER,
        lastModifiedDate = now,
        name = license.name,
        content = license.content,
        source = license.source,
        risk = license.risk
    )

    fun convert(applicationItem: ApplicationItem) = TApplicationItemData(
        path = applicationItem.path,
        component = applicationItem.component,
        version = applicationItem.version,
        licenseName = applicationItem.license?.name,
        risk = applicationItem.license?.risk
    )

    fun convert(applicationItem: TApplicationItem, tLicense: TLicense?): ApplicationItem {
        val license = tLicense
            ?.let { convertToLicense(it) }
            ?: applicationItem.data.licenseName?.let { License(name = it, content = "", source = "", risk = "") }
        return ApplicationItem(
            path = applicationItem.data.path,
            component = applicationItem.data.component,
            version = applicationItem.data.version,
            license = license
        )
    }

    fun convertToCve(cveSecItem: CveSecItem, now: LocalDateTime = LocalDateTime.now()) = with(cveSecItem) {
        TCve(
            createdBy = SYSTEM_USER,
            createdDate = now,
            lastModifiedBy = SYSTEM_USER,
            lastModifiedDate = now,
            component = component,
            versionEffected = versionEffected,
            versionFixed = versionFixed,
            name = name,
            category = category,
            categoryType = categoryType,
            description = description,
            officialSolution = officialSolution,
            defenseSolution = defenseSolution,
            references = references,
            cveYear = cveYear,
            pocId = pocId,
            cveId = cveId,
            cnvdId = cnvdId,
            cnnvdId = cnnvdId,
            cweId = cweId,
            cvssRank = cvssRank,
            cvss = cvss,
            cvssV3 = cvssV3,
            cvssV2 = cvssV2
        )
    }

    fun convert(cveSecItem: CveSecItem): TCveSecItemData = with(cveSecItem) {
        TCveSecItemData(
            path = path,
            component = component,
            versions = versions,
            pocId = pocId,
            cveId = cveId,
            cweId = cweId,
            cnnvdId = cnnvdId,
            cnvdId = cnvdId,
            cvssRank = cvssRank
        )
    }

    fun convert(cveSecItem: TCveSecItem, cve: TCve?): CveSecItem {
        return CveSecItem(
            path = cveSecItem.data.path,
            component = orEmpty(cveSecItem.data.component),
            version = cveSecItem.data.versions.firstOrNull() ?: "",
            versions = cveSecItem.data.versions.toMutableSet(),
            versionEffected = orEmpty(cve?.versionEffected),
            versionFixed = orEmpty(cve?.versionFixed),
            name = orEmpty(cve?.name),
            category = orEmpty(cve?.category),
            categoryType = orEmpty(cve?.categoryType),
            description = orEmpty(cve?.description),
            officialSolution = orEmpty(cve?.officialSolution),
            defenseSolution = orEmpty(cve?.defenseSolution),
            references = cve?.references ?: emptyList(),
            cveYear = orEmpty(cve?.cveYear),
            cveId = cveSecItem.data.cveId,
            pocId = orEmpty(cve?.pocId),
            cnvdId = orEmpty(cve?.cnvdId),
            cnnvdId = orEmpty(cve?.cnnvdId),
            cweId = orEmpty(cve?.cweId),
            cvssRank = cveSecItem.data.cvssRank,
            cvss = cve?.cvss ?: 0.0,
            cvssV3 = cve?.cvssV3,
            cvssV2 = cve?.cvssV2
        )
    }

    private fun orEmpty(value: String?): String {
        return value ?: ""
    }

    private fun convertToLicense(license: TLicense) = License(
        name = license.name,
        content = license.content,
        source = license.source,
        risk = license.risk
    )
}
