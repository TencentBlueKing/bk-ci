/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
package com.tencent.devops.store.atom.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.ServiceAtomResource
import com.tencent.devops.store.atom.service.AtomPropService
import com.tencent.devops.store.atom.service.AtomService
import com.tencent.devops.store.atom.service.MarketAtomClassifyService
import com.tencent.devops.store.atom.service.MarketAtomService
import com.tencent.devops.store.pojo.atom.AtomClassifyInfo
import com.tencent.devops.store.pojo.atom.AtomCodeVersionReqItem
import com.tencent.devops.store.pojo.atom.AtomProp
import com.tencent.devops.store.pojo.atom.AtomRunInfo
import com.tencent.devops.store.pojo.atom.InstalledAtom
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.PipelineAtom
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceAtomResourceImpl @Autowired constructor(
    private val atomService: AtomService,
    private val marketAtomService: MarketAtomService,
    private val atomPropService: AtomPropService,
    private val atomClassifyService: MarketAtomClassifyService
) : ServiceAtomResource {

    override fun list(
        userId: String,
        keyword: String?,
        classifyCode: String?,
        labelCode: String?,
        score: Int?,
        rdType: AtomTypeEnum?,
        yamlFlag: Boolean?,
        recommendFlag: Boolean?,
        qualityFlag: Boolean?,
        sortType: MarketAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<MarketAtomResp> {
        return Result(
            marketAtomService.list(
                userId = userId.trim(),
                keyword = keyword?.trim(),
                classifyCode = classifyCode?.trim(),
                labelCode = labelCode?.trim(),
                score = score,
                rdType = rdType,
                yamlFlag = yamlFlag,
                recommendFlag = recommendFlag,
                qualityFlag = qualityFlag,
                sortType = sortType,
                page = page,
                pageSize = pageSize,
                urlProtocolTrim = true
            )
        )
    }

    override fun getInstalledAtoms(
        projectCode: String
    ): Result<List<InstalledAtom>> {
        return Result(atomService.listInstalledAtomByProject(projectCode))
    }

    override fun getAtomVersionInfo(atomCode: String, version: String): Result<PipelineAtom?> {
        return atomService.getPipelineAtomDetail(atomCode = atomCode, version = version)
    }

    override fun getAtomInfos(
        codeVersions: Set<AtomCodeVersionReqItem>
    ): Result<List<AtomRunInfo>> {
        return atomService.getAtomInfos(codeVersions = codeVersions)
    }

    override fun getAtomRealVersion(projectCode: String, atomCode: String, version: String): Result<String?> {
        return atomService.getAtomRealVersion(projectCode = projectCode, atomCode = atomCode, version = version)
    }

    override fun getAtomProps(atomCodes: Set<String>): Result<Map<String, AtomProp>?> {
        return Result(atomPropService.getAtomProps(atomCodes))
    }

    override fun getAtomClassifyInfo(atomCode: String): Result<AtomClassifyInfo?> {
        return atomClassifyService.getAtomClassifyInfo(atomCode)
    }
}
