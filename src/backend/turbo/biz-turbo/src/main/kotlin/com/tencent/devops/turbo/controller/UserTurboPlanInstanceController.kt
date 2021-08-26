package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.turbo.api.IUserTurboPlanInstanceController
import com.tencent.devops.turbo.service.TurboPlanInstanceService
import com.tencent.devops.turbo.vo.TurboPlanInstanceVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@Suppress("MaxLineLength")
@RestController
class UserTurboPlanInstanceController @Autowired constructor(
    private val turboPlanInstanceService: TurboPlanInstanceService
) : IUserTurboPlanInstanceController {

    override fun getTurboPlanInstanceList(
        turboPlanId: String,
        pageNum: Int?,
        pageSize: Int?,
        sortField: String?,
        sortType: String?
    ): Response<Page<TurboPlanInstanceVO>> {
        return Response.success(turboPlanInstanceService.getTurboPlanInstanceList(turboPlanId, pageNum, pageSize, sortField, sortType))
    }
}
