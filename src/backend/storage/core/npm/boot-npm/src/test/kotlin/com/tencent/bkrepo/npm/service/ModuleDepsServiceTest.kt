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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *
 */

package com.tencent.bkrepo.npm.service

import com.tencent.bkrepo.npm.pojo.module.des.service.DepsCreateRequest
import com.tencent.bkrepo.npm.pojo.module.des.service.DepsDeleteRequest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
@DisplayName("npm依赖关系测试")
class ModuleDepsServiceTest {
    @Autowired
    private lateinit var moduleDepsService: ModuleDepsService

    @Test
    @DisplayName("npm新增依赖关系测试")
    fun createDepsTest() {
        val depsCreateRequest = DepsCreateRequest("test", "npm-local", "underscore", "code")
        val moduleDepsInfo = moduleDepsService.create(depsCreateRequest)
        Assertions.assertEquals(moduleDepsInfo.name, "underscore")
    }

    @Test
    @DisplayName("npm依赖关系删除测试")
    fun deleteDepsTest() {
        val depsDeleteRequest = DepsDeleteRequest("test", "npm-local", "underscore", "code", "system")
        moduleDepsService.delete(depsDeleteRequest)
    }

    @Test
    @DisplayName("npm依赖列表测试")
    fun listDepsTest() {
        val list = moduleDepsService.list("test", "npm-local", "helloworld-npm-publish")
        Assertions.assertEquals(list.size, 0)
    }

    @Test
    @DisplayName("npm依赖分页测试")
    fun pageDepsTest() {
        val page = moduleDepsService.page("test", "npm-local", 0, 20, "babel-messages")
        Assertions.assertEquals(page.count, 1)
    }
}
