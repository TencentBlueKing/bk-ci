//go:build !windows
// +build !windows

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

 package telegrafconf

 const TelegrafConf = `
 [global_tags]
   projectId = "##{projectId}##"
   agentId = "##{agentId}##"
   agentSecret = "##{agentSecretKey}##"
 [agent]
   interval = "1m"
   round_interval = true
   metric_batch_size = 1000
   metric_buffer_limit = 10000
   collection_jitter = "0s"
   flush_interval = "1m"
   flush_jitter = "0s"
   precision = ""
   debug = false
   quiet = false
   logfile = ""
   hostname = ""
   omit_hostname = false
 [[outputs.influxdb]]
   urls = ["##{gateway}##/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/metrix"]
   database = "agentMetrix"
   skip_database_creation = true
   ##{tls_ca}##
 [[inputs.cpu]]
   percpu = true
   totalcpu = true
   collect_cpu_time = false
   report_active = false
 [[inputs.disk]]
   ignore_fs = ["tmpfs", "devtmpfs", "devfs", "overlay", "aufs", "squashfs"]
 [[inputs.diskio]]
 [[inputs.kernel]]
 [[inputs.mem]]
 [[inputs.processes]]
 # [[inputs.swap]]
 [[inputs.system]]
 [[inputs.net]]
 
 [[processors.rename]]
   # cpu
   [[processors.rename.replace]]
     measurement = "cpu"
     dest = "system.cpu_summary"
   [[processors.rename.replace]]
     field = "usage_user"
     dest = "user"
   [[processors.rename.replace]]
     field = "usage_system"
     dest = "system"
   [[processors.rename.replace]]
     field = "usage_idle"
     dest = "idle"
   [[processors.rename.replace]]
     field = "usage_iowait"
     dest = "iowait"   
   # net
   [[processors.rename.replace]]
     measurement = "net"
     dest = "system.net"
   [[processors.rename.replace]]
     field = "bytes_recv"
     dest = "speed_recv"
   [[processors.rename.replace]]
     field = "bytes_sent"
     dest = "speed_sent"
   # mem
   [[processors.rename.replace]]
     measurement = "mem"
     dest = "system.mem"
   [[processors.rename.replace]]
     field = "used_percent"
     dest = "pct_used"
   # diskio
   [[processors.rename.replace]]
     measurement = "diskio"
     dest = "system.io"
   [[processors.rename.replace]]
     field = "read_bytes"
     dest = "rkb_s"
   [[processors.rename.replace]]
     field = "write_bytes"
     dest = "wkb_s"
   # disk
   [[processors.rename.replace]]
     measurement = "disk"
     dest = "system.disk"
   [[processors.rename.replace]]
     field = "used_percent"
     dest = "in_use"    
 
 `
 