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

package com.tencent.devops.environment.dao.devcloud

import com.tencent.devops.common.environment.agent.pojo.devcloud.ContainerType
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskAction
import com.tencent.devops.common.environment.agent.pojo.devcloud.TaskStatus
import com.tencent.devops.model.environment.tables.TDevCloudTask
import com.tencent.devops.model.environment.tables.records.TDevCloudTaskRecord
import org.jooq.DSLContext
import org.jooq.Result
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class DevCloudTaskDao {
    fun insertTask(
        dslContext: DSLContext,
        projectId: String,
        operator: String,
        action: TaskAction,
        status: TaskStatus,
        registryHost: String?,
        registryUser: String?,
        registryPwd: String?,
        containerName: String?,
        containerType: ContainerType?,
        image: String?,
        cpu: Int?,
        memory: String?,
        disk: String?,
        replica: Int?,
        password: String?,
        nodeId: Long?,
        description: String?
    ): Long {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            val now = LocalDateTime.now()
            return dslContext.insertInto(this,
                    PROJECT_ID,
                    OPERATOR,
                    ACTION,
                    STATUS,
                    REGISTRY_HOST,
                    REGISTRY_USER,
                    REGISTRY_PWD,
                    CONTAINER_NAME,
                    CONTAINER_TYPE,
                    IMAGE,
                    CPU,
                    MEMORY,
                    DISK,
                    REPLICA,
                    PASSWORD,
                    NODE_LONG_ID,
                    DESCRIPTION,
                    CREATED_TIME,
                    UPDATE_TIME)
                    .values(
                            projectId,
                            operator,
                            action.name,
                            status.name,
                            registryHost,
                            registryUser,
                            registryPwd,
                            containerName,
                            containerType?.getValue(),
                            image,
                            cpu,
                            memory,
                            disk,
                            replica,
                            password,
                            nodeId,
                            description,
                            now,
                            now
                    )
                    .returning(TASK_ID)
                    .fetchOne()!!.taskId.toLong()
        }
    }

    fun updateTaskStatus(dslContext: DSLContext, taskId: Long, status: TaskStatus): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(STATUS, status.name)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun updateTaskStatus(dslContext: DSLContext, taskId: Long, status: TaskStatus, msg: String): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(STATUS, status.name)
                    .set(DESCRIPTION, msg)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun updateDevCloudTaskId(dslContext: DSLContext, taskId: Long, devCloudTaskId: String): Int {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.update(this)
                    .set(DEV_CLOUD_TASK_ID, devCloudTaskId)
                    .where(TASK_ID.eq(taskId))
                    .execute()
        }
    }

    fun getWaitingTask(dslContext: DSLContext): Result<TDevCloudTaskRecord>? {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.selectFrom(this)
                    .where(STATUS.eq(TaskStatus.WAITING.name))
                    .fetch()
        }
    }

    fun getRunningTask(dslContext: DSLContext): Result<TDevCloudTaskRecord>? {
        with(TDevCloudTask.T_DEV_CLOUD_TASK) {
            return dslContext.selectFrom(this)
                    .where(STATUS.eq(TaskStatus.RUNNING.name))
                    .fetch()
        }
    }
}

/**

DROP TABLE IF EXISTS `devops_environment`.`T_DEV_CLOUD_TASK`;
CREATE TABLE `T_DEV_CLOUD_TASK` (
`TASK_ID` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'ID',
`PROJECT_ID` varchar(64) NOT NULL COMMENT '项目ID',
`OPERATOR` varchar(256) NOT NULL,
`ACTION` varchar(256) NOT NULL,
`STATUS` varchar(64) NOT NULL,
`REGISTRY_HOST` varchar(1024) NULL COMMENT '仓库地址',
`REGISTRY_USER` varchar(1024) NULL COMMENT '仓库用户名',
`REGISTRY_PWD` varchar(1024) NULL COMMENT '仓库密码',
`CONTAINER_NAME` varchar(64) NULL COMMENT '容器名称',
`CONTAINER_TYPE` varchar(64) NULL COMMENT '容器类型',
`IMAGE` varchar(512) NULL COMMENT '镜像',
`CPU` int(11) NULL COMMENT '容器cpu核数',
`MEMORY` varchar(128) NULL COMMENT '容器内存大小',
`DISK` varchar(128) NULL COMMENT '容器磁盘大小',
`REPLICA` int(11) NULL COMMENT '容器副本数',
`PASSWORD` varchar(64) NULL COMMENT '容器密码',
`CREATED_TIME` timestamp NULL DEFAULT NULL,
`UPDATE_TIME` timestamp NULL DEFAULT NULL,
`DEV_CLOUD_TASK_ID` varchar(64) NULL COMMENT 'devCloud的任务Id',
`NODE_LONG_ID` bigint(20) NULL COMMENT 'nodeId',
`DESCRIPTION` longtext NULL,
PRIMARY KEY (`TASK_ID`)
) ENGINE=InnoDB AUTO_INCREMENT=685 DEFAULT CHARSET=utf8

 **/
