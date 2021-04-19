/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2020 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.tencent.bkrepo.replication.service

import com.tencent.bkrepo.replication.model.TReplicationTaskLog
import com.tencent.bkrepo.replication.pojo.log.ReplicationTaskLog
import com.tencent.bkrepo.replication.repository.TaskLogRepository
import org.springframework.stereotype.Service
import java.time.format.DateTimeFormatter

@Service
class TaskLogService(
    private val taskLogRepository: TaskLogRepository
) {

    fun list(taskKey: String): List<ReplicationTaskLog> {
        return taskLogRepository.findByTaskKeyOrderByStartTimeDesc(taskKey).map { convert(it)!! }
    }

    fun latest(taskKey: String): ReplicationTaskLog? {
        return convert(taskLogRepository.findFirstByTaskKeyOrderByStartTimeDesc(taskKey))
    }

    companion object {
        private fun convert(log: TReplicationTaskLog?): ReplicationTaskLog? {
            return log?.let {
                ReplicationTaskLog(
                    taskKey = it.taskKey,
                    status = it.status,
                    replicationProgress = it.replicationProgress,
                    startTime = it.startTime.format(DateTimeFormatter.ISO_DATE_TIME),
                    endTime = it.endTime?.format(DateTimeFormatter.ISO_DATE_TIME),
                    errorReason = it.errorReason
                )
            }
        }
    }
}
