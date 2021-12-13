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
	"io/ioutil"
	"net/http"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	http2 "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/config"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/manager"
	registerdiscover "github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/register-discover"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/resource/direct/agent/pkg/types"

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
	rd        registerdiscover.RegisterDiscover
	mgr       manager.Manager
	conf      *config.ServerConfig
}

// NewHTTPHandle : return new http handle
func NewHTTPHandle(conf *config.ServerConfig) (*HTTPHandle, error) {
	a := &HTTPHandle{conf: conf}

	err := a.init()
	if err != nil {
		blog.Infof("failed to init HttpHandle,return nil")
		return nil, err
	}

	return a, nil
}

func funcwrapper(f restful.RouteFunction) func(req *restful.Request, resp *restful.Response) {
	return func(req *restful.Request, resp *restful.Response) {
		entranceTime := time.Now().Local()
		blog.Infof("Receive %s %s?%s", req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery)
		f(req, resp)
		useTime := time.Since(entranceTime).Nanoseconds() / 1000 / 1000
		blog.Infof("Return [%d] %dms %s %s",
			resp.StatusCode(), useTime, req.Request.Method, req.Request.URL.Path)
	}
}

func (a *HTTPHandle) initObjects() error {
	var err error
	a.rd, err = registerdiscover.NewRegisterDiscover(a.conf)
	if err != nil {
		blog.Errorf("get new register discover failed: %v", err)
		return err
	}

	a.mgr, err = manager.NewManager(a.conf, a.rd)
	if err != nil {
		blog.Errorf("get new manager failed: %v", err)
		return err
	}

	go a.mgr.Run()

	return nil
}

func (a *HTTPHandle) initActions() error {
	a.actionsV1 = make([]*httpserver.Action, 0, 100)

	a.actionsV1 = append(a.actionsV1, httpserver.NewAction(
		"POST", "/build/executecommand", nil, funcwrapper(a.executeCommand)))

	return nil
}

func (a *HTTPHandle) init() error {
	err := a.initObjects()
	if err != nil {
		return err
	}

	err = a.initActions()
	if err != nil {
		return err
	}

	return nil
}

// GetActions : Get V1 actions
func (a *HTTPHandle) GetActions() []*httpserver.Action {
	return a.actionsV1
}

// func (a *HTTPHandle) launchAppliation(req *restful.Request, resp *restful.Response) {
// 	blog.Infof("launchAppliation...")

// 	// get resource
// 	resources, err := getLaunchResources(req)
// 	if err != nil {
// 		blog.Errorf("getLaunchResources failed, url(%s): %v", req.Request.URL.String(), err)
// 		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
// 		return
// 	}

// 	// notify launch application
// 	err = a.mgr.LaunchApplication(resources)
// 	if err != nil {
// 		blog.Errorf("LaunchApplication failed, url(%s): %v", req.Request.URL.String(), err)
// 		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRequestResourceFailed, Message: err.Error()})
// 		return
// 	}

// 	// return response
// 	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0})
// 	return
// }

// func (a *HTTPHandle) releaseAppliation(req *restful.Request, resp *restful.Response) {
// 	blog.Infof("releaseAppliation...")

// 	// get resource
// 	resources, err := getReleaseResources(req)
// 	if err != nil {
// 		blog.Errorf("getReleaseResources failed, url(%s): %v", req.Request.URL.String(), err)
// 		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
// 		return
// 	}

// 	// notify release application
// 	err = a.mgr.ReleaseApplication(resources)
// 	if err != nil {
// 		blog.Errorf("ReleaseApplication failed, url(%s): %v", req.Request.URL.String(), err)
// 		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRequestResourceFailed, Message: err.Error()})
// 		return
// 	}

// 	// return response
// 	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0})
// 	return
// }

func (a *HTTPHandle) executeCommand(req *restful.Request, resp *restful.Response) {
	blog.Infof("executeCommand...")

	// get resource
	resources, err := getExecuteCommand(req)
	if err != nil {
		blog.Errorf("getExecuteCommand failed, url(%s): %v", req.Request.URL.String(), err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	// notify launch application
	err = a.mgr.ExecuteCommand(resources)
	if err != nil {
		blog.Errorf("executeCommand failed, url(%s): %v", req.Request.URL.String(), err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRequestResourceFailed, Message: err.Error()})
		return
	}

	// return response
	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0})
	return
}

// func getLaunchResources(req *restful.Request) (*types.AllocateServerResourceType, error) {
// 	body, err := ioutil.ReadAll(req.Request.Body)
// 	if err != nil {
// 		blog.Errorf("get request body failed [%v] when getLaunchResources", err)
// 		return nil, err
// 	}

// 	var resources types.AllocateServerResourceType
// 	if err = codec.DecJSON(body, &resources); err != nil {
// 		blog.Errorf("get server resource failed: %v, body: %s", err, string(body))
// 		return nil, err
// 	}

// 	blog.Infof("get server resource: %s", string(body))
// 	return &resources, nil
// }

// func getReleaseResources(req *restful.Request) (*types.FreeServerResourceType, error) {
// 	body, err := ioutil.ReadAll(req.Request.Body)
// 	if err != nil {
// 		blog.Errorf("get request body failed [%v] when getReleaseResources", err)
// 		return nil, err
// 	}

// 	var resources types.FreeServerResourceType
// 	if err = codec.DecJSON(body, &resources); err != nil {
// 		blog.Errorf("get server resource failed: %v, body: %s", err, string(body))
// 		return nil, err
// 	}

// 	blog.Infof("get server resource: %s", string(body))
// 	return &resources, nil
// }

func getExecuteCommand(req *restful.Request) (*types.NotifyAgentData, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get request body failed [%v] when getLaunchResources", err)
		return nil, err
	}

	var resources types.NotifyAgentData
	if err = codec.DecJSON(body, &resources); err != nil {
		blog.Errorf("get server resource failed: %v, body: %s", err, string(body))
		return nil, err
	}

	blog.Infof("get server resource: %s", string(body))
	return &resources, nil
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
