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
	"fmt"
	"math"
	"net/http"
	"os"
	"path/filepath"
	"runtime"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcType "github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	v1 "github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api/v1"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/handler/handlermap"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/client"
	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonHTTP "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/common/version"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"

	"github.com/shirou/gopsutil/process"
)

const (
	renderKeyJobs = "JOBS"

	osWindows = "windows"

	envValueTrue = "true"
)

// ExtraItems describe the info from extra-project-data
type ExtraItems struct {
	IOTimeoutSecs int `json:"io_timeout_secs"`
}

// ExtraString describe the info from task-extra
type ExtraString struct {
	ExtraProjectData string `json:"extra_project_data"`
}

// NewBooster get a new booster instance
func NewBooster(config dcType.BoosterConfig) (*Booster, error) {
	blog.Infof("booster: new booster with config:%+v", config)

	ensureConfig(&config)

	cli := httpclient.NewHTTPClient()
	cli.SetHeader("Content-Type", "application/json")
	cli.SetHeader("Accept", "application/json")
	blog.Debugf("booster: new a booster with config: %v", config)
	hdl, err := handlermap.GetHandler(config.Type)
	if err != nil {
		return nil, err
	}

	return &Booster{
		config:        config,
		handler:       hdl,
		remoteHandler: client.NewCommonRemoteWorker().Handler(0, nil, nil, nil),
		client:        cli,
		controller:    v1.NewSDK(config.Controller),
	}, nil
}

func ensureConfig(config *dcType.BoosterConfig) {
	if config.Transport.HeartBeatTick == 0 {
		config.Transport.HeartBeatTick = 5 * time.Second
	}

	if config.Transport.InspectTaskTick == 0 {
		config.Transport.InspectTaskTick = 1 * time.Second
	}

	if config.Transport.TaskPreparingTimeout == 0 {
		config.Transport.TaskPreparingTimeout = 60 * time.Second
	}

	if config.Transport.PrintTaskInfoEveryTime == 0 {
		config.Transport.PrintTaskInfoEveryTime = 5
	}
}

// Booster describe the booster handler, which provides the methods and actions to build the params from flags and
// communicate with dist-controller
type Booster struct {
	// get from server task info
	taskID string

	registered bool

	unregistered bool

	hostList []*dcProtocol.Host

	config dcType.BoosterConfig

	respTaskInfo *v2.RespTaskInfo

	client *httpclient.HTTPClient

	handler       handler.Handler
	remoteHandler dcSDK.RemoteWorkerHandler

	controller   dcSDK.ControllerSDK
	work         dcSDK.ControllerWorkSDK
	workID       string
	workEnd      bool
	workSettings *dcSDK.ControllerWorkSettings

	ExitCode    int
	ExitMessage string

	ppid      int32
	pppid     int32
	toolchain *dcSDK.Toolchain
}

// GetTaskID get registered taskID
func (b *Booster) GetTaskID() string {
	return b.taskID
}

// Run do the whole work
func (b *Booster) Run(ctx context.Context) (int, error) {
	return b.run(ctx)
}

// RegisterWork do the register
func (b *Booster) RegisterWork() error {
	return b.registerWork()
}

// UnregisterWork do the unregister
func (b *Booster) UnregisterWork() error {
	return b.unregisterWork()
}

// Wait4TaskReady get a event chan and it will receive a msg when task ready
func (b *Booster) Wait4TaskReady(ctx context.Context) (<-chan *TaskEvent, error) {
	return b.wait4TaskReady(ctx)
}

// RunWorks run the commands
func (b *Booster) RunWorks(ctx context.Context, event *TaskEvent) (int, error) {
	return b.runWorks(ctx, event, nil)
}

// RunDegradeWorks run the commands in local
func (b *Booster) RunDegradeWorks(ctx context.Context) (int, error) {
	return b.runDegradeWorks(ctx)
}

// GetApplyParam return the apply param to controller
func (b *Booster) GetApplyParam() *v2.ParamApply {
	data := disttask.ExtraData{
		User:           b.config.Works.User,
		RunDir:         b.config.Works.RunDir,
		Params:         b.config.Args,
		Cmd:            b.config.Cmd,
		BoosterType:    b.config.Type.String(),
		ProcessPerUnit: b.config.Works.LimitPerWorker,
	}
	var r []byte
	_ = codec.EncJSON(data, &r)

	return &v2.ParamApply{
		ProjectID:     b.config.ProjectID,
		Scene:         b.config.Type.String(),
		BuildID:       b.config.BuildID,
		ClientCPU:     runtime.NumCPU(),
		ClientVersion: version.Version,
		Extra:         string(r),
	}
}

// ParseEvent to parse task status event
func (b *Booster) ParseEvent(event *TaskEvent) error {
	return b.parseEvent(event)
}

// SetToolChain set tool chain to controller
func (b *Booster) SetToolChain(f string) error {
	b.config.Works.ToolChainJSONFile = f
	return b.setToolChain()
}

// SetToolChainWithJSON set tool chain wiht json to controller
func (b *Booster) SetToolChainWithJSON(tools *dcSDK.Toolchain) error {
	return b.setToolChainWithJSON(tools)
}

