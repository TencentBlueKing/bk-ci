/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package client

import (
	"net"
	"runtime/debug"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// NewCommonRemoteWorker get a new remote worker SDK
func NewCommonRemoteWorker() dcSDK.RemoteWorker {
	return &RemoteWorker{}
}

// RemoteWorker 作为链接管理单元, 通过实例化不同的handler来提供参数隔离的服务,
// 但最终的连接池都是用的同一个
// TODO: 统一管理连接池
type RemoteWorker struct {
}

// Handler get a remote handler
func (rw *RemoteWorker) Handler(
	ioTimeout int,
	stats *dcSDK.ControllerJobStats,
	updateJobStatsFunc func(),
	sandbox *syscall.Sandbox) dcSDK.RemoteWorkerHandler {
	if stats == nil {
		stats = &dcSDK.ControllerJobStats{}
	}

	if updateJobStatsFunc == nil {
		updateJobStatsFunc = func() {}
	}

	if sandbox == nil {
		sandbox = &syscall.Sandbox{}
	}

	return &CommonRemoteHandler{
		parent:             rw,
		sandbox:            sandbox,
		recordStats:        stats,
		updateJobStatsFunc: updateJobStatsFunc,
		ioTimeout:          ioTimeout,
	}
}

// CommonRemoteHandler remote executor for bk-common
type CommonRemoteHandler struct {
	parent             *RemoteWorker
	sandbox            *syscall.Sandbox
	recordStats        *dcSDK.ControllerJobStats
	updateJobStatsFunc func()
	ioTimeout          int
}

// ExecuteSyncTime get the target server's current timestamp
func (r *CommonRemoteHandler) ExecuteSyncTime(server string) (int64, error) {
	client := NewTCPClient(r.ioTimeout)
	if err := client.Connect(server); err != nil {
		blog.Warnf("error: %v", err)
		return 0, err
	}
	defer func() {
		_ = client.Close()
	}()

	// compress and prepare request
	messages, err := encodeSynctimeReq()
	if err != nil {
		blog.Warnf("error: %v", err)
		return 0, err
	}

	// send request
	err = sendMessages(client, messages)
	if err != nil {
		blog.Warnf("error: %v", err)
		return 0, err
	}

	// receive result
	rsp, err := receiveSynctimeRsp(client)

	if err != nil {
		blog.Warnf("error: %v", err)
		return 0, err
	}

	blog.Debugf("remote task done, get remote time:%d", rsp.GetTimenanosecond())
	return rsp.GetTimenanosecond(), nil
}

// ExecuteTask do execution in remote and get back the result(and files)
func (r *CommonRemoteHandler) ExecuteTask(
	server *dcProtocol.Host,
	req *dcSDK.BKDistCommand) (*dcSDK.BKDistResult, error) {
	// record the exit status.
	defer func() {
		r.updateJobStatsFunc()
	}()
	blog.Debugf("execute remote task with server %s", server)
	r.recordStats.RemoteWorker = server.Server

	client := NewTCPClient(r.ioTimeout)
	if err := client.Connect(server.Server); err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}
	defer func() {
		_ = client.Close()
	}()

	blog.Debugf("protocol: execute dist task commands: %v", req.Commands)
	r.recordStats.RemoteWorkTimeoutSec = client.timeout

	var err error
	messages := req.Messages

	// if raw messages not specified, then generate message from commands
	if messages == nil {
		// compress and prepare request
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackStartTime)
		// record the pack starting status, packing should be waiting for a while.
		r.updateJobStatsFunc()
		messages, err = EncodeCommonDistTask(req)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackEndTime)
		if err != nil {
			blog.Warnf("error: %v", err)
			return nil, err
		}
	}

	// send request
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendStartTime)
	// record the send starting status, sending should be waiting for a while.
	r.updateJobStatsFunc()
	err = sendMessages(client, messages)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendEndTime)
	if err != nil {
		r.recordStats.RemoteWorkFatal = true
		r.checkIfIOTimeout(err)
		blog.Warnf("error: %v", err)
		return nil, err
	}

	// record the receive starting status, receiving should be waiting for a while.
	r.updateJobStatsFunc()
	// receive result
	data, err := receiveCommonDispatchRsp(client, r.sandbox)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkReceiveEndTime)
	r.recordStatsFromDispatchResp(data, server)

	if err != nil {
		r.recordStats.RemoteWorkFatal = true
		r.checkIfIOTimeout(err)
		blog.Warnf("error: %v", err)
		return nil, err
	}

	// get stat data from response here and decompress
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkUnpackStartTime)
	// record the decode starting status, decoding should be waiting for a while.
	r.updateJobStatsFunc()
	result, err := decodeCommonDispatchRsp(data)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkUnpackEndTime)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	blog.Debugf("remote task done *")
	r.recordStats.RemoteWorkSuccess = true
	return result, nil
}

