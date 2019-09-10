package collector

import (
	"context"
	"fmt"
	"time"

	"pkg/util/systemutil"

	"github.com/astaxie/beego/logs"
	telegraf "github.com/influxdata/telegraf/cmd/bk-telegraf"
	"io/ioutil"
	"pkg/config"
	"strings"
)

const (
	telegrafConfigFile   = "telegraf.conf"
	telegrafRelaunchTime = 5 * time.Second

	templateKeyAgentId     = "###{agentId}###"
	templateKeyAgentSecret = "###{agentSecret}###"
	templateKeyGateway     = "###{gateway}###"
)

const configTemplateLinux = `[global_tags]
  agentId = "###{agentId}###"
  agentSecret = "###{agentSecret}###"
[agent]
  interval = "10s"
  round_interval = true
  metric_batch_size = 1000
  metric_buffer_limit = 10000
  collection_jitter = "0s"
  flush_interval = "10s"
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
  agentId = "###{agentId}###"
  agentSecret = "###{agentSecret}###"
[agent]
  interval = "10s"
  round_interval = true
  metric_batch_size = 1000
  metric_buffer_limit = 10000
  collection_jitter = "0s"
  flush_interval = "10s"
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
[[inputs.mem]]
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
	if config.GAgentConfig.CollectorOn == false {
		logs.Info("agent collector off")
		return
	}

	writeTelegrafConfig()

	tAgent, err := telegraf.GetTelegrafAgent(
		fmt.Sprintf("%s/%s", systemutil.GetWorkDir(), telegrafConfigFile),
		fmt.Sprintf("%s/logs/telegraf.log", systemutil.GetExecutableDir()),
	)
	if err != nil {
		logs.Error("init telegraf agent failed: %v", err)
		return
	}

	for {
		logs.Info("launch telegraf agent")
		if err = tAgent.Run(context.Background()); err != nil {
			logs.Error("telegraf agent exit: %v", err)
		}
		time.Sleep(telegrafRelaunchTime)
	}
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
	ioutil.WriteFile(
		systemutil.GetExecutableDir()+"/telegraf.conf",
		[]byte(configContent),
		0666)
}

func buildGateway(gateway string) string {
	if strings.HasPrefix(gateway, "http") {
		return gateway
	} else {
		return "http://" + gateway
	}
}
