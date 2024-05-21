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

import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID
import com.tencent.devops.common.api.auth.AUTH_HEADER_USER_ID_DEFAULT_VALUE
import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.INIT_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.StoreBaseEnvExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseEnvManageDao
import com.tencent.devops.store.common.dao.StoreBaseExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtManageDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureManageDao
import com.tencent.devops.store.common.dao.StoreBaseManageDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreCategoryRelDao
import com.tencent.devops.store.common.dao.StoreLabelRelDao
import com.tencent.devops.store.common.dao.StoreVersionLogDao
import com.tencent.devops.store.common.service.StoreBaseUpdateService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreReleaseSpecBusService
import com.tencent.devops.store.common.utils.StoreReleaseUtils
import com.tencent.devops.store.common.utils.StoreUtils
import com.tencent.devops.store.pojo.common.KEY_CLASSIFY_ID
import com.tencent.devops.store.pojo.common.KEY_STORE_ID
import com.tencent.devops.store.pojo.common.enums.ReleaseTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreBaseDataPO
import com.tencent.devops.store.pojo.common.publication.StoreUpdateRequest
import java.time.LocalDateTime
import org.apache.commons.codec.digest.DigestUtils
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
@Suppress("LongParameterList")
class StoreBaseUpdateServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val classifyDao: ClassifyDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseManageDao: StoreBaseManageDao,
    private val storeBaseExtManageDao: StoreBaseExtManageDao,
    private val storeBaseFeatureManageDao: StoreBaseFeatureManageDao,
    private val storeBaseFeatureExtManageDao: StoreBaseFeatureExtManageDao,
    private val storeBaseEnvManageDao: StoreBaseEnvManageDao,
    private val storeBaseEnvExtManageDao: StoreBaseEnvExtManageDao,
    private val storeLabelRelDao: StoreLabelRelDao,
    private val storeCategoryRelDao: StoreCategoryRelDao,
    private val storeVersionLogDao: StoreVersionLogDao,
    private val storeCommonService: StoreCommonService
) : StoreBaseUpdateService {

    override fun doStoreI18nConversion(storeUpdateRequest: StoreUpdateRequest) {
        val storeBaseUpdateRequest = storeUpdateRequest.baseInfo
        val storeType = storeBaseUpdateRequest.storeType
        getStoreSpecBusService(storeType).doStoreI18nConversionSpecBus(storeUpdateRequest)
    }

    override fun checkStoreUpdateParam(
        storeUpdateRequest: StoreUpdateRequest
    ) {
        val storeBaseUpdateRequest = storeUpdateRequest.baseInfo
        val storeType = storeBaseUpdateRequest.storeType
        val storeCode = storeBaseUpdateRequest.storeCode
        val name = storeBaseUpdateRequest.name
        val versionInfo = storeBaseUpdateRequest.versionInfo
        val classifyCode = storeBaseUpdateRequest.classifyCode
        // 校验分类信息是否准确
        val classifyRecord =
            classifyDao.getClassifyByCode(dslContext, classifyCode, storeType) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(classifyCode)
            )
        storeUpdateRequest.bkStoreContext[KEY_CLASSIFY_ID] = classifyRecord.id
        // 判断名称是否重复（升级允许名称一样）
        validateStoreName(storeType = storeType, storeCode = storeCode, name = name)
        // 校验前端传的版本号是否正确
        storeCommonService.validateStoreVersion(
            storeCode = storeCode,
            storeType = storeType,
            versionInfo = versionInfo,
            name = name
        )
        // 处理检查组件升级参数个性化逻辑
        getStoreSpecBusService(storeType).doCheckStoreUpdateParamSpecBus(storeUpdateRequest)
    }

    override fun doStoreUpdateDataPersistent(storeUpdateRequest: StoreUpdateRequest) {
        val storeBaseUpdateRequest = storeUpdateRequest.baseInfo
        val storeType = storeBaseUpdateRequest.storeType
        val storeCode = storeBaseUpdateRequest.storeCode
        val name = storeBaseUpdateRequest.name
        val versionInfo = storeBaseUpdateRequest.versionInfo
        val version = versionInfo.version
        val releaseType = versionInfo.releaseType
        val newestBaseRecord =
            storeBaseQueryDao.getNewestComponentByCode(dslContext, storeCode, storeType) ?: throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(storeCode)
            )
        // 判断当次发布是否是新增版本
        val newVersionFlag = !(releaseType == ReleaseTypeEnum.NEW || releaseType == ReleaseTypeEnum.CANCEL_RE_RELEASE)
        // 获取组件ID
        val storeId = if (newVersionFlag) {
            DigestUtils.md5Hex("$storeType-$storeCode-$version")
        } else {
            newestBaseRecord.id
        }
        val latestFlag = version == INIT_VERSION && !newVersionFlag
        val bkStoreContext = storeUpdateRequest.bkStoreContext
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        bkStoreContext[KEY_STORE_ID] = storeId
        val status = getStoreSpecBusService(storeType).getStoreUpdateStatus()
        val extBaseInfo = storeBaseUpdateRequest.extBaseInfo
        val storeBaseDataPO = StoreBaseDataPO(
            id = storeId,
            storeCode = storeCode,
            storeType = storeType,
            name = name,
            version = version,
            status = status,
            logoUrl = storeBaseUpdateRequest.logoUrl,
            summary = storeBaseUpdateRequest.summary,
            description = storeBaseUpdateRequest.description,
            latestFlag = latestFlag,
            publisher = versionInfo.publisher,
            pubTime = LocalDateTime.now(),
            classifyId = bkStoreContext[KEY_CLASSIFY_ID].toString(),
            creator = userId,
            modifier = userId
        )
        val storeBaseExtDataPOs = StoreReleaseUtils.generateStoreBaseExtDataPO(
            extBaseInfo = extBaseInfo,
            storeId = storeId,
            storeCode = storeCode,
            storeType = storeType,
            userId = userId
        )
        val baseFeatureInfo = storeBaseUpdateRequest.baseFeatureInfo
        val (storeBaseFeatureDataPO, storeBaseFeatureExtDataPOs) = StoreReleaseUtils.generateStoreBaseFeaturePO(
            baseFeatureInfo = baseFeatureInfo,
            storeCode = storeCode,
            storeType = storeType,
            userId = userId
        )
        val baseEnvInfos = storeBaseUpdateRequest.baseEnvInfos
        val (storeBaseEnvDataPOs, storeBaseEnvExtDataPOs) = StoreReleaseUtils.generateStoreBaseEnvPO(
            baseEnvInfos = baseEnvInfos,
            storeId = storeId,
            userId = userId
        )
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeBaseManageDao.saveStoreBaseData(context, storeBaseDataPO)
            if (!storeBaseExtDataPOs.isNullOrEmpty()) {
                storeBaseExtManageDao.deleteStoreBaseExtInfo(context, storeId)
                storeBaseExtManageDao.batchSave(context, storeBaseExtDataPOs)
            }
            storeBaseFeatureDataPO?.let {
                storeBaseFeatureManageDao.saveStoreBaseFeatureData(context, it)
            }
            if (!storeBaseFeatureExtDataPOs.isNullOrEmpty()) {
                storeBaseFeatureExtManageDao.deleteStoreBaseFeatureExtInfo(context, storeCode, storeType.type.toByte())
                storeBaseFeatureExtManageDao.batchSave(context, storeBaseFeatureExtDataPOs)
            }
            if (!storeBaseEnvDataPOs.isNullOrEmpty()) {
                storeBaseEnvManageDao.deleteStoreEnvInfo(context, storeId)
                storeBaseEnvManageDao.batchSave(context, storeBaseEnvDataPOs)
            }
            if (!storeBaseEnvExtDataPOs.isNullOrEmpty()) {
                storeBaseEnvExtManageDao.deleteStoreEnvExtInfo(context, storeId)
                storeBaseEnvExtManageDao.batchSave(context, storeBaseEnvExtDataPOs)
            }
            storeLabelRelDao.deleteByStoreId(context, storeId)
            val labelIdList = storeBaseUpdateRequest.labelIdList?.filter { it.isNotBlank() }
            if (!labelIdList.isNullOrEmpty()) {
                storeLabelRelDao.batchAdd(
                    dslContext = context,
                    userId = userId,
                    storeId = storeId,
                    labelIdList = labelIdList
                )
            }
            storeCategoryRelDao.deleteByStoreId(context, storeId)
            val categoryIdList = storeBaseUpdateRequest.categoryIdList?.filter { it.isNotBlank() }
            if (!categoryIdList.isNullOrEmpty()) {
                storeCategoryRelDao.batchAdd(
                    dslContext = context,
                    userId = userId,
                    storeId = storeId,
                    categoryIdList = categoryIdList
                )
            }
            storeVersionLogDao.saveStoreVersion(
                dslContext = context,
                userId = userId,
                storeId = storeId,
                releaseType = releaseType,
                versionContent = versionInfo.versionContent
            )
        }
    }

    private fun getStoreSpecBusService(storeType: StoreTypeEnum): StoreReleaseSpecBusService {
        return SpringContextUtil.getBean(
            StoreReleaseSpecBusService::class.java,
            StoreUtils.getReleaseSpecBusServiceBeanName(storeType)
        )
    }

    private fun validateStoreName(
        storeType: StoreTypeEnum,
        storeCode: String,
        name: String
    ) {
        var flag = false
        val count = storeBaseQueryDao.countByCondition(dslContext = dslContext, storeType = storeType, name = name)
        if (count > 0) {
            flag = storeBaseQueryDao.countByCondition(
                dslContext = dslContext,
                storeType = storeType,
                name = name,
                storeCode = storeCode
            ) < count
        }
        if (flag) {
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
                params = arrayOf(name)
            )
        }
    }
}
