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

package com.tencent.bkrepo.auth

import com.tencent.bkrepo.auth.service.KeyService
import com.tencent.bkrepo.common.api.constant.USER_KEY
import com.tencent.bkrepo.common.api.exception.ErrorCodeException
import com.tencent.bkrepo.common.service.util.HttpContextHolder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class KeyServiceTest {

    @Autowired
    private lateinit var keyService: KeyService

    private val key = "ssh-dss AAAAB3NzaC1kc3MAAACBAIBHBMwOb1xhQhZP72oCYBp76yFNv6Vk55eIVcKFZiGSO4HL" +
        "/HX8SeN5zXYHwtMIdY1Mtvmj9530SDs4wqIVeDCxIpRaWb/JolAYilBDoHKNYFUzNi/e/HL7HgIrWk7fP0wRJdPTQ5cCxi30" +
        "/1hj4wHO4U89dqdk2Kv7MqYUf3ZbAAAAFQCZguQsuyDleb2MGIr5GSiTrSyFuQAAAIA/LlSMd7bTxZfG6bGhnLK7k/h+Jr8c/tge" +
        "/mnBDXS6k1L/h+FBWnxNxmuC+EgHMY9/SA/G7QHn9rydXngyIDFaQEHRW927P+Hh7skUsB4Jz6v5GezLvN0Uih" +
        "+hIl7roig5JdUacW1U5BSWVC3Az1xMZMakpdIu8HbrBeDd9cwJdgAAAIBKdozEn+Yr8r1i0Wdl31l+sH2K+5a/C3c" +
        "Rzw9KaqcjxEURT96mMLzZRIja/LJjUv2P81kpWhhXmkwKI+Ez+xkfOolskCEaTco3OZoifTDV3us05QuvlMb48/4Ru7aEt" +
        "8MLQTGGe5JXbLSLN7xsADR45pS5JojNbstj7dYELYpsmA== test"
    private val name = "unit-test-key"
    private val userId = "test"

    @BeforeEach
    fun setUp() {
        HttpContextHolder.getRequest().setAttribute(USER_KEY, userId)
        keyService.listKey().filter { it.name == name }.forEach {
            keyService.deleteKey(it.id)
        }
    }

    @AfterEach
    fun tearDown() {
        keyService.listKey().filter { it.name == name }.forEach {
            keyService.deleteKey(it.id)
        }
    }

    @Test
    fun createKeyTest() {
        keyService.createKey(name, key)
        assertThrows<ErrorCodeException> { keyService.createKey(name, key) }
    }

    @Test
    fun getKeyTest() {
        keyService.createKey(name, key)
        val keys = keyService.listKey()
        Assertions.assertTrue(keys.find { it.name == name } != null)
    }

    @Test
    fun deleteKeyTest() {
        assertThrows<ErrorCodeException> { keyService.deleteKey("test-id") }
        keyService.createKey(name, key)
        var key = keyService.listKey().find { it.name == name }
        Assertions.assertTrue(key != null)
        keyService.deleteKey(key!!.id)
        key = keyService.listKey().find { it.name == name }
        Assertions.assertTrue(key == null)
    }
}
