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

package com.tencent.devops.common.auth.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.action.ActionPolicyDTO
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO
import com.tencent.devops.common.auth.api.AuthResourceType
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@Suppress("MaxLineLength")
class AuthUtilsTest {

    private val actionPolicys = mutableListOf<ActionPolicyDTO>()
    private val actionPolicys1 = mutableListOf<ActionPolicyDTO>()

    private val expressionList = mutableListOf<ExpressionDTO>()

    private val newExpressionList = mutableListOf<ExpressionDTO>()

    @BeforeEach
    fun setUp() {
        val actionPolicy = ActionPolicyDTO()
        val expression = ExpressionDTO()
        expression.field = "project.id"
        expression.operator = ExpressionOperationEnum.IN
        expression.value = "[demo,test1]"
        actionPolicy.actionId = "project_view"
        actionPolicy.condition = expression
        actionPolicys.add(actionPolicy)

        val actionPolicy1 = ActionPolicyDTO()
        val expression1 = ExpressionDTO()
        expression1.field = "project.id"
        expression1.operator = ExpressionOperationEnum.EQUAL
        expression1.value = "demo"
        actionPolicy1.actionId = "project_view"
        actionPolicy1.condition = expression1
        actionPolicys.add(actionPolicy1)

        val actionPolicy2 = ActionPolicyDTO()
        val expression2 = ExpressionDTO()
        expression2.field = "project.id"
        expression2.operator = ExpressionOperationEnum.ANY
        actionPolicy2.actionId = "project_view"
        actionPolicy2.condition = expression2
        actionPolicys.add(actionPolicy2)

        val actionPolicy3 = ActionPolicyDTO()
        val expression3 = ExpressionDTO()
        expression3.field = null
        expression3.operator = ExpressionOperationEnum.OR
        expression3.value = null
        expression3.content = mutableListOf()
        val expression31 = ExpressionDTO()
        val expression32 = ExpressionDTO()
        expression31.operator = ExpressionOperationEnum.ANY
        expression31.field = "project.id"
        expression31.value = "[]"
        expression31.content = null

        expression32.operator = ExpressionOperationEnum.IN
        expression32.field = "project.id"
        expression32.value = "[w1, dgm]"
        expression32.content = null
        expression3.content.add(expression31)
        expression3.content.add(expression32)
        actionPolicy3.actionId = null
        actionPolicy3.condition = expression3
        actionPolicys.add(actionPolicy3)

        buildExpression()
        buildNewExpression()

        val objectMapper = ObjectMapper()

        val expressionStr1 = "{\"content\":[{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"AND\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"}],\"op\":\"OR\"}"
        val expressionStr2 = "{\"content\":[{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"AND\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"}],\"op\":\"OR\"}"
        val expressionStr3 = "{\"content\":[{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"AND\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"OR\"}"
        val expressionStr4 = "{\"content\":[{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"AND\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"OR\"}"
        val expressionStr5 = "{\"content\":[{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,iamV3test-080303/\"}],\"op\":\"AND\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"OR\"}"
        val expressionStr6 = "{\"content\":[{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"eq\",\"value\":\"868835\"},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"},{\"content\":[{\"field\":\"pipeline.id\",\"op\":\"in\",\"value\":[\"873400\",\"873416\"]},{\"field\":\"pipeline._bk_iam_path_\",\"op\":\"starts_with\",\"value\":\"/project,bkdevops/\"}],\"op\":\"AND\"}],\"op\":\"OR\"}"

        val expressionResult1 = objectMapper.readValue(expressionStr1, ExpressionDTO::class.java)
        val actionPolicyResultDTO1 = ActionPolicyDTO()
        actionPolicyResultDTO1.actionId = null
        actionPolicyResultDTO1.condition = expressionResult1
        val expressionResult2 = objectMapper.readValue(expressionStr2, ExpressionDTO::class.java)
        val actionPolicyResultDTO2 = ActionPolicyDTO()
        actionPolicyResultDTO2.actionId = null
        actionPolicyResultDTO2.condition = expressionResult2
        val actionPolicyResultDTO3 = ActionPolicyDTO()
        val expressionResult3 = objectMapper.readValue(expressionStr3, ExpressionDTO::class.java)
        actionPolicyResultDTO3.actionId = null
        actionPolicyResultDTO3.condition = expressionResult3
        val actionPolicyResultDTO4 = ActionPolicyDTO()
        val expressionResult4 = objectMapper.readValue(expressionStr4, ExpressionDTO::class.java)
        actionPolicyResultDTO4.actionId = null
        actionPolicyResultDTO4.condition = expressionResult4
        val actionPolicyResultDTO5 = ActionPolicyDTO()
        val expressionResult5 = objectMapper.readValue(expressionStr5, ExpressionDTO::class.java)
        actionPolicyResultDTO5.actionId = null
        actionPolicyResultDTO5.condition = expressionResult5
        val actionPolicyResultDTO6 = ActionPolicyDTO()
        val expressionResult6 = objectMapper.readValue(expressionStr6, ExpressionDTO::class.java)
        actionPolicyResultDTO6.actionId = null
        actionPolicyResultDTO6.condition = expressionResult6

        actionPolicys1.add(actionPolicyResultDTO1)
        actionPolicys1.add(actionPolicyResultDTO2)
        actionPolicys1.add(actionPolicyResultDTO3)
        actionPolicys1.add(actionPolicyResultDTO4)
        actionPolicys1.add(actionPolicyResultDTO5)
        actionPolicys1.add(actionPolicyResultDTO6)
    }

