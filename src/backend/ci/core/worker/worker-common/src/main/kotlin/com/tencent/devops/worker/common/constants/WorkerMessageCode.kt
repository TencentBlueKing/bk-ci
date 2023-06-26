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
 *    25：prebuild-预建 26: dispatcher-kubernetes 27：buildless 28: lambda 29: stream  30: worker 31: dispatcher-docker
 *    32: remotedev）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）remotedev
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2023-3-20
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */

object WorkerMessageCode {

    const val REPORT_AGENT_END_STATUS_FAILURE = "2130001" // Quota上报agent运行结束状态失败
    const val REPORT_AGENT_START_STATUS_FAILURE = "2130002" // Quota上报agent开始运行失败
    const val GET_REPORT_ROOT_PATH_FAILURE = "2130003" // 获取报告根路径失败
    const val CREATE_REPORT_FAIL = "2130004" // 创建报告失败
    const val UPLOAD_CUSTOM_REPORT_FAILURE = "2130005" // 上传自定义报告失败
    const val REPORT_START_ERROR_INFO_FAIL = "2130006" // 上报启动异常信息失败
    const val SCRIPT_EXECUTION_FAIL = "2130007" // 脚本执行失败， 归档{0}文件
    const val ENV_VARIABLE_PATH_NOT_EXIST = "2130008" // 环境变量路径({0})不存在
    const val UNDEFINED_VARIABLE = "2130009" // 工作空间未定义变量:
    const val ILLEGAL_WORKSPACE = "2130010" // 无法创建工作空间:
    const val UNBEKNOWN_BUILD_TYPE = "2130011" // 未知的BuildType类型:

    // dockerHost-docker机器
    const val UPDATE_IMAGE_MARKET_INFO_FAILED = "2130012" // 更新镜像市场信息失败

    // log-日志
    const val LOGS_END_STATUS_FAILED = "2130013" // 上报结束状态失败
    const val LOGS_REPORT_FAILED = "2130014" // 上报日志失败
    const val LOG_STORAGE_STATUS_FAILED = "2130015" // 上报日志存储状态失败

    // scm-软件配置管理
    const val ADD_CODE_BASE_COMMIT_INFO_FAIL = "2130016" // 添加代码库commit信息失败
    const val GET_LAST_CODE_BASE_COMMIT_INFO_FAIL = "2130017" // 获取最后一次代码commit信息失败
    const val ADD_SOURCE_MATERIAL_INFO_FAILURE = "2130018" // 添加源材料信息失败
    const val GET_OAUTH_INFO_FAIL = "2130019" // 获取oauth认证信息失败
    const val GET_CODE_BASE_FAIL = "2130020" // 获取代码库失败
    const val GIT_CREDENTIAL_ILLEGAL = "2130021" // git凭据不合法
    const val CODE_REPO_PARAM_NOT_IN_PARAMS = "2130022" // 代码仓库{0}没存在参数中

    // dispatch-分发
    const val DOWNLOAD_CODECC_TOOL_FAIL = "2130023" // 下载Codecc的 {0} 工具失败
    const val DOWNLOAD_CODECC_COVERITY_SCRIPT_FAIL = "2130024" // 下载codecc的coverity的执行脚本失败
    const val DOWNLOAD_CODECC_MULTI_TOOL_SCRIPT_FAIL = "2130025" // 下载codecc的多工具执行脚本失败

    // process-流水线
    const val NOTIFY_SERVER_START_BUILD_FAILED = "2130026" // 通知服务端启动构建失败
    const val RECEIVE_BUILD_MACHINE_TASK_FAILED = "2130027" // 领取构建机任务失败
    const val RECEIVE_BUILD_MACHINE_TASK_DETAIL_FAILED = "2130028" // 领取构建机任务详情失败
    const val BUILD_FINISH_REQUEST_FAILED = "2130029" // 构建完成请求失败
    const val REPORT_TASK_FINISH_FAILURE = "2130030" // 报告任务完成失败
    const val HEARTBEAT_FAIL = "2130031" // 心跳失败
    const val GET_BUILD_TASK_DETAILS_FAILURE = "2130032" // 获取构建任务详情失败
    const val BUILD_TIMEOUT_END_REQUEST_FAILURE = "2130033" // 构建超时结束请求失败
    const val GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE = "2130034" // 获取模板跨项目信息失败
    const val ATOM_EXECUTION_TIMEOUT = "2130035" // 插件执行超时, 超时时间:{0}分钟

