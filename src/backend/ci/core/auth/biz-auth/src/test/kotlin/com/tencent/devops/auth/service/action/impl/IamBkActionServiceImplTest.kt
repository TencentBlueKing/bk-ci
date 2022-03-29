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

package com.tencent.devops.auth.service.action.impl

import com.nhaarman.mockito_kotlin.mock
import com.tencent.bk.sdk.iam.constants.ActionTypeEnum
import com.tencent.bk.sdk.iam.dto.resource.ResourceCreatorActionsDTO
import com.tencent.devops.auth.pojo.action.CreateActionDTO
import com.tencent.devops.common.auth.api.AuthResourceType
import org.junit.Assert
import org.junit.Test
import org.junit.jupiter.api.Assertions.*

class IamBkActionServiceImplTest {

    private val iamActionService: IamBkActionServiceImpl = mock()


    @Test
    fun buildCreateRelationTest() {
        val action = CreateActionDTO(
            actionId = "project_test",
            resourceId = AuthResourceType.PROJECT.value,
            actionEnglishName = "项目_测试action",
            actionName = "project_test",
            actionType = ActionTypeEnum.LIST.name,
            desc = "",
            relationAction = arrayOf("project_view").toList()
        )
        val systemCreateRelationInfo: ResourceCreatorActionsDTO? = null

        val relation = iamActionService.buildCreateRelation(action, systemCreateRelationInfo)
        println(relation)
        Assert.assertEquals(relation.config[0].id, "project")
    }
}