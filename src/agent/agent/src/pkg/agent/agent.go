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
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/i18n"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/imagedebug"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/job"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/pipeline"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/upgrade"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util"
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

	// 定期清理
	go cron.CleanJob()
	go cron.CleanDebugContainer()

	initModules()

	for {
		doAsk()
		time.Sleep(5 * time.Second)
	}
}

// 初始化一些模块的初始值
func initModules() {
	imagedebug.Init()
}

func doAsk() {
	// Ask请求
	exiterror := exitcode.GetAndResetExitError()
	enable := genAskEnable()
	heart, upgrad := genHeartInfoAndUpgrade(enable.Upgrade, exiterror)
	result, err := api.Ask(&api.AskInfo{
		Enable:  enable,
		Heart:   heart,
		Upgrade: upgrad,
	})

	// 每次发送完请求都判断下是否存在退出报错
	doneRequestExitError := exitcode.GetAndResetExitError()
	// 发送前和发送后都有需要专门打印下日志
	if exiterror != nil || doneRequestExitError != nil {
		if exiterror != nil && doneRequestExitError != nil {
			logs.Errorf("ExitError|%s|%s", exiterror.ErrorEnum, exiterror.Message)
			exitcode.Exit(doneRequestExitError)
		}
		if exiterror != nil {
			exitcode.Exit(exiterror)
		}
		exitcode.Exit(doneRequestExitError)
	}

	if err != nil {
		logs.Error("ask request failed: ", err.Error())
		return
	}
	if result.IsNotOk() {
		logs.Error("ask request result failed: ", result.Message)
		return
	}
	if result.AgentStatus != config.AgentStatusImportOk {
		logs.Errorf("agent status [%s] not ok", result.AgentStatus)
		if result.IsAgentDelete() {
			logs.Warn("agent has deleted, uninstall")
			upgrade.UninstallAgent()
			return
		}
		return
	}

	resp := new(api.AskResp)
	err = util.ParseJsonToData(result.Data, &resp)
	if err != nil {
		logs.Error("parse ask resp failed: ", err.Error())
		return
	}

	// 执行各类任务
	doAgentJob(enable, resp)
}

func doAgentJob(enable api.AskEnable, resp *api.AskResp) {
	if resp.Heart != nil {
		go agentHeartbeat(resp.Heart)
	}

	hasBuild := (enable.Build != api.NoneBuildType) && (resp.Build != nil)
	if hasBuild {
		go job.DoBuild(resp.Build)
	}

	if enable.Upgrade && resp.Upgrade != nil {
		go upgrade.AgentUpgrade(resp.Upgrade, hasBuild)
	}

	if enable.Pipeline && resp.Pipeline != nil {
		go pipeline.RunPipeline(resp.Pipeline)
	}

	if enable.DockerDebug && resp.Debug != nil {
		go imagedebug.DoImageDebug(resp.Debug)
	}
}
