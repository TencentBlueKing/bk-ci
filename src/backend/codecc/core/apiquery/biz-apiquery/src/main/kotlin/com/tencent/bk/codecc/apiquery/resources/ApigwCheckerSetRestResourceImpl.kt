package com.tencent.bk.codecc.apiquery.resources

import com.tencent.bk.codecc.defect.api.ServiceCheckerSetRestResource
import com.tencent.bk.codecc.defect.vo.CheckerSetListQueryReq
import com.tencent.bk.codecc.apiquery.api.ApigwCheckerSetRestResource
import com.tencent.bk.codecc.apiquery.defect.model.CheckerSetModel
import com.tencent.bk.codecc.apiquery.service.ICheckerService
import com.tencent.devops.common.api.checkerset.CheckerSetVO
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwCheckerSetRestResourceImpl @Autowired constructor(
    private val client: Client,
    private val checkerService: ICheckerService
) : ApigwCheckerSetRestResource {
    override fun getCheckerSets(queryCheckerSetReq: CheckerSetListQueryReq): Result<List<CheckerSetVO>> {
        return client.getWithoutRetry(ServiceCheckerSetRestResource::class).getCheckerSets(queryCheckerSetReq)
    }
}
