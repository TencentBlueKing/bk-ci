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
	"fmt"
	"io/ioutil"
	"net/http"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/types"

	"github.com/emicklei/go-restful"
)

// MasterRequired wrap the api handler and make it only available for master node.
func MasterRequired(f restful.RouteFunction) func(req *restful.Request, resp *restful.Response) {
	return process(f, ProcessMasterOnly)
}

// NoLimit wrap the api handler and will process all requests.
func NoLimit(f restful.RouteFunction) func(req *restful.Request, resp *restful.Response) {
	return process(f, ProcessNoLimit)
}

// Process log before and after a request. If options is mater-required, then redirect the request to master node and
// return the data from master node.
func process(f restful.RouteFunction, opts ProcessType) func(req *restful.Request, resp *restful.Response) {
	return func(req *restful.Request, resp *restful.Response) {
		entranceTime := time.Now().Local()
		blog.Infof("Receive %s %s?%s From %s",
			req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery, req.Request.RemoteAddr)

		switch opts {
		case ProcessNoLimit:
			f(req, resp)
		case ProcessMasterOnly:
			var isMaster bool
			var leader *types.ServerInfo
			var err error

			if Rd == nil {
				err = fmt.Errorf("rd not init, can not found master")
			} else {
				isMaster, leader, err = Rd.IsMaster()
			}

			if err != nil {
				blog.Errorf("process get master failed url(%s %s?%s): %v",
					req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery, err)
				ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrPreProcessFailed,
					HTTPCode: http.StatusInternalServerError})
			} else if isMaster {
				f(req, resp)
			} else {
				redirect(leader.GetURI(), req, resp)
			}
		}

		useTime := time.Since(entranceTime).Nanoseconds() / 1000 / 1000
		blog.Infof("Return [%d] %dms %s %s To %s",
			resp.StatusCode(), useTime, req.Request.Method, req.Request.URL.Path, req.Request.RemoteAddr)
	}
}

type ProcessType string

const (
	ProcessMasterOnly ProcessType = "master_only"
	ProcessNoLimit    ProcessType = "no_limit"
)

// redirect is like a proxy, it requests to the other node and return the data from that one.
func redirect(uri string, req *restful.Request, resp *restful.Response) {
	uri += req.Request.URL.Path + "?" + req.Request.URL.RawQuery

	blog.Infof("redirect to uri: %s %s", req.Request.Method, uri)
	r, err := http.NewRequest(req.Request.Method, uri, req.Request.Body)
	if err != nil {
		blog.Errorf("redirect to uri(%s %s), get new request failed: %v", req.Request.Method, uri, err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}
	r.Close = true
	rsp, err := defaultClient.Do(r)
	if err != nil {
		blog.Errorf("redirect to uri(%s %s), do request failed: %v", req.Request.Method, uri, err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}
	defer func() {
		_ = rsp.Body.Close()
	}()

	data, err := ioutil.ReadAll(rsp.Body)
	if err != nil {
		blog.Errorf("redirect to uri(%s %s) failed: %v", req.Request.Method, uri, err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}
	_, _ = resp.ResponseWriter.Write(data)
}

var defaultClient = &http.Client{}
