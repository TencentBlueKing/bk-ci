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

import org.springframework.stereotype.Component

@Component
open class MongoKnowledgeBase(
    private val cveDao: CveDao,
    private val licenseDao: LicenseDao
) : KnowledgeBase {
    override fun saveLicense(license: TLicense) {
        licenseDao.saveIfNotExists(license)
    }

    override fun saveLicenses(licenses: Collection<TLicense>) {
        licenseDao.saveIfNotExists(licenses)
    }

    override fun findLicense(licenceName: String): TLicense? {
        return licenseDao.findByName(licenceName)
    }

    override fun findLicense(licenseNames: Collection<String>): List<TLicense> {
        return licenseDao.findByNames(licenseNames)
    }

    override fun saveCve(cve: TCve) {
        cveDao.saveIfNotExists(cve)
    }

    override fun saveCve(cveList: Collection<TCve>) {
        cveDao.saveIfNotExists(cveList)
    }

    override fun findCve(cveId: String): TCve? {
        return cveDao.findByCveId(cveId)
    }

    override fun findCve(cveIds: Collection<String>): List<TCve> {
        return cveDao.findByCveIds(cveIds)
    }
}
