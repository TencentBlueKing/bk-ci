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
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/emicklei/go-restful"
)

// FuncWrapper log before and after a request. If options is mater-required,
// then redirect the request to master node and
// return the data from master node.
func FuncWrapper(f restful.RouteFunction) func(req *restful.Request, resp *restful.Response) {
	return func(req *restful.Request, resp *restful.Response) {
		entranceTime := time.Now().Local()
		blog.Infof("Receive %s %s?%s From %s",
			req.Request.Method, req.Request.URL.Path, req.Request.URL.RawQuery, req.Request.RemoteAddr)

		f(req, resp)

		useTime := time.Since(entranceTime).Nanoseconds() / 1000 / 1000
		blog.Infof("Return [%d] %dms %s %s?%s To %s",
			resp.StatusCode(), useTime, req.Request.Method, req.Request.URL.Path,
			req.Request.URL.RawQuery, req.Request.RemoteAddr)
	}
}