// GetWorkersEnv get current worker env
func (b *Booster) GetWorkersEnv() map[string]string {
	return b.getWorkersEnv()
}

// StartControllerWork to start the worker
func (b *Booster) StartControllerWork() error {
	return b.startControllerWork()
}

// SetSettings to set settings for this work
func (b *Booster) SetSettings() error {
	return b.setSettings()
}

func (b *Booster) getWorkersEnv() map[string]string {
	requiredEnv := make(map[string]string, 10)

	// set the workers' task type, it matters whe whole child workers handler type.
	requiredEnv[env.BoosterType] = b.config.Type.String()
	requiredEnv[env.ProjectID] = b.config.ProjectID
	if b.config.BatchMode {
		requiredEnv[env.BatchMode] = envValueTrue
	} else {
		requiredEnv[env.BatchMode] = "false"
	}
	requiredEnv[env.KeyExecutorTaskID] = b.taskID
	requiredEnv[env.KeyExecutorLogLevel] = b.config.Works.ExecutorLogLevel
	requiredEnv[env.KeyCommonUE4MaxProcess] = fmt.Sprintf("%d", b.getProperJobs())

	if b.config.Works.Jobs > 0 {
		requiredEnv[env.KeyCommonUE4MaxJobs] = fmt.Sprintf("%d", b.config.Works.Jobs)
	}

	if b.config.Works.Local {
		requiredEnv[env.KeyExecutorSkipSeparating] = "1"
	}

	if b.config.Works.HookMode {
		requiredEnv[env.KeyExecutorHookPreloadLibraryLinux] = b.config.Works.HookPreloadLibPath
		requiredEnv[env.KeyExecutorHookPreloadLibraryMacos] = b.config.Works.HookPreloadLibPath
		requiredEnv[env.KeyExecutorHookConfigContent] = b.config.Works.PreloadContent
		requiredEnv[env.KeyExecutorHookConfigContentRaw] = b.config.Works.PreloadContentRaw
	}

	if b.config.Works.CheckMd5 {
		requiredEnv[env.KeyCommonCheckMd5] = envValueTrue
	}

	if b.work != nil {
		requiredEnv[env.KeyExecutorControllerWorkID] = b.workID
	}
	for k, v := range dcSDK.GetControllerConfigToEnv(b.config.Controller) {
		requiredEnv[k] = v
	}

	envValue, err := b.toolchain.GetToolchainEnvValue()
	if err == nil && envValue != "" {
		requiredEnv[env.KeyExecutorToolchainPathMap] = envValue
	}

	if b.config.Works.SupportDirectives {
		requiredEnv[env.KeyExecutorSupportDirectives] = envValueTrue
	}

	if b.config.Works.Pump {
		requiredEnv[env.KeyExecutorPump] = envValueTrue
	}

	if b.config.Works.PumpDisableMacro {
		requiredEnv[env.KeyExecutorPumpDisableMacro] = envValueTrue
	}

	if b.config.Works.PumpIncludeSysHeader {
		requiredEnv[env.KeyExecutorPumpIncludeSysHeader] = envValueTrue
	}

	if b.config.Works.PumpCheck {
		requiredEnv[env.KeyExecutorPumpCheck] = envValueTrue
	}

	if b.config.Works.PumpCache {
		requiredEnv[env.KeyExecutorPumpCache] = envValueTrue
	}

	requiredEnv[env.KeyExecutorPumpCacheDir] = b.config.Works.PumpCacheDir
	requiredEnv[env.KeyExecutorPumpCacheSizeMaxMB] = strconv.Itoa(int(b.config.Works.PumpCacheSizeMaxMB))

	if len(b.config.Works.PumpBlackList) > 0 {
		requiredEnv[env.KeyExecutorPumpBlackKeys] = strings.Join(b.config.Works.PumpBlackList, env.CommonBKEnvSepKey)
	}
	requiredEnv[env.KeyExecutorPumpMinActionNum] = strconv.Itoa(int(b.config.Works.PumpMinActionNum))

	if b.config.Works.IOTimeoutSecs > 0 {
		requiredEnv[env.KeyExecutorIOTimeout] = strconv.Itoa(b.config.Works.IOTimeoutSecs)
	}

	if b.config.Works.WorkerSideCache {
		requiredEnv[env.KeyExecutorWorkerSideCache] = envValueTrue
	}

	if b.config.Works.LocalRecord {
		requiredEnv[env.KeyExecutorLocalRecord] = envValueTrue
	}

	if len(b.config.Works.ForceLocalList) > 0 {
		requiredEnv[env.KeyExecutorForceLocalKeys] = strings.Join(b.config.Works.ForceLocalList, env.CommonBKEnvSepKey)
	}

	if b.config.Works.WriteMemroy {
		requiredEnv[env.KeyExecutorWriteMemory] = envValueTrue
	}

	if b.config.Works.IdleKeepSecs > 0 {
		requiredEnv[env.KeyExecutorIdleKeepSecs] = strconv.Itoa(b.config.Works.IdleKeepSecs)
	}

	resultEnv := make(map[string]string, 10)
	for k, v := range requiredEnv {
		resultEnv[env.GetEnvKey(k)] = v
	}

	return resultEnv
}

