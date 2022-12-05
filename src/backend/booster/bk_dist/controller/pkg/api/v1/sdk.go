/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v1

import (
	"context"
	"fmt"
	"net"
	"net/http"
	"os"
	"os/user"
	"path/filepath"
	"runtime"
	"sync"
	"time"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonHTTP "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
)

// NewSDK get a new controller SDK with config
func NewSDK(config dcSDK.ControllerConfig) dcSDK.ControllerSDK {
	s := &sdk{
		config:     config,
		client:     httpclient.NewHTTPClient(),
		waitClient: httpclient.NewHTTPClient(),
		works:      make(map[string]*workSDK, 10),
	}
	s.client.SetTimeOut(config.Timeout)

	return s
}

const (
	availableURI        = "api/v1/dist/available"
	registerURI         = "api/v1/dist/work/register"
	commonConfigURI     = "api/v1/dist/work/commonconfig"
	heartbeatURI        = "api/v1/dist/work/%s/heartbeat"
	unregisterURI       = "api/v1/dist/work/%s/unregister"
	occupyLocalSlotsURI = "api/v1/dist/work/%s/slots/local/occupy"
	freeLocalSlotsURI   = "api/v1/dist/work/%s/slots/local/free"
	startURI            = "api/v1/dist/work/%s/start"
	endURI              = "api/v1/dist/work/%s/end"
	statusURI           = "api/v1/dist/work/%s/status"
	settingsURI         = "api/v1/dist/work/%s/settings"
	jobStatsURI         = "api/v1/dist/work/%s/job/stats"
	workStatsURI        = "api/v1/dist/work/%s/stats"
	remoteExecURI       = "api/v1/dist/work/%s/remote/execute"
	remoteSendURI       = "api/v1/dist/work/%s/remote/send"
	localExecURI        = "api/v1/dist/work/%s/local/execute"

	serverEnsureTime = 60 * time.Second
)

type sdk struct {
	config dcSDK.ControllerConfig

	client     *httpclient.HTTPClient
	waitClient *httpclient.HTTPClient

	worksLock sync.RWMutex
	works     map[string]*workSDK
}

// EnsureServer make sure the controller is running and ready to work
func (s *sdk) EnsureServer() (int, error) {
	return s.ensureServer()
}

// Register do the work register to controller
func (s *sdk) Register(config dcSDK.ControllerRegisterConfig) (dcSDK.ControllerWorkSDK, error) {
	return s.register(config)
}

// GetWork return the work SDK with workID
func (s *sdk) GetWork(workID string) dcSDK.ControllerWorkSDK {
	return s.getWork(workID)
}

// SetConfig update the common controller config to controller
func (s *sdk) SetConfig(config *dcSDK.CommonControllerConfig) error {
	return s.setConfig(config)
}

func (s *sdk) ensureServer() (int, error) {
	launched := false
	var launchedtime int64
	timeout := time.After(serverEnsureTime)

	for ; ; time.Sleep(100 * time.Millisecond) {
		select {
		case <-timeout:
			return 0, fmt.Errorf("ensure server timeout")
		default:
			pid, err := s.checkServer(launchedtime)

			switch err {
			case nil:
				return pid, nil

			case dcSDK.ErrControllerNotReady:
				if launched {
					continue
				}

				err = s.launchServer()
				if err != nil {
					return 0, err
				}
				launched = true
				launchedtime = time.Now().Unix()
				continue

			case dcSDK.ErrControllerKilled:
				err = s.launchServer()
				if err != nil {
					return 0, err
				}
				launched = true
				launchedtime = time.Now().Unix()
				continue

			default:
				return 0, err
			}
		}
	}
}

func (s *sdk) checkServer(launchedtime int64) (int, error) {
	blog.Infof("sdk: check server...")

	// ProcessExistTimeoutAndKill(windows.EnumProcesses) maybe block, call it carefully
	// kill launched process if not succeed after 30 seconds
	if launchedtime > 0 {
		nowsecs := time.Now().Unix()
		if nowsecs-launchedtime > 30 {
			blog.Infof("sdk: try kill existed server for unavailable after long time")
			_ = dcUtil.ProcessExistTimeoutAndKill(controllerTarget(dcSDK.ControllerBinary), 1*time.Minute)
			return 0, dcSDK.ErrControllerKilled
		}
	}

	// check tcp port
	conn, err := net.DialTimeout("tcp", s.config.Target(), 1*time.Second)
	if err != nil {
		blog.Infof("sdk: check tcp port, error: %v", err)
		return 0, dcSDK.ErrControllerNotReady
	}
	_ = conn.Close()

	// check http api
	tmp, _, err := s.request("GET", availableURI, nil, false)
	if err != nil {
		blog.Infof("sdk: check http api, error: %v", err)
		return 0, dcSDK.ErrControllerNotReady
	}

	// decode http response
	var resp AvailableResp
	if err = codec.DecJSON(tmp, &resp); err != nil {
		blog.Warnf("sdk: decode http response, error: %v", err)
		return 0, dcSDK.ErrControllerNotReady
	}

	blog.Infof("sdk: service available now, process pid is %d", resp.Pid)
	return resp.Pid, nil
}

