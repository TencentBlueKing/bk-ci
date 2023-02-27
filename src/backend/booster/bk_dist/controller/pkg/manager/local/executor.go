/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package local

import (
	"bytes"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/manager/recorder"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/handlermap"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

const (
	ioTimeoutBuffer      = 50
	retryAndSuccessLimit = 3
)

func newExecutor(mgr *Mgr, req *types.LocalTaskExecuteRequest, globalWork *types.Work) (*executor, error) {
	environ := env.NewSandbox(req.Environments)
	bt := dcType.GetBoosterType(environ.GetEnv(env.BoosterType))
	hdl, err := handlermap.GetHandler(bt)
	if err != nil {
		return nil, err
	}
	e := &executor{
		mgr:        mgr,
		req:        req,
		stats:      req.Stats,
		resource:   mgr.resource,
		handler:    hdl,
		globalWork: globalWork,
	}

	// TODO: 临时代码, 临时去除CCACHE_PREFIX, 防止其循环调用, 但还是要考虑一个周全办法
	environments := make([]string, 0, 50)
	for i := range req.Environments {
		if strings.HasPrefix(req.Environments[i], "CCACHE_PREFIX") {
			continue
		}

		environments = append(environments, req.Environments[i])
	}

	e.sandbox = &dcSyscall.Sandbox{
		Dir:    e.req.Dir,
		Env:    env.NewSandbox(environments),
		User:   e.req.User,
		Stdout: &e.outBuf,
		Stderr: &e.errBuf,
	}

	// 若在record中有额外的超时设置, 且大于当前的默认值, 则使用record中的值
	e.ioTimeout, _ = strconv.Atoi(e.sandbox.Env.GetEnv(env.KeyExecutorIOTimeout))
	e.ioTimeoutBySettings = e.ioTimeout
	e.stats.RemoteWorkTimeoutSetting = e.ioTimeout

	if e.sandbox.Env.IsSet(env.KeyExecutorLocalRecord) && mgr.recorder != nil {
		e.record = mgr.recorder.Inspect(recorder.RecordKey(req.Commands))
		if e.record.SuggestTimeout > e.ioTimeout {
			blog.Infof("executor: the command suggest timeout(%d) is greater than the setting(%d), just set it to %d",
				e.record.SuggestTimeout, e.ioTimeout, e.record.SuggestTimeout)
			e.ioTimeout = e.record.SuggestTimeout
			e.stats.RemoteWorkTimeoutUseSuggest = true
		}
	}

	blog.Infof("executor: success to new an executor with boosterType(%s)", bt.String())
	e.handler.InitSandbox(e.sandbox)
	return e, nil
}

type executor struct {
	req        *types.LocalTaskExecuteRequest
	globalWork *types.Work
	stats      *dcSDK.ControllerJobStats
	mgr        *Mgr
	resource   *resource
	sandbox    *dcSyscall.Sandbox
	handler    handler.Handler
	outBuf     bytes.Buffer
	errBuf     bytes.Buffer
	record     *recorder.Record

	ioTimeout           int
	ioTimeoutBySettings int
}

// Stdout return the execution stdout
func (e *executor) Stdout() []byte {
	return e.outBuf.Bytes()
}

// Stderr return the execution stderr
func (e *executor) Stderr() []byte {
	return e.errBuf.Bytes()
}

func (e *executor) degrade() bool {
	if e.sandbox.Env.IsSet(env.KeyExecutorSkipSeparating) {
		return true
	}

	return false
}

func (e *executor) skipLocalRetry() bool {
	if e.sandbox.Env.IsSet(env.KeyExecutorSkipLocalRetry) {
		return true
	}

	return false
}

func (e *executor) executePreTask() (*dcSDK.BKDistCommand, error) {
	// blog.Infof("executor: try to execute pre-task from pid(%d)", e.req.Pid)
	defer e.mgr.work.Basic().UpdateJobStats(e.stats)

	dcSDK.StatsTimeNow(&e.stats.PreWorkEnterTime)
	defer dcSDK.StatsTimeNow(&e.stats.PreWorkLeaveTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)

	if e.handler.PreExecuteNeedLock(e.req.Commands) {
		weight := e.handler.PreLockWeight(e.req.Commands)
		blog.Infof("executor: try to execute pre-task from pid(%d) lockweight(%d)", e.req.Pid, weight)
		if !e.lock(dcSDK.JobUsageLocalPre, weight) {
			return nil, types.ErrSlotsLockFailed
		}
		dcSDK.StatsTimeNow(&e.stats.PreWorkLockTime)
		defer dcSDK.StatsTimeNow(&e.stats.PreWorkUnlockTime)
		defer e.unlock(dcSDK.JobUsageLocalPre, weight)
		e.mgr.work.Basic().UpdateJobStats(e.stats)
	}

	dcSDK.StatsTimeNow(&e.stats.PreWorkStartTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)
	r, err := e.handler.PreExecute(e.req.Commands)
	dcSDK.StatsTimeNow(&e.stats.PreWorkEndTime)
	if err != nil {
		return nil, err
	}

	e.stats.PreWorkSuccess = true
	// delta := e.req.Stats.PreWorkEndTime.Time().Sub(e.req.Stats.PreWorkStartTime.Time())
	// blog.Infof("executor: success to execute pre-task from pid(%d) within %s", e.req.Pid, delta.String())
	blog.Debugf("executor: success to execute pre-task from pid(%d) and got data: %v", e.req.Pid, r)
	return r, nil
}

func (e *executor) needRemoteResource() bool {
	if e.handler != nil {
		return e.handler.NeedRemoteResource(e.req.Commands)
	}

	return false
}

func (e *executor) remoteTryTimes() int {
	if e.handler != nil {
		return e.handler.RemoteRetryTimes() + 1
	}

	return 1
}

func (e *executor) executePostTask(result *dcSDK.BKDistResult) error {
	blog.Infof("executor: try to execute post-task from pid(%d)", e.req.Pid)
	defer e.mgr.work.Basic().UpdateJobStats(e.stats)

	dcSDK.StatsTimeNow(&e.stats.PostWorkEnterTime)
	defer dcSDK.StatsTimeNow(&e.stats.PostWorkLeaveTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)

	if e.handler.PostExecuteNeedLock(result) {
		weight := e.handler.PostLockWeight(result)
		if !e.lock(dcSDK.JobUsageLocalPost, weight) {
			return types.ErrSlotsLockFailed
		}
		dcSDK.StatsTimeNow(&e.stats.PostWorkLockTime)
		defer dcSDK.StatsTimeNow(&e.stats.PostWorkUnlockTime)
		defer e.unlock(dcSDK.JobUsageLocalPost, weight)
		e.mgr.work.Basic().UpdateJobStats(e.stats)
	}

	dcSDK.StatsTimeNow(&e.stats.PostWorkStartTime)
	defer dcSDK.StatsTimeNow(&e.stats.PostWorkEndTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)
	err := e.handler.PostExecute(result)
	if err != nil {
		return err
	}

	for i := range result.Results {
		if i == 0 {
			e.outBuf.Write(result.Results[i].OutputMessage)
			e.errBuf.Write(result.Results[i].ErrorMessage)
		} else {
			e.outBuf.WriteString("\n")
			e.outBuf.Write(result.Results[i].OutputMessage)

			e.errBuf.WriteString("\n")
			e.errBuf.Write(result.Results[i].ErrorMessage)
		}
	}

	e.stats.PostWorkSuccess = true
	blog.Infof("executor: success to execute post-task from pid(%d)", e.req.Pid)
	return nil
}

func (e *executor) executeLocalTask() *types.LocalTaskExecuteResult {
	blog.Infof("executor: try to execute local-task from pid(%d)", e.req.Pid)
	defer e.mgr.work.Basic().UpdateJobStats(e.stats)

	dcSDK.StatsTimeNow(&e.stats.LocalWorkEnterTime)
	defer dcSDK.StatsTimeNow(&e.stats.LocalWorkLeaveTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)

	var locallockweight int32 = 1
	if e.handler != nil && e.handler.LocalLockWeight(e.req.Commands) > 0 {
		locallockweight = e.handler.LocalLockWeight(e.req.Commands)
	}
	if !e.lock(dcSDK.JobUsageLocalExe, locallockweight) {
		blog.Infof("executor:failed to lock with local job usage(%s) weight %d", dcSDK.JobUsageLocalExe, locallockweight)
		return &types.LocalTaskExecuteResult{
			Result: &dcSDK.LocalTaskResult{
				ExitCode: -1,
				Message:  types.ErrSlotsLockFailed.Error(),
				Stdout:   nil,
				Stderr:   nil,
			},
		}
	}
	blog.Infof("executor: got lock to execute local-task from pid(%d) with weight %d", e.req.Pid, locallockweight)
	dcSDK.StatsTimeNow(&e.stats.LocalWorkLockTime)
	defer dcSDK.StatsTimeNow(&e.stats.LocalWorkUnlockTime)
	defer e.unlock(dcSDK.JobUsageLocalExe, locallockweight)

	dcSDK.StatsTimeNow(&e.stats.LocalWorkStartTime)
	defer dcSDK.StatsTimeNow(&e.stats.LocalWorkEndTime)
	e.mgr.work.Basic().UpdateJobStats(e.stats)

	var code int
	var err error
	var stdout, stderr []byte

	if e.handler.LocalExecuteNeed(e.req.Commands) {
		code, err = e.handler.LocalExecute(e.req.Commands)
		stdout, stderr = e.Stdout(), e.Stderr()
	} else {
		sandbox := e.sandbox.Fork()
		var outBuf, errBuf bytes.Buffer
		sandbox.Stdout = &outBuf
		sandbox.Stderr = &errBuf
		code, err = sandbox.ExecCommand(e.req.Commands[0], e.req.Commands[1:]...)
		stdout, stderr = outBuf.Bytes(), errBuf.Bytes()
	}

	if err != nil {
		blog.Errorf("executor: failed to execute local-task from pid(%d): %v, %v",
			e.req.Pid, err, string(stderr))
		return &types.LocalTaskExecuteResult{
			Result: &dcSDK.LocalTaskResult{
				ExitCode: code,
				Message:  err.Error(),
				Stdout:   stdout,
				Stderr:   stderr,
			},
		}
	}

	e.stats.LocalWorkSuccess = true
	blog.Infof("executor: success to execute local-task from pid(%d)", e.req.Pid)
	return &types.LocalTaskExecuteResult{
		Result: &dcSDK.LocalTaskResult{
			ExitCode: code,
			Stdout:   stdout,
			Stderr:   stderr,
		},
	}
}

func (e *executor) executeFinalTask() {
	e.handler.FinalExecute(e.req.Commands)
}

// lock 持锁有两种
//	一是全局锁, 当该work指定要使用全局锁时(表现为globalWork不为空), 只使用全局锁
//  否则使用work自己的local锁
func (e *executor) lock(usage dcSDK.JobUsage, weight int32) bool {
	if e.globalWork != nil {
		return e.globalWork.Local().LockSlots(usage, weight)
	}

	return e.resource.Lock(usage, weight)
}

// unlock 分类原理同lock
func (e *executor) unlock(usage dcSDK.JobUsage, weight int32) {
	if e.globalWork != nil {
		e.globalWork.Local().UnlockSlots(usage, weight)
		return
	}

	e.resource.Unlock(usage, weight)
}

func (e *executor) handleRecord() {
	if e.record == nil {
		return
	}

	e.checkRetryAndSuccessRecord()
	e.checkIOTimeoutRecord()
}

func (e *executor) checkIOTimeoutRecord() {
	if e.record == nil {
		return
	}

	deltaTime := 0
	if e.stats.RemoteWorkTimeout && e.stats.LocalWorkSuccess {
		deltaTime = int(e.stats.LocalWorkEndTime.Unix() - e.stats.LocalWorkStartTime.Unix())
	}

	if d := int(e.stats.RemoteWorkProcessEndTime.Unix() -
		e.stats.RemoteWorkProcessStartTime.Unix()); e.stats.PostWorkSuccess {
		deltaTime = d
	}

	if deltaTime <= 0 {
		return
	}

	deltaTime += ioTimeoutBuffer
	if e.record.SuggestTimeout < deltaTime && e.stats.RemoteWorkTimeoutSec < deltaTime {
		e.record.SuggestTimeout = deltaTime
		go func() {
			if se := e.record.Save(); se != nil {
				blog.Warnf("executor: save record failed: %v", se)
			}
		}()
		return
	}

	if e.ioTimeoutBySettings > deltaTime {
		e.record.SuggestTimeout = 0
		go func() {
			if se := e.record.Save(); se != nil {
				blog.Warnf("executor: save record failed: %v", se)
			}
		}()
	}
}

// 处理当 远程编译失败, 但本地重试成功的时候, 记录这个数据
func (e *executor) checkRetryAndSuccessRecord() {
	if e.record == nil {
		return
	}

	// 要求远程成功返回, 但结果失败, 且本地成功, 否则就跳过处理
	if e.stats.RemoteWorkSuccess && !e.stats.PostWorkSuccess && e.stats.LocalWorkSuccess {
		e.record.RetryAndSuccess++
		go func() {
			if se := e.record.Save(); se != nil {
				blog.Warnf("executor: save record failed: %v", se)
			}
		}()
		return
	}

	// 若有一次成功, 则恢复
	if e.stats.PostWorkSuccess && e.record.RetryAndSuccess > 0 {
		e.record.RetryAndSuccess = 0
		go func() {
			if se := e.record.Save(); se != nil {
				blog.Warnf("executor: save record failed: %v", se)
			}
		}()
	}
}

// 确认是否 远程编译失败, 但本地重试成功的次数超过阈值
func (e *executor) retryAndSuccessTooManyAndDegradeDirectly() bool {
	if e.record == nil {
		return false
	}

	if e.record.RetryAndSuccess >= retryAndSuccessLimit {
		blog.Infof("executor: command degrade to local for it retry-and-success > %d: %v",
			retryAndSuccessLimit, e.req.Commands)
		e.stats.RemoteWorkOftenRetryAndDegraded = true
		return true
	}

	return false
}
