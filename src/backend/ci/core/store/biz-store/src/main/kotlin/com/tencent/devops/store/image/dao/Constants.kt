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
package com.tencent.devops.store.image.dao

/**
 * @Description
 * @Date 2019/9/23
 * @Version 1.0
 */
object Constants {
    // T_IMAGE表
    const val KEY_IMAGE_ID = "imageId"
    const val KEY_IMAGE_CODE = "imageCode"
    const val KEY_IMAGE_NAME = "imageName"
    const val KEY_IMAGE_RD_TYPE = "imageRDType"
    const val KEY_IMAGE_SOURCE_TYPE = "imageSourceType"
    const val KEY_IMAGE_REPO_URL = "imageRepoUrl"
    const val KEY_IMAGE_REPO_NAME = "imageRepoName"
    const val KEY_IMAGE_VERSION = "imageVersion"
    const val KEY_IMAGE_TAG = "imageTag"
    const val KEY_IMAGE_TICKET_ID = "imageTicketId"
    const val KEY_IMAGE_INIT_PROJECT = "imageInitProject"
    const val KEY_IMAGE_SIZE = "imageSize"
    const val KEY_IMAGE_STATUS = "imageStatus"
    const val KEY_IMAGE_LOGO_URL = "imageLogoUrl"
    const val KEY_IMAGE_ICON = "imageIcon"
    const val KEY_IMAGE_SUMMARY = "imageSummary"
    const val KEY_IMAGE_DESCRIPTION = "imageDescription"
    const val KEY_IMAGE_LATEST_FLAG = "imageLatestFlag"
    const val KEY_IMAGE_DOCKER_FILE_TYPE = "imageDockerFileType"
    const val KEY_IMAGE_DOCKER_FILE_CONTENT = "imageDockerFileContent"
    const val KEY_IMAGE_FEATURE_PUBLIC_FLAG = "publicFlag"
    const val KEY_IMAGE_FEATURE_RECOMMEND_FLAG = "recommendFlag"
    const val KEY_IMAGE_FEATURE_CERTIFICATION_FLAG = "certificationFlag"
    const val KEY_IMAGE_FEATURE_WEIGHT = "weight"
    // ImageAgentType表
    const val KEY_IMAGE_AGENT_TYPE = "imageAgentType"
    const val KEY_IMAGE_AGENT_TYPE_SCOPE = "imageAgentTypeScope"
    const val KEY_IMAGE_TYPE = "imageType"
}
