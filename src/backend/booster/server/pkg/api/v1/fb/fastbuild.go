/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package fb

import (
	"fmt"
	"io/ioutil"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/fastbuild"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/manager"

	"github.com/emicklei/go-restful"
)

const (
	queryTaskIDKey = "task_id"
)

var (
	defaultManager manager.Manager
)

// ApplyResource to request resource
func ApplyResource(req *restful.Request, resp *restful.Response) {
	serverSets, err := getServerSets(req)
	if err != nil {
		blog.Errorf("apply resource: get param failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	CacheEnabled := ""
	if serverSets.CCacheEnabled != nil {
		if *serverSets.CCacheEnabled {
			CacheEnabled = "true"
		} else {
			CacheEnabled = "false"
		}
	}
	var extra = fastbuild.TaskExtra{
		Params:       serverSets.Params,
		FullCmd:      fmt.Sprintf("%s %s", serverSets.Command, serverSets.Params),
		Env:          "",
		RunDir:       serverSets.RunDir,
		CommandType:  string(serverSets.CommandType),
		Command:      serverSets.Command,
		User:         serverSets.User,
		Path:         "",
		Version:      serverSets.GccVersion,
		CacheEnabled: CacheEnabled,
	}
	var extraData []byte
	err = codec.EncJSON(extra, &extraData)
	if err != nil {
		blog.Errorf("apply resource: encode extra param failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	tb, err := defaultManager.CreateTask(&manager.TaskCreateParam{
		ProjectID:     serverSets.ProjectId,
		BuildID:       serverSets.BuildId,
		ClientVersion: serverSets.ClientVersion,
		ClientIP:      serverSets.ClientIp,
		Message:       serverSets.Message,
		Extra:         string(extraData),
	})
	if err != nil {
		blog.Errorf("apply resource: create task failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrApplyResourceFailed, Message: err.Error()})
		return
	}

	info, err := getTaskInfo(tb.ID)
	if err != nil {
		blog.Errorf("apply resource: get task info(%s) failed, url(%s): %v",
			tb.ID, req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrRequestTaskInfoFailed, Message: err.Error(),
		})
		return
	}

	blog.Infof("success to apply resource for task(%s) of project(%s) in engine(%s)",
		tb.ID, tb.Client.ProjectID, tb.Client.EngineName.String())
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: info})
}

// QueryTaskInfo : query task info
func QueryTaskInfo(req *restful.Request, resp *restful.Response) {
	taskID := req.QueryParameter(queryTaskIDKey)
	if taskID == "" {
		blog.Errorf("query task info: query failed, url(%s): taskID not specified", req.Request.URL.String())
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "task_id no specific",
		})
		return
	}

	info, err := getTaskInfo(taskID)
	if err != nil {
		blog.Errorf("apply resource: get task info(%s) failed, url(%s): %v",
			taskID, req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrRequestTaskInfoFailed, Message: err.Error(),
		})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: info})
}

