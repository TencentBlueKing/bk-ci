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
import com.tencent.devops.auth.dao.AuthGroupUserDao
import com.tencent.devops.common.api.exception.OperationException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.utils.I18nUtil
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class GroupUserService @Autowired constructor(
    val dslContext: DSLContext,
    val authGroupService: AuthGroupService,
    val groupUserDao: AuthGroupUserDao
) {
    fun addUser2Group(userId: String, groupId: Int): Result<Boolean> {
        logger.info("addUser2Group |$userId| $groupId")
        val groupUserRecord = groupUserDao.get(
            dslContext = dslContext,
            userId = userId,
            groupId = groupId.toString()
        )
        if (groupUserRecord != null) {
            logger.warn("addUser2Group user $userId already in this group $groupId")
            throw OperationException(
                I18nUtil.getCodeLanMessage(
                    AuthMessageCode.GROUP_USER_ALREADY_EXIST,
                    language = I18nUtil.getLanguage(userId)
                )
            )
        }
        val groupRecord = authGroupService.getGroupCode(groupId)

        if (groupRecord == null) {
            logger.warn("addUser2Group group $groupId is not exist")
            throw OperationException(
                I18nUtil.getCodeLanMessage(AuthMessageCode.GROUP_NOT_EXIST, language = I18nUtil.getLanguage(userId))
            )
        }
        // 添加用户至用户组
        groupUserDao.create(
            dslContext = dslContext,
            userId = userId,
            groupId = groupId.toString()
        )
        return Result(true)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GroupUserService::class.java)
    }
}
