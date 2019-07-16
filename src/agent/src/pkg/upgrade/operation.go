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

package upgrade

import (
	"errors"
	"github.com/astaxie/beego/logs"
	"os"
	"pkg/api"
	"pkg/config"
	"pkg/util/command"
	"pkg/util/fileutil"
	"pkg/util/systemutil"
)

func UninstallAgent() {
	logs.Info("start uninstall agent")

	err := runUpgrader(config.ActionUninstall)
	if err != nil {
		logs.Error("start upgrader failed")
		return
	}
	logs.Warning("agent process exiting")
	systemutil.ExitProcess(0)
}

func runUpgrader(action string) error {
	logs.Info("start upgrader process")

	scripPath := config.GetAgentWorkdir() + "/tmp/" + config.GetClientUpgraderFile()

	if !systemutil.IsWindows() {
		err := os.Chmod(scripPath, 0777)
		if err != nil {
			logs.Error("chmod failed: ", err.Error())
			return errors.New("chmod failed: ")
		}
	}

	if action != config.ActionUninstall {
		action = config.ActionUpgrade
	}
	args := []string{"-action=" + action}

	pid, err := command.StartProcess(scripPath, args, config.GetAgentWorkdir(), nil, "")
	if err != nil {
		logs.Error("run upgrader failed: ", err.Error())
		return errors.New("run upgrader failed")
	}
	logs.Info("start process success, pid: ", pid)

	logs.Warning("agent process exiting")
	systemutil.ExitProcess(0)
	return nil
}

func DoUpgradeOperation(agentChanged bool, workAgentChanged bool) error {
	logs.Info("start upgrade, agent changed: ", agentChanged, ", work agent changed: ", workAgentChanged)
	config.GIsAgentUpgrading = true
	defer func() {
		config.GIsAgentUpgrading = false
		api.FinishUpgrade(true)
	}()

	if !agentChanged && !workAgentChanged {
		logs.Info("no change to upgrade, skip")
		return nil
	}

	if workAgentChanged {
		logs.Info("work agent changed, replace work agent file")
		_, err := fileutil.CopyFile(config.GetAgentWorkdir()+"/tmp/"+config.WorkAgentFile, config.GetAgentWorkdir()+"/"+config.WorkAgentFile, true)
		if err != nil {
			logs.Error("replace work agent file failed: ", err.Error())
			return errors.New("replace work agent file failed")
		}
		logs.Info("relace agent file done")

		config.GAgentEnv.SlaveVersion = config.DetectSlaveVersion()
	}

	if agentChanged {
		logs.Info("agent changed, start upgrader")
		err := runUpgrader(config.ActionUpgrade)
		if err != nil {
			return err
		}
	} else {
		logs.Info("agent not changed, skip agent upgrade")
	}
	return nil
}
