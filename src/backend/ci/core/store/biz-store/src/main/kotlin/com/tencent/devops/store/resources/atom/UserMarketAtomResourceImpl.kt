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

package com.tencent.devops.store.resources.atom

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.atom.UserMarketAtomResource
import com.tencent.devops.store.pojo.atom.AtomDevLanguage
import com.tencent.devops.store.pojo.atom.AtomOutput
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.AtomVersionListItem
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.MarketAtomResp
import com.tencent.devops.store.pojo.atom.MarketMainItem
import com.tencent.devops.store.pojo.atom.MyAtomResp
import com.tencent.devops.store.pojo.atom.enums.AtomTypeEnum
import com.tencent.devops.store.pojo.atom.enums.MarketAtomSortTypeEnum
import com.tencent.devops.store.pojo.common.InstalledProjRespItem
import com.tencent.devops.store.pojo.common.StoreErrorCodeInfo
import com.tencent.devops.store.pojo.common.StoreShowVersionInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.common.StoreProjectService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
@Suppress("ALL")
class UserMarketAtomResourceImpl @Autowired constructor(
    private val marketAtomService: MarketAtomService,
    private val storeProjectService: StoreProjectService
) : UserMarketAtomResource {

    override fun mainPageList(userId: String, page: Int?, pageSize: Int?): Result<List<MarketMainItem>> {
        return marketAtomService.mainPageList(userId, page, pageSize, urlProtocolTrim = true)
    }

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

    override fun listMyAtoms(
        accessToken: String,
        userId: String,
        atomName: String?,
        page: Int,
        pageSize: Int
    ): Result<MyAtomResp?> {
        return marketAtomService.getMyAtoms(accessToken, userId, atomName, page, pageSize)
    }

    override fun getAtomById(userId: String, atomId: String): Result<AtomVersion?> {
        return marketAtomService.getAtomById(atomId, userId)
    }

    override fun getAtomByCode(userId: String, atomCode: String): Result<AtomVersion?> {
        return marketAtomService.getAtomByCode(userId, atomCode)
    }

    override fun getAtomVersionsByCode(
        userId: String,
        atomCode: String,
        page: Int,
        pageSize: Int
    ): Result<Page<AtomVersionListItem>> {
        return marketAtomService.getAtomVersionsByCode(userId, atomCode, page, pageSize)
    }

    override fun installAtom(accessToken: String, userId: String, installAtomReq: InstallAtomReq): Result<Boolean> {
        return marketAtomService.installAtom(accessToken, userId, ChannelCode.BS, installAtomReq)
    }

    override fun getInstalledProjects(
        accessToken: String,
        userId: String,
        atomCode: String
    ): Result<List<InstalledProjRespItem?>> {
        return storeProjectService.getInstalledProjects(accessToken, userId, atomCode, StoreTypeEnum.ATOM)
    }

    override fun listLanguage(): Result<List<AtomDevLanguage?>> {
        return marketAtomService.listLanguage()
    }

    override fun deleteAtom(userId: String, atomCode: String): Result<Boolean> {
        return marketAtomService.deleteAtom(userId, atomCode)
    }

    override fun getAtomShowVersionInfo(userId: String, atomCode: String): Result<StoreShowVersionInfo> {
        return marketAtomService.getAtomShowVersionInfo(userId, atomCode)
    }

    override fun getAtomYmlInfo(userId: String, atomCode: String, defaultShowFlag: Boolean?): Result<String?> {
        return Result(marketAtomService.generateCiYaml(atomCode = atomCode, defaultShowFlag = defaultShowFlag ?: false))
    }

    override fun getAtomYmlV2Info(userId: String, atomCode: String, defaultShowFlag: Boolean?): Result<String?> {
        return Result(marketAtomService.generateCiV2Yaml(
            atomCode = atomCode,
            defaultShowFlag = defaultShowFlag ?: false)
        )
    }

    override fun getAtomOutput(userId: String, atomCode: String): Result<List<AtomOutput>> {
        return Result(marketAtomService.getAtomOutput(atomCode))
    }

    override fun updateAtomErrorCodeInfo(
        userId: String,
        projectCode: String,
        storeErrorCodeInfo: StoreErrorCodeInfo
    ): Result<Boolean> {
        return marketAtomService.updateAtomErrorCodeInfo(userId, projectCode, storeErrorCodeInfo)
    }
}