func (s *sdk) launchServer() error {
	blog.Infof("sdk: ready launchServer...")

	ctrlPath, err := dcUtil.CheckExecutable(dcSDK.ControllerBinary)
	if err != nil {
		blog.Infof("sdk: not found exe file with default path, info: %v", err)

		target := controllerTarget(dcSDK.ControllerBinary)
		ctrlPath, err = dcUtil.CheckFileWithCallerPath(target)
		if err != nil {
			blog.Errorf("sdk: not found exe file with error: %v", err)
			return err
		}
	}
	absPath, err := filepath.Abs(ctrlPath)
	if err == nil {
		ctrlPath = absPath
	}
	ctrlPath = dcUtil.QuoteSpacePath(ctrlPath)
	blog.Infof("sdk: got exe file full path: %s", ctrlPath)

	sudo := ""
	if s.config.Sudo && runtime.GOOS != "windows" {
		sudo = "sudo "
	}

	nowait := ""
	if s.config.NoWait {
		nowait = "--no_wait"
	}

	disablefilelock := ""
	if s.config.DisableFileLock {
		disablefilelock = "--disable_file_lock"
	}

	autoResourceMgr := ""
	if s.config.AutoResourceMgr {
		autoResourceMgr = "--auto_resource_mgr"
	}

	sendcork := ""
	if s.config.SendCork {
		sendcork = "--send_cork"
	}

	return dcSyscall.RunServer(fmt.Sprintf("%s%s -a=%s -p=%d --log-dir=%s --v=%d --local_slots=%d "+
		"--local_pre_slots=%d --local_exe_slots=%d --local_post_slots=%d --async_flush %s --remain_time=%d "+
		"--use_local_cpu_percent=%d %s"+
		"%s --res_idle_secs_for_free=%d"+
		" %s",
		sudo,
		ctrlPath,
		s.config.IP,
		s.config.Port,
		s.config.LogDir,
		s.config.LogVerbosity,
		s.config.TotalSlots,
		s.config.PreSlots,
		s.config.ExeSlots,
		s.config.PostSlots,
		nowait,
		s.config.RemainTime,
		s.config.UseLocalCPUPercent,
		disablefilelock,
		autoResourceMgr,
		s.config.ResIdleSecsForFree,
		sendcork,
	))
}

func (s *sdk) register(config dcSDK.ControllerRegisterConfig) (dcSDK.ControllerWorkSDK, error) {
	var data []byte
	_ = codec.EncJSON(&WorkRegisterParam{
		BatchMode:        config.BatchMode,
		ServerHost:       config.ServerHost,
		SpecificHostList: config.SpecificHostList,
		NeedApply:        config.NeedApply,
		Apply:            config.Apply,
	}, &data)

	tmp, _, err := s.request("POST", registerURI, data, config.BatchMode)
	if err != nil {
		retry := 0
		for ; ; time.Sleep(100 * time.Millisecond) {
			tmp, _, err = s.request("POST", registerURI, data, config.BatchMode)
			if err == nil {
				break
			}
			retry++
			if retry >= 10 {
				return nil, err
			}
		}
	}

	var resp WorkRegisterResp
	if err = codec.DecJSON(tmp, &resp); err != nil {
		return nil, err
	}

	// since register successfully, run heartbeat until it is unregistered
	ctx, cancel := context.WithCancel(context.Background())

	work := &workSDK{
		sdk:             s,
		batchMode:       config.BatchMode,
		batchLeader:     resp.BatchLeader,
		id:              resp.WorkID,
		heartbeatCtx:    ctx,
		heartbeatCancel: cancel,
	}
	go work.heartbeat()

	s.worksLock.Lock()
	s.works[work.id] = work
	s.worksLock.Unlock()
	return work, nil
}

func (s *sdk) getWork(workID string) dcSDK.ControllerWorkSDK {
	s.worksLock.Lock()
	defer s.worksLock.Unlock()

	if _, ok := s.works[workID]; !ok {
		work := &workSDK{
			sdk: s,
			id:  workID,
		}
		s.works[workID] = work
	}

	return s.works[workID]
}

