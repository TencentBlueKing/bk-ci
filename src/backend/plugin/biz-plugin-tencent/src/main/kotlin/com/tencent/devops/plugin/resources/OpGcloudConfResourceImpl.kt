package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.OpGcloudConfResource
import com.tencent.devops.plugin.pojo.GcloudConfReq
import com.tencent.devops.plugin.pojo.GcloudConfResponse
import com.tencent.devops.plugin.service.gcloud.GcloudConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpGcloudConfResourceImpl @Autowired constructor(
    private val gcloudConfService: GcloudConfService
) : OpGcloudConfResource {
    override fun create(userId: String, gcloudConfReq: GcloudConfReq): Result<Map<String, Int>> {
        with(gcloudConfReq) {
            return Result(gcloudConfService.createGcloudConf(region, address, fileAddress, userId, remark))
        }
    }

    override fun edit(userId: String, gcloudConfReq: GcloudConfReq): Result<Int> {
        with(gcloudConfReq) {
            return Result(gcloudConfService.updateGcloudConf(id, region, address, fileAddress, userId, remark))
        }
    }

    override fun delete(userId: String, confId: Int): Result<Int> {
        return Result(gcloudConfService.deleteGcloudConf(confId))
    }

    override fun getList(page: Int?, pageSize: Int?): Result<GcloudConfResponse> {
        val resultList = gcloudConfService.getList(page ?: 1, pageSize ?: 10)
        val resultCount = gcloudConfService.getCount()
        return Result(data = GcloudConfResponse(
                count = resultCount.toString(),
                page = page ?: 1,
                pageSize = pageSize ?: 10,
                totalPages = resultCount / (pageSize ?: 10) + 1,
                records = resultList
        ))
    }
}