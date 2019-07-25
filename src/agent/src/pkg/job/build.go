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
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package job

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"github.com/astaxie/beego/logs"
	"io/ioutil"
	"pkg/api"
	"pkg/config"
	"pkg/util"
	"pkg/util/command"
	"pkg/util/httputil"
	"pkg/util/systemutil"
	"strings"
	"time"
)

const buildIntervalInSeconds = 5

func AgentStartup() (agentStatus string, err error) {
	result, err := api.AgentStartup()
	return parseAgentStatusResult(result, err)
}

func getAgentStatus() (agentStatus string, err error) {
	result, err := api.GetAgentStatus()
	return parseAgentStatusResult(result, err)
}

func parseAgentStatusResult(result *httputil.DevopsResult, resultErr error) (agentStatus string, err error) {
	if resultErr != nil {
		logs.Error("parse agent status error: ", resultErr.Error())
		return "", errors.New("parse agent status error")
	}
	if result.IsNotOk() {
		logs.Error("parse agent status failed: ", result.Message)
		return "", errors.New("parse agent status failed")
	}

	agentStatus, ok := result.Data.(string)
	if !ok || result.Data == "" {
		logs.Error("parse agent status error")
		return "", errors.New("parse agent status error")
	}
	return agentStatus, nil
}

func DoPollAndBuild() {
	for {
		time.Sleep(buildIntervalInSeconds * time.Second)
		agentStatus, err := getAgentStatus()
		if err != nil {
			logs.Warning("get agent status err: ", err.Error())
			continue
		}
		if agentStatus != config.AgentStatusImportOk {
			logs.Error("agent is not ready for build, agent status: " + agentStatus)
			continue
		}

		if config.GAgentConfig.ParallelTaskCount != 0 && GBuildManager.GetInstanceCount() >= config.GAgentConfig.ParallelTaskCount {
			logs.Info(fmt.Sprintf("parallel task count exceed , wait job done, ParallelTaskCount config: %d, instance count: %d",
				config.GAgentConfig.ParallelTaskCount, GBuildManager.GetInstanceCount()))
			continue
		}

		if config.GIsAgentUpgrading {
			logs.Info("agent is upgrading, skip")
			continue
		}

		buildInfo, err := getBuild()
		if err != nil {
			logs.Error("get build failed, retry")
			continue
		}

		if buildInfo == nil {
			logs.Info("no build to run, skip")
			continue
		}

		err = runBuild(buildInfo)
		if err != nil {
			logs.Error("start build failed: ", err.Error())
			// TODO 写buildLog
		}
	}
}

func getBuild() (*api.ThirdPartyBuildInfo, error) {
	logs.Info("get build")
	result, err := api.GetBuild()
	if err != nil {
		return nil, err
	}

	if result.IsNotOk() {
		logs.Error("get build info failed, message", result.Message)
		return nil, errors.New("get build info failed")
	}

	if result.Data == nil {
		return nil, nil
	}

	buildInfo := new(api.ThirdPartyBuildInfo)
	err = util.ParseJsonToData(result.Data, buildInfo)
	if err != nil {
		return nil, err
	}

	return buildInfo, nil
}

func runBuild(buildInfo *api.ThirdPartyBuildInfo) error {
	runUser := config.GAgentConfig.SlaveUser

	goEnv := map[string]string{
		"DEVOPS_AGENT_VERSION": config.AgentVersion,
		"DEVOPS_SLAVE_VERSION": config.GAgentEnv.SlaveVersion,
		"PROJECT_ID":           buildInfo.ProjectId,
		"BUILD_ID":             buildInfo.BuildId,
		"VM_SEQ_ID":            buildInfo.VmSeqId,
	}
	if config.GEnvVars != nil {
		for k, v := range config.GEnvVars {
			goEnv[k] = v
		}
	}

	if systemutil.IsWindows() {
		startCmd := config.GetJava()
		args := []string{"-Ddevops.slave.agent.role=devops.slave.agent.role.slave", "-jar", config.WorkAgentFile, getEncodedBuildInfo(buildInfo)}
		pid, err := command.StartProcess(startCmd, args, config.GetAgentWorkdir(), goEnv, runUser)
		if err != nil {
			logs.Error("start agent process failed", err)
			return err
		}
		GBuildManager.AddBuild(pid, buildInfo)
		logs.Info("build started, runUser: ", runUser, ", pid: ", pid, ", buildId: ", buildInfo.BuildId, ", vmSetId: ", buildInfo.VmSeqId)
		return nil
	} else {
		scriptFile, err := writeStartBuildAgentScript(buildInfo)
		if err != nil {
			return err
		}
		pid, err := command.StartProcess(scriptFile, []string{}, config.GetAgentWorkdir(), goEnv, runUser)
		if err != nil {
			logs.Error("start agent process failed", err)
			return err
		}
		GBuildManager.AddBuild(pid, buildInfo)
		logs.Info("build started, runUser: ", runUser, ", pid: ", pid, ", buildId: ", buildInfo.BuildId, ", vmSetId: ", buildInfo.VmSeqId)
	}
	return nil
}

func getEncodedBuildInfo(buildInfo *api.ThirdPartyBuildInfo) string {
	strBuildInfo, _ := json.Marshal(buildInfo)
	logs.Info("buildInfo: ", string(strBuildInfo))
	codedBuildInfo := base64.StdEncoding.EncodeToString(strBuildInfo)
	logs.Info("base64: ", codedBuildInfo)
	return codedBuildInfo
}

func writeStartBuildAgentScript(buildInfo *api.ThirdPartyBuildInfo) (string, error) {
	logs.Info("write start build agent script to file")
	scriptFile := fmt.Sprintf("%s/devops_agent_start_%s_%s_%s.sh", config.GetAgentWorkdir(), buildInfo.ProjectId, buildInfo.BuildId, buildInfo.VmSeqId)
	jarFile := fmt.Sprintf("%s/%s", config.GetAgentWorkdir(), config.WorkAgentFile)
	logs.Info("start agent script: ", scriptFile)
	lines := []string{
		"#!/bin/bash",
		"source /etc/profile",
		"if [ -f ~/.bash_profile ]; then",
		"  source ~/.bash_profile",
		"fi",
		"if [ -f ~/.bashrc ]; then",
		"  source ~/.bashrc",
		"fi",
		fmt.Sprintf("%s -Ddevops.slave.agent.start.file=%s -Ddevops.slave.agent.role=devops.slave.agent.role.slave -jar %s %s",
			config.GetJava(), scriptFile, jarFile, getEncodedBuildInfo(buildInfo)),
	}
	scriptContent := strings.Join(lines, "\n")

	err := ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		return "", err
	} else {
		return scriptFile, nil
	}
}