func (b *Booster) ensureCommitSuicide(ctx context.Context) {
	if !b.config.Works.CommitSuicide {
		return
	}

	b.ppid = int32(os.Getppid())
	p, err := process.NewProcess(b.ppid)
	if err == nil {
		b.pppid, err = p.Ppid()
		if err != nil {
			blog.Debugf("booster: ensureCommitSuicide failed to get grandfather process id for error:%v", err)
		} else {
			blog.Debugf("booster: ensureCommitSuicide succeed to get father id[%d],grandfather process id[%d]",
				b.ppid, b.pppid)
		}
	} else {
		blog.Debugf("booster: ensureCommitSuicide failed to get grandfather process id for error:%v", err)
	}

	go b.runCommitSuicideCheck(ctx)
}

func (b *Booster) ensureWorkersEnv() {
	for k, v := range b.getWorkersEnv() {
		b.config.Works.Environments[k] = v
	}

	for k, v := range b.config.Works.Environments {
		blog.Debugf("booster: set env %s=%s", k, v)
		if err := os.Setenv(k, v); err != nil {
			blog.Warnf("booster: set env %s=%s error: %v", k, v, err)
		}
	}

	blog.Debugf("booster: got output-env-json-file: %v", b.config.Works.OutputEnvJSONFile)
	for _, f := range b.config.Works.OutputEnvJSONFile {
		if f == "nothing" {
			continue
		}

		_ = saveMapAsJSON(b.config.Works.Environments, f)
	}

	blog.Debugf("booster: got output-env-source-file: %v", b.config.Works.OutputEnvSourceFile)
	for _, f := range b.config.Works.OutputEnvSourceFile {
		_ = saveMapAsSource(b.config.Works.Environments, f)
	}

	//blog.Debugf("booster: got output-env-pure-file: %v", b.config.Works.OutputEnvSourceFile)
	//for _, f := range b.config.Works.OutputEnvPureFile {
	//	_ = saveMapAsPure(b.config.Works.Environments, f)
	//}
}

func (b *Booster) preliminaryChecks() error {
	if err := checkDNS(b.config.Transport.ServerDomain); err != nil {
		blog.Errorf("booster: check dns failed: %v", err)
		return err
	}

	if b.config.Works.HookMode {
		if err := b.ensureHookConfigurations(); err != nil {
			blog.Errorf("booster: ensure hook config failed: %v", err)
			return err
		}
	}

	return nil
}

func (b *Booster) initGlobalExtraData(data string) error {
	blog.Infof("booster: ready init global extra data: %s", data)
	var extra ExtraString
	if err := codec.DecJSON([]byte(data), &extra); err != nil {
		return err
	}

	var items ExtraItems
	if err := codec.DecJSON([]byte(extra.ExtraProjectData), &items); err != nil {
		return err
	}

	// set io timeout
	if b.config.Works.IOTimeoutSecs <= 0 && items.IOTimeoutSecs > 0 {
		b.config.Works.IOTimeoutSecs = items.IOTimeoutSecs
		blog.Infof("booster: set io time out: %d", items.IOTimeoutSecs)
	}

	return nil
}

func (b *Booster) parseEvent(event *TaskEvent) error {
	// handle task not ready errors.
	if event.Err != nil {
		message := event.Err.Error()
		if event.Info != nil {
			message += " | " + event.Info.Message
		}
		blog.Warnf("booster: apply resource for works(%s) and task(%s) failed, task not ready: %s",
			b.workID, b.taskID, message)

		return event.Err
	}

	if b.taskID == "" {
		b.taskID = b.workID
	}
	b.hostList = getHosts(event.Info.Task.HostList)
	_ = b.initGlobalExtraData(event.Info.Task.Extra)
	b.handler.InitExtra([]byte(event.Info.Task.Extra))
	return nil
}

func (b *Booster) runWithApply(pCtx context.Context) (int, error) {
	// ensure that all goroutine exit after run over.
	ctx, cancel := context.WithCancel(pCtx)
	defer cancel()

	if b.config.Works.Local {
		return b.runWorks(ctx, nil, nil)
	}

	event, err := b.wait4TaskReady(ctx)
	if err != nil {
		blog.Errorf("booster: run booster wait for task(%s) ready failed: %v", b.taskID, err)
		return -1, err
	}

	for {
		select {
		case <-pCtx.Done():
			blog.Warnf("booster: run booster for task(%s) canceled by context", b.taskID)
			return -1, ErrContextCanceled

		case e := <-event:
			if err = b.parseEvent(e); err != nil {
				// TODO (tomtian) : do not degrade when failed to apply resource
				// return b.runDegradeWorks(ctx)
				blog.Infof("booster: failed to apply resource for work(%s) with error:%v", b.workID, err)
				return b.runWorks(ctx, nil, nil)
			}
			return b.runWorks(ctx, nil, nil)
		}
	}
}

