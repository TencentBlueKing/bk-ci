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
import com.tencent.devops.common.pipeline.element.CloudStoneElement
import com.tencent.devops.common.pipeline.element.ComDistributionElement
import com.tencent.devops.common.pipeline.element.CosCdnDistributionElement
import com.tencent.devops.common.pipeline.element.CosCdnDistributionElementDev
import com.tencent.devops.common.pipeline.element.DeployDistributionElement
import com.tencent.devops.common.pipeline.element.ExperienceElement
import com.tencent.devops.common.pipeline.element.FileArchiveElement
import com.tencent.devops.common.pipeline.element.GcloudAppElement
import com.tencent.devops.common.pipeline.element.GcloudDeleteVersionElement
import com.tencent.devops.common.pipeline.element.GcloudElement
import com.tencent.devops.common.pipeline.element.GcloudPufferDeleteVersionElement
import com.tencent.devops.common.pipeline.element.GcloudPufferElement
import com.tencent.devops.common.pipeline.element.GcloudPufferUpdateVersionElement
import com.tencent.devops.common.pipeline.element.GcloudUpdateVersionElement
import com.tencent.devops.common.pipeline.element.GitCommentCheckElement
import com.tencent.devops.common.pipeline.element.GseKitProcRunCmdElementDev
import com.tencent.devops.common.pipeline.element.GseKitProcRunCmdElementProd
import com.tencent.devops.common.pipeline.element.IosCertInstallElement
import com.tencent.devops.common.pipeline.element.IosEnterpriseSignElement
import com.tencent.devops.common.pipeline.element.IosSJTYSignElement
import com.tencent.devops.common.pipeline.element.ItestProcessCreateElement
import com.tencent.devops.common.pipeline.element.ItestReviewCreateElement
import com.tencent.devops.common.pipeline.element.ItestTaskCreateElement
import com.tencent.devops.common.pipeline.element.JinGangAppElement
import com.tencent.devops.common.pipeline.element.JobCloudsFastExecuteScriptElement
import com.tencent.devops.common.pipeline.element.JobCloudsFastPushElement
import com.tencent.devops.common.pipeline.element.JobDevOpsExecuteTaskExtElement
import com.tencent.devops.common.pipeline.element.JobDevOpsFastExecuteScriptElement
import com.tencent.devops.common.pipeline.element.JobDevOpsFastPushFileElement
import com.tencent.devops.common.pipeline.element.JobExecuteTaskExtElement
import com.tencent.devops.common.pipeline.element.KtlintStyleElement
import com.tencent.devops.common.pipeline.element.LunaPushFileElement
import com.tencent.devops.common.pipeline.element.MetaFileScanElement
import com.tencent.devops.common.pipeline.element.MigCDNPushFileElement
import com.tencent.devops.common.pipeline.element.OpenStatePushFileElement
import com.tencent.devops.common.pipeline.element.PushImageToThirdRepoElement
import com.tencent.devops.common.pipeline.element.ReportArchiveServiceElement
import com.tencent.devops.common.pipeline.element.RqdElement
import com.tencent.devops.common.pipeline.element.SecurityElement
import com.tencent.devops.common.pipeline.element.SendEmailNotifyElement
import com.tencent.devops.common.pipeline.element.SendRTXNotifyElement
import com.tencent.devops.common.pipeline.element.SendSmsNotifyElement
import com.tencent.devops.common.pipeline.element.SendWechatNotifyElement
import com.tencent.devops.common.pipeline.element.SensitiveScanElement
import com.tencent.devops.common.pipeline.element.SpmDistributionElement
import com.tencent.devops.common.pipeline.element.TclsAddVersionElement
import com.tencent.devops.common.pipeline.element.TcmElement
import com.tencent.devops.common.pipeline.element.Unity3dBuildElement
import com.tencent.devops.common.pipeline.element.WetestElement
import com.tencent.devops.common.pipeline.element.XcodeBuildElement
import com.tencent.devops.common.pipeline.element.XcodeBuildElement2
import com.tencent.devops.common.pipeline.element.ZhiyunInstanceMaintenanceElement
import com.tencent.devops.common.pipeline.element.ZhiyunPushFileElement
import com.tencent.devops.common.pipeline.element.ZhiyunUpdateAsyncEXElement
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
            ItestTaskCreateElement.classType to ItestTaskCreateElement::class.java,
            BuglyElement.classType to BuglyElement::class.java,
            RqdElement.classType to RqdElement::class.java,
            IosCertInstallElement.classType to IosCertInstallElement::class.java,
            AndroidCertInstallElement.classType to AndroidCertInstallElement::class.java,
            IosEnterpriseSignElement.classType to IosEnterpriseSignElement::class.java,
            IosSJTYSignElement.classType to IosSJTYSignElement::class.java,
            KtlintStyleElement.classType to KtlintStyleElement::class.java,
            FileArchiveElement.classType to FileArchiveElement::class.java,
            GitCommentCheckElement.classType to GitCommentCheckElement::class.java,
            AtomBuildArchiveElement.classType to AtomBuildArchiveElement::class.java,
            DeployDistributionElement.classType to DeployDistributionElement::class.java,
            FileArchiveElement.classType to FileArchiveElement::class.java,
            MetaFileScanElement.classType to MetaFileScanElement::class.java,
            Unity3dBuildElement.classType to Unity3dBuildElement::class.java,
            XcodeBuildElement.classType to XcodeBuildElement::class.java,
            XcodeBuildElement2.classType to XcodeBuildElement2::class.java,
            SensitiveScanElement.classType to SensitiveScanElement::class.java,
            ExtServiceBuildDeployElement.classType to ExtServiceBuildDeployElement::class.java,
            StoreCodeccValidateElement.classType to StoreCodeccValidateElement::class.java,
            AtomRunEnvPrepareElement.classType to AtomRunEnvPrepareElement::class.java
        )
    }
}
