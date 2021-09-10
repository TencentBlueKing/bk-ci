package com.tencent.devops.turbo.controller

import com.tencent.devops.api.pojo.Response
import com.tencent.devops.turbo.api.IUserTurboDaySummaryController
import com.tencent.devops.turbo.service.TurboSummaryService
import com.tencent.devops.turbo.vo.TurboOverviewStatRowVO
import com.tencent.devops.turbo.vo.TurboOverviewTrendVO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RestController

@RestController
class UserTurboDaySummaryController @Autowired constructor(
    private val turboSummaryService: TurboSummaryService
) : IUserTurboDaySummaryController {
    /**
     * 获取总览页面统计栏数据
     */
    override fun getOverviewStatRowData(projectId: String): Response<TurboOverviewStatRowVO> {
        return Response.success(turboSummaryService.getOverviewStatRowData(projectId))
    }

    /**
     * 获取总览页面耗时分布趋势图数据
     */
    override fun getTimeConsumingTrendData(
        dateType: String,
        projectId: String
    ): Response<List<TurboOverviewTrendVO>> {
        return Response.success(turboSummaryService.getTimeConsumingTrendData(dateType, projectId))
    }

    /**
     * 获取总览页面编译次数趋势图数据
     */
    override fun getCompileNumberTrendData(
        dateType: String,
        projectId: String
    ): Response<List<TurboOverviewTrendVO>> {
        return Response.success(turboSummaryService.getCompileNumberTrendData(dateType, projectId))
    }
}
