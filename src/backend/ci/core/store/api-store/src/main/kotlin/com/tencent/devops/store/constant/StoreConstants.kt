/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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

object StoreConstants {

    const val BK_OTHER = "bkOther" // 其他
    const val BK_PIPELINED_JOB = "bkPipelinedJob" // 流水线Job
    const val BK_IMAGE_STORE_ONLINE = "bkImageStoreOnline" // 容器镜像商店上线，历史镜像数据自动生成
    const val BK_OLD_VERSION_BUILD_IMAGE = "bkOldVersionBuildImage" // 旧版的构建镜像，通过拷贝为构建镜像入口生成
    // 已自动转换为容器镜像商店数据，请项目管理员在研发商店工作台进行管理。
    const val BK_AUTOMATICALLY_CONVERTED = "bkAutomaticallyConverted"
    // 旧版的构建镜像，通过蓝盾版本仓库“拷贝为构建镜像”入口生成。
    const val BK_COPY_FOR_BUILD_IMAGE = "bkCopyForBuildImage"
    // 容器镜像商店上线后，旧版入口已下线。因历史原因，此类镜像没有办法对应到实际的镜像推送人，暂时先挂到项目管理员名下。
    const val BK_AFTER_IMAGE_STORE_ONLINE = "bkAfterImageStoreOnline"
    // 项目管理员可在研发商店工作台进行上架/升级/下架等操作，或者交接给实际负责人进行管理。
    const val BK_PROJECT_MANAGER_CAN_OPERATION = "bkProjectManagerCanOperation"
    const val BK_HISTORY_DATA_MIGRATE_PASS = "bkHistoryDataMigratePass" // historyData数据迁移自动通过
    const val BK_WORKER_BEE_PROJECT_NOT_EXIST = "bkWorkerBeeProjectNotExist" // 工蜂项目信息不存在，请检查链接
    // 工蜂项目未开启Stream，请前往仓库的CI/CD进行配置
    const val BK_WORKER_BEE_PROJECT_NOT_STREAM_ENABLED = "bkWorkerBeeProjectNotStreamEnabled"
    const val DEFAULT_PARAM_FIELD_IS_INVALID = "bkDefaultParamFieldIsInvalid" // {0}参数应为{1}
    // 研发商店：插件配置文件[task.json]config配置格式不正确，当 defaultFailPolicy = AUTO-CONTINUE 时，defaultRetryPolicy 不能设置为 MANUALLY-RETRY
    const val TASK_JSON_CONFIG_POLICY_FIELD_IS_INVALID = "bkTaskJsonConfigPolicyFieldIsInvalid"
    const val STORE_INDEX_CODE = "INDEX_CODE"
    const val STORE_INDEX_NAME = "INDEX_NAME"
    const val STORE_INDEX_ICON_URL = "ICON_URL"
    const val STORE_INDEX_DESCRIPTION = "DESCRIPTION"
    const val STORE_INDEX_ICON_TIPS = "ICON_TIPS"
    const val STORE_INDEX_LEVEL_NAME = "LEVEL_NAME"
    const val STORE_CODE = "STORE_CODE"
    const val STORE_NAME = "STORE_NAME"
    const val STORE_TYPE = "STORE_TYPE"
    const val STORE_DAILY_FAIL_DETAIL = "DAILY_FAIL_DETAIL"
    const val DELETE_STORE_INDEX_RESULT_LOCK_KEY = "DELETE_STORE_INDEX_RESULT_LOCK"
    const val DELETE_STORE_INDEX_RESULT_KEY = "DELETE_STORE_INDEX_RESULT"
    const val STORE_HONOR_ID = "HONOR_ID"
    const val STORE_HONOR_TITLE = "HONOR_TITLE"
    const val STORE_HONOR_NAME = "HONOR_NAME"
    const val STORE_CREATOR = "CREATOR"
    const val STORE_MODIFIER = "MODIFIER"
    const val STORE_CREATE_TIME = "CREATE_TIME"
    const val STORE_UPDATE_TIME = "UPDATE_TIME"
    const val STORE_HONOR_MOUNT_FLAG = "MOUNT_FLAG"
    const val CREATE_TIME = "CREATE_TIME"
    const val BK_DEFAULT_TIMEOUT = "defaultTimeout"
    const val BK_DEFAULT_FAIL_POLICY = "defaultFailPolicy"
    const val BK_DEFAULT_RETRY_POLICY = "defaultRetryPolicy"
    const val BK_RETRY_TIMES = "retryTimes"
    const val KEY_FRAMEWORK_CODE = "frameworkCode"
}
