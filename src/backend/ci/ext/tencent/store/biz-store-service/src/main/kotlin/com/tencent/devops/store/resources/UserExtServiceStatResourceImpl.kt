package com.tencent.devops.store.resources

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.store.api.UserExtServiceStatResource
import com.tencent.devops.store.pojo.ExtServiceInstallTrendReq
import com.tencent.devops.store.pojo.ExtServiceStatistic
import com.tencent.devops.store.service.StatisticService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserExtServiceStatResourceImpl @Autowired constructor(
    val statisticService: StatisticService
) : UserExtServiceStatResource {
    override fun getServiceStat(serviceCode: String): Result<ExtServiceStatistic> {
        return statisticService.getStatisticByServiceCode(serviceCode)
    }

    override fun getInstallTrend(serviceCode: String, days: Int): Result<List<ExtServiceInstallTrendReq>> {
        return statisticService.getInstallTrend(serviceCode, days.toLong())
    }
}