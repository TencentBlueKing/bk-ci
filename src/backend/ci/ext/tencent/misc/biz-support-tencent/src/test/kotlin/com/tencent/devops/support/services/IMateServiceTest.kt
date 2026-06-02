/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝盾持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝盾持续集成平台 is licensed under the MIT license.
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

package com.tencent.devops.support.services

import com.tencent.devops.support.model.imate.IMateRobotOwnerType
import com.tencent.devops.support.model.imate.IMateRobotScopeType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IMateServiceTest {

    @Test
    fun `should map shared robot info`() {
        val iMateService = IMateService()
        setField(iMateService, "baseUrl", "https://test-imate.woa.com")

        val robotInfo = iMateService.toRobotInfo(
            username = "queryUser",
            robot = IMateService.IMateRobotResponse(
                id = 206L,
                botName = "共享机器人",
                username = "owner",
                clientUuid = "uuid-1",
                clientType = "team_devcloud",
                status = "RUNNING",
                url = "https://test-imate.woa.com/webhook"
            )
        )

        assertEquals(IMateRobotScopeType.SHARED, robotInfo.robotScopeType)
        assertEquals(IMateRobotOwnerType.SHARED_TO_USER, robotInfo.ownerType)
        assertEquals("https://test-imate.woa.com/oauth?deviceId=uuid-1", robotInfo.authorizationUrl)
    }

    @Test
    fun `should map self created personal robot info`() {
        val iMateService = IMateService()
        setField(iMateService, "baseUrl", "https://test-imate.woa.com/")

        val robotInfo = iMateService.toRobotInfo(
            username = "owner",
            robot = IMateService.IMateRobotResponse(
                id = 207L,
                botName = "个人机器人",
                username = "owner",
                clientUuid = "uuid-2",
                clientType = "devcloud"
            )
        )

        assertEquals(IMateRobotScopeType.PERSONAL, robotInfo.robotScopeType)
        assertEquals(IMateRobotOwnerType.SELF_CREATED, robotInfo.ownerType)
        assertEquals("https://test-imate.woa.com/oauth?deviceId=uuid-2", robotInfo.authorizationUrl)
    }

    private fun setField(target: Any, fieldName: String, value: String) {
        val field = target.javaClass.getDeclaredField(fieldName)
        field.isAccessible = true
        field.set(target, value)
    }
}
