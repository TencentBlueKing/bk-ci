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

package com.tencent.devops.common.pipeline.init

import com.tencent.devops.common.api.constant.CommonMessageCode.BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES_NEW
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_CLOUD_HOSTING_WINDOWS_ON_DEVCLOUD
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PCG_PUBLIC_BUILD_RESOURCES
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PRIVATE_BUILD_A_CLUSTER
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PRIVATE_SINGLE_BUIL_MACHINE
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PUBLIC_DOCKER_ON_BCS
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PUBLIC_DOCKER_ON_DEVCLOUD
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_PUBLIC_DOCKER_ON_DEVNET_PHYSICAL
import com.tencent.devops.common.api.constant.CommonMessageCode.BK_TENCENT_SELF_DEVELOPED_CLOUD
import com.tencent.devops.common.api.enums.EnumModifier
import com.tencent.devops.common.api.pojo.OS
import com.tencent.devops.common.api.util.EnumUtil
import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.type.BuildType
import com.tencent.devops.common.web.utils.I18nUtil

class BuildTypeEnumModifier : EnumModifier {

    override fun modified() {
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.ESXi.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.MACOS), false, true, false)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.MACOS.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_BLUE_SHIELD_PUBLIC_BUILD_RESOURCES_NEW,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.MACOS), false, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.DOCKER.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_PUBLIC_DOCKER_ON_DEVNET_PHYSICAL,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.LINUX), true, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.PUBLIC_DEVCLOUD.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_PUBLIC_DOCKER_ON_DEVCLOUD,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.LINUX), true, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.PUBLIC_BCS.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_PUBLIC_DOCKER_ON_BCS,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.LINUX), true, true, false)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.THIRD_PARTY_AGENT_ID.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                messageCode = BK_PRIVATE_SINGLE_BUIL_MACHINE,
                language = I18nUtil.getDefaultLocaleLanguage()
            ), listOf(OS.MACOS, OS.LINUX, OS.WINDOWS), false, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.THIRD_PARTY_AGENT_ENV.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PRIVATE_BUILD_A_CLUSTER,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), listOf(OS.MACOS, OS.LINUX, OS.WINDOWS), false, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.THIRD_PARTY_PCG.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_PCG_PUBLIC_BUILD_RESOURCES,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), listOf(OS.LINUX), false, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.THIRD_PARTY_DEVCLOUD.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_TENCENT_SELF_DEVELOPED_CLOUD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), listOf(OS.LINUX), false, true, true)
        )
        EnumUtil.addEnum(
            enumType = BuildType::class.java,
            enumName = BuildType.WINDOWS.name,
            additionalValues = arrayOf(
                MessageUtil.getMessageByLocale(
                    messageCode = BK_CLOUD_HOSTING_WINDOWS_ON_DEVCLOUD,
                    language = I18nUtil.getDefaultLocaleLanguage()
                ), listOf(OS.WINDOWS), false, true, true)
        )
    }
}
