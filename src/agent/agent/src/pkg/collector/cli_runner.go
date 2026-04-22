//go:build !loong64
// +build !loong64

package collector

// cli_runner.go 给 agentcli 的 monitor 子命令用：构造一份把 output 改到
// stdout 的 telegraf agent，跑一次 Gather 即退出。采集配置（inputs /
// processors）与生产 Collect() 完全一致，便于和 monitor 包并排对比。

import (
	"bytes"
	"context"
	"fmt"
	"io"
	"strings"
	"text/template"
	"time"

	"github.com/influxdata/telegraf/agent"
	telegrafConfig "github.com/influxdata/telegraf/config"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

// stdoutTelegrafConf 复用生产模板的 inputs / processors 段，output 改成
// outputs.file 指向 stdout。inputs/processors 与 telegrafconf.TelegrafConf
// (ci 分支) 字面一致，改 input 集合时两边要同步。
const stdoutTelegrafConf = `
[global_tags]
  projectId = "{{.ProjectId}}"
  agentId = "{{.AgentId}}"
  hostName = "{{.HostName}}"
  hostIp = "{{.HostIp}}"
[agent]
  interval = "1s"
  round_interval = true
  metric_batch_size = 1000
  metric_buffer_limit = 10000
  collection_jitter = "0s"
  flush_interval = "1s"
  flush_jitter = "0s"
  precision = ""
  debug = false
  quiet = false
  logfile = ""
  omit_hostname = false

[[outputs.file]]
  files = ["stdout"]
  data_format = "influx"

[[inputs.cpu]]
  percpu = true
  totalcpu = true
  collect_cpu_time = false
  report_active = false
[[inputs.disk]]
  ignore_fs = ["tmpfs", "devtmpfs", "devfs", "overlay", "aufs", "squashfs"]
[[inputs.diskio]]
[[inputs.mem]]
[[inputs.net]]
[[inputs.system]]
[[inputs.netstat]]
[[inputs.swap]]
[[inputs.kernel]]

[[processors.rename]]
  [[processors.rename.replace]]
    measurement = "cpu"
    dest = "cpu_detail"
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
  [[processors.rename.replace]]
    field = "bytes_recv"
    dest = "speed_recv"
  [[processors.rename.replace]]
    field = "bytes_sent"
    dest = "speed_sent"
  [[processors.rename.replace]]
    field = "packets_recv"
    dest = "speed_packets_recv"
  [[processors.rename.replace]]
    field = "packets_sent"
    dest = "speed_packets_sent"
  [[processors.rename.replace]]
    field = "used_percent"
    dest = "pct_used"
  [[processors.rename.replace]]
    measurement = "diskio"
    dest = "io"
  [[processors.rename.replace]]
    field = "read_bytes"
    dest = "rkb_s"
  [[processors.rename.replace]]
    field = "write_bytes"
    dest = "wkb_s"
  [[processors.rename.replace]]
    measurement = "system"
    dest = "load"
  [[processors.rename.replace]]
    measurement = "kernel"
    dest = "env"

[[processors.rename]]
  namepass = ["disk"]
  [[processors.rename.replace]]
    field = "used_percent"
    dest = "in_use"
`

// RunOnceStdout 构造一个 telegraf agent，把 output 指向 stdout，跑 duration
// 后停止。duration 应至少 1s，保证 cpu 等基于 delta 的 input 能产出数据；
// 建议 2~3s。out 目前未使用（telegraf outputs.file 自己打开 stdout），
// 保留参数便于未来扩展（例如切换到 io.Writer）。
func RunOnceStdout(ctx context.Context, out io.Writer, duration time.Duration) error {
	if duration <= 0 {
		duration = 2 * time.Second
	}
	cfgBuf, err := genStdoutTelegrafConfig()
	if err != nil {
		return errors.Wrap(err, "build stdout telegraf config")
	}

	c := telegrafConfig.NewConfig()
	if err := c.LoadConfigData(cfgBuf.Bytes()); err != nil {
		return errors.Wrap(err, "load telegraf config")
	}
	tAgent, err := agent.NewAgent(c)
	if err != nil {
		return errors.Wrap(err, "new telegraf agent")
	}

	runCtx, cancel := context.WithTimeout(ctx, duration)
	defer cancel()

	fmt.Fprintln(out, "# source=collector(telegraf), duration=", duration)
	if err := tAgent.Run(runCtx); err != nil {
		// ctx 到期会让 Run 返回 nil；其他 error 才算失败
		if runCtx.Err() == nil {
			return errors.Wrap(err, "telegraf agent run")
		}
	}
	return nil
}

// genStdoutTelegrafConfig 渲染 stdoutTelegrafConf 模板。
// config 未初始化时填占位值，便于裸机测试。
func genStdoutTelegrafConfig() (*bytes.Buffer, error) {
	data := map[string]string{
		"ProjectId": safeProjectID(),
		"AgentId":   safeAgentID(),
		"HostName":  safeHostName(),
		"HostIp":    safeHostIP(),
	}
	var buf bytes.Buffer
	tmpl, err := template.New("stdout").Parse(stdoutTelegrafConf)
	if err != nil {
		return nil, err
	}
	if err := tmpl.Execute(&buf, data); err != nil {
		return nil, err
	}
	return &buf, nil
}

// safe* 帮助函数在 config 未初始化时返回占位值，避免 nil panic。

func safeProjectID() string {
	if config.GAgentConfig != nil {
		if p := strings.TrimSpace(config.GAgentConfig.ProjectId); p != "" {
			return p
		}
	}
	return "cli-test"
}

func safeAgentID() string {
	if config.GAgentConfig != nil && config.GAgentConfig.AgentId != "" {
		return config.GAgentConfig.AgentId
	}
	return "cli-test"
}

func safeHostName() string {
	if config.GAgentEnv != nil && config.GAgentEnv.HostName != "" {
		return config.GAgentEnv.HostName
	}
	return "cli-test-host"
}

func safeHostIP() string {
	if config.GAgentEnv != nil {
		if ip := config.GAgentEnv.GetAgentIp(); ip != "" {
			return ip
		}
	}
	return "127.0.0.1"
}
