package com.tencent.devops.common.api.util

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class JsonPathUtilTest {
    val json = """
        {
            "data": {
                "user": {
                    "id": 123,
                    "name": "张三",
                    "email": "zhangsan@example.com"
                },
                "users": [
                    {"id": 1, "name": "用户1", "active": true},
                    {"id": 2, "name": "用户2", "active": false},
                    {"id": 3, "name": "用户3", "active": true}
                ],
                "status": "success"
            }
        }
    """.trimIndent()

    @Test
    fun read() {
        val userName = JsonPathUtil.read<String>(json, "$.data.user.name")
        Assertions.assertEquals("张三", userName)

        val status = JsonPathUtil.read<String>(json, "$.data.status")
        Assertions.assertEquals("success", status)

        val notExist = JsonPathUtil.read<String>(json, "$.data.notExist")
        Assertions.assertNull(notExist)

        val defaultValue = JsonPathUtil.read(json, "$.data.notExist", "123")
        Assertions.assertEquals(defaultValue, "123")
    }

    @Test
    fun readMultiple() {
        val map = JsonPathUtil.readMultiple(
            json, listOf(
                "$.data.user.name",
                "$.data.user.id",
                "$.data.status"
            )
        )
        Assertions.assertEquals(map.size, 3)
        Assertions.assertEquals(map["$.data.user.name"], "张三")
        Assertions.assertEquals(map["$.data.user.id"], 123)
        Assertions.assertEquals(map["$.data.status"], "success")
    }

    @Test
    fun pathExists() {
        Assertions.assertTrue(JsonPathUtil.pathExists(json, "$.data.user.name"))
        Assertions.assertTrue(JsonPathUtil.pathExists(json, "$.data.status"))
        Assertions.assertFalse(JsonPathUtil.pathExists(json, "$.data.notExist"))
    }
}
