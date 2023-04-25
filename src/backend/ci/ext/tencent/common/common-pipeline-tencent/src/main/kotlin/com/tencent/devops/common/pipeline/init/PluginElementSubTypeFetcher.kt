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

package com.tencent.devops.common.pipeline.init

import com.tencent.devops.common.pipeline.ElementSubTypeFetcher
import com.tencent.devops.common.pipeline.element.AcrossProjectDistributionElement
import com.tencent.devops.common.pipeline.element.AndroidCertInstallElement
import com.tencent.devops.common.pipeline.element.BcsContainerOpByNameElement
import com.tencent.devops.common.pipeline.element.BcsContainerOpElement
import com.tencent.devops.common.pipeline.element.BuglyElement
import com.tencent.devops.common.pipeline.element.ExperienceElement
import com.tencent.devops.common.pipeline.element.GcloudPufferElement
import com.tencent.devops.common.pipeline.element.JobDevOpsExecuteTaskExtElement
import com.tencent.devops.common.pipeline.element.JobDevOpsFastExecuteScriptElement
import com.tencent.devops.common.pipeline.element.JobDevOpsFastPushFileElement
import com.tencent.devops.common.pipeline.element.KtlintStyleElement
import com.tencent.devops.common.pipeline.element.PushImageToThirdRepoElement
import com.tencent.devops.common.pipeline.element.SendEmailNotifyElement
import com.tencent.devops.common.pipeline.element.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.element.SendSmsNotifyElement
import com.tencent.devops.common.pipeline.element.SendWechatNotifyElement
import com.tencent.devops.common.pipeline.element.SensitiveScanElement
import com.tencent.devops.common.pipeline.element.ZhiyunPushFileElement
import com.tencent.devops.common.pipeline.element.store.AtomRunEnvPrepareElement
import com.tencent.devops.common.pipeline.element.store.ExtServiceBuildDeployElement
import com.tencent.devops.common.pipeline.element.store.StoreCodeccValidateElement
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.agent.LinuxPaasCodeCCScriptElement
import com.tencent.devops.common.pipeline.pojo.element.market.AtomBuildArchiveElement

class PluginElementSubTypeFetcher : ElementSubTypeFetcher {

    override fun jsonSubTypes(): Map<String, Class<out Element>> {
        return mapOf(
            LinuxCodeCCScriptElement.classType to LinuxCodeCCScriptElement::class.java,
            LinuxPaasCodeCCScriptElement.classType to LinuxPaasCodeCCScriptElement::class.java,
            SendRTXNotifyElement.classType to SendRTXNotifyElement::class.java,
            SendEmailNotifyElement.classType to SendEmailNotifyElement::class.java,
            SendSmsNotifyElement.classType to SendSmsNotifyElement::class.java,
            SendWechatNotifyElement.classType to SendWechatNotifyElement::class.java,
            ExperienceElement.classType to ExperienceElement::class.java,
            JobDevOpsFastPushFileElement.classType to JobDevOpsFastPushFileElement::class.java,
            JobDevOpsFastExecuteScriptElement.classType to JobDevOpsFastExecuteScriptElement::class.java,
            JobDevOpsExecuteTaskExtElement.classType to JobDevOpsExecuteTaskExtElement::class.java,
            BcsContainerOpElement.classType to BcsContainerOpElement::class.java,
            BcsContainerOpByNameElement.classType to BcsContainerOpByNameElement::class.java,
            AcrossProjectDistributionElement.classType to AcrossProjectDistributionElement::class.java,
            ZhiyunPushFileElement.classType to ZhiyunPushFileElement::class.java,
            GcloudPufferElement.classType to GcloudPufferElement::class.java,
            PushImageToThirdRepoElement.classType to PushImageToThirdRepoElement::class.java,
            BuglyElement.classType to BuglyElement::class.java,
            AndroidCertInstallElement.classType to AndroidCertInstallElement::class.java,
            KtlintStyleElement.classType to KtlintStyleElement::class.java,
            AtomBuildArchiveElement.classType to AtomBuildArchiveElement::class.java,
            SensitiveScanElement.classType to SensitiveScanElement::class.java,
            ExtServiceBuildDeployElement.classType to ExtServiceBuildDeployElement::class.java,
            StoreCodeccValidateElement.classType to StoreCodeccValidateElement::class.java,
            AtomRunEnvPrepareElement.classType to AtomRunEnvPrepareElement::class.java
        )
    }
}