func (b *Booster) run(pCtx context.Context) (int, error) {
	b.ensureCommitSuicide(pCtx)
	if err := b.preliminaryChecks(); err != nil {
		return b.runDegradeWorks(pCtx)
	}

	// support pump cache check
	b.checkPumpCache()

	// no work commands do not register
	if b.config.Works.NoWork {
		return b.runWorks(pCtx, nil, b.runNoWorkCommands)
	}

	if err := b.registerWork(); err != nil {
		blog.Errorf("booster: try register work failed: %v", err)
		return b.runDegradeWorks(pCtx)
	}

	b.registered = true

	defer func() {
		_ = b.unregisterWork()
	}()

	if b.config.Works.Degraded {
		return b.runDegradeWorks(pCtx)
	}

	// run with
	return b.runWithApply(pCtx)
}

func (b *Booster) registerWork() error {
	if b.registered {
		blog.Errorf("booster: register work failed: work already registered")
		return ErrWorkAlreadyRegistered
	}

	blog.Debugf("booster: try to find controller or launch it")
	pid, err := b.controller.EnsureServer()
	if err != nil {
		blog.Errorf("booster: ensure controller failed: %v", err)
		return err
	}
	blog.Infof("booster: success to connect to controller: %s", b.config.Controller.Target())

	b.work, err = b.controller.Register(dcSDK.ControllerRegisterConfig{
		BatchMode:        b.config.BatchMode,
		ServerHost:       b.config.Transport.ServerHost,
		SpecificHostList: b.config.Works.WorkerList,
		NeedApply:        !(b.config.Works.Local || b.config.Works.Degraded),
		Apply:            b.GetApplyParam(),
	})
	if err != nil {
		blog.Errorf("booster: register new work in controller failed: %v", err)
		return err
	}

	b.registered = true
	b.workID = b.work.ID()

	blog.Infof("booster: success to register to controller, pid: %d, workID: %s, taskID: %s",
		pid, b.workID, b.taskID)
	return nil
}

func (b *Booster) unregisterWork() error {
	if b.work == nil {
		return nil
	}

	if b.unregistered {
		blog.Errorf("booster: unregister work failed: work already registered")
		return ErrWorkAlreadyUnregistered
	}

	defer func() {
		b.unregistered = true
	}()

	var extra string
	if b.handler != nil {
		extra = string(b.handler.ResultExtra())
	}

	if err := b.work.Unregister(dcSDK.ControllerUnregisterConfig{
		Release: &v2.ParamRelease{
			Success: b.ExitCode == 0,
			Message: b.ExitMessage,
			Extra:   extra,
		},
	}); err != nil {
		blog.Errorf("booster: unregister work in controller failed: %v", err)
		return err
	}

	blog.Infof("booster: success to unregister work(%s) with exit code: %d", b.workID, b.ExitCode)
	return nil
}

func (b *Booster) sendAdditionFile() {
	if b.config.Works.Local || b.config.Works.Degraded {
		return
	}

	fds := make([]dcSDK.FileDesc, 0, 100)
	for _, f := range b.config.Works.AdditionFiles {
		info := dcFile.Stat(f)
		existed, fileSize, modifyTime, fileMode := info.Batch()
		if !existed {
			blog.Errorf("booster: send addition file %s failed: file not exist", f)
			continue
		}

		lt := ""
		if info.Basic().Mode().IsDir() {
			if lInfo := dcFile.Lstat(f); lInfo.Basic().Mode()&os.ModeSymlink != 0 {
				fileMode = lInfo.Mode32()
				lt, _ = os.Readlink(f)
			}
		}
		absPath, _ := filepath.Abs(f)

		fds = append(fds, dcSDK.FileDesc{
			FilePath:           f,
			Compresstype:       dcProtocol.CompressLZ4,
			FileSize:           fileSize,
			Lastmodifytime:     modifyTime,
			Md5:                "",
			Filemode:           fileMode,
			Targetrelativepath: filepath.Dir(absPath),
			LinkTarget:         lt,
			AllDistributed:     true,
		})
	}

	if err := b.work.Job(&dcSDK.ControllerJobStats{
		Pid:        os.Getpid(),
		WorkID:     b.workID,
		TaskID:     b.taskID,
		OriginArgs: []string{"send files"},
	}).SendRemoteFile2All(fds); err != nil {
		blog.Errorf("booster: send addition file failed: %v", err)
		return
	}
	blog.Infof("booster: finish send addition files: %v", b.config.Works.AdditionFiles)
}

func (b *Booster) runWorks(
	ctx context.Context,
	_ *TaskEvent,
	commandFunc func(context.Context) (int, error)) (code int, workErr error) {
	// do pre works before command runs
	if err := b.handler.PreWork(&b.config); err != nil {
		blog.Errorf("booster: run pre works for task(%s) type(%s) failed: %v",
			b.taskID, b.config.Type.String(), err)
		return b.runDegradeWorks(ctx)
	}

	if commandFunc != nil {
		code, workErr = commandFunc(ctx)
	} else {
		code, workErr = b.runCommands(ctx)
	}
	b.ExitCode = code
	if workErr != nil {
		blog.Errorf("booster: do works %s, failed: %v", b.config.Args, workErr)

		b.ExitMessage = workErr.Error()
		workErr = ErrCompile
	}
	blog.Debugf("booster: run task(%s) commands(%s) done, code: %d, err: %v",
		b.taskID, b.config.Args, code, workErr)

	// do post works after command runs
	if err := b.handler.PostWork(&b.config); err != nil {
		blog.Errorf("booster: run post works for task(%s) type(%s) failed: %v",
			b.taskID, b.config.Type.String(), err)
	}

	return code, workErr
}

