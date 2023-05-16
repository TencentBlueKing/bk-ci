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

package com.tencent.devops.repository.constant

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
object RepositoryMessageCode {
    const val USER_CREATE_GIT_CODE_REPOSITORY_FAIL = "2115001" // 创建GIT代码库失败，请稍后再试
    const val USER_UPDATE_GIT_CODE_REPOSITORY_FAIL = "2115002" // 更新GIT代码库失败，请稍后再试
    const val USER_ADD_GIT_CODE_REPOSITORY_MEMBER_FAIL = "2115003" // GIT代码添加成员{0}失败，请稍后再试
    const val USER_GIT_REPOSITORY_MOVE_GROUP_FAIL = "2115004" // 代码库迁移至{0}项目组失败，请稍后再试
    const val USER_DELETE_GIT_CODE_REPOSITORY_MEMBER_FAIL = "2115005" // GIT代码库删除成员{0}失败，请稍后再试

    const val GIT_INVALID = "2115006" // 无效的GIT仓库
    const val TGIT_INVALID = "2115007" // 无效的TGIT仓库
    const val SVN_INVALID = "2115008" // 无效的SVN仓库
    const val GITHUB_INVALID = "2115009" // 无效的GITHUB仓库
    const val GET_TICKET_FAIL = "2115010" // 获取凭证异常
    const val USER_SECRET_EMPTY = "2115011" // 用户私钥为空
    const val GIT_REPO_PEM_FAIL = "2115012" // 代码仓库访问未授权
    const val REPO_PATH_WRONG_PARM = "2115013" // 代码仓库路径不正确，仓库路径应该以{0}开头
    const val REPO_NAME_EXIST = "2115014" // 代码库别名{0}已存在
    const val USER_VIEW_PEM_ERROR = "2115015" // 用户{0}在工程{1}下没有代码库{2}查看权限
    const val USER_EDIT_PEM_ERROR = "2115016" // 用户{0}在工程{1}下没有代码库{2}编辑权限
    const val USER_DELETE_PEM_ERROR = "2115017" // 用户{0}在工程{1}下没有代码库{2}删除权限
    const val REPO_LOCK_UN_SUPPORT = "2115018" // 代码库{0}不支持锁定
    const val REPO_TYPE_NO_NEED_CERTIFICATION = "2115019" // 代码库类型{0}无需认证
    const val CREATE_TAG_FAIL = "2115020" // 创建tag失败
    const val P4_INVALID = "2115021" // 无效的p4仓库
    const val GIT_NOT_FOUND = "2115022" // 代码库{0}不存在
    const val USER_CREATE_PEM_ERROR = "2115023" // 用户{0}在工程{1}下没有代码库创建权限
    const val REPOSITORY_ID_AND_NAME_ARE_EMPTY = "2115024" // 仓库ID和仓库名都为空
    const val USER_NEED_PROJECT_X_PERMISSION = "2115025" // 用户（{0}）无（{1}）项目权限
    const val NOT_AUTHORIZED_BY_OAUTH = "2115026" // 用户[{0}]尚未进行OAUTH授权，请先授权。

    const val BK_REQUEST_FILE_SIZE_LIMIT = "bkRequestFileSizeLimit" // 请求文件不能超过1M
    const val OPERATION_ADD_CHECK_RUNS = "OperationAddCheckRuns" // 添加检测任务
    const val OPERATION_UPDATE_CHECK_RUNS = "OperationUpdateCheckRuns" // 更新检测任务
    const val OPERATION_GET_REPOS = "OperationGetRepos" // 获取仓库列表
    const val OPERATION_GET_BRANCH = "OperationGetBranch" // 获取指定分支
    const val OPERATION_GET_TAG = "OperationGetTag" // 获取指定Tag
    const val OPERATION_LIST_BRANCHS = "OperationListBranchs" // 获取分支列表
    const val OPERATION_LIST_TAGS = "OperationListTags" // 获取Tag列表
}
