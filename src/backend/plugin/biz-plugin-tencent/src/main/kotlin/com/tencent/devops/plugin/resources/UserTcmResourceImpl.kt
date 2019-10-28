package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserTcmResource
import com.tencent.devops.plugin.pojo.ParametersInfo
import com.tencent.devops.plugin.pojo.tcm.TcmApp
import com.tencent.devops.plugin.pojo.tcm.TcmTemplate
import com.tencent.devops.plugin.pojo.tcm.TcmTemplateParam
import com.tencent.devops.plugin.service.TcmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserTcmResourceImpl @Autowired constructor(private val tcmService: TcmService) : UserTcmResource {
    override fun getParamsList(userId: String, appId: String, tcmAppId: String, templateId: String): Result<List<ParametersInfo>> {
        return Result(tcmService.getParamsList(userId, appId, tcmAppId, templateId))
    }

    override fun getApps(userId: String): Result<List<TcmApp>> {
        return Result(tcmService.getApps(userId))
    }

    override fun getTemplates(userId: String, ccid: String, tcmAppId: String): Result<List<TcmTemplate>> {
        return Result(tcmService.getTemplates(userId, ccid, tcmAppId))
    }

    override fun getTemplateInfo(userId: String, ccid: String, tcmAppId: String, templateId: String): Result<List<TcmTemplateParam>> {
        return Result(tcmService.getTemplateInfo(userId, ccid, tcmAppId, templateId))
    }
}