func (b *Booster) runNoWorkCommands(ctx context.Context) (code int, err error) {
	if _, err = b.initPreloadConfig(); err != nil {
		return -1, err
	}
	// before run the commands, ensure that environments for workers are set.
	b.ensureWorkersEnv()

	args := b.renderArgs(b.config.Args)
	blog.Infof("booster: run no-work commands", b.config.Args)
	blog.Infof("booster: got origin command: %s", b.config.Args)
	blog.Infof("booster: exec command: %s", args)

	done := make(chan bool, 1)

	go func() {
		sandbox := dcSyscall.Sandbox{}
		_, err = sandbox.ExecScripts(args)

		done <- true
	}()

	for {
		select {
		case <-ctx.Done():
			blog.Warnf("booster: exec command canceled by context: %s", args)
			return -1, ErrContextCanceled

		case <-done:
			return getExitCodeFromError(err), err
		}
	}
}

func (b *Booster) runCommands(ctx context.Context) (code int, err error) {
	// set hosts to controller.
	if err = b.setSettings(); err != nil {
		blog.Errorf("booster: runCommands set settings for task(%s) type(%s) failed: %v",
			b.taskID, b.config.Type.String(), err)
		return -1, err
	}

	// set tool chain if necessary
	if err = b.setToolChain(); err != nil {
		blog.Errorf("booster: runCommands set tool chain for task(%s) type(%s) failed: %v",
			b.taskID, b.config.Type.String(), err)
		return -1, err
	}

	// if work not nil, should call controller to start it.
	if err = b.startControllerWork(); err != nil {
		blog.Errorf("booster: runCommands task(%s) start work(%s) in controller failed: %v",
			b.taskID, b.workID, err)
		return -1, err
	}
	blog.Infof("booster: work(%s) start", b.workID)
	defer func() {
		_ = b.recordWorkStats(code)
		blog.Infof("booster: work(%s) end with: %v", b.workID, err)
		_ = b.endControllerWork()
	}()

	// send addition files to workers before run commands.
	b.sendAdditionFile()

	// before run the commands, ensure that environments for workers are set.
	b.ensureWorkersEnv()

	args := b.renderArgs(b.config.Args)
	blog.Infof("booster: got origin command: %s", b.config.Args)
	blog.Infof("booster: exec command: %s", args)

	done := make(chan bool, 1)

	go func() {
		sandbox := dcSyscall.Sandbox{}
		_, err = sandbox.ExecScripts(args)

		done <- true
	}()

	for {
		select {
		case <-ctx.Done():
			blog.Warnf("booster: exec command canceled by context: %s", args)
			return -1, ErrContextCanceled

		case <-done:
			return getExitCodeFromError(err), err
		}
	}
}

func (b *Booster) startControllerWork() error {
	if b.work == nil {
		return nil
	}

	if err := b.work.Start(); err != nil {
		blog.Errorf("booster: task(%s) start work(%s) in controller failed: %v", b.taskID, b.workID, err)
		return err
	}

	blog.Debugf("booster: task(%s) success to start work(%s)", b.taskID, b.workID)
	return nil
}

func (b *Booster) endControllerWork() error {
	if b.work == nil {
		return nil
	}

	if !b.workEnd {
		b.workEnd = true
		if err := b.work.End(); err != nil {
			blog.Errorf("booster: task(%s) end work(%s) in controller failed: %v", b.taskID, b.workID, err)
			return err
		}
	}

	return nil
}

func (b *Booster) recordWorkStats(code int) error {
	if b.work == nil {
		return nil
	}

	if err := b.work.RecordWorkStats(&dcSDK.ControllerWorkStats{
		Success: code == 0,
	}); err != nil {
		blog.Errorf("booster: task(%s) record work(%s) stats to controller failed: %v", b.taskID, b.workID, err)
		return err
	}

	return nil
}

func (b *Booster) initPreloadConfig() (*dcSDK.PreloadConfig, error) {
	// Get preload config, if hook mode enable and get config failed, return error.
	// Prevent from the danger of high jobs with no preload. which may lead to overload
	var preloadConfig *dcSDK.PreloadConfig
	var err error
	if b.config.Works.HookMode {
		preloadConfig, err = b.handler.GetPreloadConfig(b.config)
		if err != nil {
			blog.Errorf("booster: under hook mode get preload config for task(%s) and work(%s) failed: %v",
				b.taskID, b.workID, err)
			return nil, err
		}

		// adjust target command if not existed
		err = b.adjustPreloadConfig(preloadConfig)
		if err != nil {
			blog.Errorf("booster: under hook mode get preload config for task(%s) and work(%s) failed: %v",
				b.taskID, b.workID, err)
			return nil, err
		}

		// set env for preload config
		var data []byte
		_ = codec.EncJSON(preloadConfig, &data)
		b.config.Works.PreloadContent = string(data)
		b.config.Works.PreloadContentRaw = preloadConfig.GetContentRaw()
	}

	return preloadConfig, nil
}

