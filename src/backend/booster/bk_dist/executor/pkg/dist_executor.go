/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package executor

import (
	"fmt"
	"os"
	"os/signal"
	"runtime"
	"strings"
	"syscall"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcTypes "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	v1 "github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// DistExecutor define dist executor
type DistExecutor struct {
	taskID string

	bt    dcTypes.BoosterType
	work  dcSDK.ControllerWorkSDK
	stats *dcSDK.ControllerJobStats
}

// NewDistExecutor return new DistExecutor
func NewDistExecutor() *DistExecutor {
	return &DistExecutor{
		bt:     dcTypes.GetBoosterType(env.GetEnv(env.BoosterType)),
		work:   v1.NewSDK(dcSDK.GetControllerConfigFromEnv()).GetWork(env.GetEnv(env.KeyExecutorControllerWorkID)),
		taskID: env.GetEnv(env.KeyExecutorTaskID),
		stats:  &dcSDK.ControllerJobStats{},
	}
}

// Run main function entry
func (d *DistExecutor) Run() (int, string, error) {
	// detect the log level from settings
	d.detectLogLevel()

	// catch the system signal and handles works before process exit
	go d.sysSignalHandler()

	blog.Infof("executor: command [%s] begins", strings.Join(os.Args, " "))
	defer blog.Infof("executor: command [%s] finished", strings.Join(os.Args, " "))

	// if work not available, means controller is not available
	// run the origin pure work instead of any handles
	if d.work.ID() == "" {
		return runDirect()
	}

	// work available, run work with executor-progress
	return d.runWork()
}

func runDirect() (int, string, error) {
	if len(os.Args) < 2 {
		return 0, "", nil
	}

	sandbox := dcSyscall.Sandbox{}
	retcode, err := sandbox.ExecCommand(os.Args[1], os.Args[2:]...)
	return retcode, "", err
}

func (d *DistExecutor) runWork() (int, string, error) {
	d.initStats()

	if len(os.Args) < 2 {
		blog.Errorf("executor: not enough args to execute")
		return 0, "", fmt.Errorf("not enough args to execute")
	}

	// ignore argv[0], it's itself
	// _, _, r, err := d.work.Job(d.stats).ExecuteLocalTask(os.Args[1:], "")
	// if err != nil {
	// 	blog.Errorf("executor: execute failed, error: %v, exit code: -1", err)
	// 	return -1, err
	// }

	retcode, retmsg, r, err := d.work.Job(d.stats).ExecuteLocalTask(os.Args[1:], "")
	if err != nil || retcode != 0 {
		if r != nil {
			blog.Errorf("executor: execute failed, error: %v, ret code: %d,retmsg:%s,outmsg:%s,errmsg:%s,cmd:%v",
				err, retcode, retmsg, string(r.Stdout), string(r.Stderr), os.Args[1:])
		} else {
			blog.Errorf("executor: execute failed, ret code:%d, retmsg:%s, error: %v, cmd:%v", retcode, retmsg, err, os.Args[1:])
		}
		return retcode, retmsg, err
	}

	charcode := 0
	if runtime.GOOS == "windows" {
		charcode = dcSyscall.GetConsoleCP()
	}

	if len(r.Stdout) > 0 {
		// https://docs.microsoft.com/en-us/windows/win32/intl/code-page-identifiers
		// 65001 means utf8, we will try convert to gbk which is not utf8
		if charcode > 0 && charcode != 65001 {
			// fmt.Printf("get charset code:%d\n", dcSyscall.GetConsoleCP())
			gbk, err := dcUtil.Utf8ToGbk(r.Stdout)
			if err == nil {
				_, _ = fmt.Fprint(os.Stdout, string(gbk))
			} else {
				_, _ = fmt.Fprint(os.Stdout, string(r.Stdout))
				// _, _ = fmt.Fprint(os.Stdout, "errro:%v\n", err)
			}
		} else {
			_, _ = fmt.Fprint(os.Stdout, string(r.Stdout))
		}
	}

	if len(r.Stderr) > 0 {
		// https://docs.microsoft.com/en-us/windows/win32/intl/code-page-identifiers
		// 65001 means utf8, we will try convert to gbk which is not utf8
		if charcode > 0 && charcode != 65001 {
			// fmt.Printf("get charset code:%d\n", dcSyscall.GetConsoleCP())
			gbk, err := dcUtil.Utf8ToGbk(r.Stderr)
			if err == nil {
				_, _ = fmt.Fprint(os.Stderr, string(gbk))
			} else {
				_, _ = fmt.Fprint(os.Stderr, string(r.Stderr))
				// _, _ = fmt.Fprint(os.Stderr, "errro:%v\n", err)
			}
		} else {
			_, _ = fmt.Fprint(os.Stderr, string(r.Stderr))
		}
	}

	if r.ExitCode != 0 {
		blog.Errorf("executor: execute failed, error: %v, exit code: %d", err, r.ExitCode)
		return r.ExitCode, retmsg, err
	}

	return 0, retmsg, nil
}

func (d *DistExecutor) initStats() {
	d.stats.Pid = os.Getpid()
	d.stats.ID = fmt.Sprintf("%d_%d", d.stats.Pid, time.Now().UnixNano())
	d.stats.WorkID = d.work.ID()
	d.stats.TaskID = d.taskID
	d.stats.BoosterType = d.bt.String()
	d.stats.OriginArgs = os.Args[1:]
}

func (d *DistExecutor) updateJobStats() {
	_ = d.work.UpdateJobStats(d.stats)
}

func (d *DistExecutor) sysSignalHandler() {
	interrupt := make(chan os.Signal)
	signal.Notify(interrupt, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-interrupt:
		blog.Warnf("executor-command: get system signal %s, going to exit", sig.String())

		// catch control-C and should return code 130(128+0x2)
		if sig == syscall.SIGINT {
			os.Exit(130)
		}

		// catch kill and should return code 143(128+0xf)
		if sig == syscall.SIGTERM {
			os.Exit(143)
		}

		os.Exit(1)
	}
}

func (d *DistExecutor) detectLogLevel() {
	switch env.GetEnv(env.KeyExecutorLogLevel) {
	case dcUtil.PrintDebug.String():
		blog.SetV(3)
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintInfo.String():
		blog.SetStderrLevel(blog.StderrLevelInfo)
	case dcUtil.PrintWarn.String():
		blog.SetStderrLevel(blog.StderrLevelWarning)
	case dcUtil.PrintError.String():
		blog.SetStderrLevel(blog.StderrLevelError)
	case dcUtil.PrintNothing.String():
		blog.SetStderrLevel(blog.StderrLevelNothing)
	default:
		// default to be error printer.
		blog.SetStderrLevel(blog.StderrLevelNothing)
	}
}
