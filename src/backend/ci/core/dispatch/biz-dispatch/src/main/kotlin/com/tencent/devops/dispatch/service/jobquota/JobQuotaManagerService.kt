/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service.jobquota

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.dispatch.dao.JobQuotaProjectDao
import com.tencent.devops.dispatch.dao.JobQuotaSystemDao
import com.tencent.devops.dispatch.dao.RunningJobsDao
import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.JobQuotaSystem
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service@Suppress("ALL")
class JobQuotaManagerService @Autowired constructor(
    private val dslContext: DSLContext,
    private val runningJobsDao: RunningJobsDao,
    private val jobQuotaSystemDao: JobQuotaSystemDao,
    private val jobQuotaProjectDao: JobQuotaProjectDao
) {
    fun listProjectQuota(projectId: String?): List<JobQuotaProject> {
        val record = jobQuotaProjectDao.list(dslContext, projectId)
        val result = mutableListOf<JobQuotaProject>()
        record.filter { it != null && JobQuotaVmType.parse(it.vmType) != null }.forEach {
            result.add(JobQuotaProject(
                projectId = it!!.projectId,
                vmType = JobQuotaVmType.parse(it.vmType)!!,
                runningJobMax = it.runningJobsMax,
                runningTimeJobMax = it.runningTimeJobMax,
                runningTimeProjectMax = it.runningTimeProjectMax,
                createdTime = it.createdTime.timestamp(),
                updatedTime = it.updatedTime.timestamp(),
                operator = it.operator
            ))
        }
        return result
    }

    fun listSystemQuota(): List<JobQuotaSystem> {
        val record = jobQuotaSystemDao.list(dslContext)
        val result = mutableListOf<JobQuotaSystem>()
        record.filter { it != null && JobQuotaVmType.parse(it.vmType) != null }.forEach {
            if (it == null) {
                return@forEach
            }
            result.add(JobQuotaSystem(
                vmType = JobQuotaVmType.valueOf(it.vmType),
                channelCode = it.channelCode,
                runningJobMaxSystem = it.runningJobsMaxSystem,
                runningJobMaxProject = it.runningJobsMaxProject,
                runningTimeJobMax = it.runningTimeJobMax,
                runningTimeJobMaxProject = it.runningTimeJobMaxProject,
                projectRunningJobThreshold = it.projectRunningJobThreshold,
                projectRunningTimeThreshold = it.projectRunningTimeThreshold,
                systemRunningJobThreshold = it.systemRunningJobThreshold,
                createdTime = it.createTime.toString(),
                updatedTime = it.updateTime.toString(),
                operator = it.operator
            ))
        }
        return result
    }

    /**
     * 获取job的某类构件机配额，如果没有，则取系统默认值
     */
    fun getProjectQuota(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ): JobQuotaProject {
        val now = System.currentTimeMillis()
        val record = jobQuotaProjectDao.get(dslContext, projectId, jobQuotaVmType, channelCode)
        if (null == record) {
            val systemDefault = getSystemQuota(jobQuotaVmType, channelCode)
            return JobQuotaProject(
                projectId = projectId,
                vmType = jobQuotaVmType,
                runningJobMax = systemDefault.runningJobMaxProject,
                runningTimeJobMax = systemDefault.runningTimeJobMax,
                runningTimeProjectMax = systemDefault.runningTimeJobMaxProject,
                createdTime = now,
                updatedTime = now,
                operator = ""
            )
        }

        return JobQuotaProject(
            projectId = projectId,
            vmType = jobQuotaVmType,
            runningJobMax = record.runningJobsMax,
            runningTimeJobMax = record.runningTimeJobMax,
            runningTimeProjectMax = record.runningTimeProjectMax,
            createdTime = record.createdTime.timestamp(),
            updatedTime = record.updatedTime.timestamp(),
            operator = record.operator
        )
    }

    /**
     * 添加项目配额
     */
    fun addProjectQuota(projectId: String, jobQuota: JobQuotaProject): Boolean {
        val record = jobQuotaProjectDao.get(dslContext, projectId, jobQuota.vmType, jobQuota.channelCode)
        if (null == record) {
            jobQuotaProjectDao.add(dslContext, jobQuota)
        } else {
            jobQuotaProjectDao.update(dslContext, projectId, jobQuota.vmType, jobQuota)
        }
        return true
    }

    /**
     * 删除项目配额
     */
    fun deleteProjectQuota(
        projectId: String,
        jobQuotaVmType: JobQuotaVmType,
        channelCode: String = ChannelCode.BS.name
    ): Boolean {
        jobQuotaProjectDao.delete(dslContext, projectId, jobQuotaVmType, channelCode)
        return true
    }

    /**
     * 更新项目配额
     */
    fun updateProjectQuota(projectId: String, jobQuotaVmType: JobQuotaVmType, jobQuota: JobQuotaProject): Boolean {
        return jobQuotaProjectDao.update(dslContext, projectId, jobQuotaVmType, jobQuota)
    }

    /**
     * 获取系统默认配额
     */
    fun getSystemQuota(jobQuotaVmType: JobQuotaVmType, channelCode: String = "BS"): JobQuotaSystem {
        val record = jobQuotaSystemDao.get(dslContext, jobQuotaVmType, channelCode)
            ?: return JobQuotaSystem(
                vmType = jobQuotaVmType,
                channelCode = channelCode,
                runningJobMaxSystem = 5000, // default value
                runningJobMaxProject = 500, // default value
                runningTimeJobMax = 24,
                runningTimeJobMaxProject = 5000,
                projectRunningJobThreshold = 80,
                projectRunningTimeThreshold = 80,
                systemRunningJobThreshold = 80,
                createdTime = LocalDateTime.now().toString(),
                updatedTime = LocalDateTime.now().toString(),
                operator = "admin"
            )

        return JobQuotaSystem(
            vmType = jobQuotaVmType,
            channelCode = channelCode,
            runningJobMaxSystem = record.runningJobsMaxSystem,
            runningJobMaxProject = record.runningJobsMaxProject,
            runningTimeJobMax = record.runningTimeJobMax,
            runningTimeJobMaxProject = record.runningTimeJobMaxProject,
            projectRunningJobThreshold = record.projectRunningJobThreshold,
            projectRunningTimeThreshold = record.projectRunningTimeThreshold,
            systemRunningJobThreshold = record.systemRunningJobThreshold,
            createdTime = record.createTime.toString(),
            updatedTime = record.updateTime.toString(),
            operator = record.operator
        )
    }

    /**
     * 添加系统配额
     */
    fun addSystemQuota(jobQuota: JobQuotaSystem): Boolean {
        val record = jobQuotaSystemDao.get(dslContext, jobQuota.vmType, jobQuota.channelCode)
        if (null == record) {
            jobQuotaSystemDao.add(dslContext, jobQuota)
        } else {
            jobQuotaSystemDao.update(dslContext, jobQuota.channelCode, jobQuota.vmType, jobQuota)
        }
        return true
    }

    /**
     * 删除系统配额
     */
    fun deleteSystemQuota(jobQuotaVmType: JobQuotaVmType, channelCode: String): Boolean {
        jobQuotaSystemDao.delete(dslContext, jobQuotaVmType, channelCode)
        return true
    }

    /**
     * 更新系统配额
     */
    fun updateSystemQuota(jobQuotaVmType: JobQuotaVmType, jobQuota: JobQuotaSystem): Boolean {
        return jobQuotaSystemDao.update(dslContext, jobQuota.channelCode, jobQuotaVmType, jobQuota)
    }

    /**
     * 清理指定项目时间点的配额记录
     */
    fun clearRunningJobs(
        projectId: String,
        vmType: JobQuotaVmType,
        createTime: String,
        channelCode: String = ChannelCode.BS.name
    ) {
        runningJobsDao.clearRunningJobs(
            dslContext = dslContext,
            projectId = projectId,
            vmType = vmType,
            channelCode = channelCode,
            createTime = LocalDateTime.parse(createTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
    }
}
