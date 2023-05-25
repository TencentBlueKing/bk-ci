/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package dcc

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
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/distcc/client/pkg"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/manager"

	"github.com/emicklei/go-restful"
)

const (
	queryTaskIDKey        = "task_id"
	queryProjectIDKey     = "project_id"
	queryCCacheEnabledKey = "ccache_enabled"
)

var (
	defaultManager manager.Manager
)

// ApplyResource handle the http request for applying a new task.
// it return immediately after the task created into database with status staging.
func ApplyResource(req *restful.Request, resp *restful.Response) {
	serverSets, err := getServerSets(req)
	if err != nil {
		blog.Errorf("apply resource: getServerSets failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	extra := distcc.ExtraData{
		User:          serverSets.User,
		GccVersion:    serverSets.GccVersion,
		RunDir:        serverSets.RunDir,
		Params:        serverSets.Params,
		CCacheEnabled: serverSets.CCacheEnabled,
		Command:       serverSets.Command,
		CommandType:   distcc.CommandType(serverSets.CommandType),
		ExtraVars: distcc.ExtraVars{
			BazelRC: serverSets.ExtraVars.BazelRC,
			MaxJobs: serverSets.ExtraVars.MaxJobs,
		},
	}
	var extraData []byte
	_ = codec.EncJSON(extra, &extraData)

	tb, err := defaultManager.CreateTask(&manager.TaskCreateParam{
		ProjectID:     serverSets.ProjectId,
		BuildID:       serverSets.BuildId,
		ClientVersion: serverSets.ClientVersion,
		ClientIP:      serverSets.ClientIp,
		ClientCPU:     serverSets.ClientCPU,
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
		blog.Errorf("request compile resource, get task info failed, url(%s): %v", req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrRequestTaskInfoFailed, Message: err.Error(),
		})
		return
	}

	blog.Infof("success to apply resource for task(%s) of project(%s) in engine(%s)",
		tb.ID, tb.Client.ProjectID, tb.Client.EngineName.String())
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: info})
}

// QueryTaskInfo handle the http request for querying a existing task
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
		blog.Errorf("query task info: get task info(%s) failed, url(%s): %v",
			taskID, req.Request.URL.String(), err)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrRequestTaskInfoFailed, Message: err.Error(),
		})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: info})
}

// ReleaseResource handle the http request for releasing a existing task
// it return immediately after the task saved into database with status finish or failed.
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

	msg := distcc.Message{
		Type: distcc.MessageTypeRecordStats,
		MessageRecordStats: distcc.MessageRecordStats{
			Message: clientInfo.Message,
		},
	}
	if clientInfo.Ccache != nil {
		msg.MessageRecordStats.CCacheStats.CacheDir = clientInfo.Ccache.CacheDir
		msg.MessageRecordStats.CCacheStats.PrimaryConfig = clientInfo.Ccache.PrimaryConfig
		msg.MessageRecordStats.CCacheStats.SecondaryConfig = clientInfo.Ccache.SecondaryConfig
		msg.MessageRecordStats.CCacheStats.DirectHit = clientInfo.Ccache.DirectHit
		msg.MessageRecordStats.CCacheStats.PreprocessedHit = clientInfo.Ccache.PreprocessedHit
		msg.MessageRecordStats.CCacheStats.CacheMiss = clientInfo.Ccache.CacheMiss
		msg.MessageRecordStats.CCacheStats.CalledForLink = clientInfo.Ccache.CalledForLink
		msg.MessageRecordStats.CCacheStats.CalledForPreProcessing = clientInfo.Ccache.CalledForPreProcessing
		msg.MessageRecordStats.CCacheStats.UnsupportedSourceLanguage = clientInfo.Ccache.UnsupportedSourceLanguage
		msg.MessageRecordStats.CCacheStats.NoInputFile = clientInfo.Ccache.NoInputFile
		msg.MessageRecordStats.CCacheStats.FilesInCache = clientInfo.Ccache.FilesInCache
		msg.MessageRecordStats.CCacheStats.CacheSize = clientInfo.Ccache.CacheSize
		msg.MessageRecordStats.CCacheStats.MaxCacheSize = clientInfo.Ccache.MaxCacheSize
	}
	var extra []byte
	_ = codec.EncJSON(msg, &extra)

	if err = defaultManager.ReleaseTask(&manager.TaskReleaseParam{
		TaskID:  clientInfo.TaskID,
		Message: clientInfo.Message,
		Success: success,
		Extra:   string(extra),
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

// GetCmakeArgs handle the http request for getting the cmake args according to send project message to distcc engine.
func GetCmakeArgs(req *restful.Request, resp *restful.Response) {
	projectID := req.QueryParameter(queryProjectIDKey)
	if projectID == "" {
		blog.Errorf("get cmake args: get param failed, projectID no specific")
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "project_id no specific",
		})
		return
	}

	cacheEnabledV := req.QueryParameter(queryCCacheEnabledKey)
	if cacheEnabledV != "" && cacheEnabledV != "true" && cacheEnabledV != "false" {
		blog.Errorf("get cmake args: get param failed, ccache_enabled is invalid: %s", cacheEnabledV)
		api.ReturnRest(&api.RestResponse{
			Resp: resp, ErrCode: api.ServerErrInvalidParam, Message: "ccache_enabled is invalid",
		})
		return
	}

	var cacheEnabled *bool
	switch cacheEnabledV {
	case "true":
		tmp := true
		cacheEnabled = &tmp
	case "false":
		tmp := false
		cacheEnabled = &tmp
	}

	msg := distcc.Message{
		Type: distcc.MessageTypeGetCMakeArgs,
		MessageGetCMakeArgs: distcc.MessageGetCMakeArgs{
			CCacheEnabled: cacheEnabled,
		},
	}
	var extra []byte
	_ = codec.EncJSON(msg, &extra)

	data, err := defaultManager.SendProjectMessage(projectID, extra)
	if err != nil {
		blog.Errorf("get cmake args: send message project(%s) failed: %v", projectID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrSendMessageFailed, Message: err.Error()})
		return
	}

	var mResp distcc.MessageResponse
	if err = codec.DecJSON(data, &mResp); err != nil {
		blog.Errorf("get cmake args: send message project(%s), decode data(%s) failed: %v",
			projectID, string(data), err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrSendMessageFailed, Message: err.Error()})
		return
	}
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: &commonTypes.CMakeArgs{
		Args: mResp.CMakeArgs,
	}})
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

	// fill ClientIp with RemoteAddr
	arr := strings.Split(req.Request.Header.Get(api.HeaderRemote), ":")
	if len(arr) > 0 {
		remoteIP := arr[0]
		blog.Infof("get remote ip [%s]", remoteIP)

		if remoteIP != serverSets.ClientIp {
			blog.Warnf("project(%s) remote ip(%s) different from client ip(%s)",
				serverSets.ProjectId, remoteIP, serverSets.ClientIp)
		}

		// use remoteIP for clientIP check, never trust the client one.
		serverSets.ClientIp = remoteIP
	}

	blog.Infof("get server sets: %s", string(body))
	blog.Infof("get client status(project ID/client IP/user version: %s %s %s",
		serverSets.ProjectId, serverSets.ClientIp, serverSets.ClientVersion)
	return &serverSets, nil
}