func (s *sdk) setConfig(config *dcSDK.CommonControllerConfig) error {
	var data []byte
	_ = codec.EncJSON(&CommonConfigParam{
		Configkey: config.Configkey,
		WorkerKey: WorkerKeyConfigParam{
			BatchMode: config.WorkerKey.BatchMode,
			ProjectID: config.WorkerKey.ProjectID,
			Scene:     config.WorkerKey.Scene,
		},
		Data: config.Data,
	}, &data)

	_, _, err := s.request("POST", commonConfigURI, data, false)
	if err != nil {
		retry := 0
		for ; ; time.Sleep(100 * time.Millisecond) {
			_, _, err = s.request("POST", commonConfigURI, data, false)
			if err == nil {
				break
			}
			retry++
			if retry >= 10 {
				return err
			}
		}
	}

	return nil
}

func (s *sdk) request(method, uri string, data []byte, wait bool) ([]byte, bool, error) {
	resp, ok, err := s.requestEx(method, uri, data, wait)
	if err != nil {
		return nil, ok, err
	}

	if resp.Code != api.ServerErrOK.Int() {
		return nil, true, fmt.Errorf(resp.Message)
	}

	var by []byte
	if err = codec.EncJSON(resp.Data, &by); err != nil {
		return nil, true, fmt.Errorf("encode apiResp.Data error %s", err.Error())
	}

	return by, true, nil
}

func (s *sdk) requestRaw(method, uri string, data []byte, wait bool) (resp *httpclient.HttpResponse, err error) {
	uri = fmt.Sprintf("%s/%s", s.config.Address(), uri)

	client := s.client
	if wait {
		client = s.waitClient
	}

	switch method {
	case "GET":
		resp, err = client.Get(uri, nil, data)

	case "POST":
		resp, err = client.Post(uri, nil, data)

	case "DELETE":
		resp, err = client.Delete(uri, nil, data)

	case "PUT":
		resp, err = client.Put(uri, nil, data)

	default:
		err = fmt.Errorf("uri %s method %s is invalid", uri, method)
	}

	if err != nil {
		return nil, err
	}

	if resp.StatusCode != http.StatusOK {
		return nil, fmt.Errorf("%s", string(resp.Reply))
	}

	return
}

func (s *sdk) requestEx(method, uri string, data []byte, wait bool) (*commonHTTP.APIResponse, bool, error) {
	resp, err := s.requestRaw(method, uri, data, wait)
	if err != nil {
		return nil, false, err
	}

	var apiResp *commonHTTP.APIResponse
	if err = codec.DecJSON(resp.Reply, &apiResp); err != nil {
		return nil, true, fmt.Errorf("decode request %s response %s error %s",
			uri, string(resp.Reply), err.Error())
	}

	return apiResp, true, nil
}

type workSDK struct {
	sdk *sdk

	id string

	batchMode   bool
	batchLeader bool

	heartbeatCtx    context.Context
	heartbeatCancel context.CancelFunc
}

// ID return the workID
func (ws *workSDK) ID() string {
	return ws.id
}

// Unregister do the work unregister to controller
func (ws *workSDK) Unregister(config dcSDK.ControllerUnregisterConfig) error {
	return ws.unregister(config)
}

// LockLocalSlot lock a slot with provided usage to controller
func (ws *workSDK) LockLocalSlot(usage dcSDK.JobUsage) error {
	return ws.lockLocalSlot(usage)
}

// UnLockLocalSlot unlock a slot with provided usage to controller
func (ws *workSDK) UnLockLocalSlot(usage dcSDK.JobUsage) error {
	return ws.unlockLocalSlot(usage)
}

// Start do the work start to controller
func (ws *workSDK) Start() error {
	return ws.start()
}

// End do the work end to controller
func (ws *workSDK) End() error {
	return ws.end()
}

// Status return the work status detail from controller
func (ws *workSDK) Status() (*dcSDK.WorkStatusDetail, error) {
	return ws.status()
}

// SetSettings update the work settings to controller
func (ws *workSDK) SetSettings(settings *dcSDK.ControllerWorkSettings) error {
	return ws.setSettings(settings)
}

// GetSettings get the work settings from controller
func (ws *workSDK) GetSettings() (*dcSDK.ControllerWorkSettings, error) {
	return ws.getSettings()
}

// UpdateJobStats update the single job stats to controller
func (ws *workSDK) UpdateJobStats(stats *dcSDK.ControllerJobStats) error {
	var data []byte
	_ = codec.EncJSON(&JobStatsParam{
		ControllerJobStats: *stats,
	}, &data)

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(jobStatsURI, ws.id), data, false)
	if err != nil {
		return err
	}

	return nil
}

