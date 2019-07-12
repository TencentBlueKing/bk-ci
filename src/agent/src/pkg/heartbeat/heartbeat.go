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

package heartbeat

import (
	"errors"
	"github.com/astaxie/beego/logs"
	"pkg/api"
	"pkg/config"
	"pkg/job"
	"pkg/upgrade"
	"pkg/util"
	"time"
)

func DoAgentHeartbeat() {
	for {
		err := newAgentHeartbeat()
		if err != nil {
			logs.Info("new heartbeat failed, try old heartbeat")
		}
		time.Sleep(10 * time.Second)
	}
}

func newAgentHeartbeat() error {
	result, err := api.NewAgentHeartbeat(job.GBuildManager.GetInstances())
	if err != nil {
		logs.Error("agent heartbeat(new) failed: ", err.Error())
		return errors.New("agent heartbeat(new) failed")
	}
	if result.IsNotOk() {
		logs.Error("agent heartbeat(new) failed: ", result.Message)
		return errors.New("agent heartbeat(new) failed")
	}

	heartbeatResponse := new(api.AgentHeartbeatResponse)
	err = util.ParseJsonToData(result.Data, &heartbeatResponse)
	if err != nil {
		logs.Error("agent heartbeat(new) failed: ", err.Error())
		return errors.New("agent heartbeat(new) failed")
	}

	if heartbeatResponse.AgentStatus == config.AgentStatusDelete {
		upgrade.UninstallAgent()
		return nil
	}

	// 修改agent配置
	if config.GAgentConfig.ParallelTaskCount != heartbeatResponse.ParallelTaskCount {
		config.GAgentConfig.ParallelTaskCount = heartbeatResponse.ParallelTaskCount
		config.GAgentConfig.SaveConfig()
	}

	// agent环境变量
	config.GEnvVars = heartbeatResponse.Envs
	logs.Info("agent heartbeat(new) done")
	return nil
}
