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

package com.tencent.devops.common.pipeline.utils

const val PIPELINE_SUBPIPELINE_CALL_ELEMENT_ID = "pipeline.subpipeline.call.element.id"

const val PIPELINE_SUBPIPELINE_CALL_ELEMENT_NAME = "pipeline.subpipeline.call.element.name"

// =====代码库构建Key常量=====

const val PIPELINE_GIT_REF = "GIT_CI_REF"
const val PIPELINE_GIT_HEAD_REF = "GIT_CI_HEAD_REF"
const val PIPELINE_GIT_BASE_REF = "GIT_CI_BASE_REF"
const val PIPELINE_GIT_REPO = "GIT_CI_REPO"
const val PIPELINE_GIT_REPO_ID = "GIT_CI_REPO_ID"
const val PIPELINE_GIT_REPO_NAME = "GIT_CI_REPO_NAME"
const val PIPELINE_GIT_REPO_GROUP = "GIT_CI_REPO_GROUP"
const val PIPELINE_GIT_REPO_CREATE_TIME = "GIT_CI_REPO_CREATE_TIME"
const val PIPELINE_GIT_REPO_CREATOR = "GIT_CI_REPO_CREATOR"
const val PIPELINE_GIT_EVENT = "GIT_CI_EVENT"
const val PIPELINE_GIT_EVENT_CONTENT = "GIT_CI_EVENT_CONTENT"
const val PIPELINE_GIT_SHA = "GIT_CI_SHA"
const val PIPELINE_GIT_SHA_SHORT = "GIT_CI_SHA_SHORT"
const val PIPELINE_GIT_BEFORE_SHA = "GIT_CI_BEFORE_SHA"
const val PIPELINE_GIT_BEFORE_SHA_SHORT = "GIT_CI_BEFORE_SHA_SHORT"
const val PIPELINE_GIT_COMMIT_MESSAGE = "GIT_CI_COMMIT_MESSAGE"
const val PIPELINE_GIT_EVENT_URL = "GIT_CI_EVENT_URL"
const val PIPELINE_GIT_ACTION = "GIT_CI_ACTION"
const val PIPELINE_GIT_YAML_PATH = "GIT_CI_YAML_PATH"

const val PIPELINE_GIT_REPO_URL = "GIT_CI_REPO_URL"
const val PIPELINE_GIT_BASE_REPO_URL = "GIT_CI_BASE_REPO_URL"
const val PIPELINE_GIT_HEAD_REPO_URL = "GIT_CI_HEAD_REPO_URL"

const val PIPELINE_GIT_COMMIT_AUTHOR = "GIT_CI_COMMIT_AUTHOR"
const val PIPELINE_GIT_UPDATE_USER = "GIT_CI_PIPELINE_UPDATE_USER"
const val PIPELINE_GIT_AUTHORIZER = "GIT_CI_AUTHORIZER"

const val PIPELINE_GIT_TAG_MESSAGE = "GIT_CI_TAG_MESSAGE"
const val PIPELINE_GIT_TAG_FROM = "GIT_CI_TAG_FROM"

const val PIPELINE_GIT_MR_ID = "GIT_CI_MR_ID"
const val PIPELINE_GIT_MR_IID = "GIT_CI_MR_IID"
const val PIPELINE_GIT_MR_URL = "GIT_CI_MR_URL"
const val PIPELINE_GIT_MR_TITLE = "GIT_CI_MR_TITLE"
const val PIPELINE_GIT_MR_DESC = "GIT_CI_MR_DESC"
const val PIPELINE_GIT_MR_PROPOSER = "GIT_CI_MR_PROPOSER"
const val PIPELINE_GIT_MR_ACTION = "GIT_CI_MR_ACTION"

// =====代码库构建Value常量=====
const val PIPELINE_GIT_TIME_TRIGGER_KIND = "schedule"
