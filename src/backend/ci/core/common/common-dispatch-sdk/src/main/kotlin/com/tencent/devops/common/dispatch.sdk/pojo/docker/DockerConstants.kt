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

package com.tencent.devops.common.dispatch.sdk.pojo.docker

object DockerConstants {
    /**
     * docker路由Key
     */
    const val DOCKER_ROUTING_KEY_PREFIX = "dispatchdocker:docker_routing"

    const val ENV_KEY_BUILD_ID = "devops_build_id"
    const val ENV_KEY_PROJECT_ID = "devops_project_id"
    const val ENV_KEY_AGENT_ID = "devops_agent_id"
    const val ENV_KEY_AGENT_SECRET_KEY = "devops_agent_secret_key"
    const val ENV_KEY_GATEWAY = "devops_gateway"

    const val ENV_JOB_BUILD_TYPE = "JOB_POOL"

    const val BK_FAILED_START_BUILD_MACHINE = "BkFailedStartBuildMachine"
    const val BK_UNABLE_GET_PIPELINE_JOB_STATUS = "BkUnableGetPipelineJobStatus"
    const val BK_JOB_BUILD_STOPS = "BkJobBuildStops"
    const val BK_JOB_REACHED_MAX_QUOTA_SOON_RETRY = "BkJobReachedMaxQuotaSoonRetry"
    const val BK_JOB_REACHED_MAX_QUOTA_AND_ALREADY_DELAYED = "BkJobReachedMaxQuotaAndAlreadyDelayed"
    const val BK_JOB_REACHED_MAX_QUOTA_AND_SOON_DELAYED = "BkJobReachedMaxQuotaAndAlreadyDelayed"
}
