package com.tencent.devops.auth.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ActionUtilsTest {

    @Test
    fun buildAction() {
    }

    @Test
    fun actionType() {
        val actionType = "pipeline"
        assertEquals(actionType,ActionUtils.actionType(actionType))
    }

    @Test
    fun actionType1() {
        val actionType = "pipeline_create"
        assertEquals("pipeline",ActionUtils.actionType(actionType))
    }

    @Test
    fun actionType2() {
        val actionType = "pipeline1_create"
        assertNotEquals("pipeline",ActionUtils.actionType(actionType))
    }

    @Test
    fun decord() {
        val password = "YmtfaWFtOnh5MnM4aXAyOGptaXp5aWoyN2liZ3JucmRoMmw0a3p1"
        val str = StringUtils.decodeAuth(password)
        println(str.first)
        println(str.second)
        assertEquals("bk_iam", str.first)
        assertEquals("xy2s8ip28jmizyij27ibgrnrdh2l4kzu", str.second)
        val password11 = "Basic YmtfaWFtOnh5MnM4aXAyOGptaXp5aWoyN2liZ3JucmRoMmw0a3p1"
        val str1 = StringUtils.decodeAuth(password11)
        println(str1.first)
        println(str1.second)
        assertEquals("bk_iam", str1.first)
        assertEquals("xy2s8ip28jmizyij27ibgrnrdh2l4kzu", str1.second)
    }
}