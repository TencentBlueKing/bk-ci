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
	"path/filepath"
	"strconv"
	"strings"
	"time"

	"github.com/capnspacehook/taskmaster"
	"github.com/gofrs/flock"
	"github.com/pkg/errors"
	"github.com/shirou/gopsutil/v4/process"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/wintask"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

// DoUpgradeAgent upgrades the agent (and daemon when possible).
//
// SERVICE mode: agent binary is replaced via AtomicWriteFile (with retry).
// Daemon binary is replaced via rename-and-copy (Windows allows renaming a
// running .exe). A signal file (.daemon_upgrade) is written so the daemon's
// watch loop can detect the change and exit, letting SCM restart the service
// with the new binary (SCM failure-recovery must be configured at install time).
//
// TASK mode: both agent and daemon are killed, files replaced, then the
// scheduled task is re-launched.
//
// MANUAL mode: only the agent is upgraded; daemon update requires manual restart.
func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")

	// Acquire totalLock before config/third_components init to minimise the
	// window between CheckProcess releasing totalLock and us re-acquiring it.
	// Without this, the daemon's watch loop can slip in, find the agent dead,
	// and relaunch it with the OLD binary before we replace files.
	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	if err := totalLock.Lock(); err != nil {
		logs.WithError(err).Error("get total lock failed, exit")
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	config.Init(false)
	if err := third_components.Init(); err != nil {
		logs.WithError(err).Error("init third_components error")
		systemutil.ExitProcess(1)
	}

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

	var err error
	daemonChange := false
	if startT == wintask.TaskStart || startT == wintask.ServiceStart {
		daemonChange, err = checkUpgradeFileChange(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Warn("check daemon upgrade file change failed")
		}
	}
	daemonPid := 0
	if daemonChange && startT == wintask.TaskStart {
		daemonPid, err = tryKillAgentProcess(daemonProcess)
		if err != nil {
			logs.WithError(err).Error("try kill daemon process failed")
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
			logs.WithError(err).Error("try kill agent process failed")
		}
	}

	if !agentChange && !daemonChange {
		logs.Info("upgrade nothing, exit")
		return nil
	}

	// Wait for killed processes to exit (SERVICE mode keeps daemon alive)
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
		daemonReady := !daemonChange || startT == wintask.ServiceStart || !daemonExist
		if daemonReady && !agentExist {
			logs.Infof("wait %d seconds for agent to stop done", i+1)
			break
		} else if i == 14 {
			logs.Errorf("upgrade daemon exist %t, agent exist %t, can't upgrade", daemonReady, agentExist)
			return nil
		}
		logs.Infof("wait %d seconds for agent to stop", i+1)
		time.Sleep(1 * time.Second)
	}

	if agentChange {
		err = replaceAgentFile(config.GetClienAgentFile())
		if err != nil {
			logs.WithError(err).Error("replace agent file failed")
		}
	}
	if daemonChange {
		if startT == wintask.ServiceStart {
			err = replaceDaemonFileByRename(config.GetClientDaemonFile())
		} else {
			err = replaceAgentFile(config.GetClientDaemonFile())
		}
		if err != nil {
			logs.WithError(err).Error("replace daemon file failed")
		}
	}

	if daemonChange {
		switch startT {
		case wintask.TaskStart:
			if _, err = winTask.Run(); err != nil {
				return errors.Wrapf(err, "start win task failed")
			}
		case wintask.ServiceStart:
			writeDaemonUpgradeSignal()
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

	// Verify the process is actually the expected one to avoid killing a
	// different process that reused the same PID.
	if name, nameErr := p.Name(); nameErr == nil {
		if !strings.Contains(strings.ToLower(name), processName) {
			logs.Warnf("pid %d is now %s, not %s, skip kill", intPid, name, processName)
			return intPid, nil
		}
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
	agentBin := filepath.Join(workDir, config.GetAgentBinary())
	output, err := command.RunCommand(agentBin, []string{"uninstall"}, workDir, nil)
	if err != nil {
		logs.Error("agent uninstall failed: ", err.Error())
		logs.Error("output: ", string(output))
		return errors.New("agent uninstall failed")
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

const replaceMaxRetries = 10

func replaceAgentFile(fileName string) error {
	logs.Info("replace agent file: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName

	var perm os.FileMode = 0600
	if stat, err := os.Stat(dst); err != nil {
		logs.WithError(err).Warnf("replaceAgentFile %s stat error", dst)
	} else if stat != nil {
		perm = stat.Mode()
	}
	logs.Infof("replaceAgentFile dst file permissions: %v", perm)

	var lastErr error
	for attempt := 1; attempt <= replaceMaxRetries; attempt++ {
		srcFile, err := os.Open(src)
		if err != nil {
			return errors.Wrapf(err, "replaceAgentFile open %s error", src)
		}

		err = innerFileUtil.AtomicWriteFile(dst, srcFile, perm)
		srcFile.Close()
		if err == nil {
			return nil
		}

		lastErr = err
		if attempt < replaceMaxRetries {
			logs.WithError(err).Warnf("replaceAgentFile attempt %d/%d failed, retrying in %ds",
				attempt, replaceMaxRetries, attempt)
			time.Sleep(time.Duration(attempt) * time.Second)
		}
	}

	return errors.Wrapf(lastErr, "replaceAgentFile failed after %d attempts for %s", replaceMaxRetries, dst)
}

const DaemonUpgradeFile = ".daemon_upgrade"

// replaceDaemonFileByRename replaces the daemon binary while it is still
// running as a Windows Service. Windows allows renaming (but not deleting or
// overwriting) a running .exe, so we:
//  1. rename  devopsDaemon.exe → devopsDaemon.exe.old
//  2. copy    tmp/devopsDaemon.exe → devopsDaemon.exe
//
// If step 2 fails, we attempt to roll back by renaming .old back.
func replaceDaemonFileByRename(fileName string) error {
	logs.Info("replaceDaemonFileByRename: ", fileName)
	src := systemutil.GetUpgradeDir() + "/" + fileName
	dst := systemutil.GetWorkDir() + "/" + fileName
	oldDst := dst + ".old"

	// Pre-check: verify source file is accessible before making any changes
	// to avoid leaving the daemon binary missing if the copy would fail.
	if _, err := os.Stat(src); err != nil {
		return errors.Wrapf(err, "source file %s not accessible, aborting", src)
	}

	_ = os.Remove(oldDst)

	if err := os.Rename(dst, oldDst); err != nil {
		return errors.Wrapf(err, "rename running daemon %s → %s", dst, oldDst)
	}
	logs.Infof("renamed running daemon to %s", oldDst)

	if _, err := fileutil.CopyFile(src, dst, false); err != nil {
		logs.WithError(err).Error("copy new daemon failed, attempting rollback")
		if rbErr := os.Rename(oldDst, dst); rbErr != nil {
			logs.WithError(rbErr).Error("rollback rename also failed")
		}
		return errors.Wrapf(err, "copy new daemon to %s", dst)
	}

	logs.Info("replaceDaemonFileByRename done")
	return nil
}

func writeDaemonUpgradeSignal() {
	signalPath := systemutil.GetWorkDir() + "/" + DaemonUpgradeFile
	if err := os.WriteFile(signalPath, []byte("upgrade"), 0644); err != nil {
		logs.WithError(err).Error("write daemon upgrade signal failed")
	} else {
		logs.Info("wrote daemon upgrade signal for SCM restart")
	}
}
