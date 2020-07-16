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

package com.tencent.devops.gitci.pojo

// 通用变量
const val CI_STATUS = "CI"
const val CI_PIPELINE = "CI_PIPELINE"
const val CI_REPOSITORY_URL = "CI_REPOSITORY_URL"
const val CI_REPOSITORY_NAME = "CI_REPOSITORY"
const val CI_BUILD_USER = "CI_BUILD_USER"
const val CI_COMMIT_ID = "CI_COMMIT_ID"
const val CI_COMMIT_ID_SHORT = "CI_COMMIT_ID_SHORT"
const val CI_EVENT_TYPE = "CI_EVENT_TYPE"
const val CI_BRANCH = "CI_BRANCH"
const val CI_REF = "CI_REF"
const val CI_COMMIT_MESSAGE = "CI_COMMIT_MESSAGE"
const val CI_REPOSITORY_OWNER = "CI_REPOSITORY_OWNER"

// PUSH事件变量
const val CI_PUSH_BEFORE_COMMIT = "CI_PUSH_BEFORE_COMMIT"
const val CI_PUSH_AFTER_COMMIT = "CI_PUSH_AFTER_COMMIT"
const val CI_PUSH_TOTAL_COMMIT = "CI_PUSH_TOTAL_COMMIT"
const val CI_PUSH_OPERATION_KIND = "CI_PUSH_OPERATION_KIND"

// TAG PUSH事件变量
const val CI_TAG_NAME = "CI_TAG_NAME"
const val CI_TAG_OPERATION = "CI_TAG_OPERATION"
const val CI_TAG_USERNAME = "CI_TAG_USERNAME"
const val CI_TAG_CREATE_FROM = "CI_TAG_CREATE_FROM"

// MR 事件变量
const val CI_MR_ID = "CI_MR_ID"
const val CI_MR_AUTHOR = "CI_MR_AUTHOR"
const val CI_MR_TARGET_BRANCH = "CI_MR_TARGET_BRANCH"
const val CI_MR_SOURCE_BRANCH = "CI_MR_SOURCE_BRANCH"
const val CI_MR_TARGET_REPOSITORY = "CI_MR_TARGET_REPOSITORY"
const val CI_MR_SOURCE_REPOSITORY = "CI_MR_SOURCE_REPOSITORY"
const val CI_MR_CREATE_TIME = "CI_MR_CREATE_TIME"
const val CI_MR_UPDATE_TIME = "CI_MR_UPDATE_TIME"
const val CI_MR_CREATE_TIME_TIMESTAMP = "CI_MR_CREATE_TIME_TIMESTAMP"
const val CI_MR_UPDATE_TIME_TIMESTAMP = "CI_MR_UPDATE_TIME_TIMESTAMP"
const val CI_MR_NUMBER = "CI_MR_NUMBER"
const val CI_MR_DESC = "CI_MR_DESC"
const val CI_MR_TITLE = "CI_MR_TITLE"
const val CI_MR_URL = "CI_MR_URL"
const val CI_MR_ACTION = "CI_MR_ACTION"
const val CI_MR_ASSIGNEE = "CI_MR_ASSIGNEE"
