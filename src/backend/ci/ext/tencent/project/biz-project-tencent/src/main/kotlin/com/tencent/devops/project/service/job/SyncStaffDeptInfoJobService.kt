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

package com.tencent.devops.project.service.job

import com.tencent.devops.project.dao.ProjectDao
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * 同步项目创建人机构信息
 * author: carlyin
 * since: 2018-12-09
 */
@Service
class SyncStaffDeptInfoJobService @Autowired constructor(
    private val dslContext: DSLContext,
    private val projectDao: ProjectDao
) {
    private val logger = LoggerFactory.getLogger(SyncStaffDeptInfoJobService::class.java)

    // @Scheduled(cron = "0 0 2 * * ?") // 每天早上两点执行一次（历史项目的创建人组织架构信息已经同步完，历史项目的创建人组织架构信息应保持创建项目时的信息，需停掉该定时任务）
    // TODO:已经同步完了，是不是就可以不要这个类了
    @Throws(Exception::class)
    fun syncProjectInfo() {
//        val limit = 10
//        val totalCount = projectDao.getProjectCount(dslContext, null, null, null, null, null, null, null, false, null) // 项目总数
//        logger.info("the totalCount is ：$totalCount,limit is :$limit")
//        var i = 0
//        while (i <= totalCount) {
//            val projectRecords = projectDao.getSyncProjectList(dslContext, i, limit) // 分批次查询数据库里面的项目信息
//            val creatorList = mutableListOf<QueryStaffDeptRequest>()
//            projectRecords.forEach {
//                creatorList.add(QueryStaffDeptRequest(it.creator, ""))
//            }
//            val callables: List<Callable<UserDeptDetail?>> =
//                    creatorList.map {
//                        AsyncGetStaffDeptInfoService(it.userId, it.bk_ticket)
//                    }
//                            .map {
//                                Callable {
//                                    it.content
//                                }
//                            }
//            val executor = Executors.newWorkStealingPool() // 根据计算机核数创建线程池
//            val depts = executor.invokeAll(callables).map {
//                it.get()
//            }
//            logger.info("the depts is ：$depts")
//            val dataList = mutableListOf<UpdateStaffDeptRequest>()
//            for (i in depts.indices) {
//                val projectData = projectRecords[i]
//                val dept = depts[i]
//                if (null != dept)
//                dataList.add(UpdateStaffDeptRequest(projectData.id,
//                    dept.bg_name,
//                    dept.bg_id.toInt(),
//                    dept.dept_name,
//                    dept.dept_id.toInt(),
//                    dept.center_name,
//                    dept.dept_id.toInt())
//                )
//            }
//            // 批量更新数据库的项目创建人机构信息
//            projectDao.batchUpdateCreaterDept(dslContext, dataList)
//            i += limit
//        }
    }
}
