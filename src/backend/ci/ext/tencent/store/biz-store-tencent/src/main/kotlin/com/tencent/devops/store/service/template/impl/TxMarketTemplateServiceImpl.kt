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
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.model.store.tables.records.TAtomRecord
import com.tencent.devops.model.store.tables.records.TStoreDeptRelRecord
import com.tencent.devops.project.api.service.ServiceProjectOrganizationResource
import com.tencent.devops.store.dao.common.StoreDeptRelDao
import com.tencent.devops.store.pojo.common.enums.DeptStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import com.tencent.devops.store.service.template.TemplateVisibleDeptService
import com.tencent.devops.store.service.template.TxMarketTemplateService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxMarketTemplateServiceImpl : TxMarketTemplateService, MarketTemplateServiceImpl() {

    @Autowired
    private lateinit var storeVisibleDeptService: StoreVisibleDeptService

    @Autowired
    private lateinit var templateVisibleDeptService: TemplateVisibleDeptService

    private val logger = LoggerFactory.getLogger(TxMarketTemplateServiceImpl::class.java)

    override fun generateTemplateVisibleData(
        storeCodeList: List<String?>,
        storeType: StoreTypeEnum
    ): Result<HashMap<String, MutableList<Int>>?> {
        logger.info("generateTemplateVisibleData storeCodeList is:$storeCodeList,storeType is:$storeType")
        return Result(storeVisibleDeptService.batchGetVisibleDept(storeCodeList, storeType).data)
    }

    override fun generateInstallFlag(
        defaultFlag: Boolean,
        members: MutableList<String>?,
        userId: String,
        visibleList: MutableList<Int>?,
        userDeptList: List<Int>
    ): Boolean {
        return if (defaultFlag || (members != null && members.contains(userId))) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    override fun generateUserAtomInvalidVisibleAtom(
        userId: String,
        userDeptIdList: List<Int>,
        atomCode: String,
        atomName: String,
        defaultFlag: Boolean,
        atomDeptList: List<TStoreDeptRelRecord>?
    ): List<String> {
        val invalidAtomList = mutableListOf<String>()
        // 如果插件是默认插件，则无需校验与用户的可见范围
        if (!defaultFlag) {
            val isAtomMember = storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = atomCode,
                storeType = StoreTypeEnum.ATOM.type.toByte()
            )
            val flag = getInvalidAtomFlag(
                isAtomMember = isAtomMember,
                atomDeptList = atomDeptList,
                userDeptIdList = userDeptIdList
            )
            if (!flag) {
                invalidAtomList.add(atomName)
            }
        }
        return invalidAtomList
    }

    private fun getInvalidAtomFlag(
        isAtomMember: Boolean,
        atomDeptList: List<TStoreDeptRelRecord>?,
        userDeptIdList: List<Int>
    ): Boolean {
        var flag = false
        if (isAtomMember) {
            flag = true
        } else {
            atomDeptList?.forEach deptEach@{ atomDept ->
                if (userDeptIdList.contains(atomDept.deptId)) {
                    flag = true // 用户在原子插件的可见范围内
                    return@deptEach
                } else {
                    // 判断该插件的可见范围是否设置了全公司可见
                    val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class)
                        .getParentDeptInfos(atomDept.deptId.toString(), 1).data
                    if (null != parentDeptInfoList && parentDeptInfoList.isEmpty()) {
                        // 没有上级机构说明设置的可见范围是全公司
                        flag = true // 用户在插件的可见范围内
                        return@deptEach
                    }
                }
            }
        }
        return flag
    }

    override fun validateTempleAtomVisible(templateCode: String, templateModel: Model): Result<Boolean> {
        // 校验模板与插件的可见范围
        val templateDeptInfos = storeVisibleDeptService.getVisibleDept(
            storeCode = templateCode,
            storeType = StoreTypeEnum.TEMPLATE,
            deptStatus = DeptStatusEnum.APPROVED
        ).data?.deptInfos
        return templateVisibleDeptService.validateTemplateVisibleDept(
            templateModel = templateModel,
            deptInfos = templateDeptInfos
        )
    }
}
