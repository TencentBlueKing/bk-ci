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
	"io/ioutil"
	"os"
	"strconv"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/controller/pkg/types"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"

	"github.com/emicklei/go-restful"
)

func available(_ *restful.Request, resp *restful.Response) {
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: &AvailableResp{Pid: os.Getpid()}})
}

func recordMessage(req *restful.Request, resp *restful.Response) {
	var data Message
	if err := codec.DecJSONReader(req.Request.Body, &data); err != nil {
		blog.Errorf("api: recordMessage decode data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}
	data.WorkID = req.PathParameter(pathParamWorkID)

	f := blog.Infof
	switch data.Level {
	case MessageInfo:
		f = blog.Infof
	case MessageWarn:
		f = blog.Warnf
	case MessageError:
		f = blog.Errorf
	}
	f("ExecutorLog | %s | %d | %s", data.WorkID, data.Pid, data.Message)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getWorkList(_ *restful.Request, resp *restful.Response) {
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: defaultManager.GetWorkDetailList()})
}

func getWorkDetail(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	jobIndex, _ := strconv.Atoi(req.QueryParameter(queryParamJobIndex))
	jobLeaveEndTime, _ := strconv.Atoi(req.QueryParameter(queryParamJobLeastLeaveTime))
	detail, err := defaultManager.GetWorkDetail(workID, jobIndex)
	if err != nil {
		blog.Errorf("api: getWorkDetail get work(%s) detail failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkDetailFailed, Message: err.Error()})
		return
	}

	var jobs []*dcSDK.ControllerJobStats
	for _, job := range detail.Jobs {
		if leaveTime := job.LeaveTime.UnixNano(); leaveTime > 0 && leaveTime < int64(jobLeaveEndTime) {
			continue
		}
		jobs = append(jobs, job)
	}
	detail.Jobs = jobs

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: detail})
}

