
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

package com.tencent.devops.common.auth.api.pojo

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.enums.I18nTranslateTypeEnum
import com.tencent.devops.common.web.utils.I18nUtil

/**
 * 项目角色组
 */
enum class DefaultGroupType(
    val value: String,
    @BkFieldI18n(translateType = I18nTranslateTypeEnum.VALUE, keyPrefixName = "defaultGroupType", reusePrefixFlag = false)
    val displayName: String
) {
    MANAGER("manager", "manager"), // CI管理员
    DEVELOPER("developer", "developer"), // 开发人员
    MAINTAINER("maintainer", "maintainer"), // 运维人员
    TESTER("tester", "tester"), // 测试人员
    PM("pm", "pm"), // 产品人员
    QC("qc", "qc"); // 质量管理员

    companion object {
        fun get(value: String): DefaultGroupType {
            values().forEach {
                if (value == it.value) return it
            }
            throw IllegalArgumentException("No enum for constant $value")
        }

        fun contains(value: String): Boolean {
            values().forEach {
                if (value == it.value) return true
            }
            return false
        }

        fun containsDisplayName(displayName: String): Boolean {
            values().forEach {
                if (displayName == it.getDisplayName()) return true
            }
            return false
        }

        fun getAll(): List<DefaultGroupType> {
            val allGroup = mutableListOf<DefaultGroupType>()
            allGroup.add(MANAGER)
            allGroup.add(DEVELOPER)
            allGroup.add(MAINTAINER)
            allGroup.add(PM)
            allGroup.add(QC)
            allGroup.add(TESTER)
            return allGroup
        }

        fun DefaultGroupType.getDisplayName(): String {
            return I18nUtil.getCodeLanMessage("defaultGroupType." + this.displayName)
        }
    }
}
