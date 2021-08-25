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

package com.tencent.devops.store.service.template.impl

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.Model
import com.tencent.devops.store.pojo.common.UserStoreDeptInfoRequest
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import com.tencent.devops.store.service.template.TxMarketTemplateService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxMarketTemplateServiceImpl : TxMarketTemplateService, MarketTemplateServiceImpl() {

    @Autowired
    private lateinit var storeVisibleDeptService: StoreVisibleDeptService

    @Autowired
    private lateinit var templateVisibleDeptService: TemplateVisibleDeptService

    override fun validateTemplateVisibleDept(
        templateCode: String,
        templateModel: Model,
        validImageCodes: List<String>?,
        validAtomCodes: List<String>?
    ): Result<Boolean> {
        // 校验模板与插件的可见范围
        val templateDeptInfos = storeVisibleDeptService.getVisibleDept(
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            deptStatus = DeptStatusEnum.APPROVED
        ).data?.deptInfos
        return templateVisibleDeptService.validateTemplateVisibleDept(
            templateModel = templateModel,
            deptInfos = templateDeptInfos,
            validImageCodes = validImageCodes,
            validAtomCodes = validAtomCodes
        )
    }

    override fun checkUserInvalidVisibleStoreInfo(userStoreDeptInfoRequest: UserStoreDeptInfoRequest): Boolean {
        return storeVisibleDeptService.checkUserInvalidVisibleStoreInfo(userStoreDeptInfoRequest)
    }
}