func heartbeatWork(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: heartbeatWork get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}

	blog.Debugf("api: heartbeatWork got request to update heartbeat for work: %s", workID)

	if err := defaultManager.Heartbeat(workID); err != nil {
		blog.Errorf("api: heartbeatWork update heartbeat for work(%s) failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp,
			ErrCode: api.ServerErrUpdateWorkHeartbeatFailed, Message: err.Error()})
		return
	}

	blog.Debugf("api: heartbeatWork success to update heartbeat for work: %s", workID)
	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func registerWork(req *restful.Request, resp *restful.Response) {
	blog.Infof("api: registerWork got request to register work")

	config, err := getWorkRegisterConfig(req)
	if err != nil {
		blog.Errorf("api: registerWork get work register config failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	workInfo, leader, err := defaultManager.RegisterWork(config)
	if err != nil {
		blog.Errorf("api: registerWork register work failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrRegisterWorkFailed, Message: err.Error()})
		return
	}

	blog.Infof("api: registerWork success to register work: %s, batchLeader: %t",
		workInfo.WorkID(), leader)

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: &WorkRegisterResp{
		WorkID:      workInfo.WorkID(),
		BatchLeader: leader,
	}})
	return
}

func unregisterWork(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: unregisterWork get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: unregisterWork try to unregister work: %s", workID)

	config, err := getWorkUnregisterConfig(req)
	if err != nil {
		blog.Errorf("api: unregisterWork get work unregister config failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err := defaultManager.UnregisterWork(workID, config); err != nil {
		blog.Errorf("api: unregisterWork unregister work(%s) failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrUnregisterWorkFailed, Message: err.Error()})
		return
	}

	blog.Infof("api: unregisterWork success to unregister work: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func occupyLocalSlots(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: occupyLocalSlots get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: occupyLocalSlots try to occupy local slots: %s", workID)

	config, err := getLocalSlotsOccupyConfig(req)
	if err != nil {
		blog.Errorf("api: occupyLocalSlots get local slots occupied config failed, work: %s, err: %v",
			workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	err = defaultManager.LockLocalSlots(workID, config.Usage, config.Weight)
	if err != nil {
		blog.Errorf("api: occupyLocalSlots occupy local slots(%s) failed, work: %s, err: %v",
			config.Usage, workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrOccupyLocalSlotsFailed,
			Message: err.Error()})
		return
	}

	blog.Debugf("api: occupyLocalSlots success to occupy local slots(%s): work: %s", config.Usage, workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func freeLocalSlots(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: freeLocalSlots get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: freeLocalSlots try to free local slots: %s", workID)

	config, err := getLocalSlotsFreeConfig(req)
	if err != nil {
		blog.Errorf("api: freeLocalSlots get local slots free config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.UnlockLocalSlots(workID, config.Usage, config.Weight); err != nil {
		blog.Errorf("api: freeLocalSlots free local slots(%s) failed, work: %s, err: %v",
			config.Usage, workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrFreeLocalSlotsFailed, Message: err.Error()})
		return
	}

	blog.Debugf("api: freeLocalSlots success to free local slots(%s): work: %s", config.Usage, workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func setWorkSettings(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: setWorkSettings get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: setWorkSettings try to set settings: %s", workID)

	config, err := getWorkSettingsConfig(req)
	if err != nil {
		blog.Errorf("api: setWorkSettings get work settings config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.SetWorkSettings(workID, config); err != nil {
		blog.Errorf("api: setWorkSettings set work settings failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrSetWorkSettings, Message: err.Error()})
		return
	}

	blog.Infof("api: setWorkSettings success to set settings: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getWorkSettings(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: getWorkSettings get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: getWorkSettings try to get settings: %s", workID)

	settings, err := defaultManager.GetWorkSettings(workID)
	if err != nil {
		blog.Errorf("api: getWorkSettings get work settings failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkSettings, Message: err.Error()})
		return
	}

	blog.Debugf("api: getWorkSettings success to get settings: %s: %+v", workID, settings)

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: &WorkSettingsResp{
		TaskID:          settings.TaskID,
		ProjectID:       settings.ProjectID,
		Scene:           settings.Scene,
		UsageLimit:      settings.UsageLimit,
		LocalTotalLimit: settings.LocalTotalLimit,
		Preload:         settings.Preload,
		FilterRules:     settings.FilterRules,
	}})
}

func setCommonConfig(req *restful.Request, resp *restful.Response) {
	config, err := getCommonConfig(req)
	if err != nil {
		blog.Errorf("api: setCommonConfig get common config failed, err: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.SetCommonConfig(config); err != nil {
		blog.Errorf("api: setCommonConfig set common config failed, err: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrSetWorkSettings, Message: err.Error()})
		return
	}

	// blog.Infof("api: setCommonConfig success to set common %+v", config)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func startWork(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: startWork get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: startWork try to start work: %s", workID)

	if err := defaultManager.StartWork(workID); err != nil {
		blog.Errorf("api: startWork start work(%s) failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrStartWorkFailed, Message: err.Error()})
		return
	}

	blog.Infof("api: startWork success to start work: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func endWork(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: endWork get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: endWork try to end work: %s", workID)

	if err := defaultManager.EndWork(workID); err != nil {
		blog.Errorf("api: endWork end work(%s) failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrEndWorkFailed, Message: err.Error()})
		return
	}

	blog.Infof("api: endWork success to end work: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getWorkStatus(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: getWorkStatus get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: getWorkStatus try to get work status: %s", workID)

	status, err := defaultManager.GetWorkStatus(workID)
	if err != nil {
		blog.Errorf("api: getWorkStatus get work(%s) status failed: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkStatus, Message: err.Error()})
		return
	}

	blog.Infof("api: getWorkStatus success to get work(%s) status: %s", workID, status.Status.String())

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: &WorkStatusResp{Status: status}})
}

func updateJobStats(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: updateJobStats get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: updateJobStats try to record job stats: %s", workID)

	config, err := getJobStatsConfig(req)
	if err != nil {
		blog.Errorf("api: updateJobStats get job stats config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.UpdateJobStats(workID, config); err != nil {
		blog.Errorf("api: updateJobStats record job stats failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrUpdateJobStats, Message: err.Error()})
		return
	}

	blog.Infof("api: updateJobStats success to record job stats: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func recordWorkStats(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: recordWorkStats get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: recordWorkStats try to record work stats: %s", workID)

	config, err := getWorkStatsConfig(req)
	if err != nil {
		blog.Errorf("api: recordWorkStats get work stats config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.UpdateWorkStats(workID, config); err != nil {
		blog.Errorf("api: recordWorkStats record work stats failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrRecordWorkStats, Message: err.Error()})
		return
	}

	blog.Infof("api: recordWorkStats success to record work stats: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func executeRemoteTask(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: executeRemoteTask get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: executeRemoteTask try to execute remote task: %s", workID)

	config, err := getRemoteTaskExecuteRequest(req)
	if err != nil {
		blog.Errorf("api: executeRemoteTask get execute remote task config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	result, err := defaultManager.ExecuteRemoteTask(workID, config)
	if err != nil {
		blog.Errorf("api: executeRemoteTask execute remote task failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrExecuteRemoteTaskFailed,
			Message: err.Error()})
		return
	}

	blog.Infof("api: executeRemoteTask success to execute remote task: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: RemoteTaskExecuteResp{Result: result.Result}})
}

func sendRemoteFile(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	if workID == "" {
		blog.Errorf("api: sendRemoteFile get work_id from path empty")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Debugf("api: sendRemoteFile try to send remote file: %s", workID)

	config, err := getRemoteTaskSendFileRequest(req)
	if err != nil {
		blog.Errorf("api: sendRemoteFile get send remote file config failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.SendRemoteFile(workID, config); err != nil {
		blog.Errorf("api: sendRemoteFile send remote file failed, work: %s, err: %v", workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrSendRemoteFileFailed, Message: err.Error()})
		return
	}

	blog.Infof("api: sendRemoteFile success to send remote file: %s", workID)

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func executeLocalTask(req *restful.Request, resp *restful.Response) {
	workID := req.PathParameter(pathParamWorkID)
	r := &LocalTaskExecuteResp{}

	if workID == "" {
		blog.Errorf("api: executeLocalTask get work_id from path empty")
		r.Write2Resp(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "work_id empty"})
		return
	}
	blog.Infof("api: executeLocalTask try to execute local task: %s", workID)

	config, err := getLocalTaskExecuteRequest(req)
	if err != nil {
		blog.Errorf("api: executeLocalTask get execute local task config failed, work: %s, err: %v", workID, err)
		r.Write2Resp(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	result, err := defaultManager.ExecuteLocalTask(workID, config)
	if err != nil {
		// blog.Errorf("api: executeLocalTask execute local task failed, work: %s, err: %v", workID, err)
		errcode := api.ServerErrExecuteLocalTaskFailed
		message := err.Error()
		if err == types.ErrWorkNoFound {
			errcode = api.ServerErrWorkNotFound
			newworkid, _ := defaultManager.GetFirstWorkID()
			workerchanged := WorkerChanged{
				OldWorkID: workID,
				NewWorkID: newworkid,
			}
			var data []byte
			_ = codec.EncJSON(&workerchanged, &data)
			message = string(data)
		}
		blog.Errorf("api: executeLocalTask execute local task failed, work: %s, err: %v, return code:%d message: %s",
			workID, err, errcode, message)
		r.Write2Resp(&api.RestResponse{Resp: resp, ErrCode: errcode, Message: message})
		return
	}

	blog.Infof("api: executeLocalTask success to execute local task: %s", workID)
	r.Result = result.Result
	r.Write2Resp(&api.RestResponse{Resp: resp})
}

func getWorkRegisterConfig(req *restful.Request) (*types.WorkRegisterConfig, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get work register param from body failed: %v", err)
		return nil, err
	}

	blog.Infof("api: get work register param from body: %s", string(body))
	var param WorkRegisterParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get work register param decode failed: %v", err)
		return nil, err
	}

	config := &types.WorkRegisterConfig{
		BatchMode:        param.BatchMode,
		ServerHost:       param.ServerHost,
		SpecificHostList: param.SpecificHostList,
		NeedApply:        param.NeedApply,
		Apply:            param.Apply,
	}
	blog.Info("api: get work register config: %+v", config)
	return config, nil
}

func getWorkUnregisterConfig(req *restful.Request) (*types.WorkUnregisterConfig, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get work unregister param from body failed: %v", err)
		return nil, err
	}

	blog.Infof("api: get work unregister param from body: %s", string(body))
	var param WorkUnregisterParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get work unregister param decode failed: %v", err)
		return nil, err
	}

	config := &types.WorkUnregisterConfig{
		Force:   param.Force,
		Release: param.Release,
	}
	blog.Info("api: get work unregister config: %+v", config)
	return config, nil
}

func getLocalSlotsOccupyConfig(req *restful.Request) (*types.LocalSlotsOccupyConfig, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get local slots occupied param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get local slots occupied param from body: %s", string(body))
	var param LocalSlotsOccupyParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get local slots occupied param decode failed: %v", err)
		return nil, err
	}

	config := &types.LocalSlotsOccupyConfig{
		Usage:  param.Usage,
		Weight: param.Weight,
	}
	blog.Debugf("api: get local slots occupied config: %+v", config)
	return config, nil
}

func getLocalSlotsFreeConfig(req *restful.Request) (*types.LocalSlotsFreeConfig, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get local slots free param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get local slots free param from body: %s", string(body))
	var param LocalSlotsFreeParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get local slots free param decode failed: %v", err)
		return nil, err
	}

	config := &types.LocalSlotsFreeConfig{
		Usage: param.Usage,
	}
	blog.Debugf("api: get local slots free config: %+v", config)
	return config, nil
}

func getWorkSettingsConfig(req *restful.Request) (*types.WorkSettings, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get work settings param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get work settings param from body: %s", string(body))
	var param WorkSettingsParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get work settings param decode failed: %v", err)
		return nil, err
	}

	config := &types.WorkSettings{
		TaskID:          param.TaskID,
		ProjectID:       param.ProjectID,
		Scene:           param.Scene,
		UsageLimit:      param.UsageLimit,
		LocalTotalLimit: param.LocalTotalLimit,
		Preload:         param.Preload,
		FilterRules:     param.FilterRules,
		Degraded:        param.Degraded,
		GlobalSlots:     param.GlobalSlots,
	}
	blog.Debugf("api: work settings config: %+v", config)
	return config, nil
}

func getCommonConfig(req *restful.Request) (*types.CommonConfig, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get common config param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get common config param from body: %s", string(body))
	var param CommonConfigParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get common config param decode failed: %v", err)
		return nil, err
	}

	config := &types.CommonConfig{
		Configkey: param.Configkey,
		WorkerKey: types.WorkerKeyConfig{
			BatchMode: param.WorkerKey.BatchMode,
			ProjectID: param.WorkerKey.ProjectID,
			Scene:     param.WorkerKey.Scene,
		},
		Data: param.Data,
	}
	blog.Debugf("api: common config: %+v", config)
	return config, nil
}

func getJobStatsConfig(req *restful.Request) (*dcSDK.ControllerJobStats, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get job stats param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get job stats param from body: %s", string(body))
	var param JobStatsParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get job stats param decode failed: %v", err)
		return nil, err
	}

	config := &dcSDK.ControllerJobStats{
		ID:                   param.ID,
		Pid:                  param.Pid,
		WorkID:               param.WorkID,
		TaskID:               param.TaskID,
		BoosterType:          param.BoosterType,
		RemoteWorker:         param.RemoteWorker,
		RemoteWorkTimeoutSec: param.RemoteWorkTimeoutSec,

		Success:           param.Success,
		PreWorkSuccess:    param.PreWorkSuccess,
		RemoteWorkSuccess: param.RemoteWorkSuccess,
		PostWorkSuccess:   param.PostWorkSuccess,
		FinalWorkSuccess:  param.FinalWorkSuccess,
		LocalWorkSuccess:  param.LocalWorkSuccess,
		RemoteWorkTimeout: param.RemoteWorkTimeout,
		RemoteWorkFatal:   param.RemoteWorkFatal,

		OriginArgs:         param.OriginArgs,
		RemoteErrorMessage: param.RemoteErrorMessage,

		EnterTime: param.EnterTime,
		LeaveTime: param.LeaveTime,

		PreWorkEnterTime:  param.PreWorkEnterTime,
		PreWorkLeaveTime:  param.PreWorkLeaveTime,
		PreWorkLockTime:   param.PreWorkLockTime,
		PreWorkUnlockTime: param.PreWorkUnlockTime,
		PreWorkStartTime:  param.PreWorkStartTime,
		PreWorkEndTime:    param.PreWorkEndTime,

		PostWorkEnterTime:  param.PostWorkEnterTime,
		PostWorkLeaveTime:  param.PostWorkLeaveTime,
		PostWorkLockTime:   param.PostWorkLockTime,
		PostWorkUnlockTime: param.PostWorkUnlockTime,
		PostWorkStartTime:  param.PostWorkStartTime,
		PostWorkEndTime:    param.PostWorkEndTime,

		FinalWorkStartTime: param.FinalWorkStartTime,
		FinalWorkEndTime:   param.FinalWorkEndTime,

		RemoteWorkEnterTime:           param.RemoteWorkEnterTime,
		RemoteWorkLeaveTime:           param.RemoteWorkLeaveTime,
		RemoteWorkLockTime:            param.RemoteWorkLockTime,
		RemoteWorkUnlockTime:          param.RemoteWorkUnlockTime,
		RemoteWorkStartTime:           param.RemoteWorkStartTime,
		RemoteWorkEndTime:             param.RemoteWorkEndTime,
		RemoteWorkPackStartTime:       param.RemoteWorkPackStartTime,
		RemoteWorkPackEndTime:         param.RemoteWorkPackEndTime,
		RemoteWorkSendStartTime:       param.RemoteWorkSendStartTime,
		RemoteWorkSendEndTime:         param.RemoteWorkSendEndTime,
		RemoteWorkPackCommonStartTime: param.RemoteWorkPackCommonStartTime,
		RemoteWorkPackCommonEndTime:   param.RemoteWorkPackCommonEndTime,
		RemoteWorkSendCommonStartTime: param.RemoteWorkSendCommonStartTime,
		RemoteWorkSendCommonEndTime:   param.RemoteWorkSendCommonEndTime,
		RemoteWorkProcessStartTime:    param.RemoteWorkProcessStartTime,
		RemoteWorkProcessEndTime:      param.RemoteWorkProcessEndTime,
		RemoteWorkReceiveStartTime:    param.RemoteWorkReceiveStartTime,
		RemoteWorkReceiveEndTime:      param.RemoteWorkReceiveEndTime,
		RemoteWorkUnpackStartTime:     param.RemoteWorkUnpackStartTime,
		RemoteWorkUnpackEndTime:       param.RemoteWorkUnpackEndTime,

		LocalWorkEnterTime:  param.LocalWorkEnterTime,
		LocalWorkLeaveTime:  param.LocalWorkLeaveTime,
		LocalWorkLockTime:   param.LocalWorkLockTime,
		LocalWorkUnlockTime: param.LocalWorkUnlockTime,
		LocalWorkStartTime:  param.LocalWorkStartTime,
		LocalWorkEndTime:    param.LocalWorkEndTime,
	}
	blog.Debugf("api: get job stats config: %+v", config)
	return config, nil
}

func getWorkStatsConfig(req *restful.Request) (*types.WorkStats, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get work stats param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get work stats param from body: %s", string(body))
	var param WorkStatsParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get work stats param decode failed: %v", err)
		return nil, err
	}

	config := &types.WorkStats{
		Success: param.Success,
	}

	blog.Debugf("api: get work stats config: %+v", config)
	return config, nil
}

func getRemoteTaskExecuteRequest(req *restful.Request) (*types.RemoteTaskExecuteRequest, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get remote task exec param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get remote task exec param from body: %s", string(body))
	var param RemoteTaskExecuteParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get remote task exec param decode failed: %v", err)
		return nil, err
	}

	config := &types.RemoteTaskExecuteRequest{
		Pid:   param.Pid,
		Req:   param.Req,
		Stats: param.Stats,
	}

	blog.Debugf("api: get remote task exec config: %+v", config)
	return config, nil
}

func getRemoteTaskSendFileRequest(req *restful.Request) (*types.RemoteTaskSendFileRequest, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get remote task send file param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get remote task send file param from body: %s", string(body))
	var param RemoteTaskSendFileParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get remote task send file param decode failed: %v", err)
		return nil, err
	}

	config := &types.RemoteTaskSendFileRequest{
		Pid:     param.Pid,
		Sandbox: &dcSyscall.Sandbox{Dir: param.Dir},
		Req:     param.Req,
		Stats:   param.Stats,
	}

	blog.Debugf("api: get remote task send file config: %+v", config)
	return config, nil
}

func getLocalTaskExecuteRequest(req *restful.Request) (*types.LocalTaskExecuteRequest, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("api: get local task exec param from body failed: %v", err)
		return nil, err
	}

	blog.Debugf("api: get local task exec param from body: %s", string(body))
	var param LocalTaskExecuteParam
	if err = codec.DecJSON(body, &param); err != nil {
		blog.Errorf("api: get local task exec param decode failed: %v", err)
		return nil, err
	}

	config := &types.LocalTaskExecuteRequest{
		Pid:          param.Pid,
		Dir:          param.Dir,
		User:         param.User,
		Commands:     param.Commands,
		Environments: param.Environments,
		Stats:        param.Stats,
	}

	if config.Stats == nil {
		config.Stats = &dcSDK.ControllerJobStats{}
	}

	blog.Debugf("api: get local task exec config: %+v", config)
	return config, nil
}

// GetManager return the singleton manager
func GetManager() types.Mgr {
	return defaultManager
}
