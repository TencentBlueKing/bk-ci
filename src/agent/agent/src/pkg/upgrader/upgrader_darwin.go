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
	"os/exec"
	"os/user"
	"path/filepath"
	"strconv"
	"strings"

	"github.com/gofrs/flock"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/command"
	innerFileUtil "github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/third_components"
)

const (
	agentProcess  = "agent"
	daemonProcess = "daemon"
)

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

	daemonChange, _ := checkUpgradeFileChange(config.GetClientDaemonFile())
	agentChange, _ := checkUpgradeFileChange(config.GetClienAgentFile())

	if !agentChange && !daemonChange {
		logs.Info("upgrade nothing, exit")
		return nil
	}

	// Replace files BEFORE killing processes.
	// macOS allows replacing files that are in use; the running process
	// keeps the old inode in memory. When the daemon restarts the agent
	// it will pick up the new binary from disk.
	// If we kill first, the daemon immediately restarts the agent with
	// the OLD binary (race condition).
	if agentChange {
		if err := replaceAgentFile(config.GetClienAgentFile()); err != nil {
			logs.WithError(err).Error("replace agent file failed")
		}
	}

	if daemonChange {
		if err := replaceAgentFile(config.GetClientDaemonFile()); err != nil {
			logs.WithError(err).Error("replace daemon file failed")
		}
	}

	if agentChange {
		tryKillAgentProcess(agentProcess)
	}

	if daemonChange {
		if err := restartDaemonViaLaunchd(); err != nil {
			logs.WithError(err).Warn("skip daemon restart, file replaced on disk; " +
				"new daemon will take effect on next manual restart")
		} else {
			logs.Info("daemon restarted via launchd")
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
	src := filepath.Join(systemutil.GetUpgradeDir(), fileName)
	dst := filepath.Join(systemutil.GetWorkDir(), fileName)

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

// ── daemon restart via launchd (old-version safe) ────────────────────────

const (
	installModeLogin      = "LOGIN"
	installModeBackground = "BACKGROUND"
	installTypeFile       = ".install_type"
)

// restartDaemonViaLaunchd restarts the daemon through launchd.
// It returns a non-nil error when no reliable restart path exists, in which
// case the caller should NOT kill the daemon — the replaced binary on disk
// will take effect on the next manual restart.
//
// Three preconditions must be met:
//  1. A launchd plist exists (the service is managed by launchd).
//  2. Both .path and .env env-snapshot files exist (created by the new
//     install flow via snapshotEnvFiles). Without them, launchd would
//     restart the daemon in a minimal environment, losing the user's
//     PATH/JAVA_HOME/etc. that old installs relied on by inheriting the
//     interactive shell.
func restartDaemonViaLaunchd() error {
	workDir := systemutil.GetWorkDir()

	for _, name := range []string{".path", ".env"} {
		p := filepath.Join(workDir, name)
		if _, err := os.Stat(p); err != nil {
			return fmt.Errorf("%s not found at %s; old install without env snapshot, "+
				"launchd restart would lose user environment", name, p)
		}
	}

	serviceName := "devops_agent_" + config.GAgentConfig.AgentId

	pp := daemonPlistPath(serviceName)
	if _, err := os.Stat(pp); err != nil {
		return fmt.Errorf("plist not found at %s, cannot safely restart daemon", pp)
	}

	mode := readDaemonInstallMode()
	domain := daemonLaunchdDomain(mode)
	target := domain + "/" + serviceName

	if hasDarwinModernLaunchctl() {
		out, err := exec.Command("launchctl", "kickstart", "-k", target).CombinedOutput()
		if err != nil {
			return fmt.Errorf("launchctl kickstart -k %s failed: %s (%w)", target, strings.TrimSpace(string(out)), err)
		}
		return nil
	}

	// Legacy macOS (< 10.10): unload + load
	_ = exec.Command("launchctl", "unload", pp).Run()
	out, err := exec.Command("launchctl", "load", "-w", pp).CombinedOutput()
	if err != nil {
		return fmt.Errorf("launchctl load %s failed: %s (%w)", pp, strings.TrimSpace(string(out)), err)
	}
	return nil
}

func daemonPlistPath(serviceName string) string {
	if os.Geteuid() == 0 {
		return filepath.Join("/Library/LaunchDaemons", serviceName+".plist")
	}
	home, _ := os.UserHomeDir()
	return filepath.Join(home, "Library", "LaunchAgents", serviceName+".plist")
}

func daemonLaunchdDomain(mode string) string {
	if os.Geteuid() == 0 {
		return "system"
	}
	u, _ := user.Current()
	uid := "0"
	if u != nil {
		uid = u.Uid
	}
	if mode == installModeBackground {
		return "user/" + uid
	}
	return "gui/" + uid
}

func readDaemonInstallMode() string {
	data, err := os.ReadFile(filepath.Join(systemutil.GetWorkDir(), installTypeFile))
	if err != nil {
		return installModeLogin
	}
	m := strings.TrimSpace(string(data))
	if strings.EqualFold(m, installModeBackground) {
		return installModeBackground
	}
	return installModeLogin
}

// hasDarwinModernLaunchctl probes whether launchctl supports the modern
// bootstrap/bootout/kickstart API (macOS 10.10+).
func hasDarwinModernLaunchctl() bool {
	out, _ := exec.Command("launchctl", "bootstrap").CombinedOutput()
	s := strings.ToLower(string(out))
	return !strings.Contains(s, "unrecognized") && !strings.Contains(s, "unknown")
}
