//go:build windows
// +build windows

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
  projectId = "###{projectId}###"
  agentId = "###{agentId}###"
  agentSecret = "###{agentSecret}###"
  hostName = "###{hostName}###"
  hostIp = "###{hostIp}###"
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
  urls = ["###{gateway}###/ms/environment/api/buildAgent/agent/thirdPartyAgent/agents/metrix"]
  database = "agentMetric"
  skip_database_creation = true
  ###{tls_ca}###
[[inputs.win_perf_counters]]
  [[inputs.win_perf_counters.object]]
    ObjectName = "Processor"
    Instances = ["*"]
    Counters = [
      "% Idle Time",
      "% Interrupt Time",
      "% Privileged Time",
      "% User Time",
      "% Processor Time",
      "% DPC Time",
    ]
    Measurement = "cpu"
    IncludeTotal=true
  [[inputs.win_perf_counters.object]]
    ObjectName = "PhysicalDisk"
    Instances = ["*"]
    Counters = [
      "Disk Read Bytes/sec",
      "Disk Write Bytes/sec",
      "Current Disk Queue Length",
      "Disk Reads/sec",
      "Disk Writes/sec",
      "% Disk Time",
      "% Disk Read Time",
      "% Disk Write Time",
    ]
    Measurement = "diskio"
  [[inputs.win_perf_counters.object]]
    ObjectName = "Network Interface"
    Instances = ["*"]
    Counters = [
      "Bytes Received/sec",
      "Bytes Sent/sec",
      "Packets Received/sec",
      "Packets Sent/sec",
      "Packets Received Discarded",
      "Packets Outbound Discarded",
      "Packets Received Errors",
      "Packets Outbound Errors",
    ]
    Measurement = "net"
[[inputs.mem]]
[[inputs.disk]]
  ignore_fs = ["tmpfs", "devtmpfs", "devfs", "overlay", "aufs", "squashfs"]
[[inputs.system]]

[[processors.rename]]
  # cpu
  [[processors.rename.replace]]
    measurement = "cpu"
    dest = "cpu_detail"
  [[processors.rename.replace]]
    field = "Percent_User_Time"
    dest = "user"
  [[processors.rename.replace]]
    field = "Percent_Privileged_Time"
    dest = "system"
  [[processors.rename.replace]]
    field = "Percent_Idle_Time"
    dest = "idle"
  # net
  [[processors.rename.replace]]
    field = "Bytes_Received_persec"
    dest = "speed_recv"
  [[processors.rename.replace]]
    field = "Bytes_Sent_persec"
    dest = "speed_sent"
  [[processors.rename.replace]]
    field = "Packets_Received_persec"
    dest = "speed_packets_recv"
  [[processors.rename.replace]]
    field = "Packets_Sent_persec"
    dest = "speed_packets_sent"
  # mem
  [[processors.rename.replace]]
    field = "used_percent"
    dest = "pct_used"
  # diskio
  [[processors.rename.replace]]
    measurement = "diskio"
    dest = "io"
  [[processors.rename.replace]]
    field = "Disk_Read_Bytes_persec"
    dest = "rkb_s"
  [[processors.rename.replace]]
    field = "Disk_Write_Bytes_persec"
    dest = "wkb_s"
  # netstat
  [[processors.rename.replace]]
    field = "tcp_close_wait"
    dest = "cur_tcp_closewait"
  [[processors.rename.replace]]
    field = "tcp_time_wait"
    dest = "cur_tcp_timewait"
  [[processors.rename.replace]]
    field = "tcp_close"
    dest = "cur_tcp_closed"
  [[processors.rename.replace]]
    field = "tcp_closing"
    dest = "cur_tcp_closing"
  [[processors.rename.replace]]
    field = "tcp_established"
    dest = "cur_tcp_estab"
  [[processors.rename.replace]]
    field = "tcp_fin_wait1"
    dest = "cur_tcp_finwait1"
  [[processors.rename.replace]]
    field = "tcp_fin_wait2"
    dest = "cur_tcp_finwait2"
  [[processors.rename.replace]]
    field = "tcp_last_ack"
    dest = "cur_tcp_lastack"
  [[processors.rename.replace]]
    field = "tcp_listen"
    dest = "cur_tcp_listen"
  [[processors.rename.replace]]
    field = "tcp_syn_recv"
    dest = "cur_tcp_syn_recv"
  [[processors.rename.replace]]
    field = "tcp_syn_sent"
    dest = "cur_tcp_syn_sent"
  # load
  [[processors.rename.replace]]
    measurement = "system"
    dest = "load"

# disk的指标同名但改完名不同单独拿出来    
[[processors.rename]]
  namepass = ["disk"]
  [[processors.rename.replace]]
    field = "used_percent"
    dest = "in_use"  

`
