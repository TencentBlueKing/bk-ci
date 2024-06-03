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
package com.tencent.devops.store.pojo.image.enums

/**
 * 镜像可运行的机器环境
 */
enum class ImageAgentTypeEnum(val type: Int) {
    DOCKER(0), // Docker on Devnet 物理机
    IDC(1), // Docker on IDC CVM
    PUBLIC_DEVCLOUD(2), // Docker on DevCloud
    KUBERNETES(3); // Kubernetes

    companion object {

        fun getImageAgentType(name: String): ImageAgentTypeEnum? {
            if (name.equals(KUBERNETES.name, ignoreCase = true)) {
                return DOCKER
            }

            values().forEach { enumObj ->
                if (enumObj.name.equals(name, ignoreCase = true)) {
                    return enumObj
                }
            }
            return null
        }

        fun getImageAgentType(type: Int): String {
            return when (type) {
                0 -> DOCKER.name
                1 -> IDC.name
                2 -> PUBLIC_DEVCLOUD.name
                3 -> KUBERNETES.name
                else -> DOCKER.name
            }
        }

        fun getAllAgentTypes(): MutableList<ImageAgentTypeEnum> {
            return mutableListOf(
                DOCKER,
                IDC,
                PUBLIC_DEVCLOUD,
                KUBERNETES
            )
        }
    }
}
