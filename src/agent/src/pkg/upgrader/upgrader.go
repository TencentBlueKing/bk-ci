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

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"

	"github.com/gofrs/flock"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")
	config.Init(false)

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.Error("get total lock failed, exit", err.Error())
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	daemonChange, _ := checkUpgradeFileChange(config.GetClientDaemonFile())
	/*
		#4686
		 1、kill devopsDaemon进程的行为在 macos 下， 如果当前是由 launchd 启动的（比如mac重启之后，devopsDaemon会由launchd接管启动）
			当upgrader进程触发kill devopsDaemon时，会导致当前upgrader进程也被系统一并停掉，所以要排除macos的进程停止操作，否则会导致升级中断

		 2、windows 因早期daemon缺失 pid文件，在安装多个agent的机器上无法很正确的寻找到正确的进程，并且windows的启动方式较多，早期用户会使用
		直接双击devopsDaemon.exe文件来启动，以此来保证构建进程能够正确拉起带UI的程序，所以这块无法正确查找到进程，因此暂时也不考虑windows的
		devopsDaemon.exe文件升级。 windows需要手动升级
	*/
	if daemonChange && systemutil.IsLinux() {
		tryKillAgentProcess(daemonProcess) // macos 在升级后只能使用手动重启
	}

	agentChange, _ := checkUpgradeFileChange(config.GetClienAgentFile())
	if agentChange {
		tryKillAgentProcess(agentProcess)
	}

	if !agentChange && !daemonChange {
		logs.Info("upgrade nothing, exit")
		return nil
	}

	logs.Info("wait 2 seconds for agent to stop")
	time.Sleep(2 * time.Second)

	if agentChange {
		err = replaceAgentFile(config.GetClienAgentFile())
		if err != nil {
			logs.Error("replace agent file failed: ", err.Error())
		}
	}

	if daemonChange {
		err = replaceAgentFile(config.GetClientDaemonFile()) // #4686 如果windows下daemon进程仍然存在，则会替换失败
		if err != nil {
			logs.Error("replace daemon file failed: ", err.Error())
		}
		if systemutil.IsLinux() { // #4686 如上，上面仅停止Linux的devopsDaemon进程，则也只重启动Linux的
			if startErr := StartDaemon(); startErr != nil {
				logs.Error("start daemon failed: ", startErr.Error())
				return startErr
			}
			logs.Info("agent start done")
		}
	}
	logs.Info("agent upgrade done, upgrade process exiting")
	return nil
}

func tryKillAgentProcess(processName string) {
	logs.Info(fmt.Sprintf("try kill %s process", processName))
	pidFile := fmt.Sprintf("%s/%s.pid", systemutil.GetRuntimeDir(), processName)
	agentPid, err := fileutil.GetString(pidFile)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid failed: %s", processName, err))
		return
	}
	intPid, err := strconv.Atoi(agentPid)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid: %s failed", processName, agentPid))
		return
	}
	process, err := os.FindProcess(intPid)
	if err != nil || process == nil {
		logs.Warn(fmt.Sprintf("find %s process pid: %s failed", processName, agentPid))
		return
	} else {
		logs.Info(fmt.Sprintf("kill %s process, pid: %s", processName, agentPid))
		err = process.Kill()
		if err != nil {
			logs.Warn(fmt.Sprintf("kill %s pid: %s failed: %s", processName, agentPid, err))
			return
		}
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

func checkUpgradeFileChange(fileName string) (change bool, err error) {

	oldMd5, err := fileutil.GetFileMd5(systemutil.GetWorkDir() + "/" + fileName)
	if err != nil {
		logs.Error(fmt.Sprintf("[agentUpgrade]|check %s md5 failed", fileName), err)
		return false, errors.New("check old md5 failed")
	}

	newMd5, err := fileutil.GetFileMd5(systemutil.GetUpgradeDir() + "/" + fileName)
	if err != nil {
		logs.Error(fmt.Sprintf("[agentUpgrade]|check %s md5 failed", fileName), err)
		return false, errors.New("check new md5 failed")
	}

	return oldMd5 != newMd5, nil
}

func StartDaemon() error {
	logs.Info("starting ", config.GetClientDaemonFile())

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetClientDaemonFile()

	if err := fileutil.SetExecutable(startCmd); err != nil {
		logs.Warn(fmt.Errorf("chmod daemon file failed: %v", err))
		return err
	}

	pid, err := command.StartProcess(startCmd, nil, workDir, nil, "")
	logs.Info("pid: ", pid)
	if err != nil {
		logs.Error("run start daemon failed: ", err.Error())
		return err
	}
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

func replaceAgentFile(fileName string) error {
	logs.Info("replace agent file: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName
	if _, err := fileutil.CopyFile(src, dst, true); err != nil {
		logs.Warn(fmt.Sprintf("copy file %s to %s failed: %s", src, dst, err))
		return err
	}
	if err := fileutil.SetExecutable(dst); err != nil {
		logs.Warn(fmt.Sprintf("chmod %s file failed: %s", dst, err))
		return err
	}
	return nil
}
