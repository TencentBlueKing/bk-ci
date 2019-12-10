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
