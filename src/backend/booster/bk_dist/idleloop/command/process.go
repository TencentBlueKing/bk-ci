/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"context"
	"os"
	"os/signal"
	"syscall"

	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/idleloop/pkg"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/conf"

	"github.com/shirou/gopsutil/process"
	commandCli "github.com/urfave/cli"
)

// mainProcess do the make process:
func mainProcess(c *commandCli.Context) error {
	initialLogDir(getLogDir(c.String(FlagLogDir)))
	setLogLevel(c.String(FlagLog))

	// get the new obj
	loop := newIdleLoop(c)
	ctx, cancel := context.WithCancel(context.Background())

	// run a system signal watcher for breaking process
	go sysSignalHandler(cancel, loop)

	// run loop
	_, err := loop.Run(ctx)

	return err
}

func sysSignalHandler(cancel context.CancelFunc, _ *pkg.IdleLoop) {
	interrupt := make(chan os.Signal)
	signal.Notify(interrupt, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-interrupt:
		blog.Warnf("idelloop: get system signal %s, going to exit", sig.String())

		// cancel booster's context and make sure that task is released.
		cancel()

		p, err := process.NewProcess(int32(os.Getpid()))
		if err == nil {
			blog.Debugf("idelloop: ready kill children when recieved sinal")
			// kill children
			pkg.KillChildren(p)
		}

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

func newIdleLoop(_ *commandCli.Context) *pkg.IdleLoop {
	return pkg.NewIdleLoop()
}

func setLogLevel(level string) {
	switch level {
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
		blog.SetStderrLevel(blog.StderrLevelInfo)
	}
}

func initialLogDir(dir string) {
	blog.InitLogs(conf.LogConfig{
		LogDir:       dir,
		LogMaxNum:    10,
		LogMaxSize:   500,
		AlsoToStdErr: false,
	})
}

func getLogDir(dir string) string {
	if dir == "" {
		return dcUtil.GetLogsDir()
	}

	return dir
}
