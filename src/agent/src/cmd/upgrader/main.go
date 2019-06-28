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

package main

import (
	"encoding/json"
	"flag"
	"github.com/astaxie/beego/logs"
	"os"
	"pkg/config"
	"pkg/upgrader"
	"pkg/util/systemutil"
	"runtime"
)

func main() {
	runtime.GOMAXPROCS(4)
	initLog()
	defer func() {
		if err := recover(); err != nil {
			logs.Error("panic: ", err)
		}
	}()

	action := flag.String("action", "upgrade", "action, upgrade or uninstall")
	flag.Parse()
	logs.Info("upgrader start, action: ", *action)
	logs.Info("pid: ", os.Getpid())
	logs.Info("current user userName: ", systemutil.GetCurrentUser().Username)
	logs.Info("work dir: ", systemutil.GetWorkDir())

	if config.ActionUpgrade == *action {
		err := upgrader.DoUpgradeAgent()
		if err != nil {
			logs.Error("upgrade agent failed")
			systemutil.ExitProcess(1)
		}
	} else if config.ActionUninstall == *action {
		err := upgrader.DoUninstallAgent()
		if err != nil {
			logs.Error("upgrade agent failed")
			systemutil.ExitProcess(1)
		}
	} else {
		logs.Error("unsupport action")
		systemutil.ExitProcess(1)
	}
	systemutil.ExitProcess(0)
}

func initLog() {
	logConfig := make(map[string]string)
	logConfig["filename"] = systemutil.GetWorkDir() + "/logs/devopsUpgrader.log"
	logConfig["perm"] = "0666"
	jsonConfig, _ := json.Marshal(logConfig)
	logs.SetLogger(logs.AdapterFile, string(jsonConfig))
}
