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

import com.tencent.devops.environment.pojo.DevCloudModel

object StaticData {

    fun getDevCloudModelList() = listOf(
        DevCloudModel(
            moduleId = "system_base", moduleName = "8核16G（普通版）", cpu = 8, memory = "16384M", disk = "100G",
            description = listOf(
                "2.5GHz 64核 Intel Xeon Skylake 6133处理器",
                "32GB*12 DDR3 内存",
                "100GB 固态硬盘"
            ),
            produceTime = "预计交付周期：5分钟"
        ),
        DevCloudModel(
            moduleId = "system_pro", moduleName = "32核64G（高配版）", cpu = 32, memory = "65535M", disk = "500G",
            description = listOf(
                "2.5GHz 64核 Intel Xeon Skylake 6133处理器",
                "32GB*12 DDR3 内存",
                "500GB 固态硬盘"
            ),
            produceTime = "预计交付周期：10分钟"
        )
    )
}
