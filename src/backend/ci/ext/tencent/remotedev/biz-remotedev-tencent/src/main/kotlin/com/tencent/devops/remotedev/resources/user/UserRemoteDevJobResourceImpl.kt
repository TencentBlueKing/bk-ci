package com.tencent.devops.remotedev.resources.user

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.remotedev.api.user.UserRemoteDevJobResource
import com.tencent.devops.remotedev.common.exception.ErrorCodeEnum
import com.tencent.devops.remotedev.pojo.job.CronJob
import com.tencent.devops.remotedev.pojo.job.CronJobSearchParam
import com.tencent.devops.remotedev.pojo.job.JobCreateData
import com.tencent.devops.remotedev.pojo.job.JobDetail
import com.tencent.devops.remotedev.pojo.job.JobRecord
import com.tencent.devops.remotedev.pojo.job.JobRecordSearchParam
import com.tencent.devops.remotedev.pojo.job.JobSchema
import com.tencent.devops.remotedev.pojo.job.JobSchemaShort
import com.tencent.devops.remotedev.pojo.job.JobScope
import com.tencent.devops.remotedev.pojo.job.JobType
import com.tencent.devops.remotedev.service.PermissionService
import com.tencent.devops.remotedev.service.job.RemoteDevJobService
import com.tencent.devops.remotedev.service.job.RemoteDevSchemaService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserRemoteDevJobResourceImpl @Autowired constructor(
    private val remoteDevSchemaService: RemoteDevSchemaService,
    private val remoteDevJobService: RemoteDevJobService,
    private val permissionService: PermissionService
) : UserRemoteDevJobResource {
    override fun fetchJobSchemaList(userId: String, type: JobType, projectId: String): Result<List<JobSchemaShort>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(remoteDevSchemaService.getJobIdAndNames(type))
    }

    override fun fetchMachineTypeList(userId: String, projectId: String): Result<Set<String>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(remoteDevJobService.getMachineTypes(projectId))
    }

    override fun fetchOwners(userId: String, projectId: String): Result<Set<String>> {
        permissionService.checkUserManager(userId, projectId)
        return Result(remoteDevJobService.getOwners(projectId))
    }

    override fun getJobSchema(userId: String, schemaId: String, projectId: String): Result<JobSchema?> {
        permissionService.checkUserManager(userId, projectId)
        return Result(remoteDevSchemaService.getSchema(schemaId, false))
    }

    override fun fetchJobRecord(userId: String, search: JobRecordSearchParam): Result<Page<JobRecord>> {
        permissionService.checkUserManager(userId, search.projectId)
        return Result(remoteDevJobService.fetchJobRecord(search))
    }

    override fun fetchCronList(userId: String, search: CronJobSearchParam): Result<Page<CronJob>> {
        permissionService.checkUserManager(userId, search.projectId)
        return Result(remoteDevJobService.fetchCronJob(search))
    }

    override fun recordRerun(userId: String, id: Long, projectId: String): Result<Boolean> {
        permissionService.checkUserManager(userId, projectId)
        remoteDevJobService.recordRerun(userId, id)
        return Result(true)
    }

    override fun fetchJobDetail(userId: String, id: Long, projectId: String): Result<JobDetail?> {
        permissionService.checkUserManager(userId, projectId)
        return Result(remoteDevJobService.fetchJobDetail(id))
    }
}