    // quality-质量红线
    const val SAVE_SCRIPT_METADATA_FAILURE = "2130036" // 保存脚本元数据失败

    // ticket-证书凭据
    const val GET_CREDENTIAL_FAILED = "2130037" // 获取凭证失败
    const val CREDENTIAL_ID_NOT_EXIST = "2130038" // 凭证ID变量({0})不存在
    const val WAIT_BUILD_IMAGE_FINISH = "2130039" // 启动构建镜像成功，等待构建镜像结束，镜像名称：
    const val BUILD_IMAGE_FAIL_DETAIL = "2130040" // 构建镜像失败，错误详情：
    const val DOCKERFILE_FIRST_LINE_CHECK = "2130041" // Dockerfile第一行请确认使用 {0}
    const val START_BUILD_IMAGE_FAIL = "2130042" // 启动构建镜像失败！请联系【蓝盾助手】
    const val QUERY_BUILD_IMAGE_STATUS_FAIL = "2130043" // 查询构建镜像状态失败！请联系【蓝盾助手】
    const val START_BUILD_FAIL = "2130044" // 启动构建失败！请联系【蓝盾助手】
    const val FOLDER_NOT_EXIST = "2130045" // 文件夹{0}不存在
    const val ENTRANCE_FILE_NOT_IN_FOLDER = "2130046" // 入口文件({0})不在文件夹({1})下
    const val ENTRANCE_FILE_CHECK_FINISH = "2130047" // 入口文件检测完成
    const val UPLOAD_CUSTOM_OUTPUT_SUCCESS = "2130048" // 上传自定义产出物成功，共产生了{0}个文件
    const val NO_MATCHING_ARCHIVE_FILE = "2130049" // 没有匹配到任何待归档文件，请检查工作空间下面的文件
    const val ARCHIVE_FILE_LIMIT = "2130050" // 单次归档文件数太多，请打包后再归档！
    const val ARCHIVE_ATOM_FILE_FAIL = "2130051" // 归档插件文件失败
    const val START_BUILD_IMAGE_NAME = "2130052" // 启动构建镜像，镜像名称：
    const val GET_DOWNLOAD_LINK_REQUEST_ERROR = "2130053" // 获取下载链接请求出错
    const val UPLOAD_CUSTOM_FILE_FAILED = "2130054" // 上传自定义文件失败
    const val UPLOAD_PIPELINE_FILE_FAILED = "2130055" // 上传流水线文件失败
    const val GET_BUILD_BASE_INFO_FAIL = "2130056" // 获取构建机基本信息失败
    const val GET_CREDENTIAL_INFO_FAILED = "2130057" // 获取凭证信息失败
    const val UPLOAD_FILE_FAILED = "2130058" // 上传文件失败
    const val ARCHIVE_PLUGIN_FILE_FAILED = "2130059" // 归档插件文件失败
    const val GET_PLUGIN_ENV_INFO_FAILED = "2130060" // 获取插件执行环境信息失败
    const val GET_PLUGIN_SENSITIVE_INFO_FAILED = "2130061" // 获取插件敏感信息失败
    const val UPDATE_PLUGIN_ENV_INFO_FAILED = "2130062" // 更新插件执行环境信息失败
    const val ADD_PLUGIN_PLATFORM_INFO_FAILED = "2130063" // 添加插件对接平台信息失败
    const val GET_PLUGIN_LANGUAGE_ENV_INFO_FAILED = "2130064" // 获取插件开发语言相关的环境变量信息失败
    const val URL_INCORRECT = "2130065" // 外链({0})不是一个正确的URL地址
    const val GET_GIT_HOST_INFO_FAIL = "2130066" // 获取git代码库主机信息失败: {0}
    const val PULL_THE_REPOSITORY_IN_FULL = "2130067" // 从({0})变为({1}), 全量拉取代码仓库
    const val PULL_THE_REPOSITORY_IN_SWITCH = "2130068" // 从({0})变为({1}), switch拉取代码
    const val GET_SVN_DIRECTORY_ERROR = "2130069" // 获取Svn目录错误
    const val PARAMETER_ERROR = "2130070" // 参数错误
    const val RUN_AGENT_WITHOUT_PERMISSION = "2130071" // 运行Agent需要构建机临时目录的写权限
    const val UNKNOWN_ERROR = "2130072" // 未知错误:
    const val AGENT_DNS_ERROR = "2130073" // 构建机DNS解析问题(Agent DNS Error)
    const val AGENT_NETWORK_CONNECT_FAILED = "2130074" // 构建机网络连接问题(Agent Network Connect Failed)
    const val AGENT_NETWORK_TIMEOUT = "2130075" // 构建机网络超时问题(Agent Network Timeout)
    const val AGENT_NETWORK_UNKNOWN = "2130076" // 构建机网络未知异常(Agent Network Unknown)

