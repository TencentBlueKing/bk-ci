package com.tencent.devops.support.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.support.api.op.OpAppVersionResource
import com.tencent.devops.support.model.app.AppVersionRequest
import com.tencent.devops.support.model.app.pojo.AppVersion
import com.tencent.devops.support.services.AppVersionService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpAppVersionResourceImpl @Autowired constructor(private val appVersionService: AppVersionService) : OpAppVersionResource {
    override fun updateAppVersion(appVersionId: Long, appVersionRequest: AppVersionRequest): Result<Int> {
        return Result(data = appVersionService.setAppVersion(appVersionId, appVersionRequest))
    }

    override fun addAppVersion(appVersionRequest: AppVersionRequest): Result<Int> {
        return Result(data = appVersionService.setAppVersion(null, appVersionRequest))
    }

    override fun getAppVersion(appVersionId: Long): Result<AppVersion?> {
        return Result(data = appVersionService.getAppVersion(appVersionId))
    }

    override fun getAllAppVersion(): Result<List<AppVersion>> {
        return Result(data = appVersionService.getAllAppVersion())
    }

    override fun deleteAppVersion(appVersionId: Long): Result<Int> {
        return Result(data = appVersionService.deleteAppVersion(appVersionId))
    }

    override fun addAppVersions(appVersionRequests: List<AppVersionRequest>): Result<Int> {
        var result = 1
        appVersionRequests.forEach {
            var insertResult = appVersionService.setAppVersion(null, it)
            if (insertResult != 1) {
                result = insertResult
            }
        }
        return Result(data = result)
    }
}