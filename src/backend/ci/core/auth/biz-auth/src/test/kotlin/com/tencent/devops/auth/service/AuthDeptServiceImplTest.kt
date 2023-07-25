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

package com.tencent.devops.auth.service

import com.tencent.devops.common.test.BkCiAbstractTest
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test

class AuthDeptServiceImplTest : BkCiAbstractTest() {
    private val authDeptServiceImpl = AuthDeptServiceImpl(redisOperation, objectMapper)

    @Test
    fun test() {
        val response = "{\n" +
                "        \"count\": 162,\n" +
                "        \"results\": [\n" +
                "            {\n" +
                "                \"status\": \"NORMAL\",\n" +
                "                \"domain\": \"XXX.com\",\n" +
                "                \"telephone\": \"XXX\",\n" +
                "                \"create_time\": \"2021-04-27T15:33:59.000000Z\",\n" +
                "                \"country_code\": \"86\",\n" +
                "                \"logo\": null,\n" +
                "                \"iso_code\": \"CN\",\n" +
                "                \"id\": 173169,\n" +
                "                \"display_name\": \"abc\",\n" +
                "                \"leader\": [\n" +
                "                    {\n" +
                "                        \"username\": \"abcv\",\n" +
                "                        \"display_name\": \"abc\",\n" +
                "                        \"id\": 2435\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"username\": \"abc\",\n" +
                "                \"update_time\": \"2021-04-27T15:33:59.000000Z\",\n" +
                "                \"wx_userid\": \"\",\n" +
                "                \"staff_status\": \"IN\",\n" +
                "                \"password_valid_days\": -1,\n" +
                "                \"qq\": \"\",\n" +
                "                \"language\": \"zh-cn\",\n" +
                "                \"enabled\": true,\n" +
                "                \"time_zone\": \"Asia/Shanghai\",\n" +
                "                \"departments\": [\n" +
                "                    {\n" +
                "                        \"order\": 1,\n" +
                "                        \"id\": 6580,\n" +
                "                        \"full_name\": \"123/345\",\n" +
                "                        \"name\": \"应用开发组\"\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"email\": \"abc@XXX.com\",\n" +
                "                \"extras\": {\n" +
                "                    \"gender\": \"男\",\n" +
                "                    \"postname\": \"应用开发组员工\"\n" +
                "                },\n" +
                "                \"position\": 0,\n" +
                "                \"category_id\": 2\n" +
                "            },\n" +
                "            {\n" +
                "                \"status\": \"NORMAL\",\n" +
                "                \"domain\": \"XXX.com\",\n" +
                "                \"telephone\": \"XXX\",\n" +
                "                \"create_time\": \"2021-04-27T15:33:59.000000Z\",\n" +
                "                \"country_code\": \"86\",\n" +
                "                \"logo\": null,\n" +
                "                \"iso_code\": \"CN\",\n" +
                "                \"id\": 173168,\n" +
                "                \"display_name\": \"abc\",\n" +
                "                \"leader\": [\n" +
                "                    {\n" +
                "                        \"username\": \"abc\",\n" +
                "                        \"display_name\": \"abc\",\n" +
                "                        \"id\": 2435\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"username\": \"def\",\n" +
                "                \"update_time\": \"2021-04-27T15:33:59.000000Z\",\n" +
                "                \"wx_userid\": \"\",\n" +
                "                \"staff_status\": \"IN\",\n" +
                "                \"password_valid_days\": -1,\n" +
                "                \"qq\": \"\",\n" +
                "                \"language\": \"zh-cn\",\n" +
                "                \"enabled\": true,\n" +
                "                \"time_zone\": \"Asia/Shanghai\",\n" +
                "                \"departments\": [\n" +
                "                    {\n" +
                "                        \"order\": 1,\n" +
                "                        \"id\": 6580,\n" +
                "                        \"full_name\": \"123/455\",\n" +
                "                        \"name\": \"应用开发组\"\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"email\": \"XXX@126.com\",\n" +
                "                \"extras\": {\n" +
                "                    \"gender\": \"男\",\n" +
                "                    \"postname\": \"应用开发组员工\"\n" +
                "                },\n" +
                "                \"position\": 0,\n" +
                "                \"category_id\": 2\n" +
                "            }\n" +
                "        ]\n" +
                "    }"
        val users = authDeptServiceImpl.findUserName(response)
        val mockUsers = mutableListOf<String>()
        mockUsers.add("abc")
        mockUsers.add("def")
        Assertions.assertEquals(users, mockUsers)
    }

    @Test
    fun test1() {
        val response = "[ {\n" +
                "  \"id\" : \"29510\",\n" +
                "  \"family\" : [ {\n" +
                "    \"order\" : 1,\n" +
                "    \"id\" : 123,\n" +
                "    \"full_name\" : \"XXX公司\",\n" +
                "    \"name\" : \"XXX公司\"\n" +
                "  }, {\n" +
                "    \"order\" : 1,\n" +
                "    \"id\" : 12345,\n" +
                "    \"full_name\" : \"XXX公司/XXX事业群\",\n" +
                "    \"name\" : \"XXX事业群\"\n" +
                "  }, {\n" +
                "    \"order\" : 1,\n" +
                "    \"id\" : 456,\n" +
                "    \"full_name\" : \"XXX公司/XXX事业群/XXX部\",\n" +
                "    \"name\" : \"XXX部\"\n" +
                "  }, {\n" +
                "    \"order\" : 1,\n" +
                "    \"id\" : 9878,\n" +
                "    \"full_name\" : \"XXX公司/XXX事业群/XXX部/XXX中心\",\n" +
                "    \"name\" : \"XXX中心\"\n" +
                "  } ],\n" +
                "  \"name\" : \"XXX组\"\n" +
                "} ]"
        val users = authDeptServiceImpl.getUserDeptTreeIds(response)
        val mockUsers = mutableSetOf<String>()
        mockUsers.add("29510")
        mockUsers.add("123")
        mockUsers.add("12345")
        mockUsers.add("456")
        mockUsers.add("9878")
        Assertions.assertEquals(users, mockUsers)
    }
}
