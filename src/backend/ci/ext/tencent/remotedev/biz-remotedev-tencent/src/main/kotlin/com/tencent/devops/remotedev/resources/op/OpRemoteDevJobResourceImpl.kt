package com.tencent.devops.remotedev.resources.op

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.op.OpRemoteDevJobResource
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaCreateData
import com.tencent.devops.remotedev.service.job.RemoteDevSchemaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class OpRemoteDevJobResourceImpl @Autowired constructor(
    private val remoteDevSchemaService: RemoteDevSchemaService
) : OpRemoteDevJobResource {
    override fun createJobSchema(userId: String, data: JobSchemaCreateData): Result<Boolean> {
        remoteDevSchemaService.createOrUpdateSchema(data)
        return Result(true)
    }

    override fun getSchemaList(userId: String): Result<List<JobSchema>> {
        return Result(remoteDevSchemaService.getSchemaList())
    }

    override fun getSchema(userId: String, schemaId: String): Result<JobSchema?> {
        return Result(remoteDevSchemaService.getSchema(schemaId))
    }
}
