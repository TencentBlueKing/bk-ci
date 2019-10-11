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
        websocketService.changePage(changePage.userId, changePage.sessionId, changePage.page, changePage.projectId)
    }

    @MessageMapping("/loginOut")
    fun loginOut(loginOutDTO: LoginOutDTO) {
        websocketService.loginOut(loginOutDTO.userId, loginOutDTO.sessionId, loginOutDTO.page)
    }

    @MessageMapping("/clearUserSession")
    fun clearUserSession(clearUserDTO: ClearUserDTO) {
        websocketService.clearUserSession(clearUserDTO.userId, clearUserDTO.sessionId)
    }
}