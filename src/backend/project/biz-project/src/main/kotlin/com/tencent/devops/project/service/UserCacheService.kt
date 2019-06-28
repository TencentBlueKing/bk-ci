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

package com.tencent.devops.project.service

import com.tencent.devops.project.dao.UserDao
import com.tencent.devops.project.pojo.user.UserDeptDetail
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class UserCacheService @Autowired constructor(
    private val dslContext: DSLContext,
    private val userDao: UserDao
) {
    /**
     * 从缓存的用户表中获取用户组织架构信息
     */
    fun getDetailFromCache(userId: String): UserDeptDetail {
        val userRecord = userDao.get(dslContext, userId)
        return if (userRecord == null) {
            UserDeptDetail("", 0, "", 0, "", 0, 0, "")
        } else {
            UserDeptDetail(
                userRecord["BG_NAME"] as String,
                userRecord["BG_ID"] as Int,
                userRecord["DEPT_NAME"] as String,
                userRecord["DEPT_ID"] as Int,
                userRecord["CENTER_NAME"] as String,
                userRecord["CENTER_ID"] as Int,
                userRecord["GROYP_ID"] as Int,
                userRecord["GROUP_NAME"] as String
            )
        }
    }
}