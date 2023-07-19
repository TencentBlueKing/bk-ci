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

package com.tencent.devops.store.service.common

@Suppress("LongParameterList")
interface StoreI18nMessageService {

    /**
     * 解析map集合，把map字段的值替换成蓝盾默认语言对应的值
     * @param userId 用户ID
     * @param projectCode 项目标识
     * @param jsonMap map集合
     * @param fileDir 资源文件目录
     * @param i18nDir 国际化目录
     * @param propertiesKeyPrefix map字段在properties中key的前缀
     * @param dbKeyPrefix map字段在db中key的前缀
     * @param repositoryHashId 代码库哈希ID
     * @return 替换成蓝盾默认语言对应的值的map集合
     */
    fun parseJsonMapI18nInfo(
        userId: String,
        projectCode: String,
        jsonMap: MutableMap<String, Any>,
        fileDir: String,
        i18nDir: String,
        propertiesKeyPrefix: String? = null,
        dbKeyPrefix: String? = null,
        repositoryHashId: String? = null
    ): Map<String, Any>

    /**
     * 解析错误码，把错误码的国际化信息持久化到数据库
     * @param userId 用户ID
     * @param projectCode 项目标识
     * @param errorCodes map集合
     * @param fileDir 资源文件目录
     * @param i18nDir 国际化目录
     * @param keyPrefix map字段key的前缀
     * @param repositoryHashId 代码库哈希ID
     * @return 替换成蓝盾默认语言对应的值的map集合
     */
    fun parseErrorCodeI18nInfo(
        userId: String,
        projectCode: String,
        errorCodes: Set<Int>,
        fileDir: String,
        i18nDir: String,
        keyPrefix: String? = null,
        repositoryHashId: String? = null
    )

    /**
     * 解析json字符串，把json字符串中的国际化字段进行国际化翻译
     * @param jsonStr json字符串
     * @param keyPrefix key的前缀
     * @return 翻译后的json字符串
     */
    fun parseJsonStrI18nInfo(
        jsonStr: String,
        keyPrefix: String
    ): String
}
