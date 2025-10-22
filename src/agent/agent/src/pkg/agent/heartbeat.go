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

package agent

import (
	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

func agentHeartbeat(heartbeatResponse *api.AgentHeartbeatResponse) {
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

	if heartbeatResponse.Props.EnablePipeline != config.GAgentConfig.EnablePipeline {
		config.GAgentConfig.EnablePipeline = heartbeatResponse.Props.EnablePipeline
		configChanged = true
	}

	if configChanged {
		_ = config.GAgentConfig.SaveConfig()
	}

	// agent环境变量
	if heartbeatResponse.Envs != nil {
		if config.GApiEnvVars.Size() <= 0 {
			config.GApiEnvVars.SetEnvs(heartbeatResponse.Envs)
		} else {
			flag := false
			config.GApiEnvVars.RangeDo(func(k, v string) bool {
				if heartbeatResponse.Envs[k] != v {
					flag = true
					return false
				}
				return true
			})
			if flag {
				config.GApiEnvVars.SetEnvs(heartbeatResponse.Envs)
			}
		}
	}

	/*
	   忽略一些在Windows机器上VPN代理软件所产生的虚拟网卡（有Mac地址）的IP，一般这类IP
	   更像是一些路由器的192开头的IP，属于干扰IP，安装了这类软件的windows机器IP都会变成相同，所以需要忽略掉
	*/
	if len(config.GAgentConfig.IgnoreLocalIps) > 0 {
		splitIps := util.SplitAndTrimSpace(config.GAgentConfig.IgnoreLocalIps, ",")
		if util.Contains(splitIps, config.GAgentEnv.GetAgentIp()) { // Agent检测到的IP与要忽略的本地VPN IP相同，则更换真正IP
			config.GAgentEnv.SetAgentIp(systemutil.GetAgentIp(splitIps))
		}
	}

	logs.Debug("agent heartbeat done")
}