func (b *Booster) setSettings() error {
	blog.Infof("booster: start set settings with task(%s) work(%s) ", b.taskID, b.workID)

	// if b.work == nil || b.respTaskInfo == nil {
	if b.work == nil {
		return nil
	}

	preloadConfig, err := b.initPreloadConfig()
	if err != nil {
		return err
	}

	rules, err := b.handler.GetFilterRules()
	if err != nil {
		blog.Errorf("booster: task(%s) set settings to work(%s) get send file filter rules failed: %v",
			b.taskID, b.workID, err)
		return err
	}
	blog.Infof("booster: task(%s) get filter rules: %+v", b.taskID, rules)

	b.workSettings = &dcSDK.ControllerWorkSettings{
		TaskID:    b.taskID,
		ProjectID: b.config.ProjectID,
		Scene:     b.config.Type.String(),
		UsageLimit: map[dcSDK.JobUsage]int{
			dcSDK.JobUsageRemoteExe: 0,
			dcSDK.JobUsageLocalPre:  b.config.Works.MaxLocalPreJobs,
			dcSDK.JobUsageLocalExe:  b.config.Works.MaxLocalExeJobs,
			dcSDK.JobUsageLocalPost: b.config.Works.MaxLocalPostJobs,
		},
		LocalTotalLimit: b.config.Works.MaxLocalTotalJobs,
		Preload:         preloadConfig,
		FilterRules:     rules,
		Degraded:        b.config.Works.Degraded,
		GlobalSlots:     b.config.Works.GlobalSlots,
	}
	if err := b.work.SetSettings(b.workSettings); err != nil {
		blog.Errorf("booster: task(%s) set settings to work(%s) in controller failed: %v",
			b.taskID, b.workID, err)
		return err
	}

	blog.Infof("booster: task(%s) success to set settings to work(%s) in controller", b.taskID, b.workID)
	return nil
}

func (b *Booster) adjustPreloadConfig(preloadConfig *dcSDK.PreloadConfig) error {
	for _, v := range preloadConfig.Hooks {
		targetstrs := strings.Split(v.TargetCommand, " ")
		if len(targetstrs) > 0 {
			targetexe := targetstrs[0]
			_, err := dcUtil.CheckExecutable(targetexe)
			if err != nil {
				// blog.Infof("booster: not found hook target command[%s] in default path", v.TargetCommand)
				abspath, err := dcUtil.CheckFileWithCallerPath(targetexe)
				if err != nil {
					blog.Infof("booster: not found hook target command[%s] in booster path", v.TargetCommand)
					return err
				}

				targetstrs[0] = abspath
				// blog.Infof("booster: before adjust: hook target command[%s]", v.TargetCommand)
				v.TargetCommand = strings.Join(targetstrs, " ")
				// blog.Infof("booster: after adjust: hook target command[%s]", v.TargetCommand)
			}
		}
	}
	return nil
}

func (b *Booster) getProperJobs() int {
	if b.config.Works.Degraded || b.workSettings == nil {
		if b.config.Works.MaxDegradedJobs > 0 && b.config.Works.MaxDegradedJobs < runtime.NumCPU() {
			return b.config.Works.MaxDegradedJobs
		}

		return runtime.NumCPU()
	}

	jobs := 0

	if b.config.Works.Jobs > 0 {
		jobs = b.config.Works.Jobs
	} else {
		for _, host := range b.hostList {
			if host == nil {
				continue
			}

			jobs += host.Jobs
		}
		jobs = int(float64(jobs) * 1.5)

		// set jobs of ue4 by client cpus
		if b.config.Type == dcType.BoosterUE4 {
			localcpunum := runtime.NumCPU()
			jobsByCPU := 0
			if localcpunum <= 8 {
				jobsByCPU = runtime.NumCPU() * 12
			} else if localcpunum <= 16 {
				jobsByCPU = 144
			} else {
				jobsByCPU = 192
			}
			jobs = int(math.Min(float64(jobsByCPU), float64(jobs)))
		}
	}

	if b.config.Works.MaxJobs > 0 && b.config.Works.MaxJobs < jobs {
		return b.config.Works.MaxJobs
	}

	if jobs <= 0 {
		return runtime.NumCPU()
	}

	return jobs
}

func (b *Booster) renderArgs(s string) string {
	s = b.handler.RenderArgs(b.config, s)

	s = replaceArgsReversedKey(s, renderKeyJobs, fmt.Sprintf("%d", b.getProperJobs()))
	return s
}

func (b *Booster) ensureHookConfigurations() error {
	var err error

	if runtime.GOOS != osWindows {
		if err = fileReadable(b.config.Works.HookPreloadLibPath); err != nil {
			return err
		}
	}

	return nil
}

