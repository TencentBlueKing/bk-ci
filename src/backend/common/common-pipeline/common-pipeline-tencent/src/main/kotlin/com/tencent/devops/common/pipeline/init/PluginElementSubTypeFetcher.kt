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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.common.pipeline.init

import com.tencent.devops.common.pipeline.ElementSubTypeFetcher
import com.tencent.devops.common.pipeline.pojo.element.Element
import com.tencent.devops.common.pipeline.element.*

class PluginElementSubTypeFetcher : ElementSubTypeFetcher {

    override fun jsonSubTypes(): Map<String, Class<out Element>> {
        return mapOf(
            SendRTXNotifyElement.classType to SendRTXNotifyElement::class.java,
            SendEmailNotifyElement.classType to SendEmailNotifyElement::class.java,
            SendSmsNotifyElement.classType to SendSmsNotifyElement::class.java,
            SendWechatNotifyElement.classType to SendWechatNotifyElement::class.java,
            ExperienceElement.classType to ExperienceElement::class.java,
            GcloudElement.classType to GcloudElement::class.java,
            JobExecuteTaskExtElement.classType to JobExecuteTaskExtElement::class.java,
            ComDistributionElement.classType to ComDistributionElement::class.java,
            JobDevOpsFastPushFileElement.classType to JobDevOpsFastPushFileElement::class.java,
            JobDevOpsFastExecuteScriptElement.classType to JobDevOpsFastExecuteScriptElement::class.java,
            JobDevOpsExecuteTaskExtElement.classType to JobDevOpsExecuteTaskExtElement::class.java,
            CosCdnDistributionElementDev.classType to CosCdnDistributionElementDev::class.java,
            CosCdnDistributionElement.classType to CosCdnDistributionElement::class.java,
            SecurityElement.classType to SecurityElement::class.java,
            TclsAddVersionElement.classType to TclsAddVersionElement::class.java,
            GseKitProcRunCmdElementDev.classType to GseKitProcRunCmdElementDev::class.java,
            GseKitProcRunCmdElementProd.classType to GseKitProcRunCmdElementProd::class.java,
            CloudStoneElement.classType to CloudStoneElement::class.java,
            JinGangAppElement.classType to JinGangAppElement::class.java,
            BcsContainerOpElement.classType to BcsContainerOpElement::class.java,
            BcsContainerOpByNameElement.classType to BcsContainerOpByNameElement::class.java,
            GcloudAppElement.classType to GcloudAppElement::class.java,
            AcrossProjectDistributionElement.classType to AcrossProjectDistributionElement::class.java,
            ReportArchiveServiceElement.classType to ReportArchiveServiceElement::class.java,
            TcmElement.classType to TcmElement::class.java,
            ZhiyunPushFileElement.classType to ZhiyunPushFileElement::class.java,
            ZhiyunUpdateAsyncEXElement.classType to ZhiyunUpdateAsyncEXElement::class.java,
            ZhiyunInstanceMaintenanceElement.classType to ZhiyunInstanceMaintenanceElement::class.java,
            MigCDNPushFileElement.classType to MigCDNPushFileElement::class.java,
            LunaPushFileElement.classType to LunaPushFileElement::class.java,
            GcloudUpdateVersionElement.classType to GcloudUpdateVersionElement::class.java,
            OpenStatePushFileElement.classType to OpenStatePushFileElement::class.java,
            GcloudPufferElement.classType to GcloudPufferElement::class.java,
            GcloudDeleteVersionElement.classType to GcloudDeleteVersionElement::class.java,
            GcloudPufferDeleteVersionElement.classType to GcloudPufferDeleteVersionElement::class.java,
            GcloudPufferUpdateVersionElement.classType to GcloudPufferUpdateVersionElement::class.java,
            WetestElement.classType to WetestElement::class.java,
            PushImageToThirdRepoElement.classType to PushImageToThirdRepoElement::class.java,
            JobCloudsFastExecuteScriptElement.classType to JobCloudsFastExecuteScriptElement::class.java,
            JobCloudsFastPushElement.classType to JobCloudsFastPushElement::class.java,
            SpmDistributionElement.classType to SpmDistributionElement::class.java,
            ItestProcessCreateElement.classType to ItestProcessCreateElement::class.java,
            ItestReviewCreateElement.classType to ItestReviewCreateElement::class.java,
            ItestTaskCreateElement.classType to ItestTaskCreateElement::class.java
        )
    }
}