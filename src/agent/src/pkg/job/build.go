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

	scriptFile, err := writeStartBuildAgentScript(buildInfo)
	if err != nil {
		errMsg := "write worker start script failed: " + err.Error()
		logs.Error(errMsg)
		workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errMsg})
		return err
	}
	pid, err := command.StartProcess(scriptFile, []string{}, workDir, goEnv, runUser)
	if err != nil {
		errMsg := "start worker process failed: " + err.Error()
		logs.Error(errMsg)
		workerBuildFinish(&api.ThirdPartyBuildWithStatus{*buildInfo, false, errMsg})
		return err
	}
	GBuildManager.AddBuild(pid, buildInfo)
	logs.Info("build started, runUser: ", runUser, ", pid: ", pid, ", buildId: ",
		buildInfo.BuildId, ", vmSetId: ", buildInfo.VmSeqId)
	return nil
}

func getEncodedBuildInfo(buildInfo *api.ThirdPartyBuildInfo) string {
	strBuildInfo, _ := json.Marshal(buildInfo)
	logs.Info("buildInfo: ", string(strBuildInfo))
	codedBuildInfo := base64.StdEncoding.EncodeToString(strBuildInfo)
	logs.Info("base64: ", codedBuildInfo)
	return codedBuildInfo
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
