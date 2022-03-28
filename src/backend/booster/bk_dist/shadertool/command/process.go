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
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/shadertool/common"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/shadertool/pkg"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/conf"

	commandCli "github.com/urfave/cli"
)

// mainProcess do the make process:
func mainProcess(c *commandCli.Context) error {
	initialLogDir(getLogDir(c.String(FlagLogDir)))
	common.SetLogLevel(c.String(FlagLog))

	// get the new obj
	handle := newCustomProcess(c)
	ctx, cancel := context.WithCancel(context.Background())

	// run a system signal watcher for breaking process
	go sysSignalHandler(cancel, handle)

	// run handle
	_, err := handle.Run(ctx)

	blog.CloseLogs()

	return err
}

func sysSignalHandler(cancel context.CancelFunc, handle *pkg.ShaderTool) {
	interrupt := make(chan os.Signal)
	signal.Notify(interrupt, syscall.SIGINT, syscall.SIGTERM)

	select {
	case sig := <-interrupt:
		blog.Warnf("ShaderTool: get system signal %s, going to exit", sig.String())

		// cancel handle's context and make sure that task is released.
		cancel()

		// handle.Clean()
		handle.ReleaseResource()

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

func newCustomProcess(c *commandCli.Context) *pkg.ShaderTool {
	return pkg.NewShaderTool(&common.Flags{
		ToolDir:       c.String(FlagToolDir),
		JobDir:        c.String(FlagJobDir),
		JobJSONPrefix: c.String(FlagJobJSONPrefix),
		JobStartIndex: int32(c.Int(FlagJobStartIndex)),
		CommitSuicide: c.Bool(FlagCommitSuicide),
		Port:          int32(c.Int(FlagPort)),
	}, sdk.ControllerConfig{
		NoLocal: false,
		Scheme:  ControllerScheme,
		IP:      ControllerIP,
		Port:    ControllerPort,
		Timeout: 5 * time.Second,
		LogDir:  getLogDir(c.String(FlagLogDir)),
		LogVerbosity: func() int {
			// debug模式下, --v=3
			if c.String(FlagLog) == dcUtil.PrintDebug.String() {
				return 3
			}
			return 0
		}(),
	})
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
