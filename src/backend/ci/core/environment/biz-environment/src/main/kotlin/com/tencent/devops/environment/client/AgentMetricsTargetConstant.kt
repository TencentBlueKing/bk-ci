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

package com.tencent.devops.environment.client

// Agent 监控指标别名常量，用到的所有指标名都在这里修改方便使用
// 开头 t 表名 f 字段名
object AgentMetricsTargetConstant {
    const val t_system = "system"
    const val f_system_n_cpus = "n_cpus"

    const val t_mem = "system.mem"
    const val f_mem_used_percent = "pct_used"
    const val f_mem_total = "total"

    const val t_cpu = "system.cpu_summary"
    const val f_cpu_usage_idle = "idle"
    const val f_cpu_usage_iowait = "iowait"
    const val f_cpu_usage_user = "user"
    const val f_cpu_usage_system = "system"
    // 目前仅有windows有
    const val f_cpu_usage_interrupt = "Percent_Interrupt_Time"

    const val t_disk = "system.disk"
    const val f_disk_total = "total"

    const val t_diskio = "system.io"
    const val f_diskio_read_bytes = "rkb_s"
    const val f_diskio_write_bytes = "wkb_s"

    const val t_net = "system.net"
    const val f_net_bytes_recv = "speed_recv"
    const val f_net_bytes_sent = "speed_sent"
}
