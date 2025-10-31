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

package com.tencent.devops.store.common.utils

import com.tencent.devops.common.api.constant.CommonMessageCode
import com.tencent.devops.common.api.constant.KEY_VERSION
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.web.constant.BkStyleEnum
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.store.pojo.common.enums.StoreTypeEnum
import java.util.regex.Pattern

object VersionUtils {

    fun convertLatestVersion(version: String): String {
        val versionPrefix = version.substring(0, version.indexOf(".") + 1)
        return "$versionPrefix*"
    }

    fun convertLatestVersionName(version: String): String {
        val versionPrefix = version.substring(0, version.indexOf(".") + 1)
        return "${versionPrefix}latest"
    }

    /**
     * 生成查询版本号
     * @param version 版本号
     */
    fun generateQueryVersion(version: String): String {
        return if (isLatestVersion(version)) {
            version.replace("*", "") + "%"
        } else {
            version
        }
    }

    /**
     * 是否是x.latest这种最新版本号
     * @param version 版本号
     */
    fun isLatestVersion(version: String) = version.contains("*")

    /**
     * 获取主版本号
     * @param version 版本号
     * @param storeType 组件类型
     * @return 主版本号
     */
    fun getMajorVersion(version: String, storeType: StoreTypeEnum): Int = when (storeType) {
        StoreTypeEnum.DEVX -> 1
        else -> version.split(".").first().toIntOrNull() ?: 1
    }

    /**
     * 校验版本号合法性
     * @param version 版本号
     * @param storeType 组件类型
     * @return 布尔值
     */
    fun validateVersion(version: String, storeType: StoreTypeEnum) {
        val patternStyle = when (storeType) {
            StoreTypeEnum.DEVX -> BkStyleEnum.DEVX_VERSION_STYLE
            else -> BkStyleEnum.VERSION_STYLE
        }
        if (!Pattern.matches(patternStyle.style, version)) {
            val validateMessage = I18nUtil.getCodeLanMessage(patternStyle.name)
            val versionFiledName = I18nUtil.getCodeLanMessage(KEY_VERSION)
            throw ErrorCodeException(
                errorCode = CommonMessageCode.PARAMETER_VALIDATE_ERROR,
                params = arrayOf(versionFiledName, validateMessage)
            )
        }
    }
}
