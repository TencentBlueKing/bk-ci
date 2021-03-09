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

package com.tencent.devops.store.pojo.enums

enum class ExtServiceStatusEnum(val status: Int) {
    INIT(0), // 初始化
    COMMITTING(1), // 提交中
    BUILDING(2), // 构建中
    BUILD_FAIL(3), // 构建失败
    DEPLOYING(4), // 部署中
    DEPLOY_FAIL(5), // 部署失败
    TESTING(6), // 测试中
    EDIT(7), // 提交资料
    AUDITING(8), // 审核中
    AUDIT_REJECT(9), // 审核驳回
    RELEASE_DEPLOYING(10), // 正式发布部署中
    RELEASE_DEPLOY_FAIL(11), // 正式发布部署失败
    RELEASED(12), // 已发布
    GROUNDING_SUSPENSION(13), // 上架中止
    UNDERCARRIAGING(14), // 下架中
    UNDERCARRIAGED(15); // 已下架

    companion object {

        fun getServiceStatus(name: String): ExtServiceStatusEnum? {
            values().forEach { enumObj ->
                if (enumObj.name == name) {
                    return enumObj
                }
            }
            return null
        }

        fun getServiceStatus(status: Int): String {
            return when (status) {
                0 -> INIT.name
                1 -> COMMITTING.name
                2 -> BUILDING.name
                3 -> BUILD_FAIL.name
                4 -> DEPLOYING.name
                5 -> DEPLOY_FAIL.name
                6 -> TESTING.name
                7 -> EDIT.name
                8 -> AUDITING.name
                9 -> AUDIT_REJECT.name
                10 -> RELEASE_DEPLOYING.name
                11 -> RELEASE_DEPLOY_FAIL.name
                12 -> RELEASED.name
                13 -> GROUNDING_SUSPENSION.name
                14 -> UNDERCARRIAGING.name
                15 -> UNDERCARRIAGED.name
                else -> INIT.name
            }
        }
    }
}