func (b *Booster) runDegradeWorks(ctx context.Context) (int, error) {
	blog.Infof("booster: task(%s) work(%s) degraded to local working", b.taskID, b.workID)

	if b.config.Works.NoLocal {
		return -1, ErrNoLocal
	}

	// set config degraded is true, means the rest time all functions will acts for degrade work.
	b.config.Works.Degraded = true
	code, err := b.runCommands(ctx)
	b.ExitCode = code
	return code, err
}

// TaskEvent describe the message of task info
type TaskEvent struct {
	Info *dcSDK.WorkStatusDetail
	Err  error
}

// taskWatcher brings up a ticker to inspect task info, until the task is running or terminated(finish/failed).
// Also exit when the context canceled or reach the TaskPreparingTimeout.
func (b *Booster) taskWatcher(ctx context.Context, event chan<- *TaskEvent) {
	ticker := time.NewTicker(b.config.Transport.InspectTaskTick)
	timeout := time.After(b.config.Transport.TaskPreparingTimeout)
	errTimes := 0
	var lastMessage string

	for t := 0; ; t++ {
		select {
		case <-ctx.Done():
			blog.Debugf("booster: wait for task(%s) ready canceled by context", b.taskID)
			event <- &TaskEvent{
				Info: nil,
				Err:  ErrContextCanceled,
			}
			return

		case <-ticker.C:
			info, err := b.work.Status()
			if err != nil {
				errTimes++
				blog.Warnf("booster: wait for task(%s) ready and inspect failed: %v", b.taskID, err)

				// 查询status期间多次失败, 可能是controller异常, 不要等太久, 直接失败
				if errTimes >= 3 {
					blog.Warnf("booster: task(%s) resource applying failed: get status err too many times",
						b.taskID)
					event <- &TaskEvent{
						Info: nil,
						Err:  ErrTaskApplyingFailed,
					}
					return
				}
				continue
			}

			blog.Debugf("booster: status(%s)", info.Status.String())
			if info.Message != lastMessage || t%b.config.Transport.PrintTaskInfoEveryTime == 0 {
				blog.Infof("booster: %s", info.Message)
				lastMessage = info.Message
			}

			if info.Task != nil {
				b.taskID = info.Task.TaskID
			}

			if info.Status.IsResourceApplyFailed() {
				blog.Warnf("booster: task(%s) resource applying failed: %v", b.taskID, info.Message)
				event <- &TaskEvent{
					Info: nil,
					Err:  ErrTaskApplyingFailed,
				}
				return
			}

			if info.Task == nil {
				continue
			}

			// batch mode条件下, 有可能已经被他人start了, 这个时候只要是working即可
			// 若是一般情况下, 则resource applied即可
			ok := info.Status.IsResourceApplied() || info.Status.IsWorking()

			if ok {
				blog.Debugf("booster: wait for task(%s) ready and get result: %+v", b.taskID, info)
				event <- &TaskEvent{
					Info: info,
					Err:  nil,
				}
				return
			}

		case <-timeout:
			blog.Warnf("booster: wait for task ready timeout(%s)",
				b.config.Transport.TaskPreparingTimeout.String())
			event <- &TaskEvent{
				Info: nil,
				Err:  ErrTaskPreparingTimeout,
			}
			return
		}
	}
}

func (b *Booster) wait4TaskReady(ctx context.Context) (<-chan *TaskEvent, error) {
	if !b.registered {
		return nil, ErrBoosterNoRegistered
	}

	event := make(chan *TaskEvent, 1)
	go b.taskWatcher(ctx, event)
	return event, nil
}

func (b *Booster) runCommitSuicideCheck(ctx context.Context) {
	ticker := time.NewTicker(b.config.Transport.CommitSuicideCheckTick)

	for {
		select {
		case <-ctx.Done():
			blog.Debugf("booster: run commit suicide check canceled by context")
			return

		case <-ticker.C:
			blog.Debugf("booster: run commit suicide check with parent process %d", b.ppid)
			if ok, err := process.PidExists(b.ppid); err == nil && !ok {
				_ = b.unregisterWork()

				// p, err := process.NewProcess(int32(os.Getpid()))
				// if err == nil {
				// 	blog.Debugf("booster: ready commit suicide for parent process %d not existed", b.ppid)
				// 	// by tomtian 20200225, do not kill children process
				// 	// kill children
				// 	//KillChildren(p)

				// 	// kill self
				// 	_ = p.Kill()
				// }
				blog.Infof("booster: commit suicide for parent process %d not existed", b.ppid)
				blog.CloseLogs()
				os.Exit(0)
			}

			blog.Debugf("booster: run commit suicide check with grandfather process %d", b.pppid)
			if ok, err := process.PidExists(b.pppid); err == nil && !ok {
				_ = b.unregisterWork()

				// p, err := process.NewProcess(int32(os.Getpid()))
				// if err == nil {
				// 	blog.Debugf("booster: ready commit suicide for grandfather process %d not existed", b.pppid)
				// 	// kill children
				// 	//KillChildren(p)

				// 	// kill self
				// 	_ = p.Kill()
				// }
				blog.Infof("booster: commit suicide for grandfather process %d not existed", b.pppid)
				blog.CloseLogs()
				os.Exit(0)
			}
		}
	}
}

