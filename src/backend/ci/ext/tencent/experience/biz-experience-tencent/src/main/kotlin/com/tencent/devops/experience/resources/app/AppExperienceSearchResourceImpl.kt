package com.tencent.devops.experience.resources.app

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.experience.api.app.AppExperienceSearchResource
import com.tencent.devops.experience.pojo.search.SearchAppInfoVO
import java.util.Date

@RestResource
class AppExperienceSearchResourceImpl : AppExperienceSearchResource {

    override fun search(userId: String, experienceNames: String): Result<List<SearchAppInfoVO>> {
        //TODO 真实的实现
        val searchAppInfoVO = SearchAppInfoVO(
            experienceHashId = HashUtil.encodeIntId(111),
            experienceName = experienceNames,
            createTime = Date().time,
            size = 15 * 1031467 + 1013L,
            url = "https://v2.bkdevops.qq.com/app/download/devops_app.apk",
            logoUrl = "http://radosgw.open.oa.com/paas_backend/ieod/prod/file/png/random_15663728753195467594717312328557.png"
        )

        return Result(listOf(searchAppInfoVO))
    }
}