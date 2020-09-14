/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.dispatch.service

import com.tencent.devops.common.api.util.timestamp
import com.tencent.devops.dispatch.dao.JobQuotaProjectDao
import com.tencent.devops.dispatch.dao.JobQuotaSystemDao
import com.tencent.devops.dispatch.pojo.JobQuotaProject
import com.tencent.devops.dispatch.pojo.JobQuotaSystem
import com.tencent.devops.dispatch.pojo.enums.JobQuotaVmType
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class JobQuotaManagerService @Autowired constructor(
    private val jobQuotaProjectDao: JobQuotaProjectDao,
    private val jobQuotaSystemDao: JobQuotaSystemDao,
    private val dslContext: DSLContext
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
            result.add(jobQuotaSystemDao.convert(JobQuotaVmType.parse(it!!.vmType)!!, it))
        }
        return result
    }

    /**
     * 获取job的某类构件机配额，如果没有，则取系统默认值
     */
    fun getProjectQuota(projectId: String, jobQuotaVmType: JobQuotaVmType): JobQuotaProject {
        val now = System.currentTimeMillis()
        val record = jobQuotaProjectDao.get(dslContext, projectId, jobQuotaVmType)
        if (null == record) {
            val systemDefault = getSystemQuota(jobQuotaVmType)
            return JobQuotaProject(
                projectId = projectId,
                vmType = jobQuotaVmType,
                runningJobMax = if (isGitCiProject(projectId)) { systemDefault.runningJobMaxGitCiProject } else { systemDefault.runningJobMaxProject },
                runningTimeJobMax = if (isGitCiProject(projectId)) { systemDefault.runningTimeJobMaxGitCi } else { systemDefault.runningTimeJobMax },
                runningTimeProjectMax = if (isGitCiProject(projectId)) { systemDefault.runningTimeJobMaxProjectGitCi } else { systemDefault.runningTimeJobMaxProject },
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
        val record = jobQuotaProjectDao.get(dslContext, projectId, jobQuota.vmType)
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
    fun deleteProjectQuota(projectId: String, jobQuotaVmType: JobQuotaVmType): Boolean {
        jobQuotaProjectDao.delete(dslContext, projectId, jobQuotaVmType)
        return true
    }

    /**
     * 更新项目配额
     */
    fun updateProjectQuota(projectId: String, jobQuotaVmType: JobQuotaVmType, jobQuota: JobQuotaProject): Boolean {
        return jobQuotaProjectDao.update(dslContext, projectId, jobQuotaVmType, jobQuota)
    }

    /**
     * 获取系统默认配额-批量
     */
    fun getSystemQuota(): List<JobQuotaSystem> {
        val records = jobQuotaSystemDao.list(dslContext)
        if (records.isEmpty()) {
            return emptyList()
        }
        val result = mutableListOf<JobQuotaSystem>()
        records.filterNotNull().forEach {
            val vmType = JobQuotaVmType.parse(it.vmType)
            if (vmType != null) {
                result.add(jobQuotaSystemDao.convert(vmType, it))
            }
        }
        return result
    }

    /**
     * 获取系统默认配额-批量
     */
    fun getSystemQuota(jobQuotaVmType: JobQuotaVmType): JobQuotaSystem {
        val now = System.currentTimeMillis()
        val record = jobQuotaSystemDao.get(dslContext, jobQuotaVmType)
            ?: return JobQuotaSystem(
                vmType = jobQuotaVmType,
                runningJobMaxSystem = 500, // default value
                runningJobMaxProject = 50, // default value
                runningTimeJobMax = 8,
                runningTimeJobMaxProject = 40,
                runningJobMaxGitCiSystem = when (jobQuotaVmType) {
                    JobQuotaVmType.DOCKER_DEVCLOUD -> 300 // devcloud默认300
                    JobQuotaVmType.MACOS_DEVCLOUD -> 100 // macos默认100
                    else -> 100 // 其他情况默认300，暂时不存在
                },
                runningJobMaxGitCiProject = 10,
                runningTimeJobMaxGitCi = 8,
                runningTimeJobMaxProjectGitCi = 40,
                projectRunningJobThreshold = 80,
                projectRunningTimeThreshold = 80,
                systemRunningJobThreshold = 80,
                createdTime = now,
                updatedTime = now,
                operator = ""
            )

        return jobQuotaSystemDao.convert(jobQuotaVmType, record)
    }

    /**
     * 添加系统配额
     */
    fun addSystemQuota(jobQuota: JobQuotaSystem): Boolean {
        val record = jobQuotaSystemDao.get(dslContext, jobQuota.vmType)
        if (null == record) {
            jobQuotaSystemDao.add(dslContext, jobQuota)
        } else {
            jobQuotaSystemDao.update(dslContext, jobQuota.vmType, jobQuota)
        }
        return true
    }

    /**
     * 删除系统配额
     */
    fun deleteSystemQuota(jobQuotaVmType: JobQuotaVmType): Boolean {
        jobQuotaSystemDao.delete(dslContext, jobQuotaVmType)
        return true
    }

    /**
     * 更新系统配额
     */
    fun updateSystemQuota(jobQuotaVmType: JobQuotaVmType, jobQuota: JobQuotaSystem): Boolean {
        return jobQuotaSystemDao.update(dslContext, jobQuotaVmType, jobQuota)
    }

    private fun isGitCiProject(projectId: String): Boolean {
        return projectId.startsWith("git_")
    }
}