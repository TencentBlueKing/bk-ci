/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
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
	"bytes"
	"context"
	"fmt"
	"text/template"
	"time"

	"github.com/pkg/errors"

	telegrafconf "github.com/TencentBlueKing/bk-ci/agent/src/pkg/collector/telegrafConf"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
	"github.com/influxdata/telegraf/logger"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"

	"github.com/influxdata/telegraf/agent"
	telegrafConfig "github.com/influxdata/telegraf/config"

	"strings"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

const (
	telegrafRelaunchTime = 5 * time.Second
	eBusId               = "Collect"
)

func Collect() {
	logs.Debug("do Collect")
	if config.GAgentConfig.CollectorOn == false {
		logs.Info("agent collector off")
		return
	}

	ipChan := config.EBus.Subscribe(config.IpEvent, eBusId, 1)

	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent collect panic: ", err)
		}
		config.EBus.Unsubscribe(config.IpEvent, eBusId)
	}()

	for {
		ctx, cancel := context.WithCancel(context.Background())
		go func() {
			ipData := <-ipChan.DChan
			logs.Infof("collect ip change data: %s", ipData.Data)
			cancel()
		}()
		doAgentCollect(ctx)
	}
}

func doAgentCollect(ctx context.Context) {
	configContent, err := genTelegrafConfig()
	if err != nil {
		logs.WithError(err).Error("genTelegrafConfig error")
		return
	}

	logs.Debug("generate telegraf config")

	// 每次重启agent要清理掉无意义的telegraf.log日志，重新记录
	logFile := fmt.Sprintf("%s/logs/telegraf.log", systemutil.GetWorkDir())
	if fileutil.Exists(logFile) {
		_ = fileutil.TryRemoveFile(logFile)
	}

	tAgent, err := getTelegrafAgent(configContent.Bytes(), logFile)
	if err != nil {
		logs.WithError(err).Error("init telegraf agent failed")
		return
	}

	for {
		logs.Info("launch telegraf agent")
		err = tAgent.Run(ctx)
		select {
		case <-ctx.Done():
			// 上下文被取消需要返回调用方重新获取上下文，不然一直是取消状态
			logs.Info("telegraf agent ctx done")
			return
		default:
			// 普通的 telegraf 退出直接重新启动即可
			if err != nil {
				logs.WithError(err).Error("telegraf agent exit")
			}
		}
		time.Sleep(telegrafRelaunchTime)
	}
}

func getTelegrafAgent(configData []byte, logFile string) (*agent.Agent, error) {
	// get a new config and parse configuration from file.
	c := telegrafConfig.NewConfig()
	if err := c.LoadConfigData(configData); err != nil {
		return nil, err
	}

	logConfig := logger.LogConfig{
		Logfile:             logFile,
		LogTarget:           logger.LogTargetFile,
		RotationMaxArchives: -1,
	}

	if err := logger.SetupLogging(logConfig); err != nil {
		return nil, err
	}
	return agent.NewAgent(c)
}

func genTelegrafConfig() (*bytes.Buffer, error) {
	// 区分 stream 项目使用模板分割，PAC 上线后删除
	projectType := "ci"
	if strings.HasPrefix(config.GAgentConfig.ProjectId, "git_") {
		projectType = "stream"
	}

	tlsCa := ""
	if config.UseCert {
		tlsCa = `tls_ca = ".cert"`
	}

	buildGateway := config.GAgentConfig.Gateway
	if !strings.HasPrefix(buildGateway, "http") {
		buildGateway = "http://" + buildGateway
	}

	ip := config.GAgentEnv.GetAgentIp()
	templateData := map[string]string{
		"ProjectType": projectType,
		"AgentId":     config.GAgentConfig.AgentId,
		"AgentSecret": config.GAgentConfig.SecretKey,
		"Gateway":     buildGateway,
		"ProjectId":   config.GAgentConfig.ProjectId,
		"HostName":    config.GAgentEnv.HostName,
		"HostIp":      config.GAgentEnv.GetAgentIp(),
		"BuildType":   config.GAgentConfig.BuildType,
		"TlsCa":       tlsCa,
	}
	logs.Debugf("telegraf agentip %s", ip)

	var content = new(bytes.Buffer)
	tmpl, err := template.New("tmpl").Parse(telegrafconf.TelegrafConf)
	if err != nil {
		return nil, errors.Wrap(err, "parse telegraf config template err")
	}
	err = tmpl.Execute(content, templateData)
	if err != nil {
		return nil, errors.Wrap(err, "execute telegraf config template err")
	}

	return content, nil
}
