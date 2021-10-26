package com.tencent.devops.process.service.notify

import org.junit.Assert
import org.junit.Test
import java.util.regex.Pattern

internal class TxNotifySendGroupMsgCmdImplTest{
    private val roomPatten = "ww\\w" // ww 开头且接数字的正则表达式, 适用于应用号获取的roomid
    private val chatPatten = "^[A-Za-z0-9]+\$" // 数字和字母组成的群chatId正则表达式
    @Test
    fun chatTest() {
        val rootId = "ww3009388819"
        val chatId = "wrkSFfCgAA1fQtGJXOY9tM2jgCW0YEAA"
        Assert.assertEquals(true, Pattern.matches(roomPatten, rootId))
        Assert.assertEquals(false, Pattern.matches(roomPatten, chatId))
        Assert.assertEquals(true, Pattern.matches(chatPatten, chatId))
        Assert.assertEquals(false, Pattern.matches(chatPatten, rootId))
    }
}
