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

package com.tencent.devops.stream.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */

object StreamMessageCode {
    // 从2129101起
    const val BRANCH_INFO_ACCESS_DENIED = "2129101" // 无权限获取分支信息
    const val PIPELINE_NOT_FOUND_OR_DELETED = "2129102" // 该流水线不存在或已删除，如有疑问请联系蓝盾助手
    const val BUILD_TASK_NOT_FOUND_UNRETRYABLE = "2129103" // 构建任务不存在，无法重试
    const val USER_NOT_AUTHORIZED = "2129104" // 用户[{0}]尚未进行OAUTH授权，请先授权。
    const val STARTUP_CONFIG_MISSING = "2129105" // 启动配置缺少 {0}
    const val CI_START_USER_NO_CURRENT_PROJECT_EXECUTE_PERMISSIONS = "2129106" // ci开启人{0} 无当前项目执行权限, 请重新授权
    const val CROSS_PROJECT_REFERENCE_THIRD_PARTY_BUILD_POOL_ERROR = "2129107" // 跨项目引用第三方构建资源池错误: 获取远程仓库({0})信息失败, 请检查填写是否正确
    const val TIMER_PARAM_TOO_LONG = "2129108" // 添加流水线的定时触发器保存失败！可能是定时器参数过长！
    const val PARAM_INCORRECT = "2129109" // 蓝盾项目ID {0} 不正确
    const val PROJECT_NOT_OPEN_STREAM = "2129110" // 项目未开启Stream，无法查询
    const val PROJECT_ALREADY_EXISTS = "2129111" //项目已存在
    const val PROJECT_NOT_EXIST = "2129112" //项目不存在
    const val NOT_AUTHORIZED_BY_OAUTH = "2129104" //用户[{0}]尚未进行OAUTH授权，请先授权。
    const val PROJECT_STREAM_NOT_ENABLED = "2129114" //工蜂项目{0}未开启Stream
    const val NO_RECORD_MIRROR_VERSION = "2129115" //没有此镜像版本记录
    const val MIRROR_VERSION_NOT_AVAILABLE = "2129116" //镜像版本不可用
    const val VARIABLE_NAME = "2129117" //变量名称必须是英文字母、数字或下划线(_)
    const val MUST_HAVE_ONE = "2129118" //stages, jobs, steps, extends 必须存在一个
    const val STARTUP_CONFIGURATION_MISSING = "2129119" //启动配置缺少 rtx.v2GitUrl
    const val GIT_CI_NO_RECOR = "2129120" //Git CI没有此镜像版本记录
    const val PROJECT_CANNOT_OPEN_STREAM = "2129121" //项目无法开启Stream，请联系蓝盾助手
    const val PROJECT_CANNOT_QUERIED = "2129110" //项目未开启Stream，无法查询
    const val PIPELINE_NOT_EXIST_OR_DELETED = "2129123" //流水线不存在或已删除，如有疑问请联系蓝盾助手
    const val BUILD_TASK_NOT_EXIST = "2129103" //构建任务不存在，无法重试
    const val USER_NOT_PERMISSION_FOR_WORKER_BEE = "2129125" //用户没有工蜂项目权限，无法获取下载链接
    const val INCORRECT_ID_BLUE_SHIELD_PROJECT = "2129126" //蓝盾项目ID不正确

    const val BK_FAILED_VERIFY_AUTHORITY = "bkFailedVerifyAuthority" //授权人权限校验失败
    const val BK_STREAM_MESSAGE_NOTIFICATION = "bkStreamMessageNotification" //@Stream消息通知
    const val BK_NEED_SUPPLEMEN = "bkNeedSupplemen" //对接其他Git平台时需要补充
    const val BK_PULL_CODE = "bkPullCode" //拉代码
}
