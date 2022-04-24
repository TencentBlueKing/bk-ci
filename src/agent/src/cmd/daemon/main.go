//go:build linux || darwin
// +build linux darwin

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
	"runtime"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/astaxie/beego/logs"
	"github.com/gofrs/flock"

	"encoding/json"
)

const (
	daemonProcess = "daemon"
	agentCheckGap = 5 * time.Second
)

func main() {
	if len(os.Args) == 2 && os.Args[1] == "version" {
		fmt.Println(config.AgentVersion)
		systemutil.ExitProcess(0)
	}
	logs.Info("GOOS=%s, GOARCH=%s", runtime.GOOS, runtime.GOARCH)

	runtime.GOMAXPROCS(4)

	workDir := systemutil.GetExecutableDir()
	err := os.Chdir(workDir)
	if err != nil {
		logs.Info("change work dir failed, err: ", err.Error())
		systemutil.ExitProcess(1)
	}

	initLog()
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

	watch()
	systemutil.KeepProcessAlive()
}

func initLog() {
	logConfig := make(map[string]string)
	logConfig["filename"] = systemutil.GetWorkDir() + "/logs/devopsDaemon.log"
	logConfig["perm"] = "0666"
	jsonConfig, _ := json.Marshal(logConfig)
	logs.SetLogger(logs.AdapterFile, string(jsonConfig))
}

func watch() {
	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))

	// first check immediately
	totalLock.Lock()
	doCheckAndLaunchAgent()
	totalLock.Unlock()

	checkTimeTicker := time.NewTicker(agentCheckGap)
	for ; ; totalLock.Unlock() {
		select {
		case <-checkTimeTicker.C:
			if err := totalLock.Lock(); err != nil {
				logs.Error("failed to get agent lock: %v", err)
				continue
			}

			doCheckAndLaunchAgent()
		}
	}
}

func doCheckAndLaunchAgent() {
	workDir := systemutil.GetWorkDir()
	agentLock := flock.New(fmt.Sprintf("%s/agent.lock", systemutil.GetRuntimeDir()))

	locked, err := agentLock.TryLock()
	if err == nil && locked {
		// #1613 fix open too many files
		defer func() {
			err = agentLock.Unlock()
			logs.Error("try to unlock agent.lock failed: %v", err)
		}()
	}
	if err != nil {
		logs.Error("try to get agent.lock failed: %v", err)
		return
	}
	if !locked {
		return
	}

	logs.Warn("agent is not available, will launch it")

	process, err := launch(workDir + "/" + config.AgentFileClientLinux)
	if err != nil {
		logs.Error("launch agent failed: %v", err)
		return
	}
	if process == nil {
		logs.Error("launch agent failed: got a nil process")
		return
	}
	logs.Info("success to launch agent, pid: %d", process.Pid)
}

func launch(agentPath string) (*os.Process, error) {
	cmd := exec.Command(agentPath)
	cmd.Dir = systemutil.GetWorkDir()

	logs.Info("start devops agent: %s", agentPath)
	if !fileutil.Exists(agentPath) {
		return nil, fmt.Errorf("agent file %s not exists", agentPath)
	}

	err := fileutil.SetExecutable(agentPath)
	if err != nil {
		return nil, fmt.Errorf("chmod agent file failed: %v", err)
	}

	if err = cmd.Start(); err != nil {
		return nil, fmt.Errorf("start agent failed: %v", err)
	}

	go func() {
		_ = cmd.Wait()
	}()

	return cmd.Process, nil
}
