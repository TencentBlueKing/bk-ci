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

package com.tencent.devops.process.enums

import com.tencent.devops.common.api.util.MessageUtil
import com.tencent.devops.common.pipeline.enums.VersionStatus

enum class OperationLogType(val description: String) {
    CREATE_PIPELINE_AND_DRAFT("创建流水线首次保存草稿：「创建了草稿」"),
    CREATE_DRAFT_VERSION("编辑流水线生成草稿：「从 P1.T2.0 创建了草稿」"),
    UPDATE_DRAFT_VERSION("修改草稿保存后：「修改了草稿」"),
    CREATE_BRANCH_VERSION("新增分支版本：「新增版本 P1.T2.0」"),
    UPDATE_BRANCH_VERSION("修改分支版本：「修改版本 P1.T2.0」"),
    RELEASE_MASTER_VERSION("正式版本完成时：「发布版本 P1.T2.0」"),
    DISABLE_PIPELINE("禁用流水线时：「禁用了流水线」"),
    ENABLE_PIPELINE("启用流水线时：「启用了流水线」"),
    ADD_PIPELINE_OWNER("添加流水线成员时：「添加 xxx,yyy 为执行者」"),
    ADD_PIPELINE_TO_GROUP("将流水线添加到流水线组时：「添加到流水线组 a」"),
    MOVE_PIPELINE_OUT_OF_GROUP("将流水线移出流水线组时：「从流水线组 a 中移出」"),
    UPDATE_PIPELINE_SETTING("修改流水线设置：「修改配置新增版本 P2.T1.2」"),
    RESET_RECOMMENDED_VERSION_BUILD_NO("重置流水线推荐版本号：「重置流水线推荐版本号为 xxx」"),
    NORMAL_SAVE_OPERATION("普通保存操作"),
    PIPELINE_ARCHIVE("流水线归档操作");

    fun getI18n(language: String): String {
        return MessageUtil.getMessageByLocale(
            messageCode = "operationLogType.${this.name}",
            language = language
        )
    }

    companion object {

        fun parseType(type: String?): OperationLogType {
            if (type.isNullOrBlank()) return NORMAL_SAVE_OPERATION
            return try {
                OperationLogType.valueOf(type)
            } catch (ignore: Throwable) {
                NORMAL_SAVE_OPERATION
            }
        }

        fun fetchType(versionStatus: VersionStatus?): OperationLogType {
            return when (versionStatus) {
                VersionStatus.COMMITTING -> CREATE_PIPELINE_AND_DRAFT
                VersionStatus.BRANCH -> CREATE_BRANCH_VERSION
                VersionStatus.RELEASED -> RELEASE_MASTER_VERSION
                else -> NORMAL_SAVE_OPERATION
            }
        }
    }
}
