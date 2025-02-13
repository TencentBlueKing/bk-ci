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
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.store.atom.dao.TxAtomDao
import com.tencent.devops.store.common.dao.TxStoreBaseQueryDao
import com.tencent.devops.store.common.dao.TxStoreBelongDeptRelDao
import com.tencent.devops.store.common.service.StoreUserService
import com.tencent.devops.store.common.service.TxStoreBelongDeptService
import com.tencent.devops.store.ideatom.dao.IdeAtomDao
import com.tencent.devops.store.image.dao.OpImageDao
import com.tencent.devops.store.pojo.common.StoreBelongDeptRel
import com.tencent.devops.store.pojo.common.StoreDeptInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.ATOM
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.DEVX
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.IDE_ATOM
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.IMAGE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.SERVICE
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum.TEMPLATE
import com.tencent.devops.store.service.dao.ExtServiceDao
import java.util.concurrent.Executors
import org.jooq.DSLContext
import org.jooq.Record2
import org.jooq.Result
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxStoreBelongDeptServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val txStoreBelongDeptRelDao: TxStoreBelongDeptRelDao,
    private val txAtomDao: TxAtomDao,
    private val storeUserService: StoreUserService,
    private val client: Client,
    private val ideAtomDao: IdeAtomDao,
    private val extServiceDao: ExtServiceDao,
    private val opImageDao: OpImageDao,
    private val txStoreBaseQueryDao: TxStoreBaseQueryDao
) : TxStoreBelongDeptService {

    companion object {
        private const val DEFAULT_PAGE_SIZE = 100
        private val logger = LoggerFactory.getLogger(TxStoreBelongDeptServiceImpl::class.java)
    }

    override fun updateStoreBelongDept(userId: String, storeBelongDeptRel: StoreBelongDeptRel): Boolean {
        txStoreBelongDeptRelDao.batchAdd(userId, dslContext, listOf(storeBelongDeptRel))
        return true
    }

    override fun initStoreBelongDept(): Boolean {
        initStoreBelongDept(StoreTypeEnum.ATOM)
        initStoreBelongDept(StoreTypeEnum.SERVICE)
        initStoreBelongDept(StoreTypeEnum.IMAGE)
        initStoreBelongDept(StoreTypeEnum.IDE_ATOM)
        initStoreBelongDept(StoreTypeEnum.DEVX)
        return true
    }

    private fun initStoreBelongDept(storeTypeEnum: StoreTypeEnum) {
        Executors.newFixedThreadPool(1).submit {
            logger.info("begin initAtomBelongDept!!")
            var offset = 0
            do {
                val listStoreInitCreator = listStoreInitCreator(storeTypeEnum, offset)
                val storeBelongDeptRelList = mutableListOf<StoreBelongDeptRel>()
                listStoreInitCreator.forEach { atomInitCreator ->
                    val atomCode = atomInitCreator.value1()
                    val creator = atomInitCreator.value1()
                    // 获取用户组织架构
                    val userDeptInfo = getUserDeptInfo(creator)
                    userDeptInfo?.let {
                        storeBelongDeptRelList.add(
                            StoreBelongDeptRel(
                                storeCode = atomCode,
                                storeType = StoreTypeEnum.ATOM,
                                storeDeptInfo = it
                            )
                        )
                    }
                }
                txStoreBelongDeptRelDao.batchAdd(DEVOPS, dslContext, storeBelongDeptRelList)
                offset += DEFAULT_PAGE_SIZE
            } while (listStoreInitCreator.size == DEFAULT_PAGE_SIZE)
            logger.info("end initAtomBelongDept!!")
        }
    }

    private fun listStoreInitCreator(storeType: StoreTypeEnum, offset: Int): Result<Record2<String, String>> {
        return when (storeType) {
            ATOM -> {
                txAtomDao.listAtomInitCreator(dslContext, offset, DEFAULT_PAGE_SIZE)
            }
            IDE_ATOM -> {
                ideAtomDao.listAtomInitCreator(dslContext, offset, DEFAULT_PAGE_SIZE)
            }
            SERVICE -> {
                extServiceDao.listServiceInitCreator(dslContext, offset, DEFAULT_PAGE_SIZE)
            }
            IMAGE -> {
                opImageDao.listImageInitCreator(dslContext, offset, DEFAULT_PAGE_SIZE)
            }
            DEVX -> {
                txStoreBaseQueryDao.listStoreInitCreator(
                    dslContext = dslContext,
                    storeTypeEnum = DEVX,
                    offset = offset,
                    limit = DEFAULT_PAGE_SIZE
                )
            }

            TEMPLATE -> {
                txStoreBaseQueryDao.listTempLateInitCreator(dslContext, offset, DEFAULT_PAGE_SIZE)
            }
        }
    }

    private fun getUserDeptInfo(userId: String): StoreDeptInfo? {
        val userInfo = client.get(ServiceTxUserResource::class).get(userId).data
        return if (userInfo == null) {
            null
        } else {
            StoreDeptInfo(
                bgId = userInfo.bgId.toInt(),
                bgName = userInfo.bgName,
                deptId = userInfo.deptId.toInt(),
                deptName = userInfo.deptName,
                centerId = userInfo.centerId.toInt(),
                centerName = userInfo.centerName,
                groupId = userInfo.groupId.toInt(),
                groupName = userInfo.groupName,
                businessLineId = userInfo.businessLineId?.toLong(),
                businessLineName = userInfo.businessLineName,
            )
        }
    }
}
