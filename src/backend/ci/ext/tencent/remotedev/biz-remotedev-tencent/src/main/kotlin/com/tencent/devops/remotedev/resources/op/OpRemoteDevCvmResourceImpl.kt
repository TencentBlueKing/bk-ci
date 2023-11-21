package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevCvmResource
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmData
import com.tencent.devops.remotedev.pojo.op.RemotedevCvmFetchData
import com.tencent.devops.remotedev.service.RemoteDevCvmService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevCvmResourceImpl @Autowired constructor(
    private val remoteDevCvmService: RemoteDevCvmService
) : OpRemoteDevCvmResource {
    override fun addRemotedevCvm(userId: String, cvmData: List<RemotedevCvmData>): Result<Boolean> {
        return Result(remoteDevCvmService.batchAddCvm(userId, cvmData))
    }

    override fun getRemotedevCvmList(
        userId: String,
        data: RemotedevCvmFetchData
    ): Result<Page<RemotedevCvmData>> {
        return Result(
            remoteDevCvmService.getRemotedevCvmList(
            projectId = data.projectId,
            zone = data.zone,
            ips = data.ips,
            page = data.page,
            pageSize = data.pageSize
        )
        )
    }

    override fun updateRemotedevCvm(userId: String, cvmId: Long, remotedevCvmData: RemotedevCvmData): Result<Boolean> {
        return Result(remoteDevCvmService.updateCvm(cvmId, remotedevCvmData))
    }

    override fun deleteRemotedevCvm(userId: String, cvmId: Long): Result<Boolean> {
        return Result(remoteDevCvmService.deleteCvm(cvmId))
    }
}