// RecordWorkStats update the work stats to controller
func (ws *workSDK) RecordWorkStats(stats *dcSDK.ControllerWorkStats) error {
	return ws.recordWorkStats(stats)
}

// IsBatchLeader check if I am the leader(who apply the resource) in batch mode
func (ws *workSDK) IsBatchLeader() bool {
	return ws.batchLeader
}

// Job return the work job SDK
func (ws *workSDK) Job(stats *dcSDK.ControllerJobStats) dcSDK.WorkJob {
	if stats == nil {
		stats = &dcSDK.ControllerJobStats{}
	}

	return &workJob{
		sdk:   ws,
		stats: stats,
	}
}

// func (ws *workSDK) SetToolChain(key string, toolchain *dcSDK.OneToolChain) error {
// 	return ws.setToolChain(key, toolchain)
// }

// func (ws *workSDK) setToolChain(key string, toolchain *dcSDK.OneToolChain) error {
// 	if ws.batchMode && !ws.batchLeader {
// 		return nil
// 	}

// 	var data []byte
// 	_ = codec.EncJSON(&ToolChainParam{
// 		ToolKey:                key,
// 		ToolName:               toolchain.ToolName,
// 		ToolLocalFullPath:      toolchain.ToolLocalFullPath,
// 		ToolRemoteRelativePath: toolchain.ToolRemoteRelativePath,
// 		Files:                  toolchain.Files,
// 	}, &data)

// 	_, _, err := ws.sdk.request("POST", fmt.Sprintf(toolchainURI, ws.id), data, false)
// 	if err != nil {
// 		return err
// 	}

// 	return nil
// }

func (ws *workSDK) unregister(config dcSDK.ControllerUnregisterConfig) error {
	var data []byte
	_ = codec.EncJSON(&WorkUnregisterParam{
		Release: config.Release,
		Force:   config.Force,
	}, &data)

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(unregisterURI, ws.id), data, false)
	if err != nil {
		return err
	}

	if ws.heartbeatCancel != nil {
		ws.heartbeatCancel()
	}

	return nil
}

func (ws *workSDK) heartbeat() {
	ticker := time.NewTicker(dcSDK.WorkHeartbeatTick)
	defer ticker.Stop()

	for {
		select {
		case <-ws.heartbeatCtx.Done():
			return
		case <-ticker.C:
			go func() {
				_, _, _ = ws.sdk.request("POST", fmt.Sprintf(heartbeatURI, ws.id), nil, false)
			}()
		}
	}
}

func (ws *workSDK) lockLocalSlot(usage dcSDK.JobUsage) error {
	var data []byte
	_ = codec.EncJSON(&LocalSlotsOccupyParam{
		Usage: usage,
	}, &data)

	r, _, err := ws.sdk.requestEx("POST", fmt.Sprintf(occupyLocalSlotsURI, ws.id), data, true)
	if err != nil {
		return err
	}

	if r.Code != api.ServerErrOK.Int() {
		return fmt.Errorf(r.Message)
	}

	return nil
}

func (ws *workSDK) unlockLocalSlot(usage dcSDK.JobUsage) error {
	var data []byte
	_ = codec.EncJSON(&LocalSlotsFreeParam{
		Usage: usage,
	}, &data)

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(freeLocalSlotsURI, ws.id), data, false)
	if err != nil {
		return err
	}

	return nil
}

func (ws *workSDK) start() error {
	_, _, err := ws.sdk.request("POST", fmt.Sprintf(startURI, ws.id), nil, false)
	if err != nil {
		return err
	}

	return nil
}

func (ws *workSDK) end() error {
	if ws.batchMode {
		return nil
	}

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(endURI, ws.id), nil, false)
	if err != nil {
		return err
	}

	return nil
}

func (ws *workSDK) status() (*dcSDK.WorkStatusDetail, error) {
	tmp, _, err := ws.sdk.request("GET", fmt.Sprintf(statusURI, ws.id), nil, false)
	if err != nil {
		return nil, err
	}

	var resp WorkStatusResp
	if err = codec.DecJSON(tmp, &resp); err != nil {
		return nil, err
	}

	return resp.Status, nil
}

func (ws *workSDK) setSettings(settings *dcSDK.ControllerWorkSettings) error {
	var data []byte
	_ = codec.EncJSON(&WorkSettingsParam{
		TaskID:          settings.TaskID,
		ProjectID:       settings.ProjectID,
		Scene:           settings.Scene,
		UsageLimit:      settings.UsageLimit,
		LocalTotalLimit: settings.LocalTotalLimit,
		Preload:         settings.Preload,
		FilterRules:     settings.FilterRules,
		Degraded:        settings.Degraded,
		GlobalSlots:     settings.GlobalSlots,
	}, &data)

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(settingsURI, ws.id), data, false)
	if err != nil {
		return err
	}

	return nil
}

