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

package com.tencent.devops.project.service

import com.tencent.devops.common.api.pojo.LocaleInfo
import com.tencent.devops.project.pojo.LanguageInfo

interface UserLocaleService {

    /**
     * 添加用户国际化信息
     * @param userId 用户ID
     * @param language 国际化语言信息
     * @return 布尔值
     */
    fun addUserLocale(userId: String, language: String): Boolean

    /**
     * 删除用户国际化信息
     * @param userId 用户ID
     * @return 布尔值
     */
    fun deleteUserLocale(userId: String): Boolean

    /**
     * 更新用户国际化信息
     * @param userId 用户ID
     * @param language 国际化信息
     * @return 布尔值
     */
    fun updateUserLocale(userId: String, language: String): Boolean

    /**
     * 根据用户ID查找用户国际化信息
     * @param userId 用户ID
     * @return 用户国际化信息
     */
    fun getUserLocale(userId: String): LocaleInfo

    /**
     * 获取蓝盾支持的语言信息
     * @param userId 用户ID
     * @return 支持的语言信息列表
     */
    fun listSupportLanguages(userId: String): List<LanguageInfo>
}
