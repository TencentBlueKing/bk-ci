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

package com.tencent.devops.worker.common.constants

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-环境 06：experience-版本体验 07：image-镜像 08：log-日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-商店 21： auth-权限 22:sign-签名服务 23:metrics-度量服务 24：external-外部
 *    25：prebuild-预建 26:dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */

object WorkerMessageCode {

    const val REPORT_AGENT_END_STATUS_FAILURE = "2130001"// Quota上报agent运行结束状态失败
    const val REPORT_AGENT_START_STATUS_FAILURE = "2130002"// Quota上报agent开始运行失败
    const val GET_REPORT_ROOT_PATH_FAILURE = "2130003"// 获取报告根路径失败
    const val CREATE_REPORT_FAIL = "2130004"// 创建报告失败
    const val UPLOAD_CUSTOM_REPORT_FAILURE = "2130005"// 上传自定义报告失败
    const val REPORT_START_ERROR_INFO_FAIL = "2130006"// 上报启动异常信息失败
    const val SCRIPT_EXECUTION_FAIL = "2130007"// 脚本执行失败， 归档{0}文件
    const val ENV_VARIABLE_PATH_NOT_EXIST = "2130008"// 环境变量路径({0})不存在
    const val UNDEFINED_VARIABLE = "2130009"// 工作空间未定义变量:
    const val ILLEGAL_WORKSPACE = "2130010"// 无法创建工作空间:
    const val UNBEKNOWN_BUILD_TYPE = "2130011"// 未知的BuildType类型:

    // dockerHost-docker机器
    const val UPDATE_IMAGE_MARKET_INFO_FAILED = "2130101"// 更新镜像市场信息失败

    // log-日志
    const val LOGS_END_STATUS_FAILED = "2130151"// 上报结束状态失败
    const val LOGS_REPORT_FAILED = "2130152"// 上报日志失败
    const val LOG_STORAGE_STATUS_FAILED = "2130153"// 上报日志存储状态失败


    // scm-软件配置管理
    const val ADD_CODE_BASE_COMMIT_INFO_FAIL = "2130201"// 添加代码库commit信息失败
    const val GET_LAST_CODE_BASE_COMMIT_INFO_FAIL = "2130202"// 获取最后一次代码commit信息失败
    const val ADD_SOURCE_MATERIAL_INFO_FAILURE = "2130203"// 添加源材料信息失败
    const val GET_OAUTH_INFO_FAIL = "2130204"// 获取oauth认证信息失败
    const val GET_CODE_BASE_FAIL = "2130205"// 获取代码库失败
    const val GIT_CREDENTIAL_ILLEGAL = "2130206"// git凭据不合法
    const val CODE_REPO_PARAM_NOT_IN_PARAMS = "2130207"// 代码仓库{0}没存在参数中

    // dispatch-分发
    const val DOWNLOAD_CODECC_TOOL_FAIL = "2130251"// 下载Codecc的 {0} 工具失败
    const val DOWNLOAD_CODECC_COVERITY_SCRIPT_FAIL = "2130252"// 下载codecc的coverity的执行脚本失败
    const val DOWNLOAD_CODECC_MULTI_TOOL_SCRIPT_FAIL = "2130253"// 下载codecc的多工具执行脚本失败


    // process-流水线
    const val NOTIFY_SERVER_START_BUILD_FAILED = "2130301"// 通知服务端启动构建失败
    const val RECEIVE_BUILD_MACHINE_TASK_FAILED = "2130302"// 领取构建机任务失败
    const val RECEIVE_BUILD_MACHINE_TASK_DETAIL_FAILED = "2130303"// 领取构建机任务详情失败
    const val BUILD_FINISH_REQUEST_FAILED = "2130304" // 构建完成请求失败
    const val REPORT_TASK_FINISH_FAILURE = "2130305"// 报告任务完成失败
    const val HEARTBEAT_FAIL = "2130306"// 心跳失败
    const val GET_BUILD_TASK_DETAILS_FAILURE = "2130307"// 获取构建任务详情失败
    const val BUILD_TIMEOUT_END_REQUEST_FAILURE = "2130308"// 构建超时结束请求失败
    const val GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE = "2130309"// 获取模板跨项目信息失败
    const val ATOM_EXECUTION_TIMEOUT = "2130309"// 插件执行超时, 超时时间:{0}分钟

    // quality-质量红线
    const val SAVE_SCRIPT_METADATA_FAILURE = "2130351"// 保存脚本元数据失败

    // ticket-证书凭据
    const val GET_CREDENTIAL_FAILED = "2130401"// 获取凭证失败
    const val CREDENTIAL_ID_NOT_EXIST = "2130402"// 凭证ID变量({0})不存在
    // artifactory-版本仓库
    const val START_BUILD_IMAGE_NAME = "2130402"// 启动构建镜像，镜像名称：
    const val WAIT_BUILD_IMAGE_FINISH = "2130403"// 启动构建镜像成功，等待构建镜像结束，镜像名称：
    const val BUILD_IMAGE_FAIL_DETAIL = "2130404"// 构建镜像失败，错误详情：
    const val DOCKERFILE_FIRST_LINE_CHECK = "2130405"// Dockerfile第一行请确认使用 {0}
    const val START_BUILD_IMAGE_FAIL = "2130406"// 启动构建镜像失败！请联系【蓝盾助手】
    const val QUERY_BUILD_IMAGE_STATUS_FAIL = "2130407"// 查询构建镜像状态失败！请联系【蓝盾助手】
    const val START_BUILD_FAIL = "2130408"// 启动构建失败！请联系【蓝盾助手】
    const val FOLDER_NOT_EXIST = "2130409"// 文件夹{0}不存在
    const val ENTRANCE_FILE_NOT_IN_FOLDER = "2130410"// 入口文件({0})不在文件夹({1})下
    const val ENTRANCE_FILE_CHECK_FINISH = "2130411"// 入口文件检测完成
    const val UPLOAD_CUSTOM_OUTPUT_SUCCESS = "2130412"// 上传自定义产出物成功，共产生了{0}个文件
    const val NO_MATCHING_ARCHIVE_FILE = "2130413"// 没有匹配到任何待归档文件，请检查工作空间下面的文件
    const val ARCHIVE_FILE_LIMIT = "2130413"// 单次归档文件数太多，请打包后再归档！

    const val BK_BUILD_IMAGE_SUCCEED = "bkBuildImageSucceed"// 构建镜像成功！
    const val BK_ARCHIVE_PLUGIN_FILE= "bkArchivePluginFile"// 归档插件文件
    const val BK_NO_FILES_TO_ARCHIVE = "bkNoFilesToArchive"// 脚本执行失败之后没有匹配到任何待归档文件
    const val BK_COMMAND_LINE_RETURN_VALUE_NON_ZERO = "bkCommandLineReturnValueNonZero"// 每行命令运行返回值非零时，继续执行脚本


}