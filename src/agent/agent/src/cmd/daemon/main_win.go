//go:build windows
// +build windows

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

package main

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"
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
	workDir := systemutil.GetExecutableDir()
	err := logs.Init(filepath.Join(workDir, "logs", "devopsDaemon.log"), isDebug, false)
	if err != nil {
		fmt.Printf("init daemon log error %v\n", err)
		systemutil.ExitProcess(1)
	}

	logs.Infof("GOOS=%s, GOARCH=%s", runtime.GOOS, runtime.GOARCH)

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

	if ok := systemutil.CheckProcess(daemonProcess); !ok {
		logs.Info("get process lock failed, exit")
		return
	}

	logs.Info("devops daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("workDir: ", workDir)

	//服务定义
	serviceConfig := &service.Config{
		Name: "name",
	}

	daemonProgram := &program{}
	sys := service.ChosenSystem()
	daemonService, err := sys.New(daemonProgram, serviceConfig)
	if err != nil {
		logs.WithError(err).Error("Init service error")
		systemutil.ExitProcess(1)
	}

	err = daemonService.Run()
	if err != nil {
		logs.WithError(err).Error("run agent program error")
	}
}

var GAgentProcess *os.Process = nil

func watch() {
	workDir := systemutil.GetExecutableDir()
	var agentPath = systemutil.GetWorkDir() + "/devopsAgent.exe"
	for {
		func() {
			cmd := exec.Command(agentPath)
			cmd.Dir = workDir

			logs.Info("start devops agent")
			if !fileutil.Exists(agentPath) {
				logs.Errorf("agent file: %s not exists", agentPath)
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
			}

			err := fileutil.SetExecutable(agentPath)
			if err != nil {
				logs.WithError(err).Error("chmod failed, err")
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
				return
			}

			err = cmd.Start()
			if err != nil {
				logs.WithError(err).Error("agent start failed, err")
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
				return
			}

			GAgentProcess = cmd.Process
			logs.Info("devops agent started, pid: ", cmd.Process.Pid)
			err = cmd.Wait()
			if err != nil {
				var exitErr *exec.ExitError
				if errors.As(err, &exitErr) {
					if exitErr.ExitCode() == constant.DaemonExitCode {
						logs.Warnf("exit code %d daemon exit", constant.DaemonExitCode)
						systemutil.ExitProcess(constant.DaemonExitCode)
					}
				}
				logs.WithError(err).Error("agent process error")
			}
			logs.Info("agent process exited")

			logs.Info("restart after 30 seconds")
			time.Sleep(30 * time.Second)
		}()
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
