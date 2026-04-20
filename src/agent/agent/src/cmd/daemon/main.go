//go:build linux || darwin
// +build linux darwin

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
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
	"runtime"
	"time"

	"github.com/gofrs/flock"
	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/agentcli"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/utils/fileutil"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/util/systemutil"
)

const (
	daemonProcess = "daemon"
	agentCheckGap = 5 * time.Second
)

func main() {
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
		}
	}

	// 初始化日志
	workDir := systemutil.GetExecutableDir()
	logFilePath := filepath.Join(workDir, "logs", "devopsDaemon.log")
	err := logs.Init(logFilePath, agentcli.DebugFileExists(workDir), false)
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
		logs.Warn("get process lock failed, exit")
		return
	}

	logs.Info("devops daemon start")
	logs.Info("pid: ", os.Getpid())
	logs.Info("workDir: ", workDir)

	watch()
	systemutil.KeepProcessAlive()
}

func watch() {
	totalLock := flock.New(fmt.Sprintf("%s/%s.lock", systemutil.GetRuntimeDir(), systemutil.TotalLock))

	// first check immediately
	totalLock.Lock()
	doCheckAndLaunchAgent()
	totalLock.Unlock()

	checkTimeTicker := time.NewTicker(agentCheckGap)
	for ; ; totalLock.Unlock() {
		select {
		case <-checkTimeTicker.C:
			if err := totalLock.Lock(); err != nil {
				logs.WithError(err).Error("failed to get agent lock")
				continue
			}

			doCheckAndLaunchAgent()
		}
	}
}

func doCheckAndLaunchAgent() {
	// 使用可执行文件所在目录作为可信基准，与 main() 中 Chdir 的来源一致，
	// 避免因 CWD 被外部修改导致启动不受控的二进制文件。
	workDir := systemutil.GetExecutableDir()
	agentLock := flock.New(fmt.Sprintf("%s/agent.lock", systemutil.GetRuntimeDir()))

	locked, err := agentLock.TryLock()
	if err == nil && locked {
		// #1613 fix open too many files
		defer func() {
			err = agentLock.Unlock()
			if err != nil {
				logs.WithError(err).Error("try to unlock agent.lock failed")
			}
		}()
	}
	if err != nil {
		logs.WithError(err).Error("try to get agent.lock failed")
		return
	}
	if !locked {
		return
	}

	logs.Warn("agent is not available, will launch it")

	process, err := launch(filepath.Join(workDir, config.AgentFileClientLinux))
	if err != nil {
		logs.WithError(err).Error("launch agent failed")
		return
	}
	if process == nil {
		logs.Error("launch agent failed: got a nil process")
		return
	}
	logs.Infof("success to launch agent, pid: %d", process.Pid)
}

func launch(agentPath string) (*os.Process, error) {
	cmd := exec.Command(agentPath)
	cmd.Dir = systemutil.GetExecutableDir()

	logs.Infof("start devops agent: %s", cmd.String())
	if !fileutil.Exists(agentPath) {
		return nil, fmt.Errorf("agent file %s not exists", agentPath)
	}

	// 拒绝符号链接：daemon 以高权限运行，若 agentPath 被替换为符号链接，
	// 可导致任意二进制以 daemon 权限执行（Symlink Following 攻击）。
	info, err := os.Lstat(agentPath)
	if err != nil {
		return nil, fmt.Errorf("agent file %s lstat failed: %w", agentPath, err)
	}
	if info.Mode()&os.ModeSymlink != 0 {
		return nil, fmt.Errorf("agent file %s is a symlink, refusing to execute for security", agentPath)
	}

	err = fileutil.SetExecutable(agentPath)
	if err != nil {
		return nil, errors.Wrap(err, "chmod agent file failed")
	}

	// 获取 agent 的错误输出，这样有助于打印出崩溃的堆栈方便排查问题
	stdErr, errstd := cmd.StderrPipe()
	if errstd != nil {
		logs.WithError(errstd).Error("get agent stderr pipe error")
	}

	if err = cmd.Start(); err != nil {
		if stdErr != nil {
			stdErr.Close()
		}
		return nil, errors.Wrap(err, "start agent failed")
	}

	go func() {
		// 无论进程如何退出，都需要关闭管道，防止文件描述符泄漏。
		// pipe 建立失败时（errstd != nil）stdErr 为 nil，Close 调用需跳过。
		if errstd == nil && stdErr != nil {
			defer stdErr.Close()
		}

		if err := cmd.Wait(); err != nil {
			if exiterr, ok := err.(*exec.ExitError); ok {
				if exiterr.ExitCode() == constant.DaemonExitCode {
					logs.Warnf("exit code %d daemon exit", constant.DaemonExitCode)
					systemutil.ExitProcess(constant.DaemonExitCode)
				}
			}
			logs.WithError(err).Error("agent process error")
			// pipe 建立失败时无法读取 stderr，直接返回
			if errstd != nil {
				return
			}
			out, err := io.ReadAll(stdErr)
			if err != nil {
				logs.WithError(err).Error("read agent stderr out error")
				return
			}
			logs.Error("agent process error out", string(out))
		}
	}()

	return cmd.Process, nil
}
