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

package com.tencent.devops.store.constant

/**
 * 流水线微服务模块请求返回状态码
 * 返回码制定规则（0代表成功，为了兼容历史接口的成功状态都是返回0）：
 * 1、返回码总长度为7位，
 * 2、前2位数字代表系统名称（如21代表持续集成平台）
 * 3、第3位和第4位数字代表微服务模块（00：common-公共模块 01：process-流水线 02：artifactory-版本仓库 03:dispatch-分发 04：dockerhost-docker机器
 *    05:environment-持续集成环境 06：experience-版本体验 07：image-镜像 08：log-持续集成日志 09：measure-度量 10：monitoring-监控 11：notify-通知
 *    12：openapi-开放api接口 13：plugin-插件 14：quality-质量红线 15：repository-代码库 16：scm-软件配置管理 17：support-持续集成支撑服务
 *    18：ticket-证书凭据 19：project-项目管理 20：store-应用商店）
 * 4、最后3位数字代表具体微服务模块下返回给客户端的业务逻辑含义（如001代表系统服务繁忙，建议一个模块一类的返回码按照一定的规则制定）
 * 5、系统公共的返回码写在CommonMessageCode这个类里面，具体微服务模块的返回码写在相应模块的常量类里面
 *
 * @since: 2018-12-21
 * @version: $Revision$ $Date$ $LastChangedBy$
 *
 */
@Suppress("ALL")
object StoreMessageCode {