// ExecuteTaskWithoutSaveFile same as ExecuteTask but do not write file to disk directly,
// the result file will be kept in memory and wait for custom process
func (r *CommonRemoteHandler) ExecuteTaskWithoutSaveFile(
	server *dcProtocol.Host,
	req *dcSDK.BKDistCommand) (*dcSDK.BKDistResult, error) {
	// record the exit status.
	defer func() {
		r.updateJobStatsFunc()
	}()
	blog.Debugf("execute remote task with server %s and do not save file", server)
	r.recordStats.RemoteWorker = server.Server

	client := NewTCPClient(r.ioTimeout)
	if err := client.Connect(server.Server); err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}
	defer func() {
		_ = client.Close()
	}()

	blog.Debugf("protocol: execute dist task commands: %v", req.Commands)
	r.recordStats.RemoteWorkTimeoutSec = client.timeout

	var err error
	messages := req.Messages

	// if raw messages not specified, then generate message from commands
	if messages == nil {
		// compress and prepare request
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackStartTime)
		// record the pack starting status, packing should be waiting for a while.
		r.updateJobStatsFunc()
		messages, err = EncodeCommonDistTask(req)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackEndTime)
		if err != nil {
			blog.Warnf("error: %v", err)
			return nil, err
		}
	}

	// send request
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendStartTime)
	// record the send starting status, sending should be waiting for a while.
	r.updateJobStatsFunc()
	err = sendMessages(client, messages)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendEndTime)
	if err != nil {
		r.recordStats.RemoteWorkFatal = true
		blog.Warnf("error: %v", err)
		return nil, err
	}

	debug.FreeOSMemory() // free memory anyway

	// record the receive starting status, receiving should be waiting for a while.
	r.updateJobStatsFunc()
	// receive result
	data, err := receiveCommonDispatchRspWithoutSaveFile(client)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkReceiveEndTime)
	r.recordStatsFromDispatchResp(data, server)

	if err != nil {
		r.recordStats.RemoteWorkFatal = true
		r.checkIfIOTimeout(err)
		blog.Warnf("error: %v", err)
		return nil, err
	}

	debug.FreeOSMemory() // free memory anyway

	// get stat data from response here and decompress
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkUnpackStartTime)
	// record the decode starting status, decoding should be waiting for a while.
	r.updateJobStatsFunc()
	result, err := decodeCommonDispatchRsp(data)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkUnpackEndTime)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	blog.Debugf("remote task done *")
	r.recordStats.RemoteWorkSuccess = true
	return result, nil
}

func (r *CommonRemoteHandler) checkIfIOTimeout(remoteErr error) {
	if remoteErr == nil {
		return
	}

	if err, ok := remoteErr.(net.Error); ok && err.Timeout() {
		r.recordStats.RemoteWorkTimeout = true
	}
}

func (r *CommonRemoteHandler) recordStatsFromDispatchResp(
	resp *protocol.PBBodyDispatchTaskRsp,
	server *dcProtocol.Host) {
	if resp == nil {
		return
	}

	if len(resp.Results) == 0 {
		return
	}

	var delta int64 = 0
	if server != nil {
		delta = server.TimeDelta
		blog.Debugf("server(%s) delta time: %d", server.Server, server.TimeDelta)
	}

	result := resp.Results[0]
	for _, s := range result.Stats {
		if s.Key == nil || s.Time == nil {
			continue
		}

		switch *s.Key {
		case protocol.BKStatKeyStartTime:
			r.recordStats.RemoteWorkProcessStartTime = dcSDK.StatsTime(time.Unix(0, *s.Time-delta).Local())
		case protocol.BKStatKeyEndTime:
			r.recordStats.RemoteWorkProcessEndTime = dcSDK.StatsTime(time.Unix(0, *s.Time-delta).Local())
			r.recordStats.RemoteWorkReceiveStartTime = dcSDK.StatsTime(time.Unix(0, *s.Time-delta).Local())
		}
	}
}

