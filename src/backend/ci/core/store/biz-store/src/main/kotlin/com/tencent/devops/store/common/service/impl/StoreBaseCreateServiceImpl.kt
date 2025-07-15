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

package com.tencent.devops.store.common.service.impl

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.CommonUtils
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.dao.StoreProjectRelDao
import com.tencent.devops.store.common.dao.StoreStatisticTotalDao
import com.tencent.devops.store.common.service.StoreBaseCreateService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.common.utils.VersionUtils
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.STORE_BUS_NUM_LEN
import com.tencent.devops.store.pojo.common.enums.StoreMemberTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreProjectTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.StoreCreateRequest
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class StoreBaseCreateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseExtManageDao: StoreBaseExtManageDao,
    private val storeBaseFeatureManageDao: StoreBaseFeatureManageDao,
    private val storeBaseFeatureExtManageDao: StoreBaseFeatureExtManageDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao,
    private val storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao,
    private val storeStatisticTotalDao: StoreStatisticTotalDao,
    private val storeMemberDao: StoreMemberDao,
    private val storeProjectRelDao: StoreProjectRelDao
) : StoreBaseCreateService {

    override fun checkStoreCreateParam(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeType = storeBaseCreateRequest.storeType
        val version = storeBaseCreateRequest.version
        // 判断版本号是否合法
        VersionUtils.validateVersion(version, storeType)
        val storeCode = storeBaseCreateRequest.storeCode
        // 判断组件标识是否存在
        val codeCount = storeBaseQueryDao.countByCondition(
            dslContext = dslContext,
            storeType = storeType,
            storeCode = storeCode
        )
        if (codeCount > 0) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(storeCode)
            )
        }
        val name = storeBaseCreateRequest.name
        // 判断组件名称是否存在
        val nameCount = storeBaseQueryDao.countByCondition(dslContext = dslContext, storeType = storeType, name = name)
        if (nameCount > 0) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_EXIST,
                params = arrayOf(name)
            )
        }
    }

    override fun doStoreCreatePreBus(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeType = storeBaseCreateRequest.storeType
        getStoreSpecBusService(storeType).doStoreCreatePreBus(storeCreateRequest)
    }

    override fun doStoreCreateDataPersistent(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeType = storeBaseCreateRequest.storeType
        val storeCode = storeBaseCreateRequest.storeCode
        val version = storeBaseCreateRequest.version
        val storeId = DigestUtils.md5Hex("$storeType-$storeCode-$version")
        val name = storeBaseCreateRequest.name
        val bkStoreContext = storeCreateRequest.bkStoreContext
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        bkStoreContext[KEY_STORE_ID] = storeId
        val storeBaseDataPO = StoreBaseDataPO(
            id = storeId,
            storeCode = storeCode,
            storeType = storeType,
            name = name,
            version = version,
            status = StoreStatusEnum.INIT,
            creator = userId,
            modifier = userId,
            latestFlag = true,
            busNum = CommonUtils.generateNumber(VersionUtils.getMajorVersion(version, storeType), 1, STORE_BUS_NUM_LEN)
        )
        val storeBaseExtDataPOs = StoreReleaseUtils.generateStoreBaseExtDataPO(
            extBaseInfo = storeBaseCreateRequest.extBaseInfo,
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType,
            userId = userId
        )
        val (storeBaseFeatureDataPO, storeBaseFeatureExtDataPOs) = StoreReleaseUtils.generateStoreBaseFeaturePO(
            baseFeatureInfo = storeBaseCreateRequest.baseFeatureInfo,
            storeCode = storeCode,
            storeType = storeType,
            userId = userId
        )
        val (storeBaseEnvDataPOs, storeBaseEnvExtDataPOs) = StoreReleaseUtils.generateStoreBaseEnvPO(
            baseEnvInfos = storeBaseCreateRequest.baseEnvInfos,
            storeId = storeId,
            userId = userId
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeBaseManageDao.saveStoreBaseData(context, storeBaseDataPO)
            if (!storeBaseExtDataPOs.isNullOrEmpty()) {
                storeBaseExtManageDao.batchSave(context, storeBaseExtDataPOs)
            }
            storeBaseFeatureDataPO?.let {
                storeBaseFeatureManageDao.saveStoreBaseFeatureData(context, it)
            }
            if (!storeBaseFeatureExtDataPOs.isNullOrEmpty()) {
                storeBaseFeatureExtManageDao.batchSave(context, storeBaseFeatureExtDataPOs)
            }
            if (!storeBaseEnvDataPOs.isNullOrEmpty()) {
                storeBaseEnvManageDao.batchSave(context, storeBaseEnvDataPOs)
            }
            if (!storeBaseEnvExtDataPOs.isNullOrEmpty()) {
                storeBaseEnvExtManageDao.batchSave(context, storeBaseEnvExtDataPOs)
            }
            initStoreData(context, storeCode, storeType, userId, storeCreateRequest)
        }
    }

    override fun handlePostCreateBus(storeCreateRequest: StoreCreateRequest) {
        val storeBaseCreateRequest = storeCreateRequest.baseInfo
        val storeType = storeBaseCreateRequest.storeType
        val bkStoreContext = storeCreateRequest.bkStoreContext
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        val storeCode = storeBaseCreateRequest.storeCode
        getStoreSpecBusService(storeType).doStorePostCreateBus(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
    }

    private fun getStoreSpecBusService(storeType: StoreTypeEnum): StoreReleaseSpecBusService {
        return SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
    }

    private fun initStoreData(
        context: DSLContext,
        storeCode: String,
        storeType: StoreTypeEnum,
        userId: String,
        storeCreateRequest: StoreCreateRequest
    ) {
        // 初始化统计表数据
        storeStatisticTotalDao.initStatisticData(
            dslContext = context,
            storeCode = storeCode,
            storeType = storeType.type.toByte()
        )
        // 默认给新建组件的人赋予管理员权限
        storeMemberDao.addStoreMember(
            dslContext = context,
            userId = userId,
            storeCode = storeCode,
            userName = userId,
            type = StoreMemberTypeEnum.ADMIN.type.toByte(),
            storeType = storeType.type.toByte()
        )
        storeCreateRequest.projectCode?.let {
            // 添加组件与项目关联关系，type为0代表新增组件时关联的初始化项目
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = storeCode,
                projectCode = it,
                type = StoreProjectTypeEnum.INIT.type.toByte(),
                storeType = storeType.type.toByte()
            )
            storeProjectRelDao.addStoreProjectRel(
                dslContext = context,
                userId = userId,
                storeCode = storeCode,
                projectCode = it,
                type = StoreProjectTypeEnum.TEST.type.toByte(),
                storeType = storeType.type.toByte()
            )
        }
    }
}
