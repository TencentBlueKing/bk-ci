/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package direct

import (
	"io/ioutil"
	"net/http"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	http2 "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/config"

	"github.com/emicklei/go-restful"
)

// define vars
const (
	prefix    = "/api/"
	VersionV1 = "v1"

	PathV1 = prefix + VersionV1
)

// ResHTTPHandle : http handle for resource report
type ResHTTPHandle struct {
	actionsV1 []*httpserver.Action
	//rd        registerdiscover.RegisterDiscover
	mgr  *directResourceManager
	conf *config.DirectResourceConfig
}

// NewResourceHTTPHandle : return new http handle
func NewResourceHTTPHandle(conf *config.DirectResourceConfig, mgr *directResourceManager) (*ResHTTPHandle, error) {
	a := &ResHTTPHandle{conf: conf}

	a.mgr = mgr

	err := a.initActions()
	if err != nil {
		return nil, err
	}

	return a, nil
}

// internalError : return internal error
func internalError(req *restful.Request, resp *restful.Response) {
	ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrServerInternalError,
		Message: commonTypes.ServerErrServerInternalError.String()})
}

func (a *ResHTTPHandle) initActions() error {
	a.actionsV1 = make([]*httpserver.Action, 0, 100)
	a.actionsV1 = append(a.actionsV1, httpserver.NewAction(
		"POST", "/build/reportresource", nil, a.reportresource))

	return nil
}

// GetActions : Get V1 actions
func (a *ResHTTPHandle) GetActions() []*httpserver.Action {
	return a.actionsV1
}

func (a *ResHTTPHandle) reportresource(req *restful.Request, resp *restful.Response) {
	blog.Infof("reportresource...")

	// get request
	task, err := getReportResource(req)
	if err != nil {
		blog.Errorf("getCreateTask failed, url(%s): %v", req.Request.URL.String(), err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	// notify launch application
	err = a.mgr.onResourceReport(task)
	if err != nil {
		blog.Errorf("reportresource failed, url(%s): %v", req.Request.URL.String(), err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrReportResourceError, Message: err.Error()})
		return
	}

	// return response
	ReturnRest(&RestResponse{Resp: resp, HTTPCode: 0, ErrCode: 0})
	return
}

func getReportResource(req *restful.Request) (*ReportAgentResource, error) {
	body, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("get request body failed [%v] when getCreateTask", err)
		return nil, err
	}

	var task ReportAgentResource
	if err = codec.DecJSON(body, &task); err != nil {
		blog.Errorf("get report resource failed: [%v], body: [%s]", err, string(body))
		return nil, err
	}

	blog.Infof("get report resource: %s", string(body))
	return &task, nil
}

// RestResponse : data struct for http response
type RestResponse struct {
	Resp     *restful.Response
	HTTPCode int

	Data    interface{}
	ErrCode commonTypes.ServerErrCode
	//ErrCode int
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
	resp.Resp.Write(result)
}