    const val MSG_CODE_STORE_CLASSIFY_PREFIX = "MSG_CODE_STORE_CLASSIFY_PREFIX_" // 研发商店插分类国际化前缀
    const val MSG_CODE_STORE_CATEGORY_PREFIX = "MSG_CODE_STORE_CATEGORY_PREFIX_" // 研发商店范畴国际化前缀
    const val MSG_CODE_STORE_LABEL_PREFIX = "MSG_CODE_STORE_LABEL_PREFIX_" // 研发商店标签国际化前缀
    const val MSG_CODE_BUILD_TYPE_PREFIX = "MSG_CODE_BUILD_TYPE_PREFIX_" // 构建资源类型国际化前缀
    // 插件相关的错误提示
    const val USER_QUERY_ATOM_PERMISSION_IS_INVALID = "2120001" // 研发商店：没有插件的查看权限
    const val USER_QUERY_PROJECT_PERMISSION_IS_INVALID = "2120002" // 研发商店：没有项目的查看权限
    const val USER_CREATE_REPOSITORY_FAIL = "2120003" // 研发商店：创建代码库失败，请稍后再试
    const val USER_INSTALL_ATOM_CODE_IS_INVALID = "2120004" // 研发商店：安装插件失败
    // 研发商店：从[{0}]分支拉取插件配置文件[task.json]失败,请确认是否OAUTH授权、文件是否正确上传代码库等
    const val USER_REPOSITORY_PULL_TASK_JSON_FILE_FAIL = "2120005"
    const val USER_REPOSITORY_TASK_JSON_FIELD_IS_NULL = "2120006" // 研发商店：插件配置文件[task.json]{0}字段不能为空
    const val USER_REPOSITORY_TASK_JSON_FIELD_IS_NOT_MATCH = "2120007" // 研发商店：插件配置文件[task.json]{0}字段与工作台录入的不一致
    const val USER_ATOM_RELEASE_STEPS_ERROR = "2120008" // 研发商店：插件发布流程状态变更顺序不正确
    const val USER_ATOM_VERSION_IS_NOT_FINISH = "2120009" // 研发商店：插件{0}的{1}版本发布未结束，请稍后再试
    const val USER_ATOM_VERSION_IS_INVALID = "2120010" // 研发商店：插件升级的版本号{0}错误，应为{1}
    const val USER_ATOM_LOGO_SIZE_IS_INVALID = "2120011" // 研发商店：插件logo的尺寸应为{0}x{1}
    const val USER_ATOM_LOGO_TYPE_IS_NOT_SUPPORT = "2120012" // 研发商店：logo不支持{0}类型，可以上传{1}类型
    const val UPLOAD_LOGO_IS_TOO_LARGE = "2120013" // 研发商店：上传的logo文件不能超过{0}
    const val USER_ATOM_CONF_INVALID = "2120014" // 研发商店：插件配置文件{0}格式不正确，请检查
    const val USER_ATOM_VISIBLE_DEPT_IS_INVALID = "2120015" // 研发商店：你不在{0}插件的可见范围之内，请联系插件发布者
    const val USER_COMPONENT_ADMIN_COUNT_ERROR = "2120016" // 研发商店：管理员个数不能少于1个
    const val USER_ATOM_QUALITY_CONF_INVALID = "2120018" // 研发商店：插件配置文件[quality.json]{0}格式不正确，请检查
    // 研发商店：从[{0}]分支拉取插件配置文件[quality.json失败,请确认是否OAUTH授权、文件是否正确上传代码库等
    const val USER_REPOSITORY_PULL_QUALITY_JSON_FILE_FAIL = "2120019"
    const val USER_ATOM_USED = "2120020" // 研发商店：插件{0}已被项目{1}下的流水线使用，不可以卸载
    const val USER_ATOM_UNINSTALL_REASON_USED = "2120021" // 研发商店：插件卸载原因{0}已被使用，不能删除。建议禁用
    const val USER_ATOM_RELEASED_IS_NOT_ALLOW_DELETE = "2120022" // 研发商店：插件{0}已发布到商店，请先下架再删除
    const val USER_ATOM_USED_IS_NOT_ALLOW_DELETE = "2120023" // 研发商店：插件{0}已安装到其他项目下使用，请勿移除
    const val USER_REPOSITORY_BK_FRONTEND_DIR_IS_NULL = "2120024" // 研发商店：插件代码库文件夹[{0}]未创建
    const val USER_ATOM_IS_NOT_ALLOW_USE_IN_PROJECT = "2120025" // 你的项目[{0}]不允许使用插件[{1}]，请检查插件是否被正确安装
    const val USER_REPOSITORY_TASK_JSON_FIELD_IS_INVALID = "2120026" // 研发商店：插件配置文件[task.json]{0}格式不正确，请检查
    const val USER_ATOM_IS_NOT_ALLOW_REPLACE = "2120027" // 研发商店：插件[{0}]的[{1}]版本无法被插件[{2}]的[{3}]版本替换，插件[{2}]的{4}参数无法映射替换
    const val USER_TO_ATOM_IS_NOT_BE_HIS_ATOM = "2120028" // 研发商店：目标替换插件不能是历史内置老插件
    // 研发商店：插件当前版本新增了无默认值的必填入参[{0}]，将导致存量流水线执行异常，请修改发布类型进行非兼容式升级
    const val USER_ATOM_NOT_COMPATIBLE_INPUT_FIELD = "2120029"
    // 研发商店：插件当前版本新增了入参[{0}]，请确认插件执行逻辑对这些参数不存在或值为空的情况做了兼容处理，否则有可能导致存量流水线执行异常
    const val USER_ATOM_COMPATIBLE_INPUT_FIELD_CONFIRM = "2120030"
    // 研发商店：插件当前版本减少了出参[{0}]，请确认插件执行逻辑对这些参数不存在或值为空的情况做了兼容处理，否则有可能导致存量流水线执行异常
    const val USER_ATOM_COMPATIBLE_OUTPUT_FIELD_CONFIRM = "2120031"
    const val USER_ATOM_INPUT_NUM_IS_TOO_MANY = "2120032" // 研发商店：插件入参数量太多，系统规定的数量最大为：{0}
    const val USER_ATOM_OUTPUT_NUM_IS_TOO_MANY = "2120033" // 研发商店：插件出参数量太多，系统规定的数量最大为：{0}
    // 研发商店：插件配置文件[task.json]{0}操作系统默认环境配置数量为{1}个，默认环境配置数量有且只能为1个
    const val USER_REPOSITORY_TASK_JSON_OS_DEFAULT_ENV_IS_INVALID = "2120034"
    const val USER_REPOSITORY_PULL_ERROR_JSON_FILE_FAIL = "2120035"
    // 研发商店：从[{0}]分支拉取插件配置文件[error.json]失败,请确认是否OAUTH授权、文件是否正确上传代码库等
    const val USER_REPOSITORY_ERROR_JSON_ERROR_CODE_EXIST_DUPLICATE = "2120036" // 研发商店：插件配置文件[error.json]errorCode字段数据重复，请检查{0}
    const val USER_REPOSITORY_ERROR_JSON_FIELD_IS_INVALID = "2120037" // 研发商店：插件配置文件[error.json]errorCode格式不正确，请检查

