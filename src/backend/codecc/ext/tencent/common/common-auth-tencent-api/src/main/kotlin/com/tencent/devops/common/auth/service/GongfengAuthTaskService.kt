package com.tencent.devops.common.auth.service

import com.tencent.devops.common.auth.pojo.GongfengBaseInfo


interface GongfengAuthTaskService {
    /**
     * 获取工蜂项目基本信息
     */
    fun getGongfengProjInfo(
            taskId: Long
    ) : GongfengBaseInfo?

    /**
     * 获取工蜂CI项目基本信息
     * */
    fun getGongfengCIProjInfo(
            gongfengId: Int
    ): GongfengBaseInfo?
}