    private fun buildNewExpression() {
        // 单项目下任务 {"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,test1/"}
        val expressionDTO1 = ExpressionDTO()
        expressionDTO1.field = "pipeline._bk_iam_path_"
        expressionDTO1.operator = ExpressionOperationEnum.START_WITH
        expressionDTO1.value = "/project,test1/"
        newExpressionList.add(expressionDTO1)

        val expressionDTO2 = ExpressionDTO()
        val childExpression1 = ExpressionDTO()
        val childExpression2 = ExpressionDTO()
        childExpression1.field = "pipeline.id"
        childExpression1.operator = ExpressionOperationEnum.IN
        expressionDTO2.content = mutableListOf()
        val pipelineList1 = mutableListOf<String>()
        pipelineList1.add("p-098b68a251ae4ec4b6f4fde87767387f")
        pipelineList1.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        pipelineList1.add("p-54a8619d1f754d32b5b2bc249a74f26c")
        childExpression1.value = pipelineList1

        childExpression2.field = "pipeline._bk_iam_path_"
        childExpression2.operator = ExpressionOperationEnum.START_WITH
        childExpression2.value = "/project,demo/"
        expressionDTO2.content.add(childExpression1)
        expressionDTO2.content.add(childExpression2)
        expressionDTO2.operator = ExpressionOperationEnum.AND

        newExpressionList.add(expressionDTO2)

        val expressionDTO3 = ExpressionDTO()
        expressionDTO3.content = mutableListOf()
        val childExpression3 = ExpressionDTO()
        childExpression3.content = mutableListOf()
        val lastExpression1 = ExpressionDTO()
        lastExpression1.field = "pipeline.id"
        lastExpression1.operator = ExpressionOperationEnum.IN
        val pipelineList2 = mutableListOf<String>()
        pipelineList2.add("p-0d1fff4dabca4fc282e5ff63644bd339")
        pipelineList2.add("p-54fb8b6562584df4b3693f7c787c105a")
        lastExpression1.value = pipelineList2
        val lastExpression2 = ExpressionDTO()
        lastExpression2.field = "pipeline._bk_iam_path_"
        lastExpression2.operator = ExpressionOperationEnum.START_WITH
        lastExpression2.value = "/project,v3test/"

        childExpression3.content.add(lastExpression1)
        childExpression3.content.add(lastExpression2)
        childExpression3.operator = ExpressionOperationEnum.AND

        expressionDTO3.content.add(childExpression3)
        expressionDTO3.content.add(expressionDTO2)
        expressionDTO3.operator = ExpressionOperationEnum.OR
        newExpressionList.add(expressionDTO3)

        val expressionDTO4 = ExpressionDTO()
        expressionDTO4.content = mutableListOf()
        val childExpression4 = ExpressionDTO()
        childExpression4.content = mutableListOf()
        expressionDTO4.operator = ExpressionOperationEnum.OR
        expressionDTO4.content.add(childExpression3)

        childExpression4.field = "pipeline._bk_iam_path_"
        childExpression4.operator = ExpressionOperationEnum.START_WITH
        childExpression4.value = "/project,demo/"
        expressionDTO4.content.add(childExpression4)
        newExpressionList.add(expressionDTO4)

        val expressionDTO5 = ExpressionDTO()
        expressionDTO5.content = mutableListOf()
        expressionDTO5.operator = ExpressionOperationEnum.OR
        val childExpression5 = ExpressionDTO()
        childExpression5.field = "credential.id"
        childExpression5.operator = ExpressionOperationEnum.EQUAL
        childExpression5.value = "test_3"

        val childExpression6 = ExpressionDTO()
        val childExpression7 = ExpressionDTO()
        val childExpression8 = ExpressionDTO()
        childExpression5.content = mutableListOf()
        childExpression6.content = mutableListOf()
        childExpression7.operator = ExpressionOperationEnum.EQUAL
        childExpression7.value = "test"
        childExpression7.field = "credential.id"

        childExpression8.operator = ExpressionOperationEnum.START_WITH
        childExpression8.value = "/project,v3test/"
        childExpression8.field = "credential._bk_iam_path_"

        childExpression6.content.add(childExpression7)
        childExpression6.content.add(childExpression8)
        childExpression6.operator = ExpressionOperationEnum.AND
        expressionDTO5.content.add(childExpression5)
        expressionDTO5.content.add(childExpression6)
        newExpressionList.add(expressionDTO5)
    }

