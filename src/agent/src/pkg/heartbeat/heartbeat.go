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

package heartbeat

import (
	"errors"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/upgrade"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

func DoAgentHeartbeat() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent heartbeat panic: ", err)
		}
	}()

	// 部分逻辑只在启动时运行一次
	var jdkOnce = &sync.Once{}
	var dockerfileSyncOnce = &sync.Once{}

	for {
		_ = agentHeartbeat(jdkOnce, dockerfileSyncOnce)
		time.Sleep(10 * time.Second)
	}
}

func agentHeartbeat(jdkSyncOnce, dockerfileSyncOnce *sync.Once) error {
	// 在第一次启动时同步一次jdk version，防止重启时因为upgrade的执行慢导致了升级jdk
	var jdkVersion []string
	jdkSyncOnce.Do(func() {
		version, err := upgrade.SyncJdkVersion()
		if err != nil {
			logs.Error("agent heart sync jdkVersion error", err)
			return
		}
		jdkVersion = version
	})
	if jdkVersion == nil {
		version := upgrade.JdkVersion.GetVersion()
		if version != nil {
			jdkVersion = version
		}
	}

	// 获取docker的filemd5前也同步一次
	dockerfileSyncOnce.Do(func() {
		if err := upgrade.SyncDockerInitFileMd5(); err != nil {
			logs.Error("agent heart sync docker file md5 error", err)
		}
	})

	result, err := api.Heartbeat(
		job.GBuildManager.GetInstances(),
		jdkVersion,
		job.GBuildDockerManager.GetInstances(),
		api.DockerInitFileInfo{
			FileMd5:     upgrade.DockerFileMd5.Md5,
			NeedUpgrade: upgrade.DockerFileMd5.NeedUpgrade,
		})
	if err != nil {
		logs.Error("agent heartbeat failed: ", err.Error())
		return errors.New("agent heartbeat failed")
	}
	if result.IsNotOk() {
		logs.Error("agent heartbeat failed: ", result.Message)
		return errors.New("agent heartbeat failed")
	}

	heartbeatResponse := new(api.AgentHeartbeatResponse)
	err = util.ParseJsonToData(result.Data, &heartbeatResponse)
	if err != nil {
		logs.Error("agent heartbeat failed: ", err.Error())
		return errors.New("agent heartbeat failed")
	}

	if heartbeatResponse.AgentStatus == config.AgentStatusDelete {
		upgrade.UninstallAgent()
		return nil
	}

	// agent配置
	configChanged := false
	if config.GAgentConfig.ParallelTaskCount != heartbeatResponse.ParallelTaskCount {
		config.GAgentConfig.ParallelTaskCount = heartbeatResponse.ParallelTaskCount
		configChanged = true
	}
	if heartbeatResponse.Gateway != "" && heartbeatResponse.Gateway != config.GAgentConfig.Gateway {
		config.GAgentConfig.Gateway = heartbeatResponse.Gateway
		systemutil.DevopsGateway = heartbeatResponse.Gateway
		configChanged = true
	}
	if heartbeatResponse.FileGateway != "" && heartbeatResponse.FileGateway != config.GAgentConfig.FileGateway {
		config.GAgentConfig.FileGateway = heartbeatResponse.FileGateway
		configChanged = true
	}
	if config.GAgentConfig.DockerParallelTaskCount != heartbeatResponse.DockerParallelTaskCount {
		config.GAgentConfig.DockerParallelTaskCount = heartbeatResponse.DockerParallelTaskCount
		configChanged = true
	}

	if heartbeatResponse.Props.KeepLogsHours > 0 &&
		config.GAgentConfig.LogsKeepHours != heartbeatResponse.Props.KeepLogsHours {
		config.GAgentConfig.LogsKeepHours = heartbeatResponse.Props.KeepLogsHours
		configChanged = true
	}

	if heartbeatResponse.Props.IgnoreLocalIps != "" &&
		config.GAgentConfig.IgnoreLocalIps != heartbeatResponse.Props.IgnoreLocalIps {
		config.GAgentConfig.IgnoreLocalIps = heartbeatResponse.Props.IgnoreLocalIps
		configChanged = true
	}

	if heartbeatResponse.Language != "" &&
		config.GAgentConfig.Language != heartbeatResponse.Language {
		config.GAgentConfig.Language = heartbeatResponse.Language
		configChanged = true
	}

	if configChanged {
		_ = config.GAgentConfig.SaveConfig()
	}

	// agent环境变量
	config.GEnvVars = heartbeatResponse.Envs

	/*
	   忽略一些在Windows机器上VPN代理软件所产生的虚拟网卡（有Mac地址）的IP，一般这类IP
	   更像是一些路由器的192开头的IP，属于干扰IP，安装了这类软件的windows机器IP都会变成相同，所以需要忽略掉
	*/
	if len(config.GAgentConfig.IgnoreLocalIps) > 0 {
		splitIps := util.SplitAndTrimSpace(config.GAgentConfig.IgnoreLocalIps, ",")
		if util.Contains(splitIps, config.GAgentEnv.AgentIp) { // Agent检测到的IP与要忽略的本地VPN IP相同，则更换真正IP
			config.GAgentEnv.AgentIp = systemutil.GetAgentIp(splitIps)
		}
	}

	// 检测agent版本与agent文件是否匹配
	if config.AgentVersion != heartbeatResponse.MasterVersion {
		agentFileVersion := config.DetectAgentVersion()
		if agentFileVersion != "" && config.AgentVersion != agentFileVersion {
			logs.Warn("agent version mismatch, exiting agent process")
			systemutil.ExitProcess(1)
		}
	}

	logs.Info("agent heartbeat done")
	return nil
}
