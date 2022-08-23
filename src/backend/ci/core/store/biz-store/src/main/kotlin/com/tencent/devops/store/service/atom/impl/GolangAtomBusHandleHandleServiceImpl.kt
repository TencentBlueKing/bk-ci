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

package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.enums.OSType
import com.tencent.devops.store.service.atom.AtomBusHandleService

class GolangAtomBusHandleHandleServiceImpl : AtomBusHandleService {

    override fun handleOsName(osName: String): String {
        return if (osName.toUpperCase() == OSType.MAC_OS.name) {
            "darwin"
        } else {
            osName.toLowerCase()
        }
    }

    override fun handleOsArch(osName: String, osArch: String): String {
        return when (val osType = OSType.valueOf(osName.toUpperCase())) {
            OSType.LINUX, OSType.MAC_OS -> {
                if (osArch.contains("amd") || osArch.contains("x86")) {
                    "amd64"
                } else if (osArch.contains("arm") || osArch.contains("aarch")) {
                    "arm64"
                } else if (osArch.contains("mips") && osType == OSType.LINUX) {
                    "mips64"
                } else {
                    // 默认使用amd64，非m1芯片
                    "amd64"
                }
            }
            OSType.WINDOWS -> {
                // 保持和agent编译一致，使用386指令集
                "386"
            }
            else -> {
                "amd64"
            }
        }
    }
}
