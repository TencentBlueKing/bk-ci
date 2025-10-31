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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.client.Client
import com.tencent.devops.project.api.service.ServiceProjectResource
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.TxStoreBelongDeptRelDao
import com.tencent.devops.store.common.service.TxStoreBelongDeptService
import com.tencent.devops.store.pojo.common.StoreBelongDeptRel
import com.tencent.devops.store.pojo.common.StoreDeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.ATOM
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.IDE_ATOM
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.IMAGE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.SERVICE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.TEMPLATE
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxStoreBelongDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val txStoreBelongDeptRelDao: TxStoreBelongDeptRelDao,
    private val client: Client,
    private val storeProjectRelDao: StoreProjectRelDao
) : TxStoreBelongDeptService {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private val logger = LoggerFactory.getLogger(TxStoreBelongDeptServiceImpl::class.java)
    }

    override fun updateStoreBelongDept(userId: String, storeBelongDeptRel: StoreBelongDeptRel): Boolean {
        txStoreBelongDeptRelDao.batchAdd(userId, dslContext, listOf(storeBelongDeptRel))
        return true
    }

    override fun initAllStoreBelongDept(): Boolean {
        initStoreBelongDept(ATOM)
        initStoreBelongDept(SERVICE)
        initStoreBelongDept(IMAGE)
        initStoreBelongDept(IDE_ATOM)
        initStoreBelongDept(TEMPLATE)
        return true
    }

    override fun initStoreBelongDept(userId: String, storeType: StoreTypeEnum, storeCode: String) {
        try {
            val userDeptInfo = getUserDeptInfo(userId)
            userDeptInfo?.let {
                txStoreBelongDeptRelDao.add(
                    userId,
                    dslContext,
                    StoreBelongDeptRel(
                        storeCode = storeCode,
                        storeType = storeType,
                        storeDeptInfo = it
                    )
                )
            }
        } catch (ignored: Throwable) {
            logger.warn("initStoreBelongDept error: ${ignored.message}")
        }
    }

    override fun getStoreBelongDept(userId: String, storeCode: String, storeType: StoreTypeEnum): StoreBelongDeptRel? {
        return txStoreBelongDeptRelDao.getByStoreCodeAndType(dslContext, storeCode, storeType)
    }

    fun initStoreBelongDept(storeTypeEnum: StoreTypeEnum) {
        val executor = Executors.newFixedThreadPool(1)
        try {
            executor.submit {
                logger.info("begin initAtomBelongDept!!")
                var offset = 0
                do {
                    val listStoreInitProjectInfo = storeProjectRelDao.listStoreInitProjectCode(
                        dslContext = dslContext,
                        storeType = storeTypeEnum.type.toByte(),
                        limit = DEFAULT_PAGE_SIZE,
                        offset = offset
                    )
                    val initProjectCodes = listStoreInitProjectInfo.map { it.value2() }.toSet()
                    val storeBelongDeptRelList = mutableListOf<StoreBelongDeptRel>()
                    val projectInfoList =
                        client.get(ServiceProjectResource::class).listOnlyByProjectCode(initProjectCodes).data
                    projectInfoList ?: return@submit
                    listStoreInitProjectInfo.forEach { storeInitProject ->
                        val storeCode = storeInitProject.value1()
                        val initProjectCode = storeInitProject.value2()
                        // 获取初始化项目组织架构
                        projectInfoList.find { it.englishName == initProjectCode }?.let {
                            storeBelongDeptRelList.add(
                                StoreBelongDeptRel(
                                    storeCode = storeCode,
                                    storeType = storeTypeEnum,
                                    storeDeptInfo = StoreDeptInfo(
                                        bgId = it.bgId!!,
                                        bgName = it.bgName!!,
                                        deptId = it.deptId,
                                        deptName = it.deptName,
                                        centerId = it.centerId,
                                        centerName = it.centerName,
                                        businessLineId = it.businessLineId,
                                        businessLineName = it.businessLineName
                                    )
                                )
                            )
                        }

                    }
                    txStoreBelongDeptRelDao.batchAdd(DEVOPS, dslContext, storeBelongDeptRelList)
                    offset += DEFAULT_PAGE_SIZE
                } while (listStoreInitProjectInfo.size == DEFAULT_PAGE_SIZE)
                logger.info("end initAtomBelongDept!!")
            }
        } catch (ignored: Throwable) {
            logger.warn("initStoreBelongDept error: ${ignored.message}")
        } finally {
            executor.shutdown()
        }
    }

    private fun getUserDeptInfo(userId: String): StoreDeptInfo? {
        val userInfo = client.get(ServiceTxUserResource::class).get(userId).data
        return if (userInfo == null) {
            null
        } else {
            StoreDeptInfo(
                bgId = userInfo.bgId,
                bgName = userInfo.bgName,
                deptId = userInfo.deptId,
                deptName = userInfo.deptName,
                centerId = userInfo.centerId,
                centerName = userInfo.centerName,
                groupId = userInfo.groupId,
                groupName = userInfo.groupName,
                businessLineId = userInfo.businessLineId,
                businessLineName = userInfo.businessLineName,
            )
        }
    }
}
