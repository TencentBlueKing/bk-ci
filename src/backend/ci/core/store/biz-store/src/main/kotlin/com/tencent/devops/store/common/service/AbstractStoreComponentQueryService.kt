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

package com.tencent.devops.store.common.service

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.ErrorCode
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.store.common.dao.ClassifyDao
import com.tencent.devops.store.common.dao.StoreBaseEnvQueryDao
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.step.QueryStep
import com.tencent.devops.store.common.step.StoreBaseQueryStep
import com.tencent.devops.store.pojo.common.MarketItem
import com.tencent.devops.store.pojo.common.MarketMainItem
import com.tencent.devops.store.pojo.common.MyStoreComponent
import com.tencent.devops.store.pojo.common.StoreDetailInfo
import com.tencent.devops.store.pojo.common.enums.StoreSortTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.version.StoreDeskVersionItem
import com.tencent.devops.store.pojo.common.version.StoreShowVersionInfo
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractStoreComponentQueryService @@Autowired constructor(
    val dslContext: DSLContext,
    val storeBaseQueryDao: StoreBaseQueryDao,
    val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao,
    val storeUserService: StoreUserService,
    val storeBaseEnvQueryDao: StoreBaseEnvQueryDao,
    val storeLabelService: StoreLabelService,
    val storeCommentService: StoreCommentService,
    val classifyDao: ClassifyDao
): StoreComponentQueryService {

    override fun getMyComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): Page<MyStoreComponent>? {
        TODO("Not yet implemented")
    }

    override fun getComponentVersionsByCode(
        userId: String,
        storeType: String,
        storeCode: String,
        page: Int,
        pageSize: Int
    ): Page<StoreDeskVersionItem> {
        TODO("Not yet implemented")
    }

    override fun getComponentDetailInfoById(userId: String, storeType: String, storeId: String): StoreDetailInfo? {
        TODO("Not yet implemented")
    }

    override fun getComponentDetailInfoByCode(userId: String, storeType: String, storeCode: String): StoreDetailInfo? {
        TODO("Not yet implemented")
    }

    override fun getMainPageComponents(
        userId: String,
        storeType: String,
        name: String?,
        page: Int,
        pageSize: Int
    ): List<MarketMainItem> {
        TODO("Not yet implemented")
    }

    override fun queryComponents(
        userId: String,
        storeType: String,
        projectCode: String?,
        keyword: String?,
        classifyId: String?,
        labelId: String?,
        score: Int?,
        recommendFlag: Boolean?,
        queryProjectComponentFlag: Boolean,
        sortType: StoreSortTypeEnum?,
        page: Int,
        pageSize: Int
    ): Page<MarketItem> {
        TODO("Not yet implemented")
    }

    override fun getComponentShowVersionInfo(
        userId: String,
        storeType: String,
        storeCode: String
    ): StoreShowVersionInfo {
        TODO("Not yet implemented")
    }

    private fun getComponentDetail(userId: String, storeId: String): StoreDetailInfo? {
        val storeBaseRecord = storeBaseQueryDao.getComponentById(dslContext, storeId) ?: return null
        val storeCode = storeBaseRecord.storeCode
        val storeType = StoreTypeEnum.getStoreTypeObj(storeBaseRecord.storeType.toInt())
        val storeFeatureRecord = storeBaseFeatureQueryDao.getComponentFeatureDataByCode(
            dslContext = dslContext,
            storeCode = storeBaseRecord.storeCode,
            storeType = storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        val storeBaseEnvInfo = storeBaseEnvQueryDao.getStoreEnvInfo(dslContext, storeId) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(storeCode)
        )
        // 用户是否可安装组件
        val installFlag = storeUserService.isCanInstallStoreComponent(
            defaultFlag = storeBaseEnvInfo.defaultFlag,
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        val labels = storeLabelService.getLabelsByStoreId(storeBaseRecord.id)
        val userCommentInfo = storeCommentService.getStoreUserCommentInfo(userId, storeCode, storeType)
        val componentRelRecord = storeBaseQueryDao.getComponentRelClassifyAndVersionInfoById(dslContext, storeId)
        val mutableMapOf = mutableMapOf<String, Class<QueryStep<StoreDetailInfo>>>()
        StoreDetailInfo(

        )
    }
}