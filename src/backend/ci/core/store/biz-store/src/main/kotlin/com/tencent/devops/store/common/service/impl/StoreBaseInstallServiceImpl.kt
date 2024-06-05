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

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.store.common.dao.StoreBaseFeatureQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.service.StoreBaseInstallService
import com.tencent.devops.store.pojo.common.InstallStoreReq
import com.tencent.devops.store.pojo.common.StoreBaseInfo
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("STORE_BASE_INSTALL_SERVICE")
class StoreBaseInstallServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureQueryDao: StoreBaseFeatureQueryDao
) : StoreBaseInstallService {

    override fun installComponentCheck(
        userId: String,
        channelCode: ChannelCode,
        installStoreReq: InstallStoreReq
    ): Result<StoreBaseInfo> {
        if (installStoreReq.storeType == StoreTypeEnum.DEVX) {
            if (installStoreReq.instanceId.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf(InstallStoreReq::instanceId.name)
                )
            }
            if (installStoreReq.version.isNullOrBlank()) {
                throw ErrorCodeException(
                    errorCode = CommonMessageCode.PARAMETER_IS_NULL,
                    params = arrayOf(InstallStoreReq::version.name)
                )
            }
        }
        val componentBaseInfoRecord = storeBaseQueryDao.getNewestComponentByCode(
            dslContext = dslContext,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType
        ) ?: throw ErrorCodeException(
            errorCode = CommonMessageCode.PARAMETER_IS_INVALID,
            params = arrayOf(installStoreReq.storeCode)
        )
        val publicFlag = storeBaseFeatureQueryDao.isPublic(
            dslContext = dslContext,
            storeCode = installStoreReq.storeCode,
            storeType = installStoreReq.storeType
        )
        return Result(
            componentBaseInfoRecord.let {
                StoreBaseInfo(
                    storeId = it.id,
                    storeCode = it.storeCode,
                    storeName = it.name,
                    storeType = StoreTypeEnum.getStoreTypeObj(it.storeType.toInt()),
                    version = it.name,
                    publicFlag = publicFlag,
                    status = it.status,
                    logoUrl = it.logoUrl,
                    publisher = it.publisher,
                    classifyId = it.classifyId
                )
            }
        )
    }

    override fun installComponentPrepare(
        userId: String,
        channelCode: ChannelCode,
        projectCodes: ArrayList<String>,
        storeBaseInfo: StoreBaseInfo
    ): Result<Boolean> {
        return Result(true)
    }
}
