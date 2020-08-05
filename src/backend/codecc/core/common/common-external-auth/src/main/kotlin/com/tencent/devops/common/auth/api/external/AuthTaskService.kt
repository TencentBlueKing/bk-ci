package com.tencent.devops.common.auth.api.external

import com.tencent.devops.common.pojo.GongfengBaseInfo

interface AuthTaskService {
    /**
     * 获取任务创建来源
     */
    fun getTaskCreateFrom(
            taskId: Long
    ): String

    /**
     * 获取工蜂项目基本信息
     */
    fun getGongfengProjInfo(
            taskId: Long
    ) : GongfengBaseInfo?

    /**
     * 获取任务所属流水线ID
     */
    fun getTaskPipelineId(
            taskId: Long
    ): String
}