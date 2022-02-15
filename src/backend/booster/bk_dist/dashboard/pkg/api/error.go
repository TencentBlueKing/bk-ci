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
	ServerErrGetTaskFailed
	ServerErrGetWorkFailed
)

var serverErrCode = map[ServerErrCode]string{
	ServerErrOK:               "request OK",
	ServerErrInvalidParam:     "invalid param",
	ServerErrRedirectFailed:   "redirect failed",
	ServerErrEncodeJSONFailed: "encode json failed",
	ServerErrGetTaskFailed:    "get task failed",
	ServerErrGetWorkFailed:    "get work failed",
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
