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

package com.tencent.devops.environment.dao

import com.tencent.devops.common.api.constant.I18NConstant.BK_ESTIMATED_DELIVERY_TIME
import com.tencent.devops.common.api.constant.I18NConstant.BK_HIGH_END_VERSION
import com.tencent.devops.common.api.constant.I18NConstant.BK_INTEL_XEON_SKYLAKE_PROCESSOR
import com.tencent.devops.common.api.constant.I18NConstant.BK_MEMORY
import com.tencent.devops.common.api.constant.I18NConstant.BK_NORMAL_VERSION
import com.tencent.devops.common.api.constant.I18NConstant.BK_SOLID_STATE_DISK
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.environment.pojo.DevCloudModel

object StaticData {

    fun getDevCloudModelList() = listOf(
        DevCloudModel(
            moduleId = "system_base", moduleName = MessageUtil.getMessageByLocale(
                messageCode = BK_NORMAL_VERSION,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), cpu = 8, memory = "16384M", disk = "100G",
            description = listOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_INTEL_XEON_SKYLAKE_PROCESSOR,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                MessageUtil.getMessageByLocale(
                    messageCode = BK_MEMORY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                MessageUtil.getMessageByLocale(
                        messageCode = BK_SOLID_STATE_DISK,
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        params = arrayOf("100")
                    )
            ),
            produceTime = MessageUtil.getMessageByLocale(
                    messageCode = BK_ESTIMATED_DELIVERY_TIME,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf("5")
                )
        ),
        DevCloudModel(
            moduleId = "system_pro", moduleName = MessageUtil.getMessageByLocale(
                messageCode = BK_HIGH_END_VERSION,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), cpu = 32, memory = "65535M", disk = "500G",
            description = listOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_INTEL_XEON_SKYLAKE_PROCESSOR,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                MessageUtil.getMessageByLocale(
                    messageCode = BK_MEMORY,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ),
                MessageUtil.getMessageByLocale(
                        messageCode = BK_SOLID_STATE_DISK,
                        language = I18nUtil.getDefaultLocaleLanguage(),
                        params = arrayOf("500")
                    )
            ),
            produceTime = MessageUtil.getMessageByLocale(
                    messageCode = BK_ESTIMATED_DELIVERY_TIME,
                    language = I18nUtil.getDefaultLocaleLanguage(),
                    params = arrayOf("10")
                )
        )
    )
}
