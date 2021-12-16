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
	"io/ioutil"
	"net/http"
	"os"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/shadertool/common"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	http2 "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"

	"github.com/emicklei/go-restful"
)

// define vars
const (
	prefix    = "/api/"
	VersionV1 = "v1"

	PathV1 = prefix + VersionV1
)

// HTTPHandle : http handle
type HTTPHandle struct {
	actionsV1 []*httpserver.Action
	mgr       *ShaderTool
	// conf      *config.ServerConfig
}

// NewHTTPHandle : return new http handle
// func NewHTTPHandle(conf *config.ServerConfig, mgr *ShaderTool) (*HTTPHandle, error) {
func NewHTTPHandle(mgr *ShaderTool) (*HTTPHandle, error) {
	a := &HTTPHandle{
		// conf: conf,
		mgr: mgr,
	}

	err := a.init()
	if err != nil {
		blog.Errorf("ShaderTool: failed to init HttpHandle,return nil")
		return nil, err
	}

	return a, nil
}

func funcwrapper(f restful.RouteFunction) func(req *restful.Request, resp *restful.Response) {
	return func(req *restful.Request, resp *restful.Response) {
		entranceTime := time.Now().Local()
		blog.Debugf("Receive %s %s?%s", req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery)
		f(req, resp)
		useTime := time.Since(entranceTime).Nanoseconds() / 1000 / 1000
		blog.Debugf("Return [%d] %dms %s %s",
			resp.StatusCode(), useTime, req.Request.Method, req.Request.URL.Path)
	}
}

func (a *HTTPHandle) initActions() error {
	a.actionsV1 = make([]*httpserver.Action, 0, 100)

	a.actionsV1 = append(a.actionsV1, httpserver.NewAction(
		"POST", "/available", nil, funcwrapper(a.available)))
	a.actionsV1 = append(a.actionsV1, httpserver.NewAction(
		"POST", "/shaders", nil, funcwrapper(a.shaders)))

	return nil
}

func (a *HTTPHandle) init() error {
	err := a.initActions()
	if err != nil {
		return err
	}

	return nil
}

// GetActions : Get V1 actions
func (a *HTTPHandle) GetActions() []*httpserver.Action {
	return a.actionsV1
}

// TODO (tomtian)
func (a *HTTPHandle) available(req *restful.Request, resp *restful.Response) {
	blog.Debugf("ShaderTool: available...")

	// do no care the json content now, just return ok

	// return response
	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0, Data: &common.AvailableResp{
		PID: int32(os.Getpid()),
	}})
	return
}

// TODO (tomtian)
func (a *HTTPHandle) shaders(req *restful.Request, resp *restful.Response) {
	blog.Debugf("ShaderTool: shaders...")

	actions, err := getShaderActions(req)
	if err != nil {
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrCode(
			GetErrorCode(ErrorInvalidJSON)), Message: err.Error()})
		return
	}

	err = a.mgr.shaders(actions)
	if err != nil {
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrCode(
			GetErrorCode(ErrorInvalidJSON)), Message: err.Error()})
		return
	}

	// return response
	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0})
	return
}

func getShaderActions(req *restful.Request) (*common.UE4Action, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("ShaderTool: get shader actions failed: %v", err)
		return nil, err
	}

	blog.Debugf("ShaderTool: get shader actions from body: %s", string(body))
	var actions common.UE4Action
	if err = codec.DecJSON(body, &actions); err != nil {
		blog.Errorf("ShaderTool: get shader actions decode failed: %v", err)
		return nil, err
	}

	blog.Debugf("ShaderTool: get shader actions: %+v", actions)
	return &actions, nil
}

// RestResponse : data struct for http response
type RestResponse struct {
	Resp     *restful.Response
	HTTPCode int

	Data    interface{}
	ErrCode commonTypes.ServerErrCode
	Message string
	Extra   map[string]interface{}

	WrapFunc func([]byte) []byte
}

// ReturnRest : return http response
func ReturnRest(resp *RestResponse) {
	if resp.HTTPCode == 0 {
		resp.HTTPCode = http.StatusOK
	}

	if resp.Message == "" {
		resp.Message = resp.ErrCode.String()
	} else {
		resp.Message = resp.ErrCode.String() + " | " + resp.Message
	}

	result, err := http2.GetResponseEx(int(resp.ErrCode), resp.Message, resp.Data, resp.Extra)
	if err != nil {
		blog.Errorf("%s | err: %v", commonTypes.ServerErrEncodeJSONFailed, err)
		// TODO: will this go into dead loop?
		ReturnRest(&RestResponse{Resp: resp.Resp, ErrCode: commonTypes.ServerErrEncodeJSONFailed})
		return
	}

	if resp.WrapFunc != nil {
		result = resp.WrapFunc(result)
	}
	resp.Resp.WriteHeader(resp.HTTPCode)
	_, _ = resp.Resp.Write(result)
}
