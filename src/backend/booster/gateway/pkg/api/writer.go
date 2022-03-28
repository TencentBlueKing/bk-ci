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

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	http2 "github.com/Tencent/bk-ci/src/booster/common/http"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"

	"github.com/emicklei/go-restful"
)

// RestResponse contains all response information need by a http handler
type RestResponse struct {
	Resp     *restful.Response
	HTTPCode int

	Data    interface{}
	ErrCode commonTypes.ServerErrCode
	Message string
	Extra   map[string]interface{}

	WrapFunc func([]byte) []byte
}

// ReturnRest do the return work according to a RestResponse
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
		ReturnRest(&RestResponse{Resp: resp.Resp, ErrCode: commonTypes.ServerErrEncodeJSONFailed})
		return
	}

	if resp.WrapFunc != nil {
		result = resp.WrapFunc(result)
	}
	resp.Resp.WriteHeader(resp.HTTPCode)
	_, _ = resp.Resp.Write(result)
}
