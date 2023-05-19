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

import com.tencent.devops.common.api.pojo.I18nMessage

interface I18nMessageService {

    /**
     * 批量添加国际化信息
     * @param userId 用户ID
     * @param i18nMessages 国际化信息集合
     * @return 布尔值
     */
    fun batchAddI18nMessage(userId: String, i18nMessages: List<I18nMessage>): Boolean

    /**
     * 删除用户国际化信息
     * @param userId 用户ID
     * @param moduleCode 模块标识
     * @param key 国际化变量名
     * @param language 国际化语言信息
     * @return 布尔值
     */
    fun deleteI18nMessage(
        userId: String,
        moduleCode: String,
        key: String,
        language: String? = null
    ): Boolean

    /**
     * 查询国际化信息
     * @param moduleCode 模块标识
     * @param key 国际化变量名
     * @param language 国际化语言信息
     * @return 国际化信息
     */
    fun getI18nMessage(
        moduleCode: String,
        key: String,
        language: String
    ): I18nMessage?

    /**
     * 查询国际化信息集合
     * @param moduleCode 模块标识
     * @param keys 国际化变量名列表
     * @param language 国际化语言信息
     * @return 国际化信息
     */
    fun getI18nMessages(
        moduleCode: String,
        keys: List<String>,
        language: String
    ): List<I18nMessage>?

    /**
     * 查询国际化信息集合
     * @param moduleCode 模块标识
     * @param keyPrefix 字段key前缀
     * @param language 国际化语言信息
     * @return 国际化信息
     */
    fun getI18nMessages(
        moduleCode: String,
        keyPrefix: String,
        language: String
    ): List<I18nMessage>?
}
