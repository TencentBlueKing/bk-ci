package com.tencent.devops.plugin.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.plugin.api.UserGcloudConfResource
import com.tencent.devops.plugin.pojo.GcloudConfResponse
import com.tencent.devops.plugin.service.gcloud.GcloudConfService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserGcloudConfResourceImpl @Autowired constructor(
    private val gcloudConfService: GcloudConfService
) : UserGcloudConfResource {
    override fun create(userId: String, region: String, address: String, fileAddress: String, remark: String?): Result<Map<String, Int>> {
        val id = gcloudConfService.createGcloudConf(region, address, fileAddress, userId, remark)
        return Result(mapOf(Pair("id", id)))
    }

    override fun getList(page: Int, pageSize: Int): Result<GcloudConfResponse?> {
        val resultList = gcloudConfService.getList(page, pageSize)
        val resultCount = gcloudConfService.getCount()
        return Result(data = GcloudConfResponse(
                count = resultCount.toString(),
                page = page,
                pageSize = pageSize,
                totalPages = resultCount / pageSize + 1,
                records = resultList
        ))
    }
}