    private fun buildExpression() {
        val expression1 = ExpressionDTO()
        expression1.field = "project._bk_iam_path_"
        expression1.operator = ExpressionOperationEnum.START_WITH
        expression1.value = "/project,v3test/pipeline,*/"

        val expression2 = ExpressionDTO()
        expression2.field = "project._bk_iam_path_"
        expression2.operator = ExpressionOperationEnum.START_WITH
        expression2.value = "/project,demo/pipeline,p-12b2c343109f43a58a79dcb9e3721c1b/"

        val expression3 = ExpressionDTO()
        expression3.field = "project._bk_iam_path_"
        expression3.operator = ExpressionOperationEnum.START_WITH
        expression3.value = "/project,demo/pipeline,p-098b68a251ae4ec4b6f4fde87767387f/"

        val expression4 = ExpressionDTO()
        expression4.field = "project._bk_iam_path_"
        expression4.operator = ExpressionOperationEnum.START_WITH
        expression4.value = "/project,v3test/pipeline,p-0d1fff4dabca4fc282e5ff63644bd339/"

        val expression5 = ExpressionDTO()
        expression5.field = "project._bk_iam_path_"
        expression5.operator = ExpressionOperationEnum.START_WITH
        expression5.value = "/project,v3test1/"

        expressionList.add(expression1)
        expressionList.add(expression2)
        expressionList.add(expression3)
        expressionList.add(expression4)
        expressionList.add(expression5)
    }

    @Test
    fun getProjects() {
        val projectList = AuthUtils.getProjects(actionPolicys[0].condition)
        print(projectList)
    }

    @Test
    fun getProjects1() {
        val projectList = AuthUtils.getProjects(actionPolicys[1].condition)
        print(projectList)
    }

    @Test
    fun getProjects2() {
        val projectList = AuthUtils.getProjects(actionPolicys[2].condition)
        print(projectList)
    }

    @Test
    fun getProjects3() {
        val projectList = AuthUtils.getProjects(actionPolicys[3].condition)
        print(projectList)
    }

