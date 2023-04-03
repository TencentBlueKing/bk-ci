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

package com.tencent.devops.process.extends

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.pipeline.container.Stage
import com.tencent.devops.common.pipeline.container.TriggerContainer
import com.tencent.devops.common.pipeline.pojo.BuildFormProperty
import com.tencent.devops.process.constant.ProcessMessageCode
import com.tencent.devops.process.engine.extend.DefaultModelCheckPlugin
import com.tencent.devops.process.pojo.config.JobCommonSettingConfig
import com.tencent.devops.process.pojo.config.PipelineCommonSettingConfig
import com.tencent.devops.process.pojo.config.StageCommonSettingConfig
import com.tencent.devops.process.pojo.config.TaskCommonSettingConfig
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Service

@Primary
@Service
class TencentModelCheckPlugin constructor(
    override val client: Client,
    override val pipelineCommonSettingConfig: PipelineCommonSettingConfig,
    override val stageCommonSettingConfig: StageCommonSettingConfig,
    override val jobCommonSettingConfig: JobCommonSettingConfig,
    override val taskCommonSettingConfig: TaskCommonSettingConfig
) : DefaultModelCheckPlugin(
    client,
    pipelineCommonSettingConfig,
    stageCommonSettingConfig,
    jobCommonSettingConfig,
    taskCommonSettingConfig
) {

    /**
     * 注：因内部历史原因，存在大量中文命名的变量，所以相对默认检查器，减少针对命名的规范检查
     */
    override fun checkTriggerContainer(trigger: Stage): Map<String, BuildFormProperty> {
        if (trigger.containers.size != 1) {
            throw ErrorCodeException(
                defaultMessage = "流水线只能有一个触发Stage",
                errorCode = ProcessMessageCode.ONLY_ONE_TRIGGER_JOB_IN_PIPELINE
            )
        }

        return ((trigger.containers.getOrNull(0)
            ?: throw ErrorCodeException(
                defaultMessage = "流水线Stage为空",
                errorCode = ProcessMessageCode.ERROR_PIPELINE_MODEL_NEED_JOB
            ))
            as TriggerContainer).params.associateBy { it.id } // 腾讯特供，不校验变量命名规范（有游戏业务大量使用中文命名变量）
    }
}
