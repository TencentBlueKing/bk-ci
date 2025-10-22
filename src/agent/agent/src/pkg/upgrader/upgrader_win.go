//go:build windows

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

package upgrader

import (
	"fmt"
	"os"
	"strconv"
	"time"

	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/wintask"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/capnspacehook/taskmaster"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"

	"github.com/gofrs/flock"

	"github.com/shirou/gopsutil/v4/process"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

// DoUpgradeAgent 升级agent
// 1、通过service启动的daemon因为go本身内部注册了daemon导致权限模型有些未知问题，无法更新daemon后启动，只能更新agent
// 2、通过执行计划启动的daemon因为具有登录态，可以直接执行脚本拉起，如果执行计划存在问题，则无法拉起，需要使用 1 中的方式更新
// 3、用户双击启动的daemon和service一样，无法更新daemon，只能更新agent
func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")
	config.Init(false)
	if err := third_components.Init(); err != nil {
		logs.WithError(err).Error("init third_components error")
		systemutil.ExitProcess(1)
	}

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.WithError(err).Error("get total lock failed, exit")
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	startT := wintask.ManualStart
	var winTask *taskmaster.RegisteredTask = nil
	// 先查询服务
	serviceName := "devops_agent_" + config.GAgentConfig.AgentId
	ok := wintask.FindService(serviceName)
	if ok {
		startT = wintask.ServiceStart
	} else {
		if task, taskOk := wintask.FindTask(serviceName); taskOk {
			// 启用了的task才能进行升级后的启动，否则不能升级Daemon
			if task.Enabled &&
				(task.State == taskmaster.TASK_STATE_READY || task.State == taskmaster.TASK_STATE_RUNNING) {
				winTask = task
				startT = wintask.TaskStart
			} else {
				logs.Warnf("win task exist but not enable state: %d", task.State)
			}
		}
	}
	// 理论上不可能，但是作为补充可以为后文提供逻辑依据
	if startT == wintask.TaskStart && winTask == nil {
		logs.Warn("win task not exist update agent")
		startT = wintask.ManualStart
	}

	logs.Infof("agent process start by %s", startT)

	daemonChange := false
	if startT == wintask.TaskStart {
		daemonChange, err = checkUpgradeFileChange(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Warn("check daemon upgrade file change failed")
		}
	}
	daemonPid := 0
	if daemonChange {
		daemonPid, err = tryKillAgentProcess(daemonProcess)
		if err != nil {
			logs.WithError(err).Error(fmt.Sprintf("try kill daemon process failed"))
		}
	}

	agentChange, err := checkUpgradeFileChange(config.GetClienAgentFile())
	if err != nil {
		logs.WithError(err).Warn("check agent upgrade file change failed")
	}
	agentPid := 0
	if agentChange {
		agentPid, err = tryKillAgentProcess(agentProcess)
		if err != nil {
			logs.WithError(err).Error(fmt.Sprintf("try kill agent process failed"))
		}
	}

	if !agentChange && !daemonChange {
		logs.Info("upgrade nothing, exit")
		return nil
	}

	// 检查进程是否被杀掉
	daemonExist := true
	agentExist := true
	for i := 0; i < 15; i++ {
		if daemonExist && daemonPid != 0 {
			exist, err := process.PidExists(int32(daemonPid))
			if err != nil {
				logs.WithError(err).Errorf("check daemon process exist failed, pid: %d", daemonPid)
			}
			daemonExist = exist
		}
		if agentExist && agentPid != 0 {
			exist, err := process.PidExists(int32(agentPid))
			if err != nil {
				logs.WithError(err).Errorf("check agent process exist failed, pid: %d", agentPid)
			}
			agentExist = exist
		}
		if (!daemonChange || !daemonExist) && !agentExist {
			logs.Infof("wait %d seconds for agent to stop done", i+1)
			break
		} else if i == 14 {
			logs.Errorf("upgrade daemon exist %t, agent exist %t, can't upgrade", !daemonChange || !daemonExist, agentExist)
			return nil
		}
		logs.Infof("wait %d seconds for agent to stop", i+1)
		time.Sleep(1 * time.Second)
	}

	// 替换更新文件
	if agentChange {
		err = replaceAgentFile(config.GetClienAgentFile())
		if err != nil {
			logs.WithError(err).Error("replace agent file failed")
		}
	}
	if daemonChange {
		err = replaceAgentFile(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Error("replace daemon file failed")
		}
	}

	// 只有 daemon 被杀才启动，没被杀等待被 daemon 拉起来
	if daemonChange {
		switch startT {
		case wintask.TaskStart:
			if _, err = winTask.Run(); err != nil {
				return errors.Wrapf(err, "start win task failed")
			}
		}
	}

	logs.Info("agent upgrade done, upgrade process exiting")
	return nil
}

func tryKillAgentProcess(processName string) (int, error) {
	logs.Info(fmt.Sprintf("try kill %s process", processName))
	pidFile := fmt.Sprintf("%s/%s.pid", systemutil.GetRuntimeDir(), processName)
	agentPid, err := fileutil.GetString(pidFile)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid failed: %s", processName, err))
		return 0, err
	}
	intPid, err := strconv.Atoi(agentPid)
	if err != nil {
		logs.Warn(fmt.Sprintf("parse %s pid: %s failed", processName, agentPid))
		return intPid, err
	}

	p, err := process.NewProcess(int32(intPid))
	if err != nil {
		if errors.Is(err, process.ErrorProcessNotRunning) {
			return intPid, nil
		}
		return intPid, errors.Wrapf(err, "get process %d failed", intPid)
	}

	if err := p.Kill(); err != nil {
		return intPid, errors.Wrapf(err, "kill process %d failed", intPid)
	}

	return intPid, nil
}

func DoUninstallAgent() error {
	err := UninstallAgent()
	if err != nil {
		logs.WithError(err).Error("uninstall agent failed")
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
		logs.Error(fmt.Sprintf("agentUpgrade|check %s md5 failed", fileName), err)
		return false, errors.New("check old md5 failed")
	}

	newMd5, err := fileutil.GetFileMd5(systemutil.GetUpgradeDir() + "/" + fileName)
	if err != nil {
		logs.Error(fmt.Sprintf("agentUpgrade|check %s md5 failed", fileName), err)
		return false, errors.New("check new md5 failed")
	}

	return oldMd5 != newMd5, nil
}

func replaceAgentFile(fileName string) error {
	logs.Info("replace agent file: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName

	// 查询 dst 的状态，如果没有的话使用预设权限
	var perm os.FileMode = 0600
	if stat, err := os.Stat(dst); err != nil {
		logs.WithError(err).Warnf("replaceAgentFile %s stat error", dst)
	} else if stat != nil {
		perm = stat.Mode()
	}
	logs.Infof("replaceAgentFile dst file permissions: %v", perm)

	srcFile, err := os.Open(src)
	if err != nil {
		return errors.Wrapf(err, "replaceAgentFile open %s error", src)
	}

	if err := innerFileUtil.AtomicWriteFile(dst, srcFile, perm); err != nil {
		return errors.Wrapf(err, "replaceAgentFile AtomicWriteFile %s error", dst)
	}

	return nil
}
