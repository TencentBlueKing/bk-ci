package com.tencent.devops.common.auth.utlis

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.action.ActionPolicyDTO
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO
import com.tencent.devops.common.auth.api.AuthResourceType
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthUtilsTest {

    val actionPolicys = mutableListOf<ActionPolicyDTO>()

    val expressionList = mutableListOf<ExpressionDTO>()

    val newExpressionList = mutableListOf<ExpressionDTO>()

    @Before
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

        buildExpression()
        buildNewExpression()
    }

    fun buildNewExpression() {
        // 单项目下任务 {"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,test1/"}
        val expressionDTO1 = ExpressionDTO()
        expressionDTO1.field = "pipeline._bk_iam_path_"
        expressionDTO1.operator = ExpressionOperationEnum.START_WITH
        expressionDTO1.value = "/project,test1/"
        newExpressionList.add(expressionDTO1)

        // {"content":[{"field":"pipeline.id","op":"in","value":["p-098b68a251ae4ec4b6f4fde87767387f","p-12b2c343109f43a58a79dcb9e3721c1b","p-54a8619d1f754d32b5b2bc249a74f26c"]},{"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,demo/"}],"op":"AND"}
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

        // {"content":[{"content":[{"field":"pipeline.id","op":"in","value":["p-0d1fff4dabca4fc282e5ff63644bd339","p-54fb8b6562584df4b3693f7c787c105a"]},{"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,v3test/"}],"op":"AND"},{"content":[{"field":"pipeline.id","op":"in","value":["p-098b68a251ae4ec4b6f4fde87767387f","p-12b2c343109f43a58a79dcb9e3721c1b","p-54a8619d1f754d32b5b2bc249a74f26c"]},{"field":"pipeline._bk_iam_path_","op":"starts_with","value":"/project,demo/"}],"op":"AND"}],"op":"OR"}
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

    fun buildExpression() {
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
    fun getInstanceList1() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "demo", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        Assert.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getInstanceList2() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "v3test", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assert.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getInstanceList3() {
        val instanceList = AuthUtils.getResourceInstance(expressionList, "v3test1", AuthResourceType.PIPELINE_DEFAULT)
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        Assert.assertEquals(instanceList, mockList)
        print(instanceList)
    }

    @Test
    fun getResourceInstanceTest1() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        val emptyList = emptySet<String>()
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[0], "test1", resourceType))
        Assert.assertEquals(emptyList, AuthUtils.getResourceInstance(newExpressionList[0], "demo", resourceType))
    }

    @Test
    fun getResourceInstanceTest2() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT
        val mockList = mutableSetOf<String>()
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-54a8619d1f754d32b5b2bc249a74f26c")
        val emptyList = emptySet<String>()
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[1], "demo", resourceType))
        Assert.assertEquals(emptyList, AuthUtils.getResourceInstance(newExpressionList[1], "test1", resourceType))
    }

    @Test
    fun getResourceInstanceTest3() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT
        val mockList = mutableSetOf<String>()
        mockList.add("p-098b68a251ae4ec4b6f4fde87767387f")
        mockList.add("p-12b2c343109f43a58a79dcb9e3721c1b")
        mockList.add("p-54a8619d1f754d32b5b2bc249a74f26c")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("p-0d1fff4dabca4fc282e5ff63644bd339")
        mockList1.add("p-54fb8b6562584df4b3693f7c787c105a")
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[2], "demo", resourceType))
        Assert.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[2], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest4() {
        val resourceType = AuthResourceType.PIPELINE_DEFAULT
        val mockList = mutableSetOf<String>()
        mockList.add("*")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("p-0d1fff4dabca4fc282e5ff63644bd339")
        mockList1.add("p-54fb8b6562584df4b3693f7c787c105a")
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[3], "demo", resourceType))
        Assert.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[3], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest5() {
        val resourceType = AuthResourceType.TICKET_CREDENTIAL
        val mockList = mutableSetOf<String>()
        mockList.add("test_3")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("test")
        mockList1.add("test_3")
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[4], "", resourceType))
        Assert.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
    }

    @Test
    fun getResourceInstanceTest6() {
        val resourceType = AuthResourceType.TICKET_CREDENTIAL
        val mockList = mutableSetOf<String>()
        mockList.add("test_3")
        mockList.add("test")
        val mockList1 = mutableSetOf<String>()
        mockList1.add("test")
        mockList1.add("test_3")
        Assert.assertEquals(mockList, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
        Assert.assertEquals(mockList1, AuthUtils.getResourceInstance(newExpressionList[4], "v3test", resourceType))
    }

    fun print(projectList: List<String>) {
        println(projectList)
        projectList.map {
            println(it)
        }
    }
}