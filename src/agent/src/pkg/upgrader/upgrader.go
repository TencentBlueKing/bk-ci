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

package upgrader

import (
	"errors"
	"fmt"
	"github.com/astaxie/beego/logs"
	"github.com/gofrs/flock"
	"pkg/api"
	"pkg/config"
	"pkg/util/command"
	"pkg/util/fileutil"
	"pkg/util/systemutil"
	"time"
)

func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")
	config.Init()

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.Error("get total lock failed, exit", err.Error())
		return errors.New("get total lock failed")
	}

	logs.Info("wait 10 seconds for agent to stop")
	time.Sleep(10 * time.Second)

	// GO_20190807 版非windows agent做重装升级替换daemon，其他只替换devopsAgent
	currentAgentVersion := config.GAgentEnv.AgentVersion
	if !systemutil.IsWindows() && currentAgentVersion == "GO_20190807" {
		err := UninstallAgent()
		if err != nil {
			return errors.New("uninstall agent failed")
		}

		fileutil.TryRemoveFile(systemutil.GetWorkDir() + "/agent.zip")
		api.DownloadAgentInstallScript(systemutil.GetWorkDir() + "/" + config.GetInstallScript())

		totalLock.Unlock()
		logs.Info(totalLock.Unlock())
		err = InstallAgent()
		if err != nil {
			logs.Error("install agent failed: ", err)
			return errors.New("install agent failed")
		}

		logs.Info("reinstall agent done, upgrade process exiting")
		return nil
	} else {
		err = replaceAgentFile()
		if err != nil {
			logs.Error("replace agent file failed: ", err.Error())
			return errors.New("replace agent file failed")
		}
		totalLock.Unlock()
	}
	logs.Info("agent upgrade done, upgrade process exiting")
	return nil
}

func DoUninstallAgent() error {
	err := UninstallAgent()
	if err != nil {
		logs.Error("uninstall agent failed: ", err.Error())
		return errors.New("uninstall agent failed")
	}
	return nil
}

func UninstallAgent() error {
	logs.Info("start uninstall agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetUninstallScript()
	_, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	return nil
}

func StopAgent() error {
	logs.Info("start stop agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStopScript()
	_, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	return nil
}

func StartAgent() error {
	logs.Info("start agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStartScript()
	_, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	return nil
}

func replaceAgentFile() error {
	logs.Info("replace agent file")
	src := systemutil.GetUpgradeDir() + "/" + config.GetClienAgentFile()
	dst := systemutil.GetWorkDir() + "/" + config.GetClienAgentFile()
	_, err := fileutil.CopyFile(src, dst, true)
	return err
}

func InstallAgent() error {
	logs.Info("start install agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetInstallScript()

	err := fileutil.SetExecutable(startCmd)
	if err != nil {
		return fmt.Errorf("chmod install script failed: %s", err.Error())
	}

	_, err = command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run install script failed: ", err.Error())
		return errors.New("run install script failed")
	}
	return nil
}
