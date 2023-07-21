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
	"flag"
	"fmt"
	"os"
	"path/filepath"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/installer"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

const (
	installerProcess = "installer"
	agentProcess     = "agent"
	daemonProcess    = "daemon"
)

func main() {
	// 初始化日志
	logFilePath := filepath.Join(systemutil.GetWorkDir(), "logs", "devopsInstaller.log")
	err := logs.Init(logFilePath, false)
	if err != nil {
		fmt.Printf("init installer log error %v\n", err)
		systemutil.ExitProcess(1)
	}

	defer func() {
		if err := recover(); err != nil {
			logs.Error("panic: ", err)
		}
	}()

	if ok := systemutil.CheckProcess(installerProcess); !ok {
		logs.Warn("get installer process lock failed, exit")
		return
	}

	action := flag.String("action", "", "action, install or uninstall")
	flag.Parse()
	logs.Info("installer start, action: ", *action)
	logs.Info("pid: ", os.Getpid())
	logs.Info("current user userName: ", systemutil.GetCurrentUser().Username)
	logs.Info("work dir: ", systemutil.GetWorkDir())

	if config.ActionInstall == *action {
		err := installer.DoInstallAgent()
		if err != nil {
			logs.Error("install new agent failed: " + err.Error())
			systemutil.ExitProcess(1)
		}
	} else if config.ActionUninstall == *action {
		err := installer.DoUninstallAgent()
		if err != nil {
			logs.Error("uninstall agent failed")
			systemutil.ExitProcess(1)
		}
	} else {
		logs.Error("unsupported action: ", *action)
		systemutil.ExitProcess(1)
	}
	systemutil.ExitProcess(0)
}
