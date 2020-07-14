package com.tencent.devops.common.auth.utlis

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.action.ActionPolicyDTO
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO
import org.junit.Before
import org.junit.Test


class AuthUtilsTest {

    val actionPolicys = mutableListOf<ActionPolicyDTO>()

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

    fun print(projectList: List<String>) {
        println(projectList)
        projectList.map {
            println(it)
        }
    }
}