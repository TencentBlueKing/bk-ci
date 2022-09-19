package com.tencent.devops.turbo.api

import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_PROJECT_ID
import com.tencent.devops.common.util.constants.AUTH_HEADER_DEVOPS_USER_ID
import com.tencent.devops.turbo.pojo.CustomScheduleJobModel
import io.swagger.annotations.Api
import io.swagger.annotations.ApiOperation
import io.swagger.annotations.ApiParam
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam


@Api(tags = ["CUSTOM_SCHEDULE_JOB"], description = "自定义计划任务")
@RequestMapping("/user/customScheduleJob")
interface IUserCustomScheduleTaskController {

    @ApiOperation("新增计划任务")
    @PostMapping(
        "/addScheduleJob",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun addScheduleJob(
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String,
        @ApiParam(value = "项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "编译加速模式信息", required = true)
        @RequestBody
        customScheduleJobModel: CustomScheduleJobModel
    ): Boolean

    @ApiOperation("触发定时任务执行")
    @GetMapping(
        "/trigger",
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun triggerCustomScheduleJob(
        @ApiParam(value = "用户信息", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_USER_ID)
        user: String,
        @ApiParam(value = "项目id", required = true)
        @RequestHeader(AUTH_HEADER_DEVOPS_PROJECT_ID)
        projectId: String,
        @ApiParam(value = "任务名称", required = true)
        @RequestParam(value = "jobName")
        jobName: String
    ): String?
}
