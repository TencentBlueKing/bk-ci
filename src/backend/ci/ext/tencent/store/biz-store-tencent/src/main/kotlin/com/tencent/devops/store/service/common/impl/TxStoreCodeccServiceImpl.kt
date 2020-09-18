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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.DEVOPS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.DateTimeUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.common.service.utils.MessageCodeUtil
import com.tencent.devops.plugin.api.ServiceCodeccResource
import com.tencent.devops.plugin.codecc.pojo.CodeccCallback
import com.tencent.devops.plugin.codecc.pojo.CodeccMeasureInfo
import com.tencent.devops.repository.api.ServiceGitRepositoryResource
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.dao.common.BusinessConfigDao
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.dao.common.StorePipelineBuildRelDao
import com.tencent.devops.store.dao.common.TxStoreCodeccDao
import com.tencent.devops.store.pojo.common.STORE_CODECC_FAILED_TEMPLATE_SUFFIX
import com.tencent.devops.store.pojo.common.STORE_CODECC_QUALIFIED_TEMPLATE_SUFFIX
import com.tencent.devops.store.pojo.common.STORE_REPO_COMMIT_KEY_PREFIX
import com.tencent.devops.store.pojo.common.StoreCodeccInfo
import com.tencent.devops.store.pojo.common.StoreValidateCodeccResultRequest
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreCommonService
import com.tencent.devops.store.service.common.StoreNotifyService
import com.tencent.devops.store.service.common.TxStoreCodeccService
import com.tencent.devops.store.service.common.TxStoreCodeccValidateService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.text.MessageFormat
import java.time.LocalDateTime
import kotlin.math.pow

@Service
class TxStoreCodeccServiceImpl @Autowired constructor(
    private val client: Client,
    private val storeMemberDao: StoreMemberDao,
    private val storeCommonService: StoreCommonService,
    private val redisOperation: RedisOperation,
    private val dslContext: DSLContext
) : TxStoreCodeccService {

    private val logger = LoggerFactory.getLogger(TxStoreCodeccServiceImpl::class.java)

    override fun getCodeccMeasureInfo(
        userId: String,
        storeType: String,
        storeCode: String,
        storeId: String?
    ): Result<CodeccMeasureInfo?> {
        logger.info("getCodeccMeasureInfo userId:$userId,storeType:$storeType,storeCode:$storeCode,storeId:$storeId")
        validatePermission(userId, storeCode, storeType)
        var commitId:String? = null
        if (storeId != null) {
            // 如果组件ID不为空则会去redis中获取当时构建拉代码存的commitId
            commitId = redisOperation.get("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeId")
        }
        logger.info("getCodeccMeasureInfo commitId:$commitId")
        val mameSpaceName = storeCommonService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        return client.get(ServiceCodeccResource::class).getCodeccMeasureInfo(
            repoProjectName = "$mameSpaceName/$storeCode",
            commitId = commitId
        )
    }

    override fun startCodeccTask(
        userId: String,
        storeType: String,
        storeCode: String,
        storeId: String?
    ): Result<Boolean> {
        logger.info("startCodeccTask userId:$userId,storeType:$storeType,storeCode:$storeCode,storeId:$storeId")
        validatePermission(userId, storeCode, storeType)
        var commitId:String? = null
        if (storeId != null) {
            // 如果组件ID不为空则会去redis中获取当时构建拉代码存的commitId
            commitId = redisOperation.get("$STORE_REPO_COMMIT_KEY_PREFIX:$storeType:$storeId")
        }
        logger.info("startCodeccTask commitId:$commitId")
        val mameSpaceName = storeCommonService.getStoreRepoNameSpaceName(StoreTypeEnum.valueOf(storeType))
        return client.get(ServiceCodeccResource::class).startCodeccTask(
            repoProjectName = "$mameSpaceName/$storeCode",
            commitId = commitId
        )
    }

    private fun validatePermission(userId: String, storeCode: String, storeType: String) {
        if (storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = StoreTypeEnum.valueOf(storeType).type.toByte()
            )
        ) {
            throw ErrorCodeException(errorCode = CommonMessageCode.PERMISSION_DENIED)
        }
    }
}
