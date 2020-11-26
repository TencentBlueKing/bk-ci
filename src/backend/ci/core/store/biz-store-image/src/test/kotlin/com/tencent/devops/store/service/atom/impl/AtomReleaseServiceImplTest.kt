package com.tencent.devops.store.service.atom.impl

import com.tencent.devops.common.api.util.JsonUtil
import org.junit.Assert.*
import org.junit.Test

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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
class AtomReleaseServiceImplTest{
    @Test
    fun test() {
        val taskJsonStr = "{\n" +
            "    \"atomCode\": \"fayetest\",\n" +
            "    \"execution\": {    \n" +
            "        \"language\": \"python\",\n" +
            "        \"demands\": [],\n" +
            "        \"target\": \"atom_demo\"\n" +
            "    },\n" +
            "    \"config\" : {\n" +
            "      \"canPauseBeforeRun\" : true\n" +
            "    },\n" +
            "    \"inputGroups\": [\n" +
            "        {\n" +
            "            \"name\": \"repoinfo\",\n" +
            "            \"label\": \"repoinfo\",\n" +
            "            \"isExpanded\": false\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"staff\",\n" +
            "            \"label\": \"staff\",\n" +
            "            \"isExpanded\": false\n" +
            "        },\n" +
            "        {\n" +
            "            \"name\": \"artifact\",\n" +
            "            \"label\": \"artifact\",\n" +
            "            \"isExpanded\": false\n" +
            "        }\n" +
            "    ],\n" +
            "    \"input\": {\n" +
            "        \"devops-select\": {\n" +
            "            \"label\": \"可输入可选择下拉（值为id）\",\n" +
            "            \"type\": \"devops-select\",\n" +
            "            \"desc\": \"可输入，可选择，输入时，若值不在列表里，只能输入变量\",\n" +
            "            \"required\": false,\n" +
            "            \"options\":[\n" +
            "              {\n" +
            "                \"id\":\"storage\",\n" +
            "                \"name\":\"storage-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"editor\",\n" +
            "                \"name\":\"editor-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"wiki\",\n" +
            "                \"name\":\"wiki-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"view\",\n" +
            "                \"name\":\"view-view\"\n" +
            "              }\n" +
            "            ]\n" +
            "        },\n" +
            "        \"selector\": {\n" +
            "            \"label\": \"只能选下拉\",\n" +
            "            \"type\": \"selector\",\n" +
            "            \"desc\": \"只能选择，不能输入\",\n" +
            "            \"required\": false,\n" +
            "            \"options\":[\n" +
            "              {\n" +
            "                \"id\":\"storage\",\n" +
            "                \"name\":\"storage-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"editor\",\n" +
            "                \"name\":\"editor-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"wiki\",\n" +
            "                \"name\":\"wiki-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"view\",\n" +
            "                \"name\":\"view-view\"\n" +
            "              }\n" +
            "            ]\n" +
            "        },\n" +
            "        \"select-input\": {\n" +
            "            \"label\": \"可输入可选择下拉\",\n" +
            "            \"type\": \"select-input\",\n" +
            "            \"desc\": \"可输入，可选择，可输入列表中没有的值\",\n" +
            "            \"required\": false,\n" +
            "            \"options\":[\n" +
            "              {\n" +
            "                \"id\":\"storage\",\n" +
            "                \"name\":\"storage-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"editor\",\n" +
            "                \"name\":\"editor-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"wiki\",\n" +
            "                \"name\":\"wiki-view\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"id\":\"view\",\n" +
            "                \"name\":\"view-view\"\n" +
            "              }\n" +
            "            ]\n" +
            "        },\n" +
            "        \"inputDemo\":{                       \n" +
            "            \"label\": \"输入示例\",\n" +
            "            \"default\": \"\",\n" +
            "            \"placeholder\": \"\",\n" +
            "            \"type\": \"atom-ace-editor\",\n" +
            "            \"groupName\": \"\",\n" +
            "            \"desc\": \"输入示例，\\r\\n描述换行\",\n" +
            "            \"rules\": {}, \n" +
            "            \"disabled\": false,\n" +
            "            \"hidden\": false,\n" +
            "            \"isSensitive\": true,\n" +
            "            \"bashConf\": {\n" +
            "                \"url\": \"/ms/store/api/user/pipeline/container/test1122\",\n" +
            "                \"dataPath\": \"data.0.typeList\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"file_src\":{\n" +
            "            \"label\":\"源文件\",\n" +
            "            \"default\":\"\",\n" +
            "            \"groupName\": \"artifact\",\n" +
            "            \"placeholder\":\"\",\n" +
            "            \"type\":\"enum-input\",\n" +
            "            \"list\":[\n" +
            "              {\n" +
            "                \"value\":\"PIPELINE\",\n" +
            "                \"label\":\"从本次已归档构件中获取\"\n" +
            "              },\n" +
            "              {\n" +
            "                \"value\":\"CUSTOM_DIR\",\n" +
            "                \"label\":\"从自定义版本仓库中获取\"\n" +
            "              }\n" +
            "            ],\n" +
            "            \"group_name\":\"\",\n" +
            "            \"desc\":\"\",\n" +
            "            \"required\":true,\n" +
            "            \"disabled\":false,\n" +
            "            \"hidden\":false,\n" +
            "            \"is_sensitive\":false\n" +
            "          },\n" +
            "        \"file_path\": {\n" +
            "            \"label\":\"源文件相对路径\",\n" +
            "            \"groupName\": \"artifact\",\n" +
            "            \"default\":\"\",\n" +
            "            \"placeholder\":\"构件的相对路径，支持*通配符\",\n" +
            "            \"type\":\"vuex-input\",\n" +
            "            \"group_name\":\"\",\n" +
            "            \"desc\":\"\",\n" +
            "            \"required\":true,\n" +
            "            \"disabled\":false,\n" +
            "            \"hidden\":false,\n" +
            "            \"is_sensitive\":false\n" +
            "        },\n" +
            "        \"projectId\": {\n" +
            "            \"label\":\"项目id\",\n" +
            "            \"groupName\": \"artifact\",\n" +
            "            \"default\":\"\",\n" +
            "            \"placeholder\":\"项目id\",\n" +
            "            \"type\":\"vuex-input\"\n" +
            "        },\n" +
            "        \"pipelineId\": {\n" +
            "            \"label\":\"流水线id\",\n" +
            "            \"groupName\": \"artifact\",\n" +
            "            \"default\":\"\",\n" +
            "            \"placeholder\":\"流水线id\",\n" +
            "            \"type\":\"vuex-input\"\n" +
            "        },\n" +
            "        \"buildNo\": {\n" +
            "            \"label\":\"buildNo\",\n" +
            "            \"groupName\": \"artifact\",\n" +
            "            \"default\":\"\",\n" +
            "            \"placeholder\":\"buildNo\",\n" +
            "            \"type\":\"vuex-input\"\n" +
            "        },\n" +
            "        \"ticket\": {                       \n" +
            "            \"label\": \"我的凭证\",\n" +
            "            \"type\": \"selector\",\n" +
            "            \"optionsConf\": {\n" +
            "              \"searchable\": true,\n" +
            "              \"clearable\": true,\n" +
            "              \"multiple\": false,\n" +
            "              \"url\": \"/ticket/api/user/credentials/{projectId}/hasPermissionList?permission=USE&page=1&pageSize=100&credentialTypes=USERNAME_PASSWORD\",\n" +
            "              \"paramId\": \"credentialId\",\n" +
            "              \"paramName\": \"credentialId\",\n" +
            "              \"itemTargetUrl\": \"http://devops.oa.com/console/ticket/{projectId}/createCredential/USERNAME_PASSWORD/true\",\n" +
            "              \"itemText\": \"添加相应的凭证\",\n" +
            "              \"hasAddItem\": true\n" +
            "            }\n" +
            "        },\n" +
            "        \"repo\": {                       \n" +
            "            \"label\": \"我的代码库\",\n" +
            "            \"type\": \"selector\",            \n" +
            "            \"optionsConf\": {\n" +
            "                \"url\": \"/repository/api/user/git/getProject?projectId=landunplugins\",\n" +
            "                \"dataPath\": \"data.project\",\n" +
            "                \"paramId\": \"nameWithNameSpace\",\n" +
            "                \"paramName\": \"httpUrl\"\n" +
            "            }\n" +
            "        },\n" +
            "        \"date\": {\n" +
            "            \"label\": \"日期\",\n" +
            "            \"type\": \"time-picker\",\n" +
            "            \"startDate\": 1559555765,\n" +
            "            \"endDate\": 1577894399000,\n" +
            "            \"showTime\": false\n" +
            "        },\n" +
            "        \"language\": {\n" +
            "            \"label\": \"复选框列表\",\n" +
            "            \"type\": \"atom-checkbox-list\",\n" +
            "            \"list\": [\n" +
            "                {\n" +
            "                    \"id\": \"php\",\n" +
            "                    \"name\": \"php\",\n" +
            "                    \"disabled\": true,\n" +
            "                    \"desc\": \"php是世界上最好的语言\"\n" +
            "                },\n" +
            "                {\n" +
            "                    \"id\": \"python\",\n" +
            "                    \"name\": \"python\",\n" +
            "                    \"disabled\": false,\n" +
            "                    \"desc\": \"python哦也\"\n" +
            "                },\n" +
            "                {\n" +
            "                  \"id\": \"java\",\n" +
            "                  \"name\": \"java\",\n" +
            "                  \"disabled\": false,\n" +
            "                  \"desc\": \"java\"\n" +
            "              }\n" +
            "            ]\n" +
            "        },\n" +
            "        \"receiver1\": {\n" +
            "            \"label\": \"收件人\",\n" +
            "            \"groupName\": \"staff\",\n" +
            "            \"type\": \"staff-input\",\n" +
            "            \"desc\": \"只能填写当前项目成员\"\n" +
            "        },\n" +
            "        \"receiver2\": {\n" +
            "            \"label\": \"收件人,支持邮件组\",\n" +
            "            \"groupName\": \"staff\",\n" +
            "            \"type\": \"company-staff-input\",\n" +
            "            \"inputType\": \"all\",\n" +
            "            \"desc\": \"可以填写公司任意用户\"\n" +
            "        },\n" +
            "        \"identity\":{                       \n" +
            "            \"label\": \"identity\",\n" +
            "            \"default\": \"\",\n" +
            "            \"placeholder\": \"\",\n" +
            "            \"type\": \"vuex-input\",\n" +
            "            \"groupName\": \"repoinfo\",\n" +
            "            \"desc\": \"identity\",\n" +
            "            \"required\": false\n" +
            "        },\n" +
            "        \"identity_type\":{                       \n" +
            "            \"label\": \"identity_type\",\n" +
            "            \"default\": \"\",\n" +
            "            \"placeholder\": \"\",\n" +
            "            \"type\": \"vuex-input\",\n" +
            "            \"groupName\": \"repoinfo\",\n" +
            "            \"desc\": \"identity_type\",\n" +
            "            \"required\": false\n" +
            "        }\n" +
            "    }, \n" +
            "    \"output\": {\n" +
            "        \"outputDemo\": {        \n" +
            "            \"description\" : \"输出示例\",\n" +
            "            \"type\": \"string\",\n" +
            "            \"isSensitive\": true\n" +
            "        }\n" +
            "    }\n" +
            "}"
        val taskDataMap = JsonUtil.toMap(taskJsonStr)
        taskDataMap.keys.forEach {
            println(taskDataMap[it])
        }
    }
}
