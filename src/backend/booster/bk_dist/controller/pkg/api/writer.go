/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package api

import (
	"net/http"

	http2 "github.com/Tencent/bk-ci/src/booster/common/http"

	"github.com/emicklei/go-restful"
)

// ErrorCode describe the error from http handler
type ErrorCode interface {
	// get error string
	String() string

	// get error code int
	Int() int
}

// RestResponse contains all response information need by a http handler
type RestResponse struct {
	Resp     *restful.Response
	HTTPCode int

	Data    interface{}
	ErrCode ErrorCode
	Message string
	Extra   map[string]interface{}

	WrapFunc func([]byte) []byte
}

// Check make sure the fields set alright
func (resp *RestResponse) Check() {
	if resp.HTTPCode == 0 {
		resp.HTTPCode = http.StatusOK
	}

	if resp.ErrCode == nil {
		resp.ErrCode = ServerErrOK
	}

	if resp.Message == "" {
		resp.Message = resp.ErrCode.String()
	} else {
		resp.Message = resp.ErrCode.String() + " | " + resp.Message
	}
}

// Wrap do the wrap function if set
func (resp *RestResponse) Wrap(r []byte) []byte {
	if resp.WrapFunc != nil {
		return resp.WrapFunc(r)
	}

	return r
}

// ReturnRest do the return work according to a RestResponse
func ReturnRest(resp *RestResponse) {
	resp.Check()
	result, _ := http2.GetResponseEx(resp.ErrCode.Int(), resp.Message, resp.Data, resp.Extra)

	resp.Resp.WriteHeader(resp.HTTPCode)
	_, _ = resp.Resp.Write(resp.Wrap(result))
}
