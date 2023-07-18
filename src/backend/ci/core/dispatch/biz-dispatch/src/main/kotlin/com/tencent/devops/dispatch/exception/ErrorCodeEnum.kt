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

package com.tencent.devops.dispatch.exception

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

@Suppress("ALL")
enum class ErrorCodeEnum(
    @BkFieldI18n
    val errorType: ErrorType,
    val errorCode: Int,
    val formatErrorMessage: String
) {
    SYSTEM_ERROR(ErrorType.SYSTEM, 2103001, "Dispatcher系统错误"),
    START_VM_FAIL(ErrorType.SYSTEM, 2103002, "Fail to start up after 3 retries"),
    VM_STATUS_ERROR(ErrorType.USER, 2103003, "第三方构建机状态异常/Bad build agent status"),
    GET_BUILD_AGENT_ERROR(
        ErrorType.SYSTEM,
        2103004,
        "获取第三方构建机失败/Fail to get build agent"
    ),
    FOUND_AGENT_ERROR(
        ErrorType.SYSTEM,
        2103005,
        "获取第三方构建机失败/Can not found agent by type"
    ),
    LOAD_BUILD_AGENT_FAIL(ErrorType.USER, 2103006, "获取第三方构建机失败/Load build agent fail"),
    VM_NODE_NULL(ErrorType.USER, 2103007, "第三方构建机环境的节点为空"),
    GET_VM_ENV_ERROR(ErrorType.USER, 2103008, "获取第三方构建机环境失败"),
    GET_VM_ERROR(ErrorType.USER, 2103009, "获取第三方构建机失败"),
    JOB_QUOTA_EXCESS(ErrorType.USER, 2103010, "JOB配额超限"),
    CONSTANT_AGENTS_UPGRADING_OR_TIMED_OUT(
        ErrorType.SYSTEM,
        2103011,
        "第三方构建机Agent正在升级中或排队重试超时，请检查agent（{0}）并发任务数设置并稍后重试."
        ),
    THIRD_PARTY_BUILD_MACHINE_STATUS_ERROR(
        ErrorType.USER,
        2103012,
        "第三方构建机状态异常，请在环境管理中检查第三方构建机状态"
    ),
    BUILD_MACHINE_UPGRADE_IN_PROGRESS(ErrorType.SYSTEM, 2103013, "构建机升级中，重新调度"),
    BUILD_MACHINE_BUSY(ErrorType.SYSTEM, 2103014, "构建机正忙,重新调度"),
    BUILD_ENV_PREPARATION(ErrorType.SYSTEM, 2103015, "构建环境准备中..."),
    JOB_NUM_REACHED_MAX_QUOTA(
        ErrorType.USER,
        2103016,
        "当前项目下正在执行的【{0}】JOB数量已经达到配额最大值，正在执行JOB数量：{1}, 配额: {2}"
    ),
    JOB_NUM_EXCEED_ALARM_THRESHOLD(
        ErrorType.USER,
        2103017,
        "当前项目下正在执行的【{0}】JOB数量已经超过告警阈值，正在执行JOB数量：{1}，配额：{2}，" +
                "告警阈值：{3}%，当前已经使用：{4}%"
    ),
    BUILD_NODE_IS_EMPTY(
        ErrorType.USER,
        2103018,
        "构建机环境（{0}）的节点为空，请检查环境管理配置，构建集群："
    ),
    NO_CONTAINER_IS_READY_DEBUG(
        ErrorType.USER,
        2103019,
        "pipeline({0})没有可用的容器进行登录调试"
    ),
    DEBUG_CONTAINER_URL_ERROR(
        ErrorType.USER,
        2103020,
        "获取登录调试容器链接失败 ({0})"
    );

    fun getErrorMessage(params: Array<String>? = null, language: String? = null): String {
        return I18nUtil.getCodeLanMessage(
            messageCode = "${this.errorCode}",
            params = params,
            language = language
        )
    }
}
