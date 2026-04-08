//go:build windows
// +build windows

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

package main

import (
	"errors"
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"strings"
	"sync"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
	"github.com/gofrs/flock"
	"github.com/kardianos/service"
	"golang.org/x/sys/windows"
)

const (
	daemonProcess    = "daemon"
	restartBaseDelay = 3 * time.Second
	restartMaxWait   = 30 * time.Second
	restartPollTick  = 500 * time.Millisecond
)

func main() {
	isDebug := false
	if len(os.Args) == 2 {
		switch os.Args[1] {
		case "version":
			fmt.Println(config.AgentVersion)
			systemutil.ExitProcess(0)
		case "fullVersion":
			fmt.Println(config.AgentVersion)
			fmt.Println(config.GitCommit)
			fmt.Println(config.BuildTime)
			systemutil.ExitProcess(0)
		case "debug":
			isDebug = true
		}
	}

	// 初始化日志
	workDir := systemutil.GetExecutableDir()
	err := logs.Init(filepath.Join(workDir, "logs", "devopsDaemon.log"), isDebug, false)
	if err != nil {
		fmt.Printf("init daemon log error %v\n", err)
		systemutil.ExitProcess(1)
	}

	logs.Infof("GOOS=%s, GOARCH=%s", runtime.GOOS, runtime.GOARCH)

	err = os.Chdir(workDir)
	if err != nil {
		logs.Info("change work dir failed, err: ", err.Error())
		systemutil.ExitProcess(1)
	}

	defer func() {
		if err := recover(); err != nil {
			logs.Error("panic: ", err)
			systemutil.ExitProcess(1)
		}
	}()

	if ok := systemutil.CheckProcess(daemonProcess); !ok {
		logs.Info("get process lock failed, exit")
		return
	}

	cleanupOldDaemonBinary(workDir)

	logs.Info("devops daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("workDir: ", workDir)

	isServiceMode = !service.Interactive()
	if isServiceMode {
		installType := readInstallType()
		isSessionMode = strings.EqualFold(installType, "SESSION")
		if isSessionMode {
			logs.Info("running as Windows service in SESSION mode, will launch agent in user session")
		} else {
			logs.Info("running as Windows service in SERVICE mode")
		}
	} else {
		logs.Info("running in interactive mode (TASK)")
	}

	serviceConfig := &service.Config{
		Name: "name",
	}

	daemonProgram := &program{}
	sys := service.ChosenSystem()
	daemonService, err := sys.New(daemonProgram, serviceConfig)
	if err != nil {
		logs.WithError(err).Error("Init service error")
		systemutil.ExitProcess(1)
	}

	err = daemonService.Run()
	if err != nil {
		logs.WithError(err).Error("run agent program error")
	}
}

var (
	isServiceMode      bool
	isSessionMode      bool
	agentMu            sync.Mutex
	agentProcess       *os.Process
	agentProcessHandle windows.Handle
)

const installTypeFile = ".install_type"

// readInstallType reads the install mode from .install_type file.
// Returns "SERVICE" as default if the file is missing or unreadable.
func readInstallType() string {
	workDir := systemutil.GetExecutableDir()
	data, err := os.ReadFile(filepath.Join(workDir, installTypeFile))
	if err != nil {
		logs.Infof("no %s file found, defaulting to SERVICE mode", installTypeFile)
		return "SERVICE"
	}
	installType := strings.TrimSpace(string(data))
	logs.Infof("install type from %s: %s", installTypeFile, installType)
	return installType
}

func watch() {
	defer func() {
		if r := recover(); r != nil {
			logs.Errorf("watch goroutine panic recovered: %v, will restart watch loop in 30s", r)
			time.Sleep(30 * time.Second)
			go watch()
		}
	}()

	workDir := systemutil.GetExecutableDir()
	agentPath := systemutil.GetWorkDir() + "/devopsAgent.exe"
	for {
		func() {
			logs.Info("start devops agent")
			if !fileutil.Exists(agentPath) {
				logs.Errorf("agent file: %s not exists", agentPath)
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
				return
			}

			err := fileutil.SetExecutable(agentPath)
			if err != nil {
				logs.WithError(err).Error("chmod failed, err")
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
				return
			}

			if !waitForUpgradeFinish() {
				logs.Info("restart after 30 seconds")
				time.Sleep(30 * time.Second)
				return
			}

			if checkDaemonUpgradeSignal() {
				logs.Warn("daemon upgrade detected, exiting to let SCM restart with new binary")
				systemutil.ExitProcess(1)
			}

			if isServiceMode && isSessionMode {
				if launchAgentInUserSession(agentPath, workDir) {
					return
				}
				logs.Warn("no user session available, waiting for user login")
				time.Sleep(30 * time.Second)
				return
			}
			launchAgentDirect(agentPath, workDir)
		}()
	}
}

// waitForUpgradeFinish acquires and immediately releases total-lock to ensure
// no upgrader process is currently replacing the agent binary. On Windows the
// daemon does not hold total-lock across the agent lifetime (unlike the Linux
// daemon) because it blocks on cmd.Wait / WaitForSingleObject.
func waitForUpgradeFinish() bool {
	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))
	if err := totalLock.Lock(); err != nil {
		logs.WithError(err).Error("wait for upgrade finish: failed to get total lock")
		return false
	}
	_ = totalLock.Unlock()
	return true
}

