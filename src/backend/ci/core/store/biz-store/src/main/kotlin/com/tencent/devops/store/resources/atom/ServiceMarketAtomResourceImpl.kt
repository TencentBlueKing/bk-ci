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
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.api.atom.ServiceMarketAtomResource
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.atom.AtomPipeline
import com.tencent.devops.store.pojo.atom.AtomPostReqItem
import com.tencent.devops.store.pojo.atom.AtomPostResp
import com.tencent.devops.store.pojo.atom.AtomVersion
import com.tencent.devops.store.pojo.atom.GetRelyAtom
import com.tencent.devops.store.pojo.atom.InstallAtomReq
import com.tencent.devops.store.pojo.atom.enums.AtomStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.atom.AtomService
import com.tencent.devops.store.service.atom.MarketAtomService
import com.tencent.devops.store.service.atom.MarketAtomStatisticService
import com.tencent.devops.store.service.atom.impl.AtomMemberServiceImpl
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceMarketAtomResourceImpl @Autowired constructor(
    private val marketAtomService: MarketAtomService,
    private val atomService: AtomService,
    private val marketAtomStatisticService: MarketAtomStatisticService,
    private val atomMemberService: AtomMemberServiceImpl
) : ServiceMarketAtomResource {

    override fun setAtomBuildStatusByAtomCode(
        atomCode: String,
        version: String,
        userId: String,
        atomStatus: AtomStatusEnum,
        msg: String?
    ): Result<Boolean> {
        return marketAtomService.setAtomBuildStatusByAtomCode(atomCode, version, userId, atomStatus, msg)
    }

    override fun getProjectElements(projectCode: String): Result<Map<String, String>> {
        return atomService.getProjectElements(projectCode)
    }

    override fun getProjectElementsInfo(projectCode: String): Result<Map<String, String>> {
        return atomService.getProjectElementsInfo(projectCode)
    }

    override fun getAtomByCode(atomCode: String, username: String): Result<AtomVersion?> {
        return marketAtomService.getNewestAtomByCode(username, atomCode)
    }

    override fun getAtomPipelinesByCode(
        atomCode: String,
        username: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AtomPipeline>> {
        // 提供给openApi使用，暂时先使用username来验证权限，等后续openApi层统一加上具体资源访问权限后再去掉
        if (!atomMemberService.isStoreMember(username, atomCode, StoreTypeEnum.ATOM.type.toByte())) {
            return I18nUtil.generateResponseDataObject(
                messageCode = StoreMessageCode.USER_QUERY_ATOM_PERMISSION_IS_INVALID,
                params = arrayOf(atomCode),
                language = I18nUtil.getLanguage(username)
                )
        }
        return marketAtomStatisticService.getAtomPipelinesByCode(atomCode, username, page, pageSize)
    }

    override fun installAtom(
        userId: String,
        channelCode: ChannelCode?,
        installAtomReq: InstallAtomReq
    ): Result<Boolean> {
        return marketAtomService.installAtom("", userId, channelCode ?: ChannelCode.BS, installAtomReq)
    }

    override fun getPostAtoms(projectCode: String, atomItems: Set<AtomPostReqItem>): Result<AtomPostResp> {
        return marketAtomService.getPostAtoms(projectCode, atomItems)
    }

    override fun getAtomRely(getRelyAtom: GetRelyAtom): Result<Map<String, Map<String, Any>>?> {
        return Result(marketAtomService.getAtomsRely(getRelyAtom = getRelyAtom))
    }
}
