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

package collector

import (
	"context"
	"fmt"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"

	"github.com/influxdata/telegraf/logger"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"

	"github.com/influxdata/telegraf/agent"
	telegrafConfig "github.com/influxdata/telegraf/config"

	"io/ioutil"
	"strings"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
)

const (
	telegrafConfigFile   = "telegraf.conf"
	telegrafRelaunchTime = 5 * time.Second

	templateKeyAgentId     = "###{agentId}###"
	templateKeyAgentSecret = "###{agentSecret}###"
	templateKeyGateway     = "###{gateway}###"
	templateKeyTlsCa       = "###{tls_ca}###"
	templateKeyProjectId   = "###{projectId}###"
)

const configTemplateLinux = `[global_tags]
  projectId = "###{projectId}###"
  agentId = "###{agentId}###"
  agentSecret = "###{agentSecret}###"
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
  database = "agentMetrix"
  skip_database_creation = true
  ###{tls_ca}###
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
`

const configTemplateWindows = `[global_tags]
  projectId = "###{projectId}###"
  agentId = "###{agentId}###"
  agentSecret = "###{agentSecret}###"
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
  database = "agentMetrix"
  skip_database_creation = true
  ###{tls_ca}###
[[inputs.mem]]
[[inputs.disk]]
  ignore_fs = ["tmpfs", "devtmpfs", "devfs", "overlay", "aufs", "squashfs"]
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
    Measurement = "win_cpu"
    IncludeTotal=true
  [[inputs.win_perf_counters.object]]
    ObjectName = "LogicalDisk"
    Instances = ["*"]
    Counters = [
      "% Idle Time",
      "% Disk Time",
      "% Disk Read Time",
      "% Disk Write Time",
      "Current Disk Queue Length",
      "% Free Space",
      "Free Megabytes",
    ]
    Measurement = "win_disk"
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
    Measurement = "win_diskio"
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
    Measurement = "win_net"
  [[inputs.win_perf_counters.object]]
    ObjectName = "System"
    Counters = [
      "Context Switches/sec",
      "System Calls/sec",
      "Processor Queue Length",
      "System Up Time",
    ]
    Instances = ["------"]
    Measurement = "win_system"
  [[inputs.win_perf_counters.object]]
    ObjectName = "Memory"
    Counters = [
      "Available Bytes",
      "Cache Faults/sec",
      "Demand Zero Faults/sec",
      "Page Faults/sec",
      "Pages/sec",
      "Transition Faults/sec",
      "Pool Nonpaged Bytes",
      "Pool Paged Bytes",
      "Standby Cache Reserve Bytes",
      "Standby Cache Normal Priority Bytes",
      "Standby Cache Core Bytes",
    ]
    Instances = ["------"]
    Measurement = "win_mem"
  [[inputs.win_perf_counters.object]]
    ObjectName = "Paging File"
    Counters = [
      "% Usage",
    ]
    Instances = ["_Total"]
    Measurement = "win_swap"
`

func DoAgentCollect() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent collect panic: ", err)
		}
	}()

	if config.GAgentConfig.CollectorOn == false {
		logs.Info("agent collector off")
		return
	}

	writeTelegrafConfig()

	// 每次重启agent要清理掉无意义的telegraf.log日志，重新记录
	logFile := fmt.Sprintf("%s/logs/telegraf.log", systemutil.GetWorkDir())
	if fileutil.Exists(logFile) {
		_ = fileutil.TryRemoveFile(logFile)
	}
	tAgent, err := getTelegrafAgent(
		fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), telegrafConfigFile),
		logFile,
	)
	if err != nil {
		logs.Errorf("init telegraf agent failed: %v", err)
		return
	}

	for {
		logs.Info("launch telegraf agent")
		if err = tAgent.Run(context.Background()); err != nil {
			logs.Errorf("telegraf agent exit: %v", err)
		}
		time.Sleep(telegrafRelaunchTime)
	}
}

func getTelegrafAgent(configFile, logFile string) (*agent.Agent, error) {
	// get a new config and parse configuration from file.
	c := telegrafConfig.NewConfig()
	if err := c.LoadConfig(configFile); err != nil {
		return nil, err
	}

	logConfig := logger.LogConfig{
		Logfile:             logFile,
		LogTarget:           logger.LogTargetFile,
		RotationMaxArchives: -1,
	}

	logger.SetupLogging(logConfig)
	return agent.NewAgent(c)
}

func writeTelegrafConfig() {
	var configTemplate string
	if systemutil.IsWindows() {
		configTemplate = configTemplateWindows
	} else {
		configTemplate = configTemplateLinux
	}

	configContent := strings.Replace(configTemplate, templateKeyAgentId, config.GAgentConfig.AgentId, 1)
	configContent = strings.Replace(configContent, templateKeyAgentSecret, config.GAgentConfig.SecretKey, 1)
	configContent = strings.Replace(configContent, templateKeyGateway, buildGateway(config.GAgentConfig.Gateway), 1)
	configContent = strings.Replace(configContent, templateKeyProjectId, config.GAgentConfig.ProjectId, 1)
	if config.UseCert {
		configContent = strings.Replace(configContent, templateKeyTlsCa, `tls_ca = ".cert"`, 1)
	} else {
		configContent = strings.Replace(configContent, templateKeyTlsCa, "", 1)
	}

	err := ioutil.WriteFile(systemutil.GetWorkDir()+"/telegraf.conf", []byte(configContent), 0666)
	if err != nil {
		logs.Error("write telegraf config err: ", err)
		return
	}
}

func buildGateway(gateway string) string {
	if strings.HasPrefix(gateway, "http") {
		return gateway
	} else {
		return "http://" + gateway
	}
}
