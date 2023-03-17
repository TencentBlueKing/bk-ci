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

package com.tencent.devops.worker.common

const val BK_GET_DOWNLOAD_LINK_REQUEST_ERROR = "BkGetDownloadLinkRequestError"// 获取下载链接请求出错
const val BK_UPLOAD_CUSTOM_FILE_FAILED = "BkUploadCustomFileFailed"// 上传自定义文件失败
const val BK_UPLOAD_PIPELINE_FILE_FAILED = "BkUploadPipelineFileFailed"// 上传流水线文件失败
const val BK_UPLOAD_FILE_FAILED = "BkUploadFileFailed"// 上传文件失败
const val BK_GET_CREDENTIAL_INFO_FAILED = "BkGetCredentialInfoFailed"// 获取凭证信息失败
const val BK_GET_BUILD_BASE_INFO_FAIL = "BkGetBuildBaseInfoFail"// 获取构建机基本信息失败
const val BK_GET_PLUGIN_ENV_INFO_FAILED = "BkGetPluginEnvInfoFailed"// 获取插件执行环境信息失败
const val BK_UPDATE_PLUGIN_ENV_INFO_FAILED = "BkUpdatePluginEnvInfoFailed"// 更新插件执行环境信息失败
const val BK_GET_PLUGIN_SENSITIVE_INFO_FAILED = "BkGetPluginSensitiveInfoFailed" // 获取插件敏感信息失败
const val BK_ARCHIVE_PLUGIN_FILE_FAILED = "BkArchivePluginFileFailed"// 归档插件文件失败
const val BK_ARCHIVE_PLUGIN_FILE= "BkArchivePluginFile"// 归档插件文件
const val BK_GET_PLUGIN_LANGUAGE_ENV_INFO_FAILED = "BkGetPluginLanguageEnvInfoFailed"// 获取插件开发语言相关的环境变量信息失败
const val BK_ADD_PLUGIN_PLATFORM_INFO_FAILED = "BkAddPluginPlatformInfoFailed"// 添加插件对接平台信息失败
const val BK_DOWNLOAD_CODECC_TOOL_FAIL = "BkDownloadCodeccToolFail"// 下载Codecc的 {0} 工具失败
const val BK_DOWNLOAD_CODECC_COVERITY_SCRIPT_FAIL = "BkDownloadCodeccCoverityScriptFail"// 下载codecc的coverity的执行脚本失败
const val BK_DOWNLOAD_CODECC_MULTI_TOOL_SCRIPT_FAIL = "BkDownloadCodeccMultiToolScriptFail"// 下载codecc的多工具执行脚本失败
const val BK_UPDATE_IMAGE_MARKET_INFO_FAILED = "BkUpdateImageMarketInfoFailed"// 更新镜像市场信息失败
const val BK_LOGS_END_STATUS_FAILED = "BkLogsEndStatusFailed"// 上报结束状态失败
const val BK_LOGS_REPORT_FAILED = "BkLogsReportFailed"// 上报日志失败
const val BK_LOG_STORAGE_STATUS_FAILED = "BkLogStorageStatusFailed"// 上报日志存储状态失败
const val BK_NOTIFY_SERVER_START_BUILD_FAILED = "BkNotifyServerStartBuildFailed"// 通知服务端启动构建失败
const val BK_RECEIVE_BUILD_MACHINE_TASK_FAILED = "BkReceiveBuildMachineTaskFailed"// 领取构建机任务失败
const val BK_RECEIVE_BUILD_MACHINE_TASK_DETAIL_FAILED = "BkReceiveBuildMachineTaskDetailFailed"// 领取构建机任务详情失败
const val BK_BUILD_FINISH_REQUEST_FAILED = "BkBuildFinishRequestFailed" // 构建完成请求失败
const val BK_REPORT_TASK_FINISH_FAILURE = "BkReportTaskFinishFailure"// 报告任务完成失败
const val BK_HEARTBEAT_FAIL = "BkHeartbeatFail"// 心跳失败
const val BK_GET_BUILD_TASK_DETAILS_FAILURE = "BkGetBuildTaskDetailsFailure"// 获取构建任务详情失败
const val BK_BUILD_TIMEOUT_END_REQUEST_FAILURE = "BkBuildTimeoutEndRequestFailure"// 构建超时结束请求失败
const val BK_GET_TEMPLATE_CROSS_PROJECT_INFO_FAILURE = "BkGetTemplateCrossProjectInfoFailure"// 获取模板跨项目信息失败
const val BK_SAVE_SCRIPT_METADATA_FAILURE = "BkSaveScriptMetadataFailure"// 保存脚本元数据失败
const val BK_REPORT_AGENT_END_STATUS_FAILURE = "BkReportAgentEndStatusFailure"// Quota上报agent运行结束状态失败
const val BK_REPORT_AGENT_START_STATUS_FAILURE = "BkReportAgentStartStatusFailure"// Quota上报agent开始运行失败
const val BK_GET_REPORT_ROOT_PATH_FAILURE = "BkGetReportRootPathFailure"// 获取报告根路径失败
const val BK_CREATE_REPORT_FAIL = "BkCreateReportFail"// 创建报告失败
const val BK_UPLOAD_CUSTOM_REPORT_FAILURE = "BkUploadCustomReportFailure"// 上传自定义报告失败
const val BK_ADD_CODE_BASE_COMMIT_INFO_FAIL = "BkAddCodeBaseCommitInfoFail"// 添加代码库commit信息失败
const val BK_GET_LAST_CODE_BASE_COMMIT_INFO_FAIL = "BkGetLastCodeBaseCommitInfoFail"// 获取最后一次代码commit信息失败
const val BK_ADD_SOURCE_MATERIAL_INFO_FAILURE = "BkAddSourceMaterialInfoFailure"// 添加源材料信息失败
const val BK_GET_OAUTH_INFO_FAIL = "BkGetOauthInfoFail"// 获取oauth认证信息失败
const val BK_GET_CODE_BASE_FAIL = "BkGetCodeBaseFail"// 获取代码库失败
const val BK_GET_CREDENTIAL_FAILED = "BkGetCredentialFailed"// 获取凭证失败
const val BK_START_BUILD_IMAGE_NAME = "BkStartBuildImageName"// 启动构建镜像，镜像名称：
const val BK_WAIT_BUILD_IMAGE_FINISH = "BkWaitBuildImageFinish"// 启动构建镜像成功，等待构建镜像结束，镜像名称：
const val BK_BUILD_IMAGE_FAIL_DETAIL = "BkBuildImageFailDetail"// 构建镜像失败，错误详情：
const val BK_BUILD_IMAGE_SUCCEED = "BkBuildImageSucceed"// 构建镜像成功！
const val BK_DOCKERFILE_FIRST_LINE_CHECK = "BkDockerfileFirstLineCheck"// Dockerfile第一行请确认使用 {0}
const val BK_START_BUILD_IMAGE_FAIL = "BkStartBuildImageFail"// 启动构建镜像失败！请联系【蓝盾助手】
const val BK_QUERY_BUILD_IMAGE_STATUS_FAIL = "BkStartBuildImageFail"// 查询构建镜像状态失败！请联系【蓝盾助手】
const val BK_START_BUILD_FAIL = "BkStartBuildFail"// 启动构建失败！请联系【蓝盾助手】
const val BK_FOLDER_NOT_EXIST = "BkFolderNotExist"// 文件夹{0}不存在
const val BK_ENTRANCE_FILE_NOT_IN_FOLDER = "BkEntranceFileNotInFolder"// 入口文件({0})不在文件夹({1})下
const val BK_ENTRANCE_FILE_CHECK_FINISH = "BkEntranceFileCheckFinish"// 入口文件检测完成
const val BK_UPLOAD_CUSTOM_OUTPUT_SUCCESS = "BkUploadCustomOutputSuccess"// 上传自定义产出物成功，共产生了{0}个文件
const val BK_NO_MATCHING_ARCHIVE_FILE = "BkNoMatchingArchiveFile"// 没有匹配到任何待归档文件，请检查工作空间下面的文件
const val BK_GIT_CREDENTIAL_ILLEGAL = "BkGitCredentialIllegal"// git凭据不合法
const val BK_CODE_REPO_PARAM_NOT_IN_PARAMS = "BkCodeRepoIdNotInParams"// 代码仓库{0}没存在参数中
const val BK_REPORT_START_ERROR_INFO_FAIL = "BkReportStartErrorInfoFail"// 上报启动异常信息失败
