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

package com.tencent.devops.common.auth.utils

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

internal class IamGroupUtilsTest {

    @Test
    fun buildIamGroup() {
        val projectName = "测试项目"
        val groupName = "管理员"
        val group = IamGroupUtils.buildIamGroup(projectName, groupName)
        Assertions.assertEquals("测试项目-管理员", group)
    }

    @Test
    fun buildDefaultDescription() {
        val projectName = "测试项目"
        val groupName = "管理员"
        val userId = "admin"
        val group = IamGroupUtils.buildDefaultDescription(
            projectName,
            groupName,
            userId,
            "zh_CN"
        )
        Assertions.assertNotEquals("测试项目 分级管理员, 由admin 创建于 2021-04-27T21:58:52+0800", group)
    }

    @Test
    fun buildManagerDescription() {
        val projectName = "测试项目"
        val userId = "admin"
        val group = IamGroupUtils.buildManagerDescription(projectName, userId, "zh_CN")
        Assertions.assertNotEquals("测试项目 用户组:管理员, 由admin 创建于 2021-04-27T21:58:52+0800", group)
    }
}
