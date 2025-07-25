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

package com.tencent.devops.websocket.controller

import com.tencent.devops.websocket.pojo.ChangePageDTO
import com.tencent.devops.websocket.pojo.ClearUserDTO
import com.tencent.devops.websocket.pojo.LoginOutDTO
import com.tencent.devops.websocket.servcie.WebsocketService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller

@Controller
class WsController @Autowired constructor(
    val websocketService: WebsocketService
) {

    @MessageMapping("/changePage")
    fun changePage(changePage: ChangePageDTO) {
        websocketService.changePage(changePage)
    }

    @MessageMapping("/loginOut")
    fun loginOut(loginOutDTO: LoginOutDTO) {
        websocketService.loginOut(
            userId = loginOutDTO.userId,
            sessionId = loginOutDTO.sessionId,
            oldPage = loginOutDTO.page,
            transferData = loginOutDTO.transferData
        )
    }

    @MessageMapping("/clearUserSession")
    fun clearUserSession(clearUserDTO: ClearUserDTO) {
        websocketService.clearUserSession(
            userId = clearUserDTO.userId,
            sessionId = clearUserDTO.sessionId,
            transferData = clearUserDTO.transferData
        )
    }
}
