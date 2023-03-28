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

package com.tencent.devops.store.service.common.impl

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.project.api.service.service.ServiceTxUserResource
import com.tencent.devops.project.pojo.user.UserDeptDetail
import com.tencent.devops.store.dao.common.StoreMemberDao
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import com.tencent.devops.store.service.common.StoreUserService
import com.tencent.devops.store.service.common.StoreVisibleDeptService
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * store用户通用业务逻辑类
 *
 * since: 2019-03-26
 */
@Service
class TxStoreUserServiceImpl : StoreUserService {

    @Autowired
    lateinit var dslContext: DSLContext
    @Autowired
    lateinit var storeMemberDao: StoreMemberDao
    @Autowired
    lateinit var storeVisibleDeptService: StoreVisibleDeptService
    @Autowired
    lateinit var client: Client

    private val logger = LoggerFactory.getLogger(TxStoreUserServiceImpl::class.java)

    /**
     * 获取用户机构ID信息
     */
    override fun getUserDeptList(userId: String): List<Int> {
        val userInfo = client.get(ServiceTxUserResource::class).get(userId).data
        return if (userInfo == null) {
            listOf(0, 0, 0, 0)
        } else {
            listOf(userInfo.bgId.toInt(), userInfo.deptId.toInt(), userInfo.centerId.toInt(), userInfo.groupId.toInt())
        }
    }

    /**
     * 获取用户机构名称
     */
    override fun getUserFullDeptName(userId: String): Result<String?> {
        val userDeptInfo: UserDeptDetail?
        try {
            // 获取用户的机构信息
            userDeptInfo = client.get(ServiceTxUserResource::class).get(userId).data
        } catch (ignored: Throwable) {
            logger.warn("getUserDeptDetailFromCache fail!", ignored)
            return MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        }
        logger.info("$userId userDeptInfo is:$userDeptInfo")
        return if (null != userDeptInfo) {
            val commenterDept = StringBuilder(userDeptInfo.bgName) // 组装评论者的机构信息
            if (userDeptInfo.deptName.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.deptName)
            if (userDeptInfo.centerName.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.centerName)
            if (userDeptInfo.groupName.isNotEmpty()) commenterDept.append("/").append(userDeptInfo.groupName)
            Result(commenterDept.toString())
        } else {
            MessageUtil.generateResponseDataObject(
                messageCode = CommonMessageCode.SYSTEM_ERROR,
                language = I18nUtil.getLanguage(userId)
            )
        }
    }

    /**
     * 判断用户是否能安装store组件
     */
    override fun isCanInstallStoreComponent(
        defaultFlag: Boolean,
        userId: String,
        storeCode: String,
        storeType: StoreTypeEnum
    ): Boolean {
        return if (defaultFlag || storeMemberDao.isStoreMember(
                dslContext = dslContext,
                userId = userId,
                storeCode = storeCode,
                storeType = storeType.type.toByte())
        ) {
            true
        } else {
            // 获取用户组织架构
            val userDeptList = getUserDeptList(userId)
            val storeDept = storeVisibleDeptService.batchGetVisibleDept(
                storeCodeList = listOf(storeCode),
                storeType = storeType
            ).data?.get(storeCode)
            storeDept != null && (storeDept.contains(0) || storeDept.intersect(userDeptList).count() > 0)
        }
    }
}
