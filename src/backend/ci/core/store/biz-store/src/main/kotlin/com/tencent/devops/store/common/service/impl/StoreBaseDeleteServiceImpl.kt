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
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.service.StoreBaseDeleteService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class StoreBaseDeleteServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeCommonService: StoreCommonService
) : StoreBaseDeleteService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentManageServiceImpl::class.java)
    }

    private fun getStoreManagementExtraService(storeType: StoreTypeEnum): StoreManagementExtraService {
        return SpringContextUtil.getBean(
            StoreManagementExtraService::class.java,
            "${storeType}_MANAGEMENT_EXTRA_SERVICE"
        )
    }

    override fun deleteComponentCheck(handlerRequest: StoreDeleteRequest) {
        val bkStoreContext = handlerRequest.bkStoreContext
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        logger.info("delete component ,params:[$userId, $storeCode, $storeType]")
        val isOwner = storeMemberDao.isStoreAdmin(dslContext, userId, storeCode, storeType.type.toByte())
        if (!isOwner && handlerRequest.checkPermissionFlag) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.NO_COMPONENT_ADMIN_PERMISSION,
                params = arrayOf(storeCode)
            )
        }
        val releasedCount = storeBaseQueryDao.countByCondition(
            dslContext = dslContext,
            storeCode = storeCode,
            storeType = storeType,
            status = StoreStatusEnum.RELEASED
        )
        if (releasedCount > 0) {
            throw ErrorCodeException(
                errorCode = StoreMessageCode.USER_COMPONENT_RELEASED_IS_NOT_ALLOW_DELETE,
                params = arrayOf(storeCode)
            )
        }
        getStoreManagementExtraService(storeType).doComponentDeleteCheck(storeCode)
    }

    override fun deleteComponentRepoFile(handlerRequest: StoreDeleteRequest) {
        val bkStoreContext = handlerRequest.bkStoreContext
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
        val deleteStorePkgResult = getStoreManagementExtraService(storeType).deleteComponentRepoFile(
            userId = userId,
            storeCode = storeCode,
            storeType = storeType
        )
        if (deleteStorePkgResult.isNotOk()) {
            throw ErrorCodeException(errorCode = StoreMessageCode.STORE_COMPONENT_REPO_FILE_DELETE_FAIL)
        }
    }

    override fun doStoreDeleteDataPersistent(handlerRequest: StoreDeleteRequest) {
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        dslContext.transaction { t ->
            val context = DSL.using(t)
            storeCommonService.deleteStoreInfo(context, storeCode, storeType.type.toByte())
        }
    }
}
