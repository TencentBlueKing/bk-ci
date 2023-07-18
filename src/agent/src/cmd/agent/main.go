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
	"math/rand"
	"os"
	"path/filepath"
	"runtime"
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/agent"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

const (
	agentProcess = "agent"
)

func main() {
	rand.Seed(time.Now().UnixNano())

	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent main panic: ", err)
		}
	}()

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
	logFilePath := filepath.Join(systemutil.GetWorkDir(), "logs", "devopsAgent.log")
	err := logs.Init(logFilePath, isDebug)
	if err != nil {
		fmt.Printf("init agent log error %v\n", err)
		systemutil.ExitProcess(1)
	}

	logs.Infof("GOOS=%s, GOARCH=%s", runtime.GOOS, runtime.GOARCH)

	// 以agent安装目录为工作目录
	workDir := systemutil.GetExecutableDir()
	err = os.Chdir(workDir)
	if err != nil {
		logs.Info("change work dir failed, err: ", err.Error())
		systemutil.ExitProcess(1)
	}

	if ok := systemutil.CheckProcess(agentProcess); !ok {
		logs.Warn("get process lock failed, exit")
		return
	}

	logs.Info("agent start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("agent version: ", config.AgentVersion)
	logs.Info("git commit: ", config.GitCommit)
	logs.Info("build time: ", config.BuildTime)
	logs.Info("current user userName: ", systemutil.GetCurrentUser().Username)
	logs.Info("work dir: ", systemutil.GetWorkDir())

	logEnv()

	agent.Run(isDebug)
}

func logEnv() {
	logs.Info("agent envs: ")
	for _, v := range os.Environ() {
		index := strings.Index(v, "=")
		logs.Info("    " + v[0:index] + " = " + v[index+1:])
	}
}