    // 模板相关的错误提示
    const val USER_TEMPLATE_VERSION_IS_NOT_FINISH = "2120201" // 研发商店：模板{0}的{1}版本发布未结束，请稍后再试
    const val USER_TEMPLATE_RELEASE_STEPS_ERROR = "2120202" // 研发商店：模板发布流程状态变更顺序不正确
    const val USER_TEMPLATE_ATOM_VISIBLE_DEPT_IS_INVALID = "2120203" // 研发商店：模板的可见范围不在插件{0}的可见范围之内，如有需要请联系插件的发布者
    const val USER_TEMPLATE_ATOM_NOT_INSTALLED = "2120204" // 研发商店：模版下的插件{0}尚未安装，请先安装后再使用
    const val USER_TEMPLATE_RELEASED = "2120205" // 研发商店：模版{0}已发布到商店，请先下架再删除
    const val USER_TEMPLATE_USED = "2120206" // 研发商店：模版{0}已安装到其他项目下使用，请勿移除
    const val USER_TEMPLATE_ATOM_IS_INVALID = "2120207" // 研发商店：模版下的插件{0}不可用，请联系模板发布者
    const val USER_TEMPLATE_IMAGE_VISIBLE_DEPT_IS_INVALID = "2120208" // 研发商店：模板的可见范围不在镜像[{0}]的可见范围之内，如有需要请联系镜像的发布者
    const val USER_TEMPLATE_IMAGE_IS_INVALID = "2120209" // 研发商店：模版下的镜像{0}不可用，请联系模板发布者
    const val USER_INSTALL_TEMPLATE_CODE_IS_INVALID = "2120210" // 研发商店： 商店模板{0}安装到项目[{1}]失败，模板已安装或项目下存在与模板{0}同名的自定义模板，若有同名的自定义模板请修改模板名称后再安装商店模版。

    const val USER_IMAGE_VERSION_IS_NOT_FINISH = "2120301" // 研发商店：镜像{0}的{1}版本发布未结束，请稍后再试
    const val USER_IMAGE_VERSION_IS_INVALID = "2120302" // 研发商店：镜像升级的版本号{0}错误，应为{1}
    const val USER_IMAGE_RELEASE_STEPS_ERROR = "2120303" // 研发商店：镜像发布流程中状态变更顺序不正确
    const val USER_IMAGE_RELEASED = "2120304" // 研发商店：镜像{0}已发布到商店，请先下架再删除
    const val USER_IMAGE_USED = "2120305" // 研发商店：镜像{0}已安装到其他项目下使用，请勿移除
    const val USER_IMAGE_NOT_INSTALLED = "2120306" // 研发商店：项目{0}未安装镜像{1}，无法使用
    const val USER_IMAGE_UNKNOWN_SOURCE_TYPE = "2120307" // 研发商店：镜像原始来源类型未知：{0}
    const val USER_IMAGE_UNKNOWN_IMAGE_CATEGORY = "2120308" // 镜像范畴未知：{0}
    const val USER_IMAGE_NOT_EXIST = "2120309" // 镜像[{0}]不存在
    const val USER_IMAGE_VERSION_NOT_EXIST = "2120310" // 标识为{0}版本号为{1}的镜像不存在
    const val USER_IMAGE_PROJECT_IS_INVALID = "2120311" // 研发商店：容器镜像[{0}]在项目[{1}]下不可用，请联系镜像发布者调整可见范围，调整后手动安装镜像

