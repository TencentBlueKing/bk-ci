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
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	selfMetric "github.com/Tencent/bk-ci/src/booster/server/pkg/metric"

	"github.com/emicklei/go-restful"
)

const (
	// HeaderRemote define a http-header name which store the true source ip from requester.
	HeaderRemote = "X-Real-Ip"
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

		// handle the remote addr from other server for redirect
		remote := strings.Split(req.Request.RemoteAddr, ":")
		if len(remote) > 0 {
			//if ok, _ := GetAPIResource().Rd.IsServerIP(remote[0]); !ok {
			//	req.Request.Header.Del(HeaderRemote)
			//	req.Request.Header.Set(HeaderRemote, req.Request.RemoteAddr)
			//}
			// TODO: trust the header remote ip for now, go back to check when rd is stable.
			if req.Request.Header.Get(HeaderRemote) == "" {
				req.Request.Header.Set(HeaderRemote, req.Request.RemoteAddr)
			}
		}
		blog.Infof("Receive %s %s?%s From %s",
			req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery, req.Request.Header.Get(HeaderRemote))

		switch opts {
		case ProcessNoLimit:
			f(req, resp)
		case ProcessMasterOnly:
			isMaster, leader, err := GetAPIResource().Rd.IsMaster()

			if err != nil {
				blog.Errorf("process get master failed url(%s %s?%s): %v",
					req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery, err)
				ReturnRest(&RestResponse{Resp: resp, ErrCode: ServerErrPreProcessFailed,
					HTTPCode: http.StatusInternalServerError})
			} else if isMaster {
				f(req, resp)
			} else {
				redirect(leader.GetURI(), req, resp)
			}
		}

		useTime := time.Since(entranceTime).Nanoseconds() / 1000 / 1000
		// record request metrics data
		selfMetric.HttpRequestController.Observe(selfMetric.HttpRequestLabel{
			Code:   fmt.Sprintf("%d", resp.StatusCode()),
			Method: req.Request.Method,
			Path:   req.Request.URL.Path,
		}, float64(useTime))
		blog.Infof("Return [%d] %dms %s %s?%s To %s",
			resp.StatusCode(), useTime, req.Request.Method, req.Request.URL.Path,
			req.Request.URL.RawQuery, req.Request.Header.Get(HeaderRemote))
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
		ReturnRest(&RestResponse{Resp: resp, ErrCode: ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}
	r.Header = req.Request.Header
	r.Close = true
	rsp, err := defaultClient.Do(r)
	if err != nil {
		blog.Errorf("redirect to uri(%s %s), do request failed: %v", req.Request.Method, uri, err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}
	defer func() {
		_ = rsp.Body.Close()
	}()

	data, err := ioutil.ReadAll(rsp.Body)
	if err != nil {
		blog.Errorf("redirect to uri(%s %s) failed: %v", req.Request.Method, uri, err)
		ReturnRest(&RestResponse{Resp: resp, ErrCode: ServerErrRedirectFailed,
			HTTPCode: http.StatusInternalServerError})
		return
	}

	resp.WriteHeader(rsp.StatusCode)
	_, _ = resp.ResponseWriter.Write(data)
}

var defaultClient = &http.Client{}
