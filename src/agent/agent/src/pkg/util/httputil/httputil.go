/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package httputil

import (
	"bytes"
	"context"
	"encoding/json"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"os"
	"reflect"
	"strconv"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/agentcommon/logs"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
)

type HttpClient struct {
	client    *http.Client
	method    string
	url       string
	body      io.Reader
	header    map[string]string
	formValue map[string]string
	err       error
}

type HttpResult struct {
	Body         []byte
	Status       int
	Error        error
	IgnoreDupLog bool
}

var client = &http.Client{}

// newClient 替换客户端，重置连接池
func newClient() {
	client = &http.Client{}
}

func NewHttpClient() *HttpClient {
	return &HttpClient{
		client:    client,
		header:    make(map[string]string),
		formValue: make(map[string]string),
	}
}

func (r *HttpClient) Post(url string) *HttpClient {
	r.method = "POST"
	r.url = url
	r.header["Content-Type"] = "application/json; charset=utf-8"
	return r
}

func (r *HttpClient) Put(url string) *HttpClient {
	r.method = "PUT"
	r.url = url
	r.header["Content-Type"] = "application/json; charset=utf-8"
	return r
}

func (r *HttpClient) Get(url string) *HttpClient {
	r.method = "GET"
	r.url = url
	return r
}

func (r *HttpClient) Delete(url string) *HttpClient {
	r.method = "DELETE"
	r.url = url
	return r
}

func (r *HttpClient) SetHeader(key, value string) *HttpClient {
	r.header[key] = value
	return r
}

func (r *HttpClient) SetHeaders(header map[string]string) *HttpClient {
	for k, v := range header {
		r.header[k] = v
	}
	return r
}

func (r *HttpClient) SetForm(key, value string) *HttpClient {
	r.formValue[key] = value
	return r
}

func (r *HttpClient) Body(body interface{}, ignoreDupLog bool) *HttpClient {
	if nil == body {
		r.body = bytes.NewReader([]byte(""))
		return r
	}
	if reflect.ValueOf(body).IsNil() {
		r.body = bytes.NewReader([]byte(""))
		return r
	}
	data, err := json.Marshal(body)
	if nil != err {
		r.err = err
	}
	r.body = bytes.NewReader(data)

	if ignoreDupLog {
		logs.Info(fmt.Sprintf("%s|body repeat as before skip", r.url))
	} else {
		logs.Info(fmt.Sprintf("%s|request body: %s", r.url, string(data)))
	}
	return r
}

type IgnoreDupLogResp struct {
	Status int
	Resp   string
}

func (r *HttpClient) Execute(ignoreDupLogResp *IgnoreDupLogResp) *HttpResult {
	result := new(HttpResult)
	defer func() {
		if err := recover(); err != nil {
			logs.Error(fmt.Sprintf("%s|http request err: ", r.url), err)
			result.Error = errors.New("http request err")
		}
	}()
	withTimeout, cancel := context.WithTimeout(context.TODO(), time.Duration(config.GAgentConfig.TimeoutSec)*time.Second)
	defer cancel()
	req, err := http.NewRequestWithContext(withTimeout, r.method, r.url, r.body)
	if err != nil {
		result.Error = err
		return result
	}

	//header
	for k, v := range r.header {
		req.Header.Set(k, v)
	}

	//queryParams
	value := url.Values{}
	for k, v := range r.formValue {
		value.Add(k, v)
	}
	req.Form = value
	resp, err := r.client.Do(req)
	if err != nil {
		if os.IsTimeout(err) || errors.Is(err, context.DeadlineExceeded) {
			logs.Warn("http request time out, replace client")
			newClient()
			checkTimeOutExit(err)
		}
		result.Error = err
		return result
	} else {
		checkTimeOutExit(nil)
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(resp.Body)
	if err != nil {
		result.Error = err
		return result
	}

	result.Body = body
	result.Status = resp.StatusCode
	if ignoreDupLogResp != nil && resp.StatusCode == ignoreDupLogResp.Status && string(body) == ignoreDupLogResp.Resp {
		result.IgnoreDupLog = true
		logs.Info(fmt.Sprintf("%s|resp repeat as before skip", r.url))
	} else {
		logs.Info(fmt.Sprintf("%s|http status: %s, http respBody: %s", r.url, resp.Status, string(body)))
	}

	// 检查 http 错误异常
	checkHttpStatusErr(resp.StatusCode, body)

	return result
}

// checkTimeOutExit 检查是否因为超时直接退出
func checkTimeOutExit(err error) {
	enableExitTimeStr, ok := config.FetchEnv(constant.DevopsAgentTimeoutExitTime)
	if !ok {
		return
	}
	enableExitTime, intErr := strconv.ParseInt(enableExitTimeStr, 10, 32)
	if intErr != nil {
		logs.Warnf("enableExitTimeStr %s convert timeout err to int", enableExitTimeStr)
		return
	}
	exitcode.CheckTimeoutError(err, int32(enableExitTime))
}