// waitBeforeRestart polls the total-lock to detect when the upgrader (if any)
// has finished, so the daemon can restart the agent as soon as possible.
// A short base delay prevents rapid restart loops; 30s is the hard timeout.
func waitBeforeRestart() {
	doWaitBeforeRestart(restartBaseDelay, restartMaxWait, restartPollTick)
}

func doWaitBeforeRestart(baseDelay, maxWait, pollTick time.Duration) {
	logs.Infof("waitBeforeRestart: base delay %s, max wait %s", baseDelay, maxWait)
	time.Sleep(baseDelay)

	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))

	remaining := maxWait - baseDelay
	if remaining <= 0 {
		return
	}

	deadline := time.NewTimer(remaining)
	defer deadline.Stop()
	ticker := time.NewTicker(pollTick)
	defer ticker.Stop()

	for {
		select {
		case <-deadline.C:
			logs.Warn("waitBeforeRestart: reached timeout, proceeding with restart")
			return
		case <-ticker.C:
			locked, err := totalLock.TryLock()
			if err != nil {
				logs.WithError(err).Warn("waitBeforeRestart: TryLock error, retrying")
				continue
			}
			if locked {
				_ = totalLock.Unlock()
				logs.Info("waitBeforeRestart: no upgrader running, proceeding with restart")
				return
			}
		}
	}
}

// launchAgentInUserSession tries to start the agent in a user desktop session.
// Priority: 1) WTS active session  2) LogonUser with stored credentials  3) give up (return false).
func launchAgentInUserSession(agentPath, workDir string) bool {
	cmdLine := fmt.Sprintf(`"%s"`, agentPath)

	proc, err := StartProcessAsUser(agentPath, cmdLine, workDir)
	if err != nil {
		logs.WithError(err).Warn("StartProcessAsUser failed, no active user session")
		return false
	}

	agentMu.Lock()
	agentProcessHandle = proc.ProcessHandle
	agentMu.Unlock()

	logs.Infof("agent started in user session, pid=%d", proc.PID)

	windows.WaitForSingleObject(proc.ProcessHandle, windows.INFINITE)

	var exitCode uint32
	_ = windows.GetExitCodeProcess(proc.ProcessHandle, &exitCode)

	agentMu.Lock()
	agentProcessHandle = 0
	agentMu.Unlock()

	windows.CloseHandle(proc.ProcessHandle)
	proc.Close()

	if exitCode == uint32(constant.DaemonExitCode) {
		logs.Warnf("exit code %d daemon exit", constant.DaemonExitCode)
		systemutil.ExitProcess(constant.DaemonExitCode)
	}

	logs.Infof("agent process exited with code %d", exitCode)
	waitBeforeRestart()
	return true
}

// launchAgentDirect starts the agent as a direct child process (interactive
// mode or fallback when no user session is available).
func launchAgentDirect(agentPath, workDir string) {
	cmd := exec.Command(agentPath)
	cmd.Dir = workDir

	err := cmd.Start()
	if err != nil {
		logs.WithError(err).Error("agent start failed, err")
		waitBeforeRestart()
		return
	}

	agentMu.Lock()
	agentProcess = cmd.Process
	agentMu.Unlock()

	logs.Info("devops agent started, pid: ", cmd.Process.Pid)
	err = cmd.Wait()

	agentMu.Lock()
	agentProcess = nil
	agentMu.Unlock()

	if err != nil {
		var exitErr *exec.ExitError
		if errors.As(err, &exitErr) {
			if exitErr.ExitCode() == constant.DaemonExitCode {
				logs.Warnf("exit code %d daemon exit", constant.DaemonExitCode)
				systemutil.ExitProcess(constant.DaemonExitCode)
			}
		}
		logs.WithError(err).Error("agent process error")
	}
	logs.Info("agent process exited")
	waitBeforeRestart()
}

type program struct{}

func (p *program) Start(s service.Service) error {
	go watch()
	return nil
}

func (p *program) Stop(s service.Service) error {
	p.tryStopAgent()
	return nil
}

func (p *program) tryStopAgent() {
	agentMu.Lock()
	defer agentMu.Unlock()
	if agentProcessHandle != 0 {
		windows.TerminateProcess(agentProcessHandle, 1)
	} else if agentProcess != nil {
		agentProcess.Kill()
	}
}

const daemonUpgradeFile = ".daemon_upgrade"

// checkDaemonUpgradeSignal checks whether the upgrader has written a signal
// file indicating the daemon binary was replaced. If found, the file is removed
// and true is returned so the caller can exit and let SCM restart the service
// with the new binary.
func checkDaemonUpgradeSignal() bool {
	signalPath := filepath.Join(systemutil.GetWorkDir(), daemonUpgradeFile)
	if _, err := os.Stat(signalPath); err != nil {
		return false
	}
	if err := os.Remove(signalPath); err != nil {
		logs.WithError(err).Warn("failed to remove daemon upgrade signal file")
	}
	return true
}

// cleanupOldDaemonBinary removes the .old daemon binary left over from a
// previous rename-based upgrade.
func cleanupOldDaemonBinary(workDir string) {
	oldPath := filepath.Join(workDir, "devopsDaemon.exe.old")
	if _, err := os.Stat(oldPath); err != nil {
		return
	}
	if err := os.Remove(oldPath); err != nil {
		logs.WithError(err).Warn("failed to remove old daemon binary")
	} else {
		logs.Info("cleaned up old daemon binary: devopsDaemon.exe.old")
	}
}