func (ws *workSDK) getSettings() (*dcSDK.ControllerWorkSettings, error) {
	tmp, _, err := ws.sdk.request("GET", fmt.Sprintf(settingsURI, ws.id), nil, false)
	if err != nil {
		return nil, err
	}

	var resp WorkSettingsResp
	if err = codec.DecJSON(tmp, &resp); err != nil {
		return nil, err
	}

	return &dcSDK.ControllerWorkSettings{
		TaskID:      resp.TaskID,
		ProjectID:   resp.ProjectID,
		Scene:       resp.Scene,
		FilterRules: resp.FilterRules,
	}, nil
}

func (ws *workSDK) recordWorkStats(stats *dcSDK.ControllerWorkStats) error {
	var data []byte
	_ = codec.EncJSON(&WorkStatsParam{
		Success: stats.Success,
	}, &data)

	_, _, err := ws.sdk.request("POST", fmt.Sprintf(workStatsURI, ws.id), data, false)
	if err != nil {
		return err
	}

	return nil
}

type workJob struct {
	sdk *workSDK

	stats *dcSDK.ControllerJobStats
}

// ExecuteRemoteTask do the task in remote directly
func (wj *workJob) ExecuteRemoteTask(req *dcSDK.BKDistCommand) (*dcSDK.BKDistResult, error) {
	var data []byte
	_ = codec.EncJSON(&RemoteTaskExecuteParam{
		Pid:   os.Getpid(),
		Req:   req,
		Stats: wj.stats,
	}, &data)

	tmp, _, err := wj.sdk.sdk.request("POST", fmt.Sprintf(remoteExecURI, wj.sdk.id), data, true)
	if err != nil {
		return nil, err
	}

	var resp RemoteTaskExecuteResp
	if err = codec.DecJSON(tmp, &resp); err != nil {
		return nil, err
	}

	return resp.Result, nil
}

// ExecuteLocalTask do the task in local controller
func (wj *workJob) ExecuteLocalTask(commands []string, workdir string) (int, string, *dcSDK.LocalTaskResult, error) {
	var data []byte

	dir := ""
	if workdir != "" {
		dir = workdir
	} else {
		dir, _ = os.Getwd()
		// if realDir, err := os.Readlink(dir); err == nil {
		if realDir, err := filepath.EvalSymlinks(dir); err == nil {
			dir = realDir
		}
	}

	if !filepath.IsAbs(dir) {
		dir, _ = filepath.Abs(dir)
	}

	u, _ := user.Current()
	if u == nil {
		u = &user.User{}
	}
	_ = codec.EncJSON(&LocalTaskExecuteParam{
		Pid:          os.Getpid(),
		Dir:          dir,
		Commands:     commands,
		Environments: os.Environ(),
		Stats:        wj.stats,
		User:         *u,
	}, &data)

	servercode := int(api.ServerErrOK)
	servermessage := ""
	resp, err := wj.sdk.sdk.requestRaw("POST", fmt.Sprintf(localExecURI, wj.sdk.id), data, true)
	if err != nil {
		return servercode, servermessage, nil, err
	}

	r := &LocalTaskExecuteResp{}
	httpcode, httpmessage, err := r.Read(resp.Reply)
	if err != nil {
		return httpcode, httpmessage, nil, err
	}

	return httpcode, httpmessage, &dcSDK.LocalTaskResult{
		ExitCode: r.Result.ExitCode,
		Stdout:   r.Result.Stdout,
		Stderr:   r.Result.Stderr,
		Message:  r.Result.Message,
	}, nil
}

// SendRemoteFile2All send files to all remote worker
func (wj *workJob) SendRemoteFile2All(req []dcSDK.FileDesc) error {
	var data []byte
	dir, _ := os.Getwd()
	_ = codec.EncJSON(&RemoteTaskSendFileParam{
		Pid:   os.Getpid(),
		Dir:   dir,
		Req:   req,
		Stats: wj.stats,
	}, &data)

	_, _, err := wj.sdk.sdk.request("POST", fmt.Sprintf(remoteSendURI, wj.sdk.id), data, true)
	if err != nil {
		return err
	}

	return nil
}

func controllerTarget(_ string) string {
	target := dcSDK.ControllerBinary
	if runtime.GOOS == "windows" {
		target = dcSDK.ControllerBinary + ".exe"
	}

	return target
}
