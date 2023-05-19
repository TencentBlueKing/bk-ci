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

package com.tencent.devops.dispatch.kubernetes.pojo.common

import com.tencent.devops.common.api.annotation.BkFieldI18n
import com.tencent.devops.common.api.pojo.ErrorType
import com.tencent.devops.common.web.utils.I18nUtil

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

enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    BCS_SYSTEM_ERROR(ErrorType.SYSTEM, 2126001, "Dispatcher-bcs系统错误"),
    BCS_CREATE_VM_ERROR(
        ErrorType.THIRD_PARTY, 2126002, "第三方服务-BCS 异常，异常信息 - 构建机创建失败"
    ),
    BCS_CREATE_VM_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126003, "第三方服务-BCS 异常，异常信息 - 创建构建机接口异常"
    ),
    BCS_CREATE_VM_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY, 2126004, "第三方服务-BCS 异常，异常信息 - 创建构建机接口返回失败"
    ),
    BCS_OPERATE_VM_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126005, "第三方服务-BCS 异常，异常信息 - 操作构建机接口异常"
    ),
    BCS_OPERATE_VM_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY, 2126006, "第三方服务-BCS 异常，异常信息 - 操作构建机接口返回失败"
    ),
    BCS_VM_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126007, "第三方服务-BCS 异常，异常信息 - 获取构建机详情接口异常"
    ),
    BCS_CREATE_IMAGE_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126008, "第三方服务-BCS 异常，异常信息 - 创建镜像接口异常"
    ),
    BCS_TASK_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126009, "第三方服务-BCS 异常，异常信息 - 获取TASK状态接口异常"
    ),
    BCS_WEBSOCKET_URL_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY, 2126010, "第三方服务-BCS 异常，异常信息 - 获取websocket接口异常"
    ),
    BASE_SYSTEM_ERROR(
        ErrorType.SYSTEM,
        2126011,
        "Dispatcher-base系统错误"
    ),
    BASE_NO_IDLE_VM_ERROR(
        ErrorType.SYSTEM,
        2126012,
        "构建机启动失败，没有空闲的构建机"
    ),
    BASE_START_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126013,
        "第三方服务异常，异常信息 - 构建机启动失败"
    ),
    BASE_CREATE_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126014,
        "第三方服务异常，异常信息 - 构建机创建失败"
    ),
    BASE_STOP_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126015,
        "第三方服务异常，异常信息 - 构建机休眠失败"
    ),
    BASE_DELETE_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126016,
        "第三方服务异常，异常信息 - 构建机销毁失败"
    ),
    BASE_INTERFACE_TIMEOUT(
        ErrorType.THIRD_PARTY,
        2126017,
        "第三方服务异常，异常信息 - 接口请求超时"
    ),
    BASE_CREATE_JOB_LIMIT_ERROR(
        ErrorType.USER,
        2126018,
        "已超过dispatch base创建Job容器上限."
    ),
    KUBERNETES_SYSTEM_ERROR(
        ErrorType.SYSTEM,
        2126019,
        "Dispatcher-kubernetes系统错误"
    ),
    KUBERNETES_NO_IDLE_VM_ERROR(
        ErrorType.SYSTEM,
        2126020,
        "Dispatcher-kubernetes 构建机启动失败，没有空闲的构建机"
    ),
    KUBERNETES_CREATE_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126021,
        "Dispatcher-kubernetes 异常，异常信息 - 构建机创建失败"
    ),
    KUBERNETES_START_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126022,
        "Dispatcher-kubernetes 异常，异常信息 - 构建机启动失败"
    ),
    KUBERNETES_CREATE_VM_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126023,
        "Dispatcher-kubernetes 异常，异常信息 - 创建容器接口异常"
    ),
    KUBERNETES_CREATE_VM_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126024,
        "Dispatcher-kubernetes 异常，异常信息 - 创建容器接口返回失败"
    ),
    KUBERNETES_OPERATE_VM_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126025,
        "Dispatcher-kubernetes 异常，异常信息 - 操作容器接口异常"
    ),
    KUBERNETES_OPERATE_VM_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126026,
        "Dispatcher-kubernetes 异常，异常信息 - 操作容器接口返回失败"
    ),
    KUBERNETES_VM_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126027,
        "Dispatcher-kubernetes 异常，异常信息 - 获取容器状态接口异常"
    ),
    KUBERNETES_CREATE_IMAGE_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126028,
        "Dispatcher-kubernetes 异常，异常信息 - 创建镜像接口异常"
    ),
    KUBERNETES_CREATE_IMAGE_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126029,
        "Dispatcher-kubernetes 异常，异常信息 - 创建镜像接口返回失败"
    ),
    KUBERNETES_CREATE_IMAGE_VERSION_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126030,
        "Dispatcher-kubernetes 异常，异常信息 - 创建镜像新版本接口异常"
    ),
    KUBERNETES_CREATE_IMAGE_VERSION_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126031,
        "Dispatcher-kubernetes 异常，异常信息 - 创建镜像新版本接口返回失败"
    ),
    KUBERNETES_TASK_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126032,
        "Dispatcher-kubernetes 异常，异常信息 - 获取TASK状态接口异常"
    ),
    KUBERNETES_WEBSOCKET_URL_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126033,
        "Dispatcher-kubernetes 异常，异常信息 - 获取websocket接口异常"
    ),
    KUBERNETES_WEBSOCKET_NO_GATEWAY_PROXY(
        ErrorType.SYSTEM,
        2126034,
        "请检查webConsole网关代理配置"
    ),
    DEVCLOUD_SYSTEM_ERROR(
        ErrorType.SYSTEM,
        2126035,
        "Dispatcher-devcloud系统错误"
    ),
    DEVCLOUD_NO_IDLE_VM_ERROR(
        ErrorType.SYSTEM,
        2126036,
        "DEVCLOUD构建机启动失败，没有空闲的构建机"
    ),
    DEVCLOUD_CREATE_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126037,
        "第三方服务-DEVCLOUD 异常，异常信息 - 构建机创建失败"
    ),
    DEVCLOUD_START_VM_ERROR(
        ErrorType.THIRD_PARTY,
        2126038,
        "第三方服务-DEVCLOUD 异常，异常信息 - 构建机启动失败"
    ),
    DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126039,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口异常"
    ),
    DEVCLOUD_CREATE_ENVIRONMENT_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126040,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建环境接口返回失败"
    ),
    DEVCLOUD_OP_ENVIRONMENT_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126041,
        "第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口异常"
    ),
    DEVCLOUD_OP_ENVIRONMENT_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126042,
        "第三方服务-DEVCLOUD 异常，异常信息 - 操作环境接口返回失败"
    ),
    DEVCLOUD_ENVIRONMENT_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126043,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取环境状态接口异常"
    ),
    DEVCLOUD_ENVIRONMENT_LIST_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126044,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取环境列表接口异常"
    ),
    DEVCLOUD_CREATE_IMAGE_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126045,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像接口返回失败"
    ),
    DEVCLOUD_CREATE_IMAGE_VERSION_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126046,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口异常"
    ),
    DEVCLOUD_CREATE_IMAGE_VERSION_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126047,
        "第三方服务-DEVCLOUD 异常，异常信息 - 创建镜像新版本接口返回失败"
    ),
    DEVCLOUD_TASK_STATUS_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126048,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取TASK状态接口异常"
    ),
    DEVCLOUD_WEBSOCKET_URL_INTERFACE_ERROR(
        ErrorType.THIRD_PARTY,
        2126049,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口异常"
    ),
    DEVCLOUD_WEBSOCKET_URL_INTERFACE_FAIL(
        ErrorType.THIRD_PARTY,
        2126050,
        "第三方服务-DEVCLOUD 异常，异常信息 - 获取websocket接口返回失败"
    ),
    DEVCLOUD_RETRY_STATUS_FAIL(
        ErrorType.USER,
        2126051,
        "重试频率过快，请稍后重试"
    ),
    DEVCLOUD_DEVCLOUD_INTERFACE_TIMEOUT(
        ErrorType.THIRD_PARTY,
        2126052,
        "第三方服务-DEVCLOUD 异常，异常信息 - 接口请求超时"
    ),
    DEVCLOUD_CREATE_VM_USER_ERROR(
        ErrorType.USER,
        2126053,
        "第三方服务-DEVCLOUD 异常，异常信息 - 用户操作异常"
    ),
    DEVCLOUD_CREATE_JOB_LIMIT_ERROR(
        ErrorType.USER,
        2126054,
        "已超过DevCloud创建Job环境上限."
    );

    fun getErrorMessage(): String {
        return I18nUtil.getCodeLanMessage("${this.errorCode}")
    }
}
