//go:build darwin

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

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agentcommon/utils/fileutil"

	"github.com/gofrs/flock"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

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

	daemonChange, _ := checkUpgradeFileChange(config.GetClientDaemonFile())

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
			logs.WithError(err).Error("replace agent file failed")
		}
	}

	if daemonChange {
		err = replaceAgentFile(config.GetClientDaemonFile())
		if err != nil {
			logs.WithError(err).Error("replace daemon file failed")
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

	// 查询 dst 的状态，如果没有的话使用预设权限\
	perm := constant.CommonFileModePerm
	if stat, err := os.Stat(dst); err != nil {
		logs.WithError(err).Warnf("replaceAgentFile %s stat error", dst)
	} else if stat != nil {
		perm = stat.Mode()
	}

	srcFile, err := os.Open(src)
	if err != nil {
		return errors.Wrapf(err, "replaceAgentFile open %s error", src)
	}

	if err := innerFileUtil.AtomicWriteFile(dst, srcFile, perm); err != nil {
		return errors.Wrapf(err, "replaceAgentFile AtomicWriteFile %s error", dst)
	}
	return nil
}
