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

package com.tencent.devops.environment.constant

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
object EnvironmentMessageCode {
    const val ERROR_ENV_NOT_EXISTS = "2105000" // 环境管理：[{0}] 环境不存在
    const val ERROR_NODE_INSUFFICIENT_PERMISSIONS = "2105001" // 环境管理：环境权限不足 [{0}]
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
    const val ERROR_NODE_AGENT_STATUS_EXCEPTION = "2105018" // 环境管理：构建机状态异常
    const val ERROR_NODE_AGENT_SECRET_KEY_INVALID = "2105019" // 环境管理：构建机密钥不对
    const val ERROR_ENV_EXPIRED_DAYS = "2105020" // 环境管理：有效期不能超过[{0}]天
    const val ERROR_ENV_BUILD_CAN_NOT_ADD_SVR = "2105021" // 服务器节点[{0}]不能添加到构建环境
    const val ERROR_ENV_DEPLOY_CAN_NOT_ADD_AGENT = "2105022" // 构建节点[{0}]不能添加到非构建环境
    const val ERROR_NODE_CHANGE_USER_NOT_SUPPORT = "2105023" // 节点类型【{0}】不支持修改导入人
    const val ERROR_NODE_IMPORT_EXCEED = "2105024" // 环境管理：导入节点数不能超过配额[{0}]
    const val ERROR_NODE_IP_ILLEGAL_USER = "2105025" // 环境管理：非法 IP [{0}], 请确认是否是服务器的责任人
    const val ERROR_QUOTA_LIMIT = "2105026" // 环境管理：配额不足，总量{0}, 已使用: {1}
    const val ERROR_VM_CAN_NOT_DESTROY = "2105027" // 环境管理：虚拟机状态为:{0}, 不允许销毁！请稍后操作！
    const val ERROR_VM_CAN_NOT_IMAGED = "2105028" // 环境管理：虚拟机状态为:{0}, 无法制作镜像!
    const val ERROR_NODE_HAD_BEEN_ASSIGN = "2105029" // 环境管理：节点已被分配，不能重新分配
    const val ERROR_ENV_BCS_NOT_ACTIVED = "2105030" // 环境管理：项目[{0}]没有开通过BCS虚拟机功能
    const val ERROR_NODE_INFLUX_QUERY_HOST_INFO_FAIL = "2105031" // 环境管理：查询构建机主机信息失败: {0}
    const val ERROR_NODE_INFLUX_QUERY_CPU_INFO_FAIL = "2105032" // 环境管理：查询构建机CPU信息失败: {0}
    const val ERROR_NODE_INFLUX_QUERY_MEM_INFO_FAIL = "2105033" // 环境管理：查询构建机内存信息失败: {0}
    const val ERROR_NODE_INFLUX_QUERY_DISK_INFO_FAIL = "2105034" // 环境管理：查询构建机磁盘信息失败: {0}
    const val ERROR_NODE_INFLUX_QUERY_NET_INFO_FAIL = "2105035" // 环境管理：查询构建机网络信息失败: {0}
    const val ERROR_NODE_SHARE_PROJECT_EMPTY = "2105036" // 环境管理：共享的项目列表为空
    const val ERROR_NODE_SHARE_PROJECT_TYPE_ERROR = "2105037" // 环境管理：仅构建环境支持共享
    const val ERROR_NODE_NAME_INVALID_CHARACTER = "2105038" // 环境管理：环境名称包含非法字符@
    const val ERROR_NODE_NAME_OR_ID_INVALID = "2105039" // 环境管理：获取节点失败，请求节点hash id或别名有误
    const val ERROR_NOT_THIRD_PARTY_BUILD_MACHINE = "2105040" // 环境管理：这个节点不是第三方构建机
    const val THIRD_PARTY_BUILD_ENVIRONMENT_NOT_EXIST = "2105041" // 第三方构建机环境不存在
    const val ERROR_NO_PERMISSION_TO_USE_THIRD_PARTY_BUILD_ENV = "2105042" // 无权限使用第三方构建机环境
    const val ERROR_THIRD_PARTY_BUILD_ENV_NODE_NOT_EXIST = "2105043" // 第三方构建机环境节点不存在
    const val ERROR_PIPE_NOT_FOUND = "2105044" // 环境管理：不存在该管道信息
    const val ERROR_NODE_NO_USE_PERMISSSION = "2105045" // 环境管理：没有节点使用权限

    const val BK_NORMAL_VERSION = "bkNormalVersion" // 8核16G（普通版）
    const val BK_INTEL_XEON_SKYLAKE_PROCESSOR = "bkIntelXeonSkylakeProcessor" // 2.5GHz 64核 Intel Xeon Skylake 6133处理器
    const val BK_MEMORY = "bkMemory" // 32GB*12 DDR3 内存
    const val BK_SOLID_STATE_DISK = "bkSolidStateDisk" // {0}GB 固态硬盘
    const val BK_ESTIMATED_DELIVERY_TIME = "bkEstimatedDeliveryTime" // 预计交付周期：{0}分钟
    const val BK_HIGH_END_VERSION = "bkHighEndVersion" // 32核64G（高配版）
}
