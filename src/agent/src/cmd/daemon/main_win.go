//go:build windows
// +build windows

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

package main

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/kardianos/service"
)

const daemonProcess = "daemon"

func main() {
	isDebug := false
	if len(os.Args) == 2 {
		switch os.Args[1] {
		case "version":
			fmt.Println(config.AgentVersion)
			systemutil.ExitProcess(0)
		case "fullVersion":
			fmt.Println(config.AgentVersion)
			fmt.Println(config.GitCommit)
			fmt.Println(config.BuildTime)
			systemutil.ExitProcess(0)
		case "debug":
			isDebug = true
		}
	}

	// 初始化日志
	logFilePath := filepath.Join(systemutil.GetWorkDir(), "logs", "devopsDaemon.log")
	err := logs.Init(logFilePath, isDebug)
	if err != nil {
		fmt.Printf("init daemon log error %v\n", err)
		systemutil.ExitProcess(1)
	}

	logs.Infof("GOOS=%s, GOARCH=%s", runtime.GOOS, runtime.GOARCH)

	workDir := systemutil.GetExecutableDir()
	err = os.Chdir(workDir)
	if err != nil {
		logs.Info("change work dir failed, err: ", err.Error())
		systemutil.ExitProcess(1)
	}

	defer func() {
		if err := recover(); err != nil {
			logs.Error("panic: ", err)
			systemutil.ExitProcess(1)
		}
	}()

	logs.Info("devops daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("workDir: ", workDir)

	//服务定义
	serviceConfig := &service.Config{
		Name:             "name",
		DisplayName:      "displayName",
		Description:      "description",
		WorkingDirectory: "C:/data/landun",
	}

	if ok := systemutil.CheckProcess(daemonProcess); !ok {
		logs.Info("get process lock failed, exit")
		return
	}

	daemonProgram := &program{}
	sys := service.ChosenSystem()
	daemonService, err := sys.New(daemonProgram, serviceConfig)
	if err != nil {
		logs.Error("Init service error: ", err.Error())
		systemutil.ExitProcess(1)
	}

	err = daemonService.Run()
	if err != nil {
		logs.Error("run agent program error: ", err.Error())
	}
}

var GAgentProcess *os.Process = nil

func watch() {
	workDir := systemutil.GetExecutableDir()
	var agentPath = systemutil.GetWorkDir() + "/devopsAgent.exe"
	for {
		cmd := exec.Command(agentPath)
		cmd.Dir = workDir

		logs.Info("start devops agent")
		if !fileutil.Exists(agentPath) {
			logs.Error("agent file: ", agentPath, " not exists")
			logs.Info("restart after 30 seconds")
			time.Sleep(30 * time.Second)
		}

		err := fileutil.SetExecutable(agentPath)
		if err != nil {
			logs.Error("chmod failed, err: ", err.Error())
			logs.Info("restart after 30 seconds")
			time.Sleep(30 * time.Second)
			continue
		}

		err = cmd.Start()
		if err != nil {
			logs.Error("agent start failed, err: ", err.Error())
			logs.Info("restart after 30 seconds")
			time.Sleep(30 * time.Second)
			continue
		}

		GAgentProcess = cmd.Process
		logs.Info("devops agent started, pid: ", cmd.Process.Pid)
		_, err = cmd.Process.Wait()
		if err != nil {
			logs.Error("process wait error", err.Error())
		}
		logs.Info("agent process exited")

		logs.Info("restart after 30 seconds")
		time.Sleep(30 * time.Second)
	}
}

type program struct {
}

func (p *program) Start(s service.Service) error {
	go watch()
	return nil
}

func (p *program) Stop(s service.Service) error {
	p.tryStopAgent()
	return nil
}

func (p *program) tryStopAgent() {
	if GAgentProcess != nil {
		GAgentProcess.Kill()
	}
}
