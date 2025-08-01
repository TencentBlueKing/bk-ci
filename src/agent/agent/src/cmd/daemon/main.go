//go:build linux || darwin
// +build linux darwin

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
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"

	"github.com/gofrs/flock"
)

const (
	daemonProcess = "daemon"
	agentCheckGap = 5 * time.Second
)

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
	err := logs.Init(logFilePath, isDebug, false)
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

	if ok := systemutil.CheckProcess(daemonProcess); !ok {
		logs.Warn("get process lock failed, exit")
		return
	}

	logs.Info("devops daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("workDir: ", workDir)

	watch(isDebug)
	systemutil.KeepProcessAlive()
}

func watch(isDebug bool) {
	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))

	// first check immediately
	totalLock.Lock()
	doCheckAndLaunchAgent(isDebug)
	totalLock.Unlock()

	checkTimeTicker := time.NewTicker(agentCheckGap)
	for ; ; totalLock.Unlock() {
		select {
		case <-checkTimeTicker.C:
			if err := totalLock.Lock(); err != nil {
				logs.WithError(err).Error("failed to get agent lock")
				continue
			}

			doCheckAndLaunchAgent(isDebug)
		}
	}
}

func doCheckAndLaunchAgent(isDebug bool) {
	workDir := systemutil.GetWorkDir()
	agentLock := flock.New(fmt.Sprintf("%s/agent.lock", systemutil.GetRuntimeDir()))

	locked, err := agentLock.TryLock()
	if err == nil && locked {
		// #1613 fix open too many files
		defer func() {
			err = agentLock.Unlock()
			if err != nil {
				logs.WithError(err).Error("try to unlock agent.lock failed")
			}
		}()
	}
	if err != nil {
		logs.WithError(err).Error("try to get agent.lock failed")
		return
	}
	if !locked {
		return
	}

	logs.Warn("agent is not available, will launch it")

	process, err := launch(workDir+"/"+config.AgentFileClientLinux, isDebug)
	if err != nil {
		logs.WithError(err).Error("launch agent failed")
		return
	}
	if process == nil {
		logs.Error("launch agent failed: got a nil process")
		return
	}
	logs.Infof("success to launch agent, pid: %d", process.Pid)
}

func launch(agentPath string, isDebug bool) (*os.Process, error) {
	var cmd *exec.Cmd
	if isDebug {
		cmd = exec.Command(agentPath, "debug")
	} else {
		cmd = exec.Command(agentPath)
	}

	cmd.Dir = systemutil.GetWorkDir()

	logs.Infof("start devops agent: %s", cmd.String())
	if !fileutil.Exists(agentPath) {
		return nil, fmt.Errorf("agent file %s not exists", agentPath)
	}

	err := fileutil.SetExecutable(agentPath)
	if err != nil {
		return nil, errors.Wrap(err, "chmod agent file failed")
	}

	// 获取 agent 的错误输出，这样有助于打印出崩溃的堆栈方便排查问题
	stdErr, errstd := cmd.StderrPipe()
	if errstd != nil {
		logs.WithError(errstd).Error("get agent stderr pipe error")
	}

	if err = cmd.Start(); err != nil {
		if stdErr != nil {
			stdErr.Close()
		}
		return nil, errors.Wrap(err, "start agent failed")
	}

	go func() {
		if err := cmd.Wait(); err != nil {
			if exiterr, ok := err.(*exec.ExitError); ok {
				if exiterr.ExitCode() == constant.DaemonExitCode {
					logs.Warnf("exit code %d daemon exit", constant.DaemonExitCode)
					systemutil.ExitProcess(constant.DaemonExitCode)
				}
			}
			logs.WithError(err).Error("agent process error")
			if errstd != nil {
				return
			}
			defer stdErr.Close()
			out, err := io.ReadAll(stdErr)
			if err != nil {
				logs.WithError(err).Error("read agent stderr out error")
				return
			}
			logs.Error("agent process error out", string(out))
		}
	}()

	return cmd.Process, nil
}
