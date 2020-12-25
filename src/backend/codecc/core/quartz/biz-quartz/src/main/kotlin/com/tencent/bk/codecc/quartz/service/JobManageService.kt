package com.tencent.bk.codecc.quartz.service

import com.tencent.bk.codecc.quartz.model.JobCompensateEntity
import com.tencent.bk.codecc.quartz.model.JobInstanceEntity
import com.tencent.bk.codecc.quartz.pojo.JobExternalDto
import com.tencent.bk.codecc.quartz.pojo.JobInfoVO
import com.tencent.bk.codecc.quartz.pojo.OperationType

interface JobManageService {

    /**
     * 查找全量job
     */
    fun findAllJobs(): List<JobInstanceEntity>

    /**
     * 获取缓存全量job
     */
    fun findCachedJobs(): List<JobInstanceEntity>

    /**
     * 保存job
     */
    fun saveJob(jobExternalDto: JobExternalDto): JobInstanceEntity

    /**
     * 根据job名字查询job信息
     */
    fun findJobsByName(jobNames: List<String>): List<JobInstanceEntity>

    /**
     * 根据job名字查询单个job
     */
    fun findJobByName(jobName: String): JobInstanceEntity

    /**
     * 保存job清单
     */
    fun saveJobs(jobInstances: List<JobInstanceEntity>): List<JobInstanceEntity>

    /**
     * 保存job信息
     */
    fun saveJob(jobInstance: JobInstanceEntity): JobInstanceEntity

    /**
     * 保存job补偿信息
     */
    fun saveJobCompensate(jobCompensateEntity: JobCompensateEntity): JobCompensateEntity

    /**
     * 删除job
     */
    fun deleteJob(jobName: String?)

    /**
     * 从缓存中添加或移除job
     */
    fun addOrRemoveJobToCache(jobInstance: JobInstanceEntity, operType: OperationType)

    /**
     * 转换任务实体类
     */
    fun convert(jobInstanceEntity: JobInstanceEntity): JobInfoVO

    /**
     * 删除所有任务
     */
    fun deleteAllJobs()

    /**
     * 删除缓存任务
     */
    fun deleteAllCacheJobs()

    /**
     * 刷新开源的cron表达式
     */
    fun refreshOpensourceCronExpression(period : Int, startTime : Int)

}