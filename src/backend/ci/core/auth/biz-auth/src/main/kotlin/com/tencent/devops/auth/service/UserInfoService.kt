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

package com.tencent.devops.auth.service

import com.tencent.devops.auth.constant.AuthMessageCode
import com.tencent.devops.auth.dao.AuthUserInfoDao
import com.tencent.devops.auth.entity.UserInfoEntity
import com.tencent.devops.auth.pojo.enum.UserStatus
import com.tencent.devops.common.api.exception.ErrorCodeException
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserInfoService @Autowired constructor(
    val dslContext: DSLContext,
    val userInfoDao: AuthUserInfoDao
) {

    fun createUserInfo(entity: UserInfoEntity, existCheck: Boolean): Boolean {
        if (existCheck) {
            val userInfo = userInfoDao.getUserInfo(dslContext, entity.userId, entity.userType)
            if (userInfo != null) {
                logger.warn("${userInfo.userid} ${userInfo.userType} is exist")
                throw ErrorCodeException(errorCode = AuthMessageCode.LOGIN_USER_INFO_EXIST)
            }
        }
        return userInfoDao.createUser(dslContext, entity) == 1
    }

    fun getUserInfo(userId: String, userType: Int): UserInfoEntity? {
        val userInfo = userInfoDao.getUserInfo(dslContext, userId, userType) ?: return null
        return UserInfoEntity(
            userId = userInfo.userid,
            userType = userInfo.userType,
            email = userInfo.email,
            phone = userInfo.phone,
            userStatus = userInfo.userStatus
        )
    }

    fun thirdLoginAndRegister(userId: String, userType: Int, email: String?) {
        createUserInfo(
            entity = UserInfoEntity(
                userId = userId,
                userType = userType,
                email = email,
                phone = null,
                userStatus = UserStatus.NORMAL.id
            ), existCheck = false
        )
        logger.info("third login and register success: $userId $userType")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(UserInfoService::class.java)
    }
}
