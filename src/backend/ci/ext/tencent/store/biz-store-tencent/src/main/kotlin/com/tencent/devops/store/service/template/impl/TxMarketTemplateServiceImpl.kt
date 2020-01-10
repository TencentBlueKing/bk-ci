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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
    private lateinit var storeDeptRelDao: StoreDeptRelDao

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
        logger.info("generateInstallFlag defaultFlag is:$defaultFlag,members is:$members,userId is:$userId")
        logger.info("generateInstallFlag visibleList is:$visibleList,userDeptList is:$userDeptList")
        return if (defaultFlag || (members != null && members.contains(userId))) {
            true
        } else {
            visibleList != null && (visibleList.contains(0) || visibleList.intersect(userDeptList).count() > 0)
        }
    }

    override fun generateUserAtomInvalidVisibleAtom(
        atomCode: String,
        userId: String,
        atomRecord: TAtomRecord,
        element: Element
    ): List<String> {
        val userDeptIdList = storeUserService.getUserDeptList(userId) // 获取用户的机构ID信息
        val invalidAtomList = mutableListOf<String>()
        val atomDeptRelRecords = storeDeptRelDao.batchList(dslContext, listOf(atomCode), StoreTypeEnum.ATOM.type.toByte())
        logger.info("the atomCode is :$atomCode,atomDeptRelRecords is :$atomDeptRelRecords")
        val isAtomMember = storeMemberDao.isStoreMember(dslContext, userId, atomCode, StoreTypeEnum.ATOM.type.toByte())
        logger.info("the isAtomMember is:$isAtomMember")
        // 如果插件是默认插件，则无需校验与用户的可见范围
        if (!atomRecord.defaultFlag) {
            var flag = false
            if (isAtomMember) {
                flag = true
            } else {
                atomDeptRelRecords?.forEach deptEach@{
                    val atomDeptId = it.deptId
                    if (userDeptIdList.contains(atomDeptId)) {
                        flag = true // 用户在原子插件的可见范围内
                        return@deptEach
                    } else {
                        // 判断该原子的可见范围是否设置了全公司可见
                        val parentDeptInfoList = client.get(ServiceProjectOrganizationResource::class).getParentDeptInfos(atomDeptId.toString(), 1).data
                        logger.info("the parentDeptInfoList is:$parentDeptInfoList")
                        if (null != parentDeptInfoList && parentDeptInfoList.isEmpty()) {
                            // 没有上级机构说明设置的可见范围是全公司
                            flag = true // 用户在原子插件的可见范围内
                            return@deptEach
                        }
                    }
                }
            }
            logger.info("the flag is:$flag")
            if (!flag) {
                invalidAtomList.add(element.name)
            }
        }
        return invalidAtomList
    }

    override fun validateTempleAtomVisible(templateCode: String, templateModel: Model): Result<Boolean> {
        // 校验模板与插件的可见范围
        val templateDeptInfos = storeVisibleDeptService.getVisibleDept(templateCode, StoreTypeEnum.TEMPLATE, DeptStatusEnum.APPROVED).data?.deptInfos
        return templateVisibleDeptService.validateTemplateVisibleDept(templateModel = templateModel, deptInfos = templateDeptInfos)
    }
}
