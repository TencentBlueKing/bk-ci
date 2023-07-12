package com.tencent.devops.common.db.listener

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

class SQLCheckListenerTest {
    private val sqlCheckListener = SQLCheckListener()

    @Test
    @DisplayName("正常数据--SELECT")
    fun test_1() {
        val sql = "SELECT * FROM db.table where id=1"
        Assertions.assertTrue(sqlCheckListener.check(sql))
    }

    @Test
    @DisplayName("没有WHERE--SELECT")
    fun test_2() {
        val sql = "SELECT * FROM db.table"
        Assertions.assertFalse(sqlCheckListener.check(sql))
    }

    @Test
    @DisplayName("正常数据--UPDATE")
    fun test_3() {
        val sql = "UPDATE db.table SET column=1 where id=1"
        Assertions.assertTrue(sqlCheckListener.check(sql))
    }

    @Test
    @DisplayName("没有WHERE--UPDATE")
    fun test_4() {
        val sql = "UPDATE db.table SET column=1"
        Assertions.assertFalse(sqlCheckListener.check(sql))
    }

    @Test
    @DisplayName("正常数据--DELETE")
    fun test_5() {
        val sql = "DELETE FROM db.table where id=1"
        Assertions.assertTrue(sqlCheckListener.check(sql))
    }

    @Test
    @DisplayName("没有WHERE--DELETE")
    fun test_6() {
        val sql = "DELETE FROM db.table"
        Assertions.assertFalse(sqlCheckListener.check(sql))
    }
}
