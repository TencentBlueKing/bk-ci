package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceDownloadResource
import com.tencent.devops.experience.pojo.download.CheckVersionParam
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class AppExperienceDownloadResourceImpl @Autowired constructor(

):AppExperienceDownloadResource{
    override fun checkVersion(userId: String, platform: Int, params: List<CheckVersionParam>) {
        TODO("内部版本和外部版本都要比对")
        //拿到当前用户能够体验的recordId
        //然后where name=xxx and createTime>""
    }
}