    @Test
    fun getInstanceList1() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "demo", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        Assertions.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getInstanceList2() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "v3test", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assertions.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getInstanceList3() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "v3test1", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assertions.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getResourceInstanceTest1() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        val emptyList = emptySet<String>()
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[0], "test1", resourceType))
        Assertions.assertEquals(emptyList, AuthUtils.getResourceInstance(newExpressionList[0], "demo", resourceType))
    }

    @Test
    fun getResourceInstanceTest2() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockList = mutableSetOf<String>()
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-54a8619d1f754d32b5b2bc249a74f26c")
        val emptyList = emptySet<String>()
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[1], "demo", resourceType))
        Assertions.assertEquals(emptyList, AuthUtils.getResourceInstance(newExpressionList[1], "test1", resourceType))
    }

    @Test
    fun getResourceInstanceTest3() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockList = mutableSetOf<String>()
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-54a8619d1f754d32b5b2bc249a74f26c")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("p-0d1fff4dabca4fc282e5ff63644bd339")
        mockList1.add("p-54fb8b6562584df4b3693f7c787c105a")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[2], "demo", resourceType))
        Assertions.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[2], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest4() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("p-0d1fff4dabca4fc282e5ff63644bd339")
        mockList1.add("p-54fb8b6562584df4b3693f7c787c105a")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[3], "demo", resourceType))
        Assertions.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[3], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest5() {
        val resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        val mockList = mutableSetOf<String>()
        mockList.add("test_3")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("test")
        mockList1.add("test_3")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[4], "", resourceType))
        Assertions.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest6() {
        val resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        val mockList = mutableSetOf<String>()
        mockList.add("test_3")
        mockList.add("test")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("test")
        mockList1.add("test_3")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
        Assertions.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest7() {
        val expression1 = ExpressionDTO()
        expression1.operator = ExpressionOperationEnum.OR
        expression1.value = null
        expression1.field = null
        val childExpression1 = ExpressionDTO()
        childExpression1.operator = ExpressionOperationEnum.START_WITH
        childExpression1.field = "credential._bk_iam_path"
        childExpression1.value = "/project,fitztest/,"
        childExpression1.content = null
        val childExpression2 = ExpressionDTO()
        childExpression2.operator = ExpressionOperationEnum.START_WITH
        childExpression2.field = "credential._bk_iam_path"
        childExpression2.value = "/project,testaaa/,"
        childExpression2.content = null
        val expression1Content = mutableListOf<ExpressionDTO>()
        expression1Content.add(childExpression1)
        expression1Content.add(childExpression2)
        expression1.content = expression1Content
        val resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(expression1, "fitztest", resourceType))
        print(AuthUtils.getResourceInstance(expression1, "fitztest", resourceType))
    }

    @Test
    fun getResourceInstanceTest8() {
        val e1 = ExpressionDTO()
        e1.operator = ExpressionOperationEnum.OR
        e1.value = null
        e1.field = null
        val e21 = ExpressionDTO()
        e21.operator = ExpressionOperationEnum.START_WITH
        e21.field = "credential._bk_iam_path"
        e21.value = "/project,testaaa/,"
        e21.content = null
        val e22 = ExpressionDTO()
        e22.operator = ExpressionOperationEnum.OR
        e22.field = null
        e22.value = null

        val e31 = ExpressionDTO()
        e31.operator = ExpressionOperationEnum.AND
        e31.field = null
        e31.value = null
        val e32 = ExpressionDTO()
        e32.operator = ExpressionOperationEnum.AND
        e32.field = null
        e32.value = null

        val e41 = ExpressionDTO()
        e41.operator = ExpressionOperationEnum.IN
        e41.field = "credential.id"
        e41.value = listOf("fabio", "dsahs")
        e41.content = null
        val e42 = ExpressionDTO()
        e42.operator = ExpressionOperationEnum.START_WITH
        e42.field = "credential._bk_iam_path"
        e42.value = "/project,testaaa/"
        e42.content = null

        val e43 = ExpressionDTO()
        e43.operator = ExpressionOperationEnum.IN
        e43.field = "credential.id"
        e43.value = listOf("001", "002", "003")
        e43.content = null
        val e44 = ExpressionDTO()
        e44.operator = ExpressionOperationEnum.START_WITH
        e44.field = "credential._bk_iam_path"
        e44.value = "/project,aa20200908"
        e44.content = null
        val e31content = mutableListOf<ExpressionDTO>()
        e31content.add(e41)
        e31content.add(e42)
        e31.content = e31content
        val e32Content = mutableListOf<ExpressionDTO>()
        e32Content.add(e43)
        e32Content.add(e44)
        e32.content = e32Content
        val e22content = mutableListOf<ExpressionDTO>()
        e22content.add(e31)
        e22content.add(e32)
        e22.content = e22content
        val e1Content = mutableListOf<ExpressionDTO>()
        e1Content.add(e21)
        e1Content.add(e22)
        e1.content = e1Content
        val resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        val mockList = mutableSetOf<String>()
        mockList.add("001")
        mockList.add("002")
        mockList.add("003")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(e1, "aa20200908", resourceType))
        print(AuthUtils.getResourceInstance(e1, "aa20200908", resourceType))
    }

    @Test
    fun getResourceInstanceTest9() {
        val expression1 = ExpressionDTO()
        expression1.operator = ExpressionOperationEnum.OR
        expression1.value = null
        expression1.field = null
        val childExpression1 = ExpressionDTO()
        childExpression1.operator = ExpressionOperationEnum.ANY
        childExpression1.field = "credential.id"
        childExpression1.value = null
        childExpression1.content = null

        val childExpression2 = ExpressionDTO()
        childExpression2.operator = ExpressionOperationEnum.AND
        childExpression2.field = null
        childExpression2.value = null

        val childExpression2Child1 = ExpressionDTO()
        childExpression2Child1.operator = ExpressionOperationEnum.EQUAL
        childExpression2Child1.field = "credential.id"
        childExpression2Child1.value = "jvtest"
        childExpression2Child1.content = null
        val childExpression2Child2 = ExpressionDTO()
        childExpression2Child2.operator = ExpressionOperationEnum.START_WITH
        childExpression2Child2.field = "credential._bk_iam_path_"
        childExpression2Child2.value = "/project,jttest/"
        childExpression2Child2.content = null
        val expressionChild2Content = mutableListOf<ExpressionDTO>()
        expressionChild2Content.add(childExpression2Child1)
        expressionChild2Content.add(childExpression2Child2)
        childExpression2.content = expressionChild2Content

        val expression1Content = mutableListOf<ExpressionDTO>()
        expression1Content.add(childExpression1)
        expression1Content.add(childExpression2)
        expression1.content = expression1Content

        val resourceType = AuthResourceType.TICKET_CREDENTIAL.value
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assertions.assertEquals(mockList, AuthUtils.getResourceInstance(expression1, "jttest", resourceType))
        print(AuthUtils.getResourceInstance(expression1, "jttest", resourceType))
    }

    @Test
    fun getResourceInstanceTest10() {
        val expression = ExpressionDTO()
        expression.operator = ExpressionOperationEnum.EQUAL
        expression.value = "testProject"
        expression.field = "project.id"
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockSet = mutableSetOf<String>()
        mockSet.add("*")
        Assertions.assertEquals(mockSet, AuthUtils.getResourceInstance(expression, "testProject", resourceType))
    }

    @Test
    fun getResourceInstanceTest11() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockSet1 = mutableSetOf<String>()
        mockSet1.add("*")
        val mockSet2 = mutableSetOf<String>()
        mockSet2.add("873400")
        mockSet2.add("873416")
        val mockSet3 = mutableSetOf<String>()
        mockSet3.add("868835")
        val resultSet1 = mutableSetOf<String>()
        resultSet1.addAll(mockSet1)
        resultSet1.addAll(mockSet2)
        val resultSet2 = mutableSetOf<String>()
        resultSet2.addAll(mockSet2)
        resultSet2.addAll(mockSet3)
        Assertions.assertEquals(mockSet1, AuthUtils.getResourceInstance(actionPolicys1[0].condition, "bkdevops", resourceType))
        Assertions.assertEquals(mockSet1, AuthUtils.getResourceInstance(actionPolicys1[1].condition, "bkdevops", resourceType))
        Assertions.assertEquals(resultSet1, AuthUtils.getResourceInstance(actionPolicys1[2].condition, "bkdevops", resourceType))
        Assertions.assertEquals(resultSet1, AuthUtils.getResourceInstance(actionPolicys1[3].condition, "bkdevops", resourceType))
        Assertions.assertEquals(resultSet1, AuthUtils.getResourceInstance(actionPolicys1[4].condition, "bkdevops", resourceType))
        Assertions.assertEquals(resultSet2, AuthUtils.getResourceInstance(actionPolicys1[5].condition, "bkdevops", resourceType))
    }

    @Test
    fun getResourceInstanceTest12() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT.value
        val mockSet1 = mutableSetOf<String>()
        mockSet1.add("*")
        val mockSet2 = mutableSetOf<String>()
        mockSet2.add("*")
        mockSet2.add("868835")
        Assertions.assertEquals(mockSet1, AuthUtils.getResourceInstance(actionPolicys1[0].condition, "iamV3test-080303", resourceType))
        Assertions.assertEquals(mockSet1, AuthUtils.getResourceInstance(actionPolicys1[1].condition, "iamV3test-080303", resourceType))
        Assertions.assertEquals(mockSet2, AuthUtils.getResourceInstance(actionPolicys1[2].condition, "iamV3test-080303", resourceType))
        Assertions.assertEquals(mockSet2, AuthUtils.getResourceInstance(actionPolicys1[3].condition, "iamV3test-080303", resourceType))
        Assertions.assertEquals(mockSet1, AuthUtils.getResourceInstance(actionPolicys1[4].condition, "iamV3test-080303", resourceType))
    }

    private fun print(projectList: List<String>) {
        println(projectList)
        projectList.map {
            println(it)
        }
    }
}
