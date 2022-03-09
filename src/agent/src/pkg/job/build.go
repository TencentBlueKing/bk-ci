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

package job

import (
	"encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/api"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/httputil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/astaxie/beego/logs"
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
	workDir := systemutil.GetWorkDir()
	agentJarPath := config.BuildAgentJarPath()
	if !fileutil.Exists(agentJarPath) {
		errorMsg := fmt.Sprintf("missing %s, please check agent installation.", config.WorkAgentFile)
		logs.Error(errorMsg)
		workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errorMsg})

	}

	runUser := config.GAgentConfig.SlaveUser

	goEnv := map[string]string{
		"DEVOPS_AGENT_VERSION":  config.AgentVersion,
		"DEVOPS_WORKER_VERSION": config.GAgentEnv.SlaveVersion,
		"DEVOPS_PROJECT_ID":     buildInfo.ProjectId,
		"DEVOPS_BUILD_ID":       buildInfo.BuildId,
		"DEVOPS_VM_SEQ_ID":      buildInfo.VmSeqId,
		"DEVOPS_SLAVE_VERSION":  config.GAgentEnv.SlaveVersion, //deprecated
		"PROJECT_ID":            buildInfo.ProjectId,           //deprecated
		"BUILD_ID":              buildInfo.BuildId,             //deprecated
		"VM_SEQ_ID":             buildInfo.VmSeqId,             //deprecated

	}
	if config.GEnvVars != nil {
		for k, v := range config.GEnvVars {
			goEnv[k] = v
		}
	}

	if systemutil.IsWindows() {
		startCmd := config.GetJava()
		agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
		args := []string{
			"-Ddevops.slave.agent.role=devops.slave.agent.role.slave",
			"-Dbuild.type=AGENT",
			"-DAGENT_LOG_PREFIX=" + agentLogPrefix,
			"-jar",
			config.BuildAgentJarPath(),
			getEncodedBuildInfo(buildInfo)}
		pid, err := command.StartProcess(startCmd, args, workDir, goEnv, runUser)
		if err != nil {
			errMsg := "start worker process failed: " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errMsg})
			return err
		}
		GBuildManager.AddBuild(pid, buildInfo)
		logs.Info("build started, runUser: ", runUser, ", pid: ", pid, ", buildId: ", buildInfo.BuildId, ", vmSetId: ", buildInfo.VmSeqId)
		return nil
	} else {
		startScriptFile, err := writeStartBuildAgentScript(buildInfo)
		if err != nil {
			errMsg := "write worker start script failed: " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errMsg})
			return err
		}
		pid, err := command.StartProcess(startScriptFile, []string{}, workDir, goEnv, runUser)
		if err != nil {
			errMsg := "start worker process failed: " + err.Error()
			logs.Error(errMsg)
			workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errMsg})
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
	// 套娃，多加一层脚本，使用exec新起进程，这样才会读取 .bash_profile
	prepareScriptFile := fmt.Sprintf(
		"%s/devops_agent_prepare_start_%s_%s_%s.sh",
		systemutil.GetWorkDir(),
		buildInfo.ProjectId,
		buildInfo.BuildId,
		buildInfo.VmSeqId)
	scriptFile := fmt.Sprintf(
		"%s/devops_agent_start_%s_%s_%s.sh",
		systemutil.GetWorkDir(),
		buildInfo.ProjectId,
		buildInfo.BuildId,
		buildInfo.VmSeqId)

	logs.Info("start agent script: ", scriptFile)
	agentLogPrefix := fmt.Sprintf("%s_%s_agent", buildInfo.BuildId, buildInfo.VmSeqId)
	lines := []string{
		"#!" + getCurrentShell(),
		fmt.Sprintf("cd %s", systemutil.GetWorkDir()),
		fmt.Sprintf("%s -Ddevops.slave.agent.start.file=%s -Ddevops.slave.agent.prepare.start.file=%s -Dbuild.type=AGENT -Ddevops.slave.agent.role=devops.slave.agent.role.slave -DAGENT_LOG_PREFIX=%s -jar %s %s",
			config.GetJava(), scriptFile, prepareScriptFile, agentLogPrefix, config.BuildAgentJarPath(), getEncodedBuildInfo(buildInfo)),
	}
	scriptContent := strings.Join(lines, "\n")

	err := ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		return "", err
	} else {
		newLines := []string{
			"#!" + getCurrentShell(),
			"exec " + getCurrentShell() + " -l " + scriptFile,
		}
		prepareScriptContent := strings.Join(newLines, "\n")
		err := ioutil.WriteFile(prepareScriptFile, []byte(prepareScriptContent), 0777)
		if err != nil {
			return "", err
		} else {
			return prepareScriptFile, nil
		}
	}
}

func workerBuildFinish(buildInfo *api.ThirdPartyBuildWithStatus) {
	if buildInfo == nil {
		logs.Warn("buildInfo not exist")
		return
	}

	if buildInfo.Success {
		time.Sleep(8 * time.Second)
	}
	result, err := api.WorkerBuildFinish(buildInfo)
	if err != nil {
		logs.Error("send worker build finish failed: ", err.Error())
	}
	if result.IsNotOk() {
		logs.Error("send worker build finish failed: ", result.Message)
	}
	logs.Info("workerBuildFinish done")
}

func getCurrentShell() (shell string) {
	if config.GAgentConfig.DetectShell {
		shell = os.Getenv("SHELL")
		if strings.TrimSpace(shell) == "" {
			shell = "/bin/bash"
		}
	} else {
		shell = "/bin/bash"
	}
	logs.Info("current shell: ", shell)
	return
}
