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

package com.tencent.devops.monitoring.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.common.web.utils.I18nUtil
import com.tencent.devops.monitoring.api.service.SlaMonitorResource
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.BK_ILLEGAL_ENTERPRISE_GROUP_ID
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.BK_ILLEGAL_TIMESTAMP_RANGE
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.BK_INCORRECT_PASSWORD
import com.tencent.devops.monitoring.constant.MonitoringMessageCode.BK_SENT_SUCCESSFULLY
import com.tencent.devops.monitoring.job.MonitorNotifyJob
import com.tencent.devops.monitoring.pojo.SlaCodeccResponseData
import com.tencent.devops.monitoring.services.SlaMonitorService
import org.apache.commons.lang3.math.NumberUtils
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class SlaMonitorResourceImpl @Autowired constructor(
    private val slaMonitorService: SlaMonitorService,
    private val monitorNotifyJob: MonitorNotifyJob
) : SlaMonitorResource {
    override fun codeccQuery(bgId: String, startTime: Long, endTime: Long): Result<SlaCodeccResponseData> {
        if (startTime > System.currentTimeMillis() || startTime > endTime) {
            logger.error("wrong timestamp , startTime:$startTime , endTime:$endTime")
            return Result(-1,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_ILLEGAL_TIMESTAMP_RANGE
                )
                )
        }

        if (!NumberUtils.isParsable(bgId)) {
            logger.error("wrong bgId , bgId:$bgId")
            return Result(-2,
                I18nUtil.getCodeLanMessage(
                    messageCode = BK_ILLEGAL_ENTERPRISE_GROUP_ID
                )
                )
        }

        return Result(slaMonitorService.codeccQuery(bgId, startTime, endTime))
    }

    override fun emailTest(pwd: String): Result<String> {
        if (pwd != "234lsd&QWfjno1!") return Result(
            I18nUtil.getCodeLanMessage(
                messageCode = BK_INCORRECT_PASSWORD
            )
        )
        monitorNotifyJob.notifyDaily()
        return Result(
            I18nUtil.getCodeLanMessage(
                messageCode = BK_SENT_SUCCESSFULLY
            )
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SlaMonitorResourceImpl::class.java)
    }
}