    const val BK_CERTIFICATE_ID_EMPTY = "bkCertificateIdEmpty" // 证书ID为空
    const val BK_BUILD_IMAGE_SUCCEED = "bkBuildImageSucceed" // 构建镜像成功！
    const val BK_NO_FILES_TO_ARCHIVE = "bkNoFilesToArchive" // 脚本执行失败之后没有匹配到任何待归档文件
    // 每行命令运行返回值非零时，继续执行脚本
    const val BK_COMMAND_LINE_RETURN_VALUE_NON_ZERO = "bkCommandLineReturnValueNonZero"
    const val BK_CANNING_SENSITIVE_INFORMATION = "bkCanningSensitiveInformation" // 开始敏感信息扫描，待排除目录
    const val BK_SENSITIVE_INFORMATION = "bkSensitiveInformation" // 敏感信息扫描报告
    const val BK_NO_SENSITIVE_INFORMATION = "bkNoSensitiveInformation" // 无敏感信息，无需生成报告
    const val BK_RELATIVE_PATH_KEYSTORE = "bkRelativePathKeystore" // keystore安装相对路径
    const val BK_KEYSTORE_INSTALLED_SUCCESSFULLY = "bkKeystoreInstalledSuccessfully" // Keystore安装成功
    const val BK_FAILED_UPLOAD_BUGLY_FILE = "bkFailedUploadBuglyFile" // 上传bugly文件失败
    const val BK_FAILED_GET_BUILDER_INFORMATION = "bkFailedGetBuilderInformation" // 获取构建机基本信息失败
    const val BK_FAILED_GET_WORKER_BEE = "bkFailedGetWorkerBee" // 获取工蜂CI项目Token失败！
    const val BK_FAILED_GET_PLUG = "bkFailedGetPlug" // 获取插件执行环境信息失败
    const val BK_FAILED_UPDATE_PLUG = "bkFailedUpdatePlug" // 更新插件执行环境信息失败
    const val BK_FAILED_SENSITIVE_INFORMATION = "bkFailedSensitiveInformation" // 获取插件敏感信息失败
    // 获取插件开发语言相关的环境变量信息失败
    const val BK_FAILED_ENVIRONMENT_VARIABLE_INFORMATION = "bkFailedEnvironmentVariableInformation"
    const val BK_FAILED_ADD_INFORMATION = "bkFailedAddInformation" // 添加插件对接平台信息失败
    const val BK_ARCHIVE_PLUG_FILES = "bkArchivePlugFiles" // 归档插件文件
    const val BK_FAILED_IOS_CERTIFICATE = "bkFailedIosCertificate" // 获取IOS证书失败
    const val BK_FAILED_ANDROID_CERTIFICATE = "bkFailedAndroidCertificate" // 获取Android证书失败
    const val BK_ENTERPRISE_SIGNATURE_FAILED = "bkEnterpriseSignatureFailed" // 企业签名失败
    const val BK_PLUGIN_IS_NO_LONGER_RECOMMENDED = "bkPluginIsNoLongerRecommended" // 该插件已不推荐使用
    const val BK_WRONG_GIT_SPECIFIES_THE_PULL_METHOD = "bkWrongGitSpecifiesThePullMethod" // 错误的GIT指定拉取方式（{0}）
    const val BK_PREPARE_TO_BUILD = "bkPrepareToBuild" // 构建机已收到请求，准备构建(Build[{0}] Job#{1} is ready）
    const val BK_ATOM_HAS_BEEN_REMOVED = "bkAtomHasBeenRemoved" // [警告]该插件已被下架，有可能无法正常工作！
    // [警告]该插件处于下架过渡期，后续可能无法正常工作！
    const val BK_ATOM_IS_IN_THE_TRANSITION_PERIOD_OF_DELISTING = "bkAtomIsInTheTransitionPeriodOfDelisting"
    const val BK_GET_OUTPUT_ARTIFACTVALUE_ERROR = "bkGetOutputArtifactvalueError" // 获取输出构件[artifact]值错误
}
