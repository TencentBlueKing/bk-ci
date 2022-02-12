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

package upgrader

import (
	"errors"
	"fmt"
	"os"
	"strconv"
	"time"

	"github.com/Tencent/bk-ci/src/agent/src/pkg/config"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/command"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/Tencent/bk-ci/src/agent/src/pkg/util/systemutil"
	"github.com/astaxie/beego/logs"
	"github.com/gofrs/flock"
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
    defer func() { totalLock.Unlock() }()
	serr := StopAgent()
	if serr != nil {
		logs.Error("stop agent failed: ", err.Error())
		return err
	}

	logs.Info("wait 5 seconds for agent to stop")
	time.Sleep(5 * time.Second)

	err = replaceAgentFile(config.GetClienAgentFile())
	if err != nil {
		logs.Error("replace agent file failed: ", err.Error())
		return errors.New("replace agent file failed: " + err.Error())
	}

	err = replaceAgentFile(config.GetClientDaemonFile()) // 如果daemon进程仍然存在，则会替换失败，但以下也会退出

	if err != nil {
		logs.Error("replace daemon file failed: ", err.Error())
	}

	err2 := StartAgent()
	if err2 != nil {
		logs.Error("start daemon failed: ", err.Error())
		return err2
	}
	logs.Info("agent start done")
	logs.Info("agent upgrade done, upgrade process exiting")
	return nil
}

func tryKillAgentProcess() {
	logs.Info("try kill agent process")
	pidFile := fmt.Sprintf("%s/agent.pid", systemutil.GetRuntimeDir())
	agentPid, err := fileutil.GetString(pidFile)
	if err != nil {
		logs.Warn("read pid failed")
		return
	}
	intPid, err := strconv.Atoi(agentPid)
	if err != nil {
		logs.Warn("parse pid failed")
		return
	}
	process, err := os.FindProcess(intPid)
	if err != nil || process == nil {
		logs.Warn("find process failed")
		return
	} else {
		logs.Info("kill agent process, pid: ", intPid)
		process.Kill()
	}
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
		logs.Error("output: ", string(output))
		return errors.New("run uninstall script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}

func StopAgent() error {
	logs.Info("start stop agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStopScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run stop script failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("run stop script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}

func StartAgent() error {
	logs.Info("start agent")

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetStartScript()
	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run start script failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("run start script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}

func replaceAgentFile(fileName string) error {
	logs.Info("replace agent file: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName
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

	output, err := command.RunCommand(startCmd, []string{} /*args*/, workDir, nil)
	if err != nil {
		logs.Error("run install script failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("run install script failed")
	}
	logs.Info("output: ", string(output))
	return nil
}
