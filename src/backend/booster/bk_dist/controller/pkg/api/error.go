/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package api

// ServerErrorCode implements the ErrorCode
type ServerErrCode int

const (
	ServerErrOK ServerErrCode = iota
	ServerErrInvalidParam
	ServerErrRedirectFailed
	ServerErrEncodeJSONFailed
	ServerErrRegisterWorkFailed
	ServerErrUnregisterWorkFailed
	ServerErrOccupyWorkerSlotsFailed
	ServerErrFreeWorkerSlotsFailed
	ServerErrOccupyLocalSlotsFailed
	ServerErrFreeLocalSlotsFailed
	ServerErrStartWorkFailed
	ServerErrEndWorkFailed
	ServerErrSetWorkSettings
	ServerErrGetWorkSettings
	ServerErrUpdateJobStats
	ServerErrRecordWorkStats
	ServerErrGetWorkStatus
	ServerErrOccupySlotsOrUsageNoEnough
	ServerErrQueryFileSendStatusFailed
	ServerErrUpdateFileSendStatusFailed
	ServerErrUpdateWorkHeartbeatFailed
	ServerErrGetWorkDetailFailed
	ServerErrExecuteRemoteTaskFailed
	ServerErrSendRemoteFileFailed
	ServerErrExecuteLocalTaskFailed
	ServerErrWorkNotFound
)

var serverErrCode = map[ServerErrCode]string{
	ServerErrOK:                         "request OK",
	ServerErrInvalidParam:               "invalid param",
	ServerErrRedirectFailed:             "redirect failed",
	ServerErrEncodeJSONFailed:           "encode json failed",
	ServerErrRegisterWorkFailed:         "register work failed",
	ServerErrUnregisterWorkFailed:       "unregister work failed",
	ServerErrOccupyWorkerSlotsFailed:    "occupy work slots failed",
	ServerErrFreeWorkerSlotsFailed:      "free work slots failed",
	ServerErrOccupyLocalSlotsFailed:     "occupy local slots failed",
	ServerErrFreeLocalSlotsFailed:       "free local slots failed",
	ServerErrStartWorkFailed:            "start work failed",
	ServerErrEndWorkFailed:              "end work failed",
	ServerErrSetWorkSettings:            "set work settings failed",
	ServerErrGetWorkSettings:            "get work settings failed",
	ServerErrUpdateJobStats:             "update job stats failed",
	ServerErrRecordWorkStats:            "record work stats failed",
	ServerErrGetWorkStatus:              "get work status failed",
	ServerErrOccupySlotsOrUsageNoEnough: "occupy slots or usage no enough",
	ServerErrQueryFileSendStatusFailed:  "query file send status failed",
	ServerErrUpdateFileSendStatusFailed: "update file send status failed",
	ServerErrUpdateWorkHeartbeatFailed:  "update work heartbeat failed",
	ServerErrGetWorkDetailFailed:        "get work detail failed",
	ServerErrExecuteRemoteTaskFailed:    "execute remote task failed",
	ServerErrSendRemoteFileFailed:       "send remote file failed",
	ServerErrExecuteLocalTaskFailed:     "execute local task failed",
	ServerErrWorkNotFound:               "work not found",
}

// String get error string from error code
func (sec ServerErrCode) String() string {
	if _, ok := serverErrCode[sec]; !ok {
		return "unknown server error"
	}
	return serverErrCode[sec]
}

// Int get code int from error code
func (sec ServerErrCode) Int() int {
	return int(sec)
}
