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
import com.tencent.devops.common.api.constant.MESSAGE
import com.tencent.devops.common.api.constant.STATUS
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.repository.api.ServiceOauthResource
import com.tencent.devops.repository.constant.RepositoryConstants.KEY_REPOSITORY_ID
import com.tencent.devops.repository.pojo.enums.TokenTypeEnum
import com.tencent.devops.store.common.dao.StoreBaseFeatureExtQueryDao
import com.tencent.devops.store.common.dao.StoreBaseQueryDao
import com.tencent.devops.store.common.dao.StoreMemberDao
import com.tencent.devops.store.common.service.StoreBaseDeleteService
import com.tencent.devops.store.common.service.StoreCommonService
import com.tencent.devops.store.common.service.StoreManagementExtraService
import com.tencent.devops.store.constant.StoreConstants.KEY_FRAMEWORK_CODE
import com.tencent.devops.store.constant.StoreMessageCode
import com.tencent.devops.store.pojo.common.enums.FrameworkCodeEnum
import com.tencent.devops.store.pojo.common.enums.StoreStatusEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.pojo.common.publication.StoreDeleteRequest
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import jakarta.ws.rs.NotFoundException

@Service
class StoreBaseDeleteServiceImpl @Autowired constructor(
    private val dslContext: DSLContext,
    private val storeMemberDao: StoreMemberDao,
    private val storeBaseQueryDao: StoreBaseQueryDao,
    private val storeBaseFeatureExtQueryDao: StoreBaseFeatureExtQueryDao,
    private val storeCommonService: StoreCommonService,
    private val client: Client
) : StoreBaseDeleteService {

    companion object {
        private val logger = LoggerFactory.getLogger(StoreComponentManageServiceImpl::class.java)
    }

    @Value("\${git.devopsPrivateToken:}")
    private val devopsPrivateToken: String = ""

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

    override fun deleteComponentCodeRepository(handlerRequest: StoreDeleteRequest) {
        val storeCode = handlerRequest.storeCode
        val storeType = StoreTypeEnum.valueOf(handlerRequest.storeType)
        val repositoryId = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
            dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_REPOSITORY_ID
        )?.fieldValue
        if (!repositoryId.isNullOrBlank()) {
            val bkStoreContext = handlerRequest.bkStoreContext
            val userId = bkStoreContext[AUTH_HEADER_USER_ID]?.toString() ?: AUTH_HEADER_USER_ID_DEFAULT_VALUE
            val frameworkCode = storeBaseFeatureExtQueryDao.getStoreBaseFeatureExt(
                dslContext = dslContext, storeCode = storeCode, storeType = storeType, fieldName = KEY_FRAMEWORK_CODE
            )?.fieldValue
            val token: String
            var tokenType = TokenTypeEnum.PRIVATE_KEY
            if (frameworkCode == FrameworkCodeEnum.CUSTOM_FRAMEWORK.name) {
                // 如果用户选择自定义框架方式发布，则使用用户自已的oauthToken去删除代码库
                val gitToken = client.get(ServiceOauthResource::class).gitGet(userId).data
                    ?: throw NotFoundException("cannot found access token for user($userId)")
                token = gitToken.accessToken
                tokenType = TokenTypeEnum.OAUTH
            } else {
                token = devopsPrivateToken
            }
            try {
                val deleteRepositoryResult = getStoreManagementExtraService(storeType).deleteComponentCodeRepository(
                    userId = userId,
                    repositoryId = repositoryId,
                    token = token,
                    tokenType = tokenType
                )
                if (deleteRepositoryResult.isNotOk()) {
                    setDeleteCodeRepositoryMsg(bkStoreContext, deleteRepositoryResult.message)
                }
            } catch (ignored: Throwable) {
                // 组件删除代码库失败不终止删除流程，在接口返回报文给出提示信息
                logger.warn("deleteAtomRepository deleteComponentCodeRepository!", ignored)
                setDeleteCodeRepositoryMsg(bkStoreContext, ignored.message)
            }
        }
    }

    private fun setDeleteCodeRepositoryMsg(
        bkStoreContext: MutableMap<String, Any>,
        message: String?
    ) {
        bkStoreContext[STATUS] = StoreMessageCode.STORE_COMPONENT_CODE_REPOSITORY_DELETE_FAIL
        bkStoreContext[MESSAGE] = I18nUtil.getCodeLanMessage(
            messageCode = StoreMessageCode.STORE_COMPONENT_CODE_REPOSITORY_DELETE_FAIL,
            params = arrayOf(message ?: "")
        )
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