// ReleaseResource : release resource triggered by client
func ReleaseResource(req *restful.Request, resp *restful.Response) {
	clientInfo, err := getClientInfo(req)
	if err != nil {
		blog.Errorf("release resource, get client info failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	success := false
	switch clientInfo.Status {
	case commonTypes.ClientStatusSuccess:
		success = true
	case commonTypes.ClientStatusFailed:
		success = false
	default:
		err := fmt.Errorf("unknown client status: %s taskID(%s) url(%s)",
			clientInfo.Status, clientInfo.TaskID, req.Request.URL.String())
		blog.Errorf("%v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = defaultManager.ReleaseTask(&manager.TaskReleaseParam{
		TaskID:  clientInfo.TaskID,
		Message: clientInfo.Message,
		Success: success,
		// TODO: define an inner extra struct for this
		Extra: "",
	}); err != nil {
		blog.Errorf("release resource: release task(%s) failed, url(%s): %v",
			clientInfo.TaskID, req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrReleaseResourceFailed, Message: err.Error(),
		})
		return
	}

	blog.Infof("success to release resource for task(%s)", clientInfo.TaskID)
	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// SubTaskDone : sub tasks done, notified by fast build clients
func SubTaskDone(req *restful.Request, resp *restful.Response) {
	client, err := getClientInfo(req)
	if err != nil {
		blog.Errorf("release resource, get client info failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	// get MessageSubTaskDone
	subMsg, _ := client2SubTaskMessage(client)

	var subMsgData []byte
	err = codec.EncJSON(subMsg, &subMsgData)

	// format message data
	var msg = fastbuild.Message{
		Type: fastbuild.MessageTypeSubTaskDone,
		Data: subMsgData,
	}
	var msgdata []byte
	err = codec.EncJSON(msg, &msgdata)
	if err != nil {
		blog.Errorf("failed to encode message data for [%+v]", msg)
	} else {
		blog.Debugf("ready to SendTaskMessage for task[%s] with data[%s]",
			client.TaskID, string(msgdata))
		_, _ = defaultManager.SendTaskMessage(client.TaskID, msgdata)
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getTaskInfo(taskID string) (*commonTypes.DistccServerInfo, error) {
	tb, te, err := defaultManager.GetTask(taskID)
	if err != nil {
		blog.Errorf("get apply param: get task(%s) failed: %v", taskID, err)
		return nil, err
	}

	rank := 0
	if tb.Status.Status == engine.TaskStatusStaging {
		rank, err = defaultManager.GetTaskRank(taskID)
		if err != nil {
			blog.Errorf("get apply param: get task(%s) rank from engine(%s) queue(%s) failed: %v",
				taskID, tb.Client.EngineName.String(), tb.Client.QueueName, err)
			rank = 0
		}
	}

	message := ""
	var serverStatus commonTypes.ServerStatusType
	switch tb.Status.Status {
	case engine.TaskStatusStaging:
		message = fmt.Sprintf("There are %d tasks in queue before yours, please wait...", rank)
		serverStatus = commonTypes.ServerStatusStaging
	case engine.TaskStatusStarting:
		message = fmt.Sprintf("Servers are starting, the compiling will be started soon.")
		serverStatus = commonTypes.ServerStatusStarting
	case engine.TaskStatusRunning:
		message = tb.Status.Message
		serverStatus = commonTypes.ServerStatusRunning
	case engine.TaskStatusFailed:
		message = tb.Status.Message
		serverStatus = commonTypes.ServerStatusFailed
	case engine.TaskStatusFinish:
		message = tb.Status.Message
		serverStatus = commonTypes.ServerStatusFinish
	}

	// TODO: add Env (FbResultCompress)
	env := map[string]string{}
	CCacheEnabled := false
	worklist := strings.Join(te.WorkerList(), ";")
	customData := te.CustomData(nil).(map[string]string)
	if customData != nil {
		v, ok := customData[fastbuild.FBCompressResultEnvKey]
		if ok {
			env[fastbuild.FBCompressResultEnvKey] = v
		}

		v, ok = customData[fastbuild.FBCacheEnableKey]
		if ok {
			if v == "true" {
				CCacheEnabled = true
			}
		}

		env["FB_HOSTS"] = worklist
	}

	// TODO: complete info
	info := &commonTypes.DistccServerInfo{
		TaskID:        tb.ID,
		Status:        serverStatus,
		Message:       message,
		CCacheEnabled: CCacheEnabled,
		Envs:          env,
		DistccHosts:   worklist,
	}

	return info, nil
}

func getServerSets(req *restful.Request) (*commonTypes.DistccServerSets, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get server sets, get request body failed: %v", err)
		return nil, err
	}

	var serverSets commonTypes.DistccServerSets
	if err = codec.DecJSON(body, &serverSets); err != nil {
		blog.Errorf("get server sets failed: %v, body: %s", err, string(body))
		return nil, err
	}

	// get client IP
	var clientIP string
	arr := strings.Split(req.Request.Header.Get(api.HeaderRemote), ":")
	if len(arr) > 0 {
		remoteIP := arr[0]
		blog.Infof("get apply param: get remote ip [%s]", remoteIP)

		// use remoteIP for clientIP check, never trust the client one.
		clientIP = remoteIP
	}

	if serverSets.ClientIp != "" {
		serverSets.ClientIp += "|" + clientIP
	} else {
		serverSets.ClientIp = clientIP
	}

	blog.Infof("get server sets: %s", string(body))
	return &serverSets, nil
}

func getClientInfo(req *restful.Request) (*commonTypes.DistccClientInfo, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get client info, get request body failed: %v", err)
		return nil, err
	}

	var clientInfo commonTypes.DistccClientInfo
	if err = codec.DecJSON(body, &clientInfo); err != nil {
		blog.Errorf("get client info failed: %v, body: %s", err, string(body))
		return nil, err
	}

	blog.Debugf("get client info: %s", string(body))
	return &clientInfo, nil
}

// SubTaskDone : sub tasks done, notified by fast build clients
func client2SubTaskMessage(client *commonTypes.DistccClientInfo) (*fastbuild.MessageSubTaskDone, error) {
	compileResult := ""
	if v, ok := client.Extra[commonTypes.ClientExtraKeyCompileResult]; ok {
		compileResult = v
	}

	cmd := ""
	if v, ok := client.Extra[commonTypes.ClientExtraKeyCmd]; ok {
		cmd = v
	}

	runDir := ""
	if v, ok := client.Extra[commonTypes.ClientExtraKeyRunDir]; ok {
		runDir = v
	}

	var startTime int64
	if v, ok := client.Extra[commonTypes.ClientExtraKeyStartTime]; ok {
		intV, err := strconv.ParseInt(v, 10, 64)
		if err == nil {
			startTime = intV
		}
	}

	var endTime int64
	if v, ok := client.Extra[commonTypes.ClientExtraKeyEndTime]; ok {
		intV, err := strconv.ParseInt(v, 10, 64)
		if err == nil {
			endTime = intV
		}
	}

	subMsg := fastbuild.MessageSubTaskDone{
		TaskID:        client.TaskID,
		Params:        "",
		FullCmd:       cmd,
		Env:           "",
		RunDir:        runDir,
		CommandType:   "",
		Command:       cmd,
		User:          "",
		Status:        string(client.Status),
		StartTime:     startTime,
		EndTime:       endTime,
		CompileResult: compileResult,
		FbSummary: fastbuild.FbSummary{
			LibraryBuilt:      client.FbSummary.LibraryBuilt,
			LibraryCacheHit:   client.FbSummary.LibraryCacheHit,
			LibraryCPUTime:    client.FbSummary.LibraryCPUTime,
			ObjectBuilt:       client.FbSummary.ObjectBuilt,
			ObjectCacheHit:    client.FbSummary.ObjectCacheHit,
			ObjectCPUTime:     client.FbSummary.ObjectCPUTime,
			ExeBuilt:          client.FbSummary.ExeBuilt,
			ExeCacheHit:       client.FbSummary.ExeCacheHit,
			ExeCPUTime:        client.FbSummary.ExeCPUTime,
			CacheHits:         client.FbSummary.CacheHits,
			CacheMisses:       client.FbSummary.CacheMisses,
			CacheStores:       client.FbSummary.CacheStores,
			RealCompileTime:   client.FbSummary.RealCompileTime,
			LocalCompileTime:  client.FbSummary.LocalCompileTime,
			RemoteCompileTime: client.FbSummary.RemoteCompileTime,
		},
	}

	return &subMsg, nil
}

// InitStorage let outside be able to init the default manager. it should be called after the manager is created,
// and before the http-handler start working.
func InitStorage() (err error) {
	defaultManager = api.GetAPIResource().Manager

	return nil
}

func init() {
	api.RegisterInitFunc(InitStorage)
}
