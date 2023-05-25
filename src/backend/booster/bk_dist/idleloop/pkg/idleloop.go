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
	"context"
	"os"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/shirou/gopsutil/process"
)

// NewIdleLoop get a new idle loop instance
func NewIdleLoop() *IdleLoop {
	return &IdleLoop{}
}

// IdleLoop 空循环, 目的是保持运行以维持心跳和资源的存在
type IdleLoop struct {
	ppid int32

	// bk-booster -> cmd -> bk-idle-loop, so we should care about the pid of bk-booster
	pppid int32
}

// Run brings up idle loop
func (l *IdleLoop) Run(ctx context.Context) (int, error) {
	return l.run(ctx)
}

func (l *IdleLoop) run(pCtx context.Context) (int, error) {
	defer blog.CloseLogs()

	l.ppid = int32(os.Getppid())
	p, err := process.NewProcess(l.ppid)
	if err == nil {
		l.pppid, err = p.Ppid()
		if err != nil {
			blog.Debugf("idelloop: failed to get grandfather process id for error:%v", err)
		} else {
			blog.Debugf("idelloop: succeed to get grandfather process id %d", l.pppid)
		}
	} else {
		blog.Debugf("idelloop: failed to get grandfather process id for error:%v", err)
	}
	go l.runCommitSuicideCheck(pCtx)

	defer func() {
	}()

	l.runIdleLoop(pCtx)

	return 0, nil
}

func (l *IdleLoop) runIdleLoop(ctx context.Context) {
	for {
		time.Sleep(3600 * time.Second)
	}
}

func (l *IdleLoop) runCommitSuicideCheck(ctx context.Context) {
	ticker := time.NewTicker(5 * time.Second)

	for {
		select {
		case <-ctx.Done():
			blog.Debugf("idelloop: run commit suicide check canceled by context")
			return

		case <-ticker.C:
			blog.Debugf("idelloop: run commit suicide check with parent process %d,grandfather process %d", l.ppid, l.pppid)
			if ok, err := process.PidExists(int32(l.ppid)); err == nil && !ok {
				// p, err := process.NewProcess(int32(os.Getpid()))
				// if err == nil {
				// 	blog.Debugf("idelloop: ready commit suicide for parent process %d not existed", l.ppid)
				// 	// kill children
				// 	KillChildren(p)
				// 	// kill self
				// 	_ = p.Kill()
				// }
				blog.Infof("booster: commit suicide for parent process %d not existed", l.ppid)
				blog.CloseLogs()
				os.Exit(0)
			}

			if ok, err := process.PidExists(int32(l.pppid)); err == nil && !ok {
				// p, err := process.NewProcess(int32(os.Getpid()))
				// if err == nil {
				// 	blog.Debugf("idelloop: ready commit suicide for grandfather process %d not existed", l.pppid)
				// 	// kill children
				// 	KillChildren(p)
				// 	// kill self
				// 	_ = p.Kill()
				// }
				blog.Infof("booster: commit suicide for grandfather process %d not existed", l.pppid)
				blog.CloseLogs()
				os.Exit(0)
			}
		}
	}
}
