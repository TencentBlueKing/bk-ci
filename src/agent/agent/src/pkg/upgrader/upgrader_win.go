//go:build windows

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
	"fmt"
	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/pkg/errors"
	"os"
	"os/exec"
	"path/filepath"
	"strconv"
	"syscall"
	"time"

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

func DoUpgradeAgent() error {
	logs.Info("start upgrade agent")
	config.Init(false)

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	err := totalLock.Lock()
	if err = totalLock.Lock(); err != nil {
		logs.WithError(err).Error("get total lock failed, exit")
		return errors.New("get total lock failed")
	}
	defer func() { totalLock.Unlock() }()

	// TODO: 通过查询服务和执行计划，判断是否是手动启动的 daemon，如果是手动启动的，只更新 agent
	manualStart := false

	// daemon 可能存在早期的没有 pid 文件的情况，这里通过是否杀掉 daemon 来判断是否更新 daemon
	daemonChange := false
	if !manualStart {
		daemonChange, err = checkUpgradeFileChange(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Warn("check daemon upgrade file change failed")
		}
	}
	daemonKilled := false
	daemonPid := 0
	if daemonChange {
		daemonPid, err = tryKillAgentProcess(daemonProcess)
		if daemonPid != 0 {
			daemonKilled = true
		}
	}

	// agent 如果都没有杀掉那么久直接返回
	agentChange, _ := checkUpgradeFileChange(config.GetClienAgentFile())
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
	for i := 0; i < 30; i++ {
		if daemonExist && daemonKilled {
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
		if (!daemonKilled || !daemonExist) && !agentExist {
			break
		} else if i == 29 {
			logs.Errorf("upgrade daemon exist %t, agent exist %t, can't upgrade", !daemonKilled || !daemonExist, agentExist)
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
	if daemonKilled {
		err = replaceAgentFile(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Error("replace daemon file failed")
		}
	}

	// 只有 daemon 被杀才启动，没被杀等待被 daemon 拉起来
	if daemonKilled {
		cmd := exec.Command(filepath.Join(systemutil.GetWorkDir(), "start.bat"))
		cmd.SysProcAttr = &syscall.SysProcAttr{
			CreationFlags:    0x00000010,
			NoInheritHandles: true,
		}
		output, err := cmd.Output()
		if err != nil {
			logs.WithError(err).Errorf("start script file failed, output: %s", string(output))
			return nil
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

func StartDaemon() error {
	logs.Info("starting ", config.GetClientDaemonFile())

	workDir := systemutil.GetWorkDir()
	startCmd := workDir + "/" + config.GetClientDaemonFile()

	if err := fileutil.SetExecutable(startCmd); err != nil {
		logs.WithError(err).Warn("chmod daemon file failed")
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

	//if _, err := fileutil.CopyFile(src, dst, true); err != nil {
	//	logs.WithError(err).Warnf("replaceAgentFile %s stat error", dst)
	//	return err
	//}
	if err := innerFileUtil.AtomicWriteFile(dst, srcFile, perm); err != nil {
		return errors.Wrapf(err, "replaceAgentFile AtomicWriteFile %s error", dst)
	}

	return nil
}