// request send request to server and get return data.
// if requests failed, http code is not 200, return flag false.
// else once the http code is 200, return flat true.
// Only both flag is true and error is nil, guarantees that the data is not nil.
func (b *Booster) request(method, server, uri string, data []byte) ([]byte, bool, error) {
	uri = fmt.Sprintf("%s/%s", server, uri)
	blog.Debugf("booster: method(%s), server(%s) uri(%s), data: %s", method, server, uri, string(data))

	var resp *httpclient.HttpResponse
	var err error

	switch method {
	case "GET":
		resp, err = b.client.Get(uri, nil, data)

	case "POST":
		resp, err = b.client.Post(uri, nil, data)

	case "DELETE":
		resp, err = b.client.Delete(uri, nil, data)

	case "PUT":
		resp, err = b.client.Put(uri, nil, data)

	default:
		err = fmt.Errorf("uri %s method %s is invalid", uri, method)
	}

	if err != nil {
		return nil, false, err
	}

	if resp.StatusCode != http.StatusOK {
		return nil, false, fmt.Errorf("%s", string(resp.Reply))
	}

	var apiResp *commonHTTP.APIResponse
	if err = codec.DecJSON(resp.Reply, &apiResp); err != nil {
		return nil, true, fmt.Errorf("decode request %s response %s error %s",
			uri, string(resp.Reply), err.Error())
	}

	if apiResp.Code != common.RestSuccess {
		return nil, true, fmt.Errorf(apiResp.Message)
	}

	var by []byte
	if err = codec.EncJSON(apiResp.Data, &by); err != nil {
		return nil, true, fmt.Errorf("encode apiResp.Data error %s", err.Error())
	}

	return by, true, nil
}

func (b *Booster) setToolChain() error {
	blog.Debugf("booster: try to set tool chain")

	if b.config.Works.ToolChainJSONFile == "" || b.config.Works.ToolChainJSONFile == "nothing" {
		blog.Debugf("booster: tool chain not set, do nothing now")
		return nil
	}

	tools, err := resolveToolChainJSON(b.config.Works.ToolChainJSONFile)
	if err != nil {
		blog.Warnf("booster: failed to resolve %s with error:%v", b.config.Works.ToolChainJSONFile, err)
		return err
	}

	// for _, v := range tools.Toolchains {
	// 	var data []byte
	// 	_ = codec.EncJSON(&v, &data)
	// 	commonconfig := dcSDK.CommonControllerConfig{
	// 		Configkey: dcSDK.CommonConfigKeyToolChain,
	// 		WorkerKey: dcSDK.WorkerKeyConfig{
	// 			BatchMode: b.config.BatchMode,
	// 			ProjectID: b.config.ProjectID,
	// 			Scene:     b.config.Type.String(),
	// 		},
	// 		Data: data,
	// 	}

	// 	err = b.controller.SetConfig(&commonconfig)
	// 	if err != nil {
	// 		blog.Warnf("booster: failed to set config [%+v] with error:%v", commonconfig, err)
	// 		return err
	// 	}
	// }

	// blog.Debugf("booster: success to set tool chain")
	// return nil

	return b.setToolChainWithJSON(tools)
}

func (b *Booster) setToolChainWithJSON(tools *dcSDK.Toolchain) error {
	blog.Debugf("booster: try to set tool chain")

	if tools == nil {
		return fmt.Errorf("tools is nil")
	}

	for _, v := range tools.Toolchains {
		var data []byte
		_ = codec.EncJSON(&v, &data)
		commonconfig := dcSDK.CommonControllerConfig{
			Configkey: dcSDK.CommonConfigKeyToolChain,
			WorkerKey: dcSDK.WorkerKeyConfig{
				BatchMode: b.config.BatchMode,
				ProjectID: b.config.ProjectID,
				Scene:     b.config.Type.String(),
			},
			Data: data,
		}

		blog.Infof("booster: set tool chain with WorkerKey:%+v", commonconfig.WorkerKey)
		err := b.controller.SetConfig(&commonconfig)
		if err != nil {
			blog.Warnf("booster: failed to set config [%+v] with error:%v", commonconfig, err)
			return err
		}
	}

	blog.Debugf("booster: success to set tool chain")
	return nil
}

func (b *Booster) checkPumpCache() {
	if b.config.Works.PumpCache || b.config.Works.PumpCacheRemoveAll {
		pumpdir := b.config.Works.PumpCacheDir
		if pumpdir == "" {
			pumpdir = dcUtil.GetPumpCacheDir()
		}

		if pumpdir != "" {
			blog.Infof("booster: ready clean pump cache dir:%s", pumpdir)
			if b.config.Works.PumpCacheRemoveAll {
				os.RemoveAll(pumpdir)
			} else {
				limitsize := int64(b.config.Works.PumpCacheSizeMaxMB * 1024 * 1024)
				cleanDirByTime(pumpdir, limitsize)
			}
		} else {
			blog.Infof("booster: not found pump cache dir, do nothing")
		}
	}
}
