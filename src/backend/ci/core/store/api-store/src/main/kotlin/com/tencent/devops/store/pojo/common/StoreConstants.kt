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

package com.tencent.devops.store.pojo.common

const val PASS = "PASS"
const val REJECT = "REJECT"
const val LATEST = "latest"
const val HOTTEST = "hottest"
const val UN_RELEASE = "unRelease"
const val TASK_JSON_NAME = "task.json"
const val QUALITY_JSON_NAME = "quality.json"
const val EXTENSION_JSON_NAME = "extension.json"
const val README = "README.md"
const val STORE_ATOM_STATUS = "STORE_ATOM_STATUS" // 插件状态
const val STORE_IMAGE_STATUS = "STORE_IMAGE_STATUS" // 镜像状态
const val STORE_MEMBER_ADD_NOTIFY_TEMPLATE = "STORE_MEMBER_ADD" // store组件成员被添加的消息通知模板
const val STORE_MEMBER_DELETE_NOTIFY_TEMPLATE = "STORE_MEMBER_DELETE" // store组件成员被删除的消息通知模板
const val ATOM_COLLABORATOR_APPLY_MOA_TEMPLATE = "ATOM_COLLABORATOR_APPLY_MOA_TEMPLATE" // 插件协作开发申请MOA审批消息通知模板
const val ATOM_COLLABORATOR_APPLY_REFUSE_TEMPLATE = "ATOM_COLLABORATOR_APPLY_REFUSE" // 插件协作开发申请被拒的消息通知模板
const val ATOM_RELEASE_AUDIT_PASS_TEMPLATE = "ATOM_RELEASE_AUDIT_PASS_TEMPLATE" // 插件发布审核通过消息通知模板
const val ATOM_RELEASE_AUDIT_REFUSE_TEMPLATE = "ATOM_RELEASE_AUDIT_REFUSE_TEMPLATE" // 插件发布审核被拒消息通知模板
const val TEMPLATE_RELEASE_AUDIT_PASS_TEMPLATE = "TEMPLATE_RELEASE_AUDIT_PASS_TEMPLATE" // 模板发布审核通过消息通知模板
const val TEMPLATE_RELEASE_AUDIT_REFUSE_TEMPLATE = "TEMPLATE_RELEASE_AUDIT_REFUSE_TEMPLATE" // 模板发布审核被拒消息通知模板
const val IMAGE_RELEASE_AUDIT_PASS_TEMPLATE = "IMAGE_RELEASE_AUDIT_PASS_TEMPLATE" // 镜像发布审核通过消息通知模板
const val IMAGE_RELEASE_AUDIT_REFUSE_TEMPLATE = "IMAGE_RELEASE_AUDIT_REFUSE_TEMPLATE" // 镜像发布审核被拒消息通知模板
const val STORE_COMMENT_NOTIFY_TEMPLATE = "STORE_COMMENT_NOTIFY_TEMPLATE" // store评论消息通知模板
const val STORE_COMMENT_REPLY_NOTIFY_TEMPLATE = "STORE_COMMENT_REPLY_NOTIFY_TEMPLATE" // store评论回复消息通知模板
const val BK_FRONTEND_DIR_NAME = "bk-frontend" // 插件自定义UI前端文件夹名称
const val OPEN = "open" // 开关打开
const val CLOSE = "close" // 开关关闭

const val SERVICE_COLLABORATOR_APPLY_MOA_TEMPLATE = "SERIVCE_COLLABORATOR_APPLY_MOA_TEMPLATE" //  扩展服务协作开发申请MOA审批消息通知模板
const val SERVICE_COLLABORATOR_APPLY_REFUSE_TEMPLATE = "SERIVCE_COLLABORATOR_APPLY_REFUSE" // 扩展服务协作开发申请被拒的消息通知模板
const val EXTENSION_RELEASE_AUDIT_PASS_TEMPLATE = "EXTENSION_RELEASE_AUDIT_PASS_TEMPLATE" // 扩展服务发布审核通过消息通知模板
const val EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE = "EXTENSION_RELEASE_AUDIT_REFUSE_TEMPLATE" // 扩展服务发布审核被拒消息通知模板

const val PIPELINE_TASK_PAUSE_NOTIFY = "PIPELINE_TASK_PAUSE_NOTIFY" // 插件暂停推送消息

const val KEY_ID = "id"
const val KEY_PUB_TIME = "pubTime"
const val KEY_PUBLISHER = "publisher"
const val KEY_CREATOR = "creator"
const val KEY_MODIFIER = "modifier"
const val KEY_CREATE_TIME = "createTime"
const val KEY_UPDATE_TIME = "updateTime"
const val KEY_VERSION_LOG_CONTENT = "versionLogContent"
const val KEY_CLASSIFY_ID = "classifyId"
const val KEY_CLASSIFY_CODE = "classifyCode"
const val KEY_CLASSIFY_NAME = "classifyName"
const val KEY_CATEGORY_ID = "categoryId"
const val KEY_CATEGORY_CODE = "categoryCode"
const val KEY_CATEGORY_NAME = "categoryName"
const val KEY_CATEGORY_ICON_URL = "categoryIconUrl"
const val KEY_CATEGORY_TYPE = "categoryType"
const val KEY_LABEL_ID = "labelId"
const val KEY_LABEL_CODE = "labelCode"
const val KEY_LABEL_NAME = "labelName"
const val KEY_LABEL_TYPE = "labelType"
const val KEY_STORE_CODE = "storeCode"
const val KEY_STORE_TYPE = "storeType"
const val KEY_VERSION = "version"
const val KEY_VAR_NAME = "varName"
const val KEY_VAR_VALUE = "varValue"
const val KEY_VAR_DESC = "varDesc"
const val KEY_ENCRYPT_FLAG = "encryptFlag"
const val KEY_SCOPE = "scope"