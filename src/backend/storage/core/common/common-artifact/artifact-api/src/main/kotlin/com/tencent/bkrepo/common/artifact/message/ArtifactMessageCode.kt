/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.common.artifact.message

import com.tencent.bkrepo.common.api.message.MessageCode

/**
 * 构件相关错误码
 */
enum class ArtifactMessageCode(private val key: String) : MessageCode {
    ARTIFACT_RECEIVE_FAILED("artifact.receive.failed"),
    ARTIFACT_RESPONSE_FAILED("artifact.response.failed"),
    DIGEST_CHECK_FAILED("artifact.digest.check-failed"),
    PROJECT_NOT_FOUND("artifact.project.not-found"),
    PROJECT_EXISTED("artifact.project.existed"),
    REPOSITORY_NOT_FOUND("artifact.repository.not-found"),
    REPOSITORY_EXISTED("artifact.repository.existed"),
    REPOSITORY_CONTAINS_FILE("artifact.repository.not-empty"),
    FOLDER_CONTAINS_FILE("artifact.folder.not-empty"),
    NODE_NOT_FOUND("artifact.node.not-found"),
    NODE_PATH_INVALID("artifact.node.path.invalid"),
    NODE_EXISTED("artifact.node.existed"),
    NODE_CONFLICT("artifact.node.conflict"),
    NODE_LIST_TOO_LARGE("artifact.node.list.too-large"),
    STAGE_UPGRADE_ERROR("artifact.stage.upgrade.error"),
    STAGE_DOWNGRADE_ERROR("artifact.stage.downgrade.error"),
    PACKAGE_NOT_FOUND("artifact.package.not-found"),
    PACKAGE_EXISTED("artifact.package.existed"),
    VERSION_NOT_FOUND("artifact.version.not-found"),
    VERSION_EXISTED("artifact.version.existed"),
    TEMPORARY_TOKEN_INVALID("artifact.temporary-token.invalid"),
    TEMPORARY_TOKEN_EXPIRED("artifact.temporary-token.expired"),
    PIPELINE_BANNED("artifact.pipeline.banned"),
    ARTIFACT_DATA_NOT_FOUND("artifact.data.not-found"),
    ;

    override fun getBusinessCode() = ordinal + 1
    override fun getKey() = key
    override fun getModuleCode() = 10
}
