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

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker
 *    32: remotedev）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）remotedev
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object ProjectMessageCode {
    const val PROJECT_NOT_EXIST = "2119001" // 项目不存在
    const val PROJECT_NAME_EXIST = "2119002" // 项目名或英文名重复
    const val NAME_EMPTY = "2119003" // 名称不能为空
    const val NAME_TOO_LONG = "2119004" // 项目名至多1-64个字符
    const val EN_NAME_INTERVAL_ERROR = "2119005" // 英文名长度在3-64个字符
    const val EN_NAME_COMBINATION_ERROR = "2119006" // 英文名是字符+数字组成，并以小写字母开头
    const val EN_NAME_EXIST = "2119007" // 英文名已经存在
    const val PEM_CREATE_FAIL = "2119008" // 权限中心创建项目失败
    const val UPDATE_LOGO_FAIL = "2119009" // 更新项目logo失败
    const val QUERY_PROJECT_FAIL = "2119010" // 查询不到有效的项目
    const val SAVE_LOGO_FAIL = "2119011" // 保存项目logo失败
    const val QUERY_DEPARTMENT_FAIL = "2119012" // 获取部门信息失败
    const val QUERY_CC_NAME_FAIL = "2119013" // 获取CC APP Name失败
    const val QUERY_SUB_DEPARTMENT_FAIL = "2119014" // 获取子部门信息失败
    const val QUERY_USER_INFO_FAIL = "2119015" // 获取用户信息失败
    const val QUERY_ORG_FAIL = "2119016" // 获取公司组织架构信息失败
    const val QUERY_PAR_DEPARTMENT_FAIL = "2119017" // 获取父部门信息失败
    const val LABLE_EXIST = "2119018" // label已经存在，请换一个再试
    const val PEM_CREATE_ID_INVALID = "2119019" // 权限中心创建的项目ID无效
    const val PEM_CHECK_FAIL = "2119020" // 没有该项目的操作权限
    const val PEM_QUERY_ERROR = "2119021" // 从权限中心获取用户的项目信息失败
    const val ID_INVALID = "2119022" // ID无效获取失败
    const val COLLECTION_SUCC = "2119023" // 服务收藏成功
    const val COLLECTION_CANCEL_SUCC = "2119024" // 服务取消收藏成功
    const val SERVICE_ADD_FAIL = "2119025" // 服务添加失败
    const val LABLE_NAME_EXSIT = "2119026" // {0}已经存在，请换一个再试
    const val CALL_PEM_FAIL = "2119027" // 调用权限中心创建项目失败
    const val CALL_PEM_FAIL_PARM = "2119028" // 调用权限中心创建项目失败:{0}
    const val NOT_MANAGER = "2119029" // 项目管理：操作用户{0}非项目{1}管理员
    const val ORG_TYPE_ERROR = "2119030" // 组织类型有误
    const val ORG_NOT_PROJECT = "2119031" // {0}该组织下无项目
    const val USER_NOT_PROJECT_USER = "2119032" // 目标用户非该项目成员
    const val USER_NOT_CREATE_PERM = "2119033" // 无创建项目权限，请申请权限
    const val PROJECT_ASSIGN_DATASOURCE_FAIL = "2119034" // 无法为项目分配可用的数据源
    const val FAILED_CREATE_PROJECT_V0 = "2119035" // 调用权限中心V0创建项目失败
    const val PROJECT_ID_INVALID_V0 = "2119036" // 权限中心V0创建的项目ID无效
    const val ASSOCIATED_SYSTEM_NOT_BOUND = "2119037" // 关联系统未绑定
    const val NUMBER_AUTHORIZED_USERS_EXCEEDS_LIMIT = "2119038" // 授权用户数越界
    const val FAILED_SYNCHRONIZE_PROJECT = "2119039" // 同步项目到BCS失败
    const val FAILED_UPDATE_PROJECT_INFORMATION = "2119040" // 更新bcs的项目信息失败
    const val FAILED_UPDATE_LOGO_INFORMATION = "2119041" // 更新bcs的项目LOGO信息失败
    const val USER_RESIGNED = "2119042" // 用户{0} 已离职
    const val FAILED_USER_INFORMATION = "2119043" // 获取用户{0} 信息失败
    const val BOUND_IAM_GRADIENT_ADMIN = "2119044" // 已绑定IAM分级管理员
    const val RELATED_RESOURCE_EMPTY = "2119045" // 权限系统：绑定系统资源为空
    const val ERROR_AUTH_CALLBACK_METHOD_NOT_SUPPORT = "2119046" // iam回调方法{0}不支持
    const val PROJECT_UPDATE_FAIL = "2119047" // 修改项目错误
    const val CANCEL_CREATION_PROJECT_FAIL = "2119048" // 取消创建中的项目失败
    const val APPROVAL_PROJECT_CANT_UPDATE = "2119049" // 审批下的下项目{0}不能修改
    const val UNDER_APPROVAL_PROJECT = "2119050" // 项目{0}审批中，请耐心等待，或联系审批人处理

    const val BK_CONTAINER_SERVICE = "bkContainerService" // 容器服务
    const val BK_FAILED_BSC_CREATE_PROJECT = "bkFailedBscCreateProject" // 调用BSC接口创建项目失败
    const val BK_FAILED_GET_PAASCC_INFORMATION = "bkFailedGetPaasccInformation" // 获取PAASCC项目信息失败

    const val BK_AUTH_CENTER_CREATE_PROJECT_INFO = "bkAuthCenterCreateProjectInfo" // 权限中心创建项目信息：

    const val T_SERVICE_PREFIX = "service."
    const val T_SERVICE_TYPE_PREFIX = "serviceType."
    const val T_ACTIVITY_PREFIX = "activity."
}
