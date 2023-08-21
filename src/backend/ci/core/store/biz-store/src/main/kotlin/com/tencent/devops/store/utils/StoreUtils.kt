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

package com.tencent.devops.store.utils

import com.tencent.devops.common.api.constant.JAVA
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.common.service.utils.HomeHostUtil
import com.tencent.devops.common.service.utils.SpringContextUtil
import com.tencent.devops.store.pojo.common.STORE_PUBLIC_FLAG_KEY_PREFIX
import com.tencent.devops.store.pojo.common.STORE_NORMAL_PROJECT_RUN_INFO_KEY_PREFIX
import com.tencent.devops.store.pojo.common.enums.PackageSourceTypeEnum
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum

object StoreUtils {

    /**
     * 移除链接地址中的域名信息
     * @param url 链接地址
     */
    fun removeUrlHost(url: String): String {
        val host = getHost()
        return url.removePrefix(host)
    }

    /**
     * 为链接地址添加域名信息
     * @param url 链接地址
     */
    fun addUrlHost(url: String): String {
        val host = getHost()
        return if (!url.startsWith("http://") && !url.startsWith("https://")) {
            if (url.startsWith("/")) "$host$url" else "$host/$url"
        } else {
            url
        }
    }

    private fun getHost(): String {
        val commonConfig: CommonConfig = SpringContextUtil.getBean(CommonConfig::class.java)
        return HomeHostUtil.getHost(commonConfig.devopsHostGateway!!)
    }

    /**
     * 获取公共组件Key前缀
     * @param typeName 类型名称
     */
    fun getStorePublicFlagKey(typeName: String): String {
        return "$STORE_PUBLIC_FLAG_KEY_PREFIX:$typeName"
    }

    /**
     * 获取组件运行时信息Key前缀
     * @param typeName 类型名称
     * @param storeCode 组件代码
     */
    fun getStoreRunInfoKey(typeName: String, storeCode: String): String {
        return "$STORE_NORMAL_PROJECT_RUN_INFO_KEY_PREFIX:$typeName:$storeCode"
    }

    /**
     * 判断当前版本号是否比待比较版本号大
     * @param currentVersion 当前版本号
     * @param compareVersion 待比较版本号
     */
    fun isGreaterVersion(currentVersion: String, compareVersion: String): Boolean {
        val currentVersionParts = currentVersion.split(".")
        val firstCurrentVersionPart = currentVersionParts[0].toInt()
        val secondCurrentVersionPart = currentVersionParts[1].toInt()
        val thirdCurrentVersionPart = currentVersionParts[2].toInt()
        val compareVersionParts = compareVersion.split(".")
        val firstCompareVersionPart = compareVersionParts[0].toInt()
        val secondCompareVersionPart = compareVersionParts[1].toInt()
        val thirdCompareVersionPart = compareVersionParts[2].toInt()
        if (firstCurrentVersionPart > firstCompareVersionPart) {
            return true
        } else if (firstCurrentVersionPart == firstCompareVersionPart &&
            secondCurrentVersionPart > secondCompareVersionPart
        ) {
            return true
        } else if (firstCurrentVersionPart == firstCompareVersionPart &&
            secondCurrentVersionPart == secondCompareVersionPart &&
            thirdCurrentVersionPart > thirdCompareVersionPart
        ) {
            return true
        }
        return false
    }

    /**
     * 获取字段Key前缀
     * @param storeType 组件类型
     * @param storeCode 组件代码
     * @param version 组件版本
     * @param fixPrefixName 固定前缀名称
     */
    fun getStoreFieldKeyPrefix(
        storeType: StoreTypeEnum,
        storeCode: String,
        version: String,
        fixPrefixName: String? = null
    ): String {
        val dataKey = "${storeType.name}.$storeCode.$version"
        return if (!fixPrefixName.isNullOrBlank()) {
            "$dataKey.$fixPrefixName"
        } else {
            dataKey
        }
    }

    /**
     * 获取组件的国际化目录
     * @param language 开发语言
     * @param packageSourceTypeEnum 包类型
     * @return 国际化目录
     */
    fun getStoreI18nDir(language: String, packageSourceTypeEnum: PackageSourceTypeEnum): String {
        return if (language == JAVA && packageSourceTypeEnum == PackageSourceTypeEnum.REPO) {
            "src/main/resources/i18n"
        } else {
            "i18n"
        }
    }
}