// EncodeCommonDistTask encode request command info into protocol.Message
func EncodeCommonDistTask(req *dcSDK.BKDistCommand) ([]protocol.Message, error) {
	blog.Debugf("encodeBKCommonDistTask now")

	// ++ by tomtian for debug
	// debugRecordFileName(req)
	// --

	return encodeCommonDispatchReq(req)
}

// EncodeSendFileReq encode request files into protocol.Message
func EncodeSendFileReq(req *dcSDK.BKDistFileSender, sandbox *syscall.Sandbox) ([]protocol.Message, error) {
	blog.Debugf("encodeBKCommonDistFiles now")

	return encodeSendFileReq(req, sandbox)
}

// ExecuteSendFile send files to remote server
func (r *CommonRemoteHandler) ExecuteSendFile(
	server *dcProtocol.Host,
	req *dcSDK.BKDistFileSender,
	sandbox *syscall.Sandbox) (*dcSDK.BKSendFileResult, error) {
	// record the exit status.
	defer func() {
		r.updateJobStatsFunc()
	}()
	blog.Debugf("start send files to server %s", server)
	r.recordStats.RemoteWorker = server.Server

	if len(req.Files) == 0 {
		blog.Debugf("no files need sent")
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackCommonStartTime)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackCommonEndTime)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendCommonStartTime)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendCommonEndTime)
		return &dcSDK.BKSendFileResult{
			Results: []dcSDK.FileResult{{RetCode: 0}},
		}, nil
	}

	t := time.Now().Local()
	client := NewTCPClient(r.ioTimeout)
	if err := client.Connect(server.Server); err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}
	d := time.Now().Sub(t)
	if d > 200*time.Millisecond {
		blog.Debugf("TCP Connect to long to server(%s): %s", server.Server, d.String())
	}
	defer func() {
		_ = client.Close()
	}()

	blog.Debugf("success connect to server %s", server)
	var err error
	messages := req.Messages
	if messages == nil {
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackCommonStartTime)
		messages, err = encodeSendFileReq(req, sandbox)
		dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkPackCommonEndTime)
		if err != nil {
			blog.Warnf("error: %v", err)
			return nil, err
		}
	}

	debug.FreeOSMemory() // free memory anyway

	blog.Debugf("success pack-up to server %s", server)
	// send request
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendCommonStartTime)
	// record the send starting status, sending should be waiting for a while.
	r.updateJobStatsFunc()
	err = sendMessages(client, messages)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	debug.FreeOSMemory() // free memory anyway

	blog.Debugf("success sent to server %s", server)
	// receive result
	data, err := receiveSendFileRsp(client)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendCommonEndTime)

	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	result, err := decodeSendFileRsp(data)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	blog.Debugf("send file task done *")

	return result, nil
}

// ExecuteCheckCache check file cache in remote worker
func (r *CommonRemoteHandler) ExecuteCheckCache(
	server *dcProtocol.Host,
	req *dcSDK.BKDistFileSender,
	sandbox *syscall.Sandbox) ([]bool, error) {
	blog.Debugf("start check cache to server %s", server)

	// record the exit status.
	t := time.Now().Local()
	client := NewTCPClient(r.ioTimeout)
	if err := client.Connect(server.Server); err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}
	d := time.Now().Sub(t)
	if d > 200*time.Millisecond {
		blog.Debugf("TCP Connect to long to server(%s): %s", server.Server, d.String())
	}
	defer func() {
		_ = client.Close()
	}()

	messages, err := encodeCheckCacheReq(req, sandbox)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	err = sendMessages(client, messages)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	blog.Debugf("check cache success sent to server %s", server)

	// receive result
	data, err := receiveCheckCacheRsp(client)
	dcSDK.StatsTimeNow(&r.recordStats.RemoteWorkSendCommonEndTime)

	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	result, err := decodeCheckCacheRsp(data)
	if err != nil {
		blog.Warnf("error: %v", err)
		return nil, err
	}

	blog.Debugf("check cache task done *")

	return result, nil
}
