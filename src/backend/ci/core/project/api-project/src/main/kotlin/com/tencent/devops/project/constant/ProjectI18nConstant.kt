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

package com.tencent.devops.project.constant

    const val BK_AUTH_CENTER_CREATE_PROJECT_INFO = "BkAuthCenterCreateProjectInfo"// 权限中心创建项目信息：
    const val BK_BOUND_IAM_GRADIENT_ADMIN = "BkBoundIamGradientAdmin"// 已绑定IAM分级管理员
    const val BK_INDEX_ENGLISH_NAME_CANNOT_EMPTY = "BkIndexEnglishNameCannotEmpty"// 指标英文名不能为空
    const val BK_USER_NO_USER_GROUP_EDIT_PERMISSION = "BkUserNoGroupEditPermission"// 用户没有用户组的编辑权限
    const val BK_PROJECT_QUALITY_RULE = "BkProjectQualityRule"// 项目下红线规则
    const val BK_PROJECT_UNDER_NO_QUALITY_USER_GROUP = "BkProjectUnderNoQualityUserGroup"// 项目下无红线用户组
    const val BK_PROJECT_QUALITY_GROUPING= "BkProjectQualityGrouping"// 项目下红线分组
    const val BK_PASSED = "BkPassed"// 已通过：
    const val BK_BLOCKED= "BkBlocked"// 已拦截：
    const val BK_CURRENT_VALUE = "BkCurrentValue"// {0}当前值({1})，期望${2}\n
    const val BK_VALIDATION_PASSED = "BkValidationPassed"// {0}(#{1})在{2}验证通过
    const val BK_VALIDATION_INTERCEPTED = "BkValidationIntercepted"// {0}(#{1})在{2}验证被拦截
    const val BK_CHANGE_QUALITY_GATE_VALUE = "BkChangeQualityGateValue"// 指标[{0}]值类型为[{1}]，请修改红线阈值[{2}]

