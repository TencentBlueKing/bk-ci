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

package com.tencent.devops.environment.constant

/**
 * 环境管理微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表蓝盾平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-蓝盾环境 06：experience-版本体验 07：image-镜像 08：log-蓝盾日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-蓝盾支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 * @since: 2019-03-05
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
object EnvironmentMessageCode {
    const val ERROR_NODE_INSUFFICIENT_PERMISSIONS = "2105001" // 环境管理：节点权限不足 [{0}]
    const val ERROR_NODE_NOT_EXISTS = "2105002" // 环境管理：[{0}] 节点不存在
    const val ERROR_NODE_NAME_DUPLICATE = "2105003" // 环境管理：环境名称已存在: [{0}]
    const val ERROR_ENV_BUILD_2_DEPLOY_DENY = "2105004" // 环境管理：构建环境不能修改为部署环境
    const val ERROR_ENV_DEPLOY_2_BUILD_DENY = "2105005" // 环境管理：部署环境不能修改为构建环境
    const val ERROR_ENV_NO_CREATE_PERMISSSION = "2105006" // 环境管理：没有环境创建权限
    const val ERROR_ENV_NO_EDIT_PERMISSSION = "2105007" // 环境管理：没有环境编辑权限
    const val ERROR_ENV_NO_VIEW_PERMISSSION = "2105008" // 环境管理：没有环境查看权限
    const val ERROR_ENV_NO_DEL_PERMISSSION = "2105009" // 环境管理：没有环境删除权限
    const val ERROR_ENV_ID_NULL = "2105010" // 环境管理：环境ID不能为空
    const val ERROR_ENV_NAME_NULL = "2105011" // 环境管理：环境名称不能为空
    const val ERROR_ENV_NAME_TOO_LONG = "2105012" // 环境管理：环境名称太长
    const val ERROR_ENV_NODE_HASH_ID_ILLEGAL = "2105013" // 环境管理：环境下的节点ID不合法
    const val ERROR_NODE_NO_CREATE_PERMISSSION = "2105014" // 环境管理：没有节点创建权限
    const val ERROR_NODE_NO_EDIT_PERMISSSION = "2105015" // 环境管理：没有节点编辑权限
    const val ERROR_NODE_NO_VIEW_PERMISSSION = "2105016" // 环境管理：没有节点查看权限
    const val ERROR_NODE_NO_DEL_PERMISSSION = "2105017" // 环境管理：没有节点删除权限
    const val ERROR_NODE_NULL_BCSVM_PARAM = "2105018" // 环境管理：Invalid b c s V m Param
    const val ERROR_NODE_INVALID_BCSVM_PARAM = "2105019" // 环境管理：Invalid b c s V m Param {0}
    const val ERROR_ENV_EXPIRED_DAYS = "2105020" // 环境管理：有效期不能超过[{0}]天
}
