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
	"text/template"
	"time"

	telegrafconf "github.com/TencentBlueKing/bk-ci/agent/src/pkg/collector/telegrafConf"
	"github.com/influxdata/telegraf/agent"
	telegrafConfig "github.com/influxdata/telegraf/config"
	"github.com/pkg/errors"
)

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
	templateData := map[string]string{
		"ProjectType":    "test",
		"AgentId":        "test",
		"AgentSecret":    "test",
		"Gateway":        "test",
		"ProjectId":      "test",
		"HostName":       "test",
		"HostIp":         "test",
		"BuildType":      "test",
		"TlsCa":          "",
		"CPUProductInfo": "test",
		"GPUProductInfo": "test",
		"OutputType":     "file",
	}
	var buf bytes.Buffer
	tmpl, err := template.New("stdout").Parse(telegrafconf.TelegrafConf)
	if err != nil {
		return nil, err
	}
	if err := tmpl.Execute(&buf, templateData); err != nil {
		return nil, err
	}
	return &buf, nil
}