func pruneGccVersion(gccVersion string) string {
	if len(gccVersion) <= 1 {
		return gccVersion
	}
	_, err := strconv.ParseInt(gccVersion[len(gccVersion)-1:], 10, 0)
	if err == nil {
		return gccVersion
	}
	idx := strings.LastIndex(gccVersion, "_")
	if idx <= 0 {
		return gccVersion
	}
	return gccVersion[0:idx]
}

func getTaskInfo(taskID string) (*commonTypes.DistccServerInfo, error) {
	tb, te, err := defaultManager.GetTask(taskID)
	if err != nil {
		blog.Errorf("get apply param: get task info ID(%s) failed: %v", taskID, err)
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

	data, ok := te.CustomData(nil).(distcc.CustomData)
	if !ok {
		err = fmt.Errorf("custom data type error")
		blog.Errorf("get apply param: get task(%s) custom data from engine(%s) failed: %v",
			taskID, tb.Client.EngineName.String(), err)
		return nil, err
	}

	message := tb.Status.Message
	if tb.Status.Status == engine.TaskStatusStaging {
		message += fmt.Sprintf(" %d tasks in queue before yours, please wait...", rank)
	}

	info := &commonTypes.DistccServerInfo{
		TaskID:        tb.ID,
		Status:        commonTypes.ServerStatusType(tb.Status.Status),
		Message:       message,
		GccVersion:    pruneGccVersion(data.GccVersion),
		Cmds:          data.Commands,
		Envs:          data.Environments,
		UnsetEnvs:     data.UnsetEnvironments,
		CCacheEnabled: data.CCacheEnable,
		QueueNumber:   rank,

		CCCompiler:  data.CCCompiler,
		CXXCompiler: data.CXXCompiler,
		JobServer:   uint(data.JobServer),
		DistccHosts: data.DistCCHosts,
	}

	info.GccVersion = pkg.TransformGccVersion(info.GccVersion)
	return info, nil
}

func getClientInfo(req *restful.Request) (*commonTypes.DistccClientInfo, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get client info, get request body failed: %v", err)
		return nil, err
	}

	var clientInfo commonTypes.DistccClientInfo
	if err = codec.DecJSON(body, &clientInfo); err != nil {
		blog.Errorf("get client info: decode failed: %v, body: %s", err, string(body))
		return nil, err
	}

	blog.Debugf("get client info: %s", string(body))
	return &clientInfo, nil
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
