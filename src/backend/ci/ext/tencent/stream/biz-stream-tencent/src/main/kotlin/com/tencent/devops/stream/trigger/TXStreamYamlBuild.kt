/*
 *
 *  * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *  *
 *  * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *  *
 *  * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *  *
 *  * A copy of the MIT License is included in this file.
 *  *
 *  *
 *  * Terms of the MIT License:
 *  * ---------------------------------------------------
 *  * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 *  * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 *  * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 *  * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *  *
 *  * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 *  * the Software.
 *  *
 *  * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package com.tencent.devops.stream.trigger

import com.devops.process.yaml.modelCreate.TXModelCreate
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.stream.config.StreamGitConfig
import com.tencent.devops.stream.dao.GitPipelineResourceDao
import com.tencent.devops.stream.trigger.parsers.StreamTriggerCache
import com.tencent.devops.stream.trigger.service.DeleteEventService
import com.tencent.devops.stream.trigger.service.RepoTriggerEventService
import com.tencent.devops.stream.trigger.timer.service.StreamTimerService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TXStreamYamlBuild @Autowired constructor(
    dslContext: DSLContext,
    streamYamlBaseBuild: StreamYamlBaseBuild,
    redisOperation: RedisOperation,
    streamTimerService: StreamTimerService,
    deleteEventService: DeleteEventService,
    streamTriggerCache: StreamTriggerCache,
    repoTriggerEventService: RepoTriggerEventService,
    streamGitConfig: StreamGitConfig,
    pipelineResourceDao: GitPipelineResourceDao,
    modelCreate: TXModelCreate
) : StreamYamlBuild(
    dslContext = dslContext,
    streamYamlBaseBuild = streamYamlBaseBuild,
    redisOperation = redisOperation,
    streamTimerService = streamTimerService,
    deleteEventService = deleteEventService,
    streamTriggerCache = streamTriggerCache,
    repoTriggerEventService = repoTriggerEventService,
    streamGitConfig = streamGitConfig,
    pipelineResourceDao = pipelineResourceDao,
    modelCreate = modelCreate
)
