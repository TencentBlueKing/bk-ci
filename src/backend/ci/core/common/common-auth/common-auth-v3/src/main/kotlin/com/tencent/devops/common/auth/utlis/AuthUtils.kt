package com.tencent.devops.common.auth.utlis

import com.tencent.bk.sdk.iam.constants.ExpressionOperationEnum
import com.tencent.bk.sdk.iam.dto.action.ActionPolicyDTO
import com.tencent.bk.sdk.iam.dto.expression.ExpressionDTO

object AuthUtils {

    fun getProjects(content: ExpressionDTO) : List<String> {
        if (content.field != "project.id") {
            return emptyList()
        }
        val projectList = mutableListOf<String>()
        when (content.operator) {
            ExpressionOperationEnum.ANY -> projectList.add("*")
            ExpressionOperationEnum.EQUAL -> projectList.add(content.value.toString())
            ExpressionOperationEnum.IN -> projectList.addAll(StringUtils.obj2List(content.value.toString()))
        }
        return projectList
    }
}