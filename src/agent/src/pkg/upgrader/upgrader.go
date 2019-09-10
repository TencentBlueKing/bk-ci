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
	"github.com/astaxie/beego/logs"
	"pkg/config"
	"pkg/util/command"
	"pkg/util/fileutil"
	"pkg/util/systemutil"
	"time"
)

func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")

	logs.Info("wait 15 seconds for agent to stop")
	time.Sleep(10 * time.Second)

	err := replaceAgentFile()
	if err != nil {
		logs.Error("replace agent file failed: ", err.Error())
		return errors.New("replace agent file failed")
	}

	logs.Info("agent upgrade done, upgrader exiting")
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
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	logs.Info("script output: ", string(output))
	return nil
}

func StopAgent() error {
	logs.Info("start stop agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStopScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	logs.Info("script output: ", string(output))
	return nil
}

func StartAgent() error {
	logs.Info("start agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStartScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run uninstall script failed: ", err.Error())
		return errors.New("run uninstall script failed")
	}
	logs.Info("script output: ", string(output))
	return nil
}

func replaceAgentFile() error {
	logs.Info("replace agent file")
	src := systemutil.GetWorkDir() + "/tmp/" + config.GetClienAgentFile()
	dst := systemutil.GetWorkDir() + "/" + config.GetClienAgentFile()
	_, err := fileutil.CopyFile(src, dst, true)
	if err != nil {
		return err
	}
	return err
}

func InstallAgent() error {
	logs.Info("start install agent")

	workDir := systemutil.GetWorkDir()

	startCmd := workDir + "/" + config.GetInstallScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run install script failed: ", err.Error())
		return errors.New("run install script failed")
	}
	logs.Info("script output: ", string(output))
	return nil
}
