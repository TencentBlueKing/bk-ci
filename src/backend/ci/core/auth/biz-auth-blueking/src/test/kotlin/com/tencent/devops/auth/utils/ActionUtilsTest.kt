package com.tencent.devops.auth.utils

import org.junit.Test

import org.junit.Assert.*

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
}