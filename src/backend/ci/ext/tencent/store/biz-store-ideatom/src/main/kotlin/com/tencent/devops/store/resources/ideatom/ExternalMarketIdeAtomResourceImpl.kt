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

package com.tencent.devops.store.resources.ideatom

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.ideatom.ExternalMarketIdeAtomResource
import com.tencent.devops.store.pojo.ideatom.ExternalIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.IdeAtom
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomReq
import com.tencent.devops.store.pojo.ideatom.InstallIdeAtomResp
import com.tencent.devops.store.pojo.ideatom.enums.IdeAtomTypeEnum
import com.tencent.devops.store.pojo.ideatom.enums.MarketIdeAtomSortTypeEnum
import com.tencent.devops.store.service.ideatom.ExternalMarketIdeAtomService
import com.tencent.devops.store.service.ideatom.IdeAtomService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ExternalMarketIdeAtomResourceImpl @Autowired constructor(
    private val ideAtomService: IdeAtomService,
    private val externalMarketIdeAtomService: ExternalMarketIdeAtomService
) : ExternalMarketIdeAtomResource {

    override fun list(
        categoryCode: String,
        atomName: String?,
        classifyCode: String?,
        labelCodes: String?,
        score: Int?,
        rdType: IdeAtomTypeEnum?,
        sortType: MarketIdeAtomSortTypeEnum?,
        page: Int?,
        pageSize: Int?
    ): Result<ExternalIdeAtomResp> {
        return Result(externalMarketIdeAtomService.list(categoryCode, atomName, classifyCode, labelCodes, score, rdType, sortType, page, pageSize))
    }

    override fun getIdeAtomsByCode(atomCode: String): Result<IdeAtom?> {
        return ideAtomService.getIdeAtomByCode(atomCode)
    }

    override fun installIdeAtom(installIdeAtomReq: InstallIdeAtomReq): Result<InstallIdeAtomResp?> {
        return externalMarketIdeAtomService.installIdeAtom(installIdeAtomReq)
    }
}
