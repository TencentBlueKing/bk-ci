/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
	"os"
	"runtime"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcTypes "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	v1 "github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// Executor define dist executor
type Executor struct {
	taskID string

	bt    dcTypes.BoosterType
	work  dcSDK.ControllerWorkSDK
	stats *dcSDK.ControllerJobStats

	outputmsg []byte
	errormsg  []byte

	counter count32
}

// NewExecutor return new Executor
func NewExecutor() *Executor {
	return &Executor{
		bt:     dcTypes.GetBoosterType(env.GetEnv(env.BoosterType)),
		work:   v1.NewSDK(dcSDK.GetControllerConfigFromEnv()).GetWork(env.GetEnv(env.KeyExecutorControllerWorkID)),
		taskID: env.GetEnv(env.KeyExecutorTaskID),
		stats:  &dcSDK.ControllerJobStats{},
	}
}

// Valid
func (d *Executor) Valid() bool {
	return d.work.ID() != ""
}

// Run main function entry
func (d *Executor) Run(fullargs []string, workdir string) (int, error) {
	blog.Infof("ubtexecutor: command [%s] begins", strings.Join(fullargs, " "))
	for i, v := range fullargs {
		blog.Infof("ubtexecutor: arg[%d] : [%s]", i, v)
	}
	defer blog.Infof("ubtexecutor: command [%s] finished", strings.Join(fullargs, " "))

	// work available, run work with executor-progress
	return d.runWork(fullargs, workdir)
}

func (d *Executor) runWork(fullargs []string, workdir string) (int, error) {
	// d.initStats()

	// ignore argv[0], it's itself
	r, err := d.work.Job(d.getStats(fullargs)).ExecuteLocalTask(fullargs, workdir)
	if err != nil {
		if r != nil {
			blog.Errorf("ubtexecutor: execute failed, error: %v, exit code: -1, outmsg:%s,errmsg:%s,cmd:%v",
				err, string(r.Stdout), string(r.Stderr), fullargs)
		} else {
			blog.Errorf("ubtexecutor: execute failed, error: %v,cmd:%v", err, fullargs)
		}
		return -1, err
	}

	charcode := 0
	if runtime.GOOS == "windows" {
		charcode = dcSyscall.GetConsoleCP()
	}

	if len(r.Stdout) > 0 {
		d.outputmsg = r.Stdout
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
		d.errormsg = r.Stderr
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
		blog.Errorf("ubtexecutor: execute failed, error: %v, exit code: %d, outmsg:%s,errmsg:%s,cmd:%v",
			err, r.ExitCode, string(r.Stdout), string(r.Stderr), fullargs)
		return r.ExitCode, err
	}

	return 0, nil
}

func (d *Executor) getStats(fullargs []string) *dcSDK.ControllerJobStats {
	stats := dcSDK.ControllerJobStats{
		Pid:         os.Getpid(),
		ID:          fmt.Sprintf("%d_%d", d.counter.inc(), time.Now().UnixNano()),
		WorkID:      d.work.ID(),
		TaskID:      d.taskID,
		BoosterType: d.bt.String(),
		OriginArgs:  fullargs,
	}

	return &stats
}
