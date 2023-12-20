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

package agent

import (
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/collector"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/cron"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

func Run(isDebug bool) {
	config.Init(isDebug)

	// 初始化国际化
	i18n.InitAgentI18n()

	_, err := job.AgentStartup()
	if err != nil {
		logs.Warn("agent startup failed: ", err.Error())
	}

	// 数据采集
	go collector.DoAgentCollect()

	// // 心跳
	// go heartbeat.DoAgentHeartbeat()

	// // 检查升级
	// go upgrade.DoPollAndUpgradeAgent()

	// // 启动pipeline
	// go pipeline.Start()

	// 定期清理
	go cron.CleanJob()
	go cron.CleanDebugContainer()

	// // 登录调试任务
	// go imagedebug.DoPullAndDebug()

	// job.DoPollAndBuild()
	doAsk()
}

func doAsk() {
	for {
		time.Sleep(5 * time.Second)

		// 判断除了 heartbeat 的其他模块是否需要获取
		buildType := checkBuildType()
		upgrade := checkUpgrade()
		dockerDebug := checkDockerDebug()
		pipeline := checkPipeline()

		
	}
}

func checkBuildType() api.BuildJobType {
	dockerCanRun, normalCanRun := job.CheckParallelTaskCount()
	if !dockerCanRun && !normalCanRun {
		return api.NoneBuildType
	}
	if dockerCanRun && normalCanRun {
		return api.AllBuildType
	} else if normalCanRun {
		return api.BinaryBuildType
	} else {
		return api.DockerBuildType
	}
}

func checkUpgrade() bool {
	if job.CheckRunningJob() {
		return false
	}
	return true
}

func checkDockerDebug() bool {
	if config.GAgentConfig.EnableDockerBuild {
		return true
	}
	return false
}

func checkPipeline() bool {
	if config.GAgentConfig.EnablePipeline {
		return true
	}
	return false
}