    // 扩展服务相关的错误提示
    const val USER_SERVICE_RELEASED_IS_NOT_ALLOW_DELETE = "2120401" // 研发商店：扩展服务{0}已发布到商店，请先下架再删除
    const val USER_SERVICE_USED_IS_NOT_ALLOW_DELETE = "2120402" // 研发商店：扩展服务{0}已安装到其他项目下使用，请勿移除
    const val USER_SERVICE_VERSION_IS_INVALID = "2120403" // 研发商店：扩展服务升级的版本号{0}错误，应为{1}
    const val USER_SERVICE_VERSION_IS_NOT_FINISH = "2120404" // 研发商店：扩展服务{0}的{1}版本发布未结束，请稍后再试
    const val USER_SERVICE_CODE_DIFF = "2120405" // 研发商店：扩展编码与extension.json文件内编码不一致
    const val USER_SERVICE_RELEASE_STEPS_ERROR = "2120406" // 研发商店：扩展服务发布流程状态变更顺序不正确
    const val USER_SERVICE_PROJECT_UNENABLE = "2120407" // 研发商店：选中调试项目已被禁用
    const val USER_SERVICE_PROJECT_NOT_PERMISSION = "2120408" // 研发商店：选中调试项目无创建流水线权限
    const val USER_SERVICE_NOT_EXIST = "2120409" // 研发商店：扩展服务不存在{0}
    const val USER_ITEM_SERVICE_USED_IS_NOT_ALLOW_DELETE = "2120410" // 研发商店：扩展点下还有可用的扩展服务，不能删除

    // store公共业务相关的错误提示
    const val USER_PRAISE_IS_INVALID = "2120901" // 研发商店：你已点赞过
    const val USER_PROJECT_IS_NOT_ALLOW_INSTALL = "2120902" // 研发商店：你没有权限将组件安装到项目：{0}
    const val USER_COMMENT_IS_INVALID = "2120903" // 研发商店：你已评论过，无法继续添加评论。但可以修改原有评论
    const val USER_CLASSIFY_IS_NOT_ALLOW_DELETE = "2120904" // 研发商店：该分类下还有正在使用的组件，不允许直接删除
    const val USER_APPROVAL_IS_NOT_ALLOW_REPEAT_APPLY = "2120905" // 研发商店：你已有处于待审批或审批通过的申请单，请勿重复申请
    const val USER_UPLOAD_PACKAGE_INVALID = "2120906" // 研发商店：请确认上传的包是否正确
    const val USER_SENSITIVE_CONF_EXIST = "2120907" // 研发商店：字段名{0}已存在
    const val USER_START_CODECC_TASK_FAIL = "2120908" // 研发商店：启动代码扫描任务失败
    const val USER_CHANGE_TEST_PROJECT_FAIL = "2120909" // 研发商店：用户[{0}]没有项目[{1}]下的流水线新增/修改/执行权限，请先给用户赋权
    const val SENSITIVE_API_PASSED_IS_NOT_ALLOW_CANCEL = "2120910" // 研发商店：敏感API已经审批通过不能取消
    const val SENSITIVE_API_APPROVED_IS_NOT_ALLOW_PASS = "2120911" // 研发商店：敏感API已经取消不能审批
    const val SENSITIVE_API_NOT_EXIST = "2120912" // 研发商店：敏感API[{0}]不存在
    const val USER_HIS_VERSION_UPGRADE_INVALID = "2120913" // 研发商店：当前发布类型下仅能新增历史大版本下的小版本，请修改版本号或者发布类型
    const val USER_UPLOAD_FILE_PATH_ERROR = "2120914" // 研发商店：文件路径[{0}]错误
    const val USER_ERROR_CODE_INVALID = "2120915" // 研发商店：错误码{0}格式错误
    const val USER_LOCALE_FILE_NOT_EXIST = "2120916" // 研发商店：未提供系统[{0}]语言的配置文件
}
