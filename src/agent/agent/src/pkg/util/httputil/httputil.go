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
	"strings"
	"time"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/config"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/constant"
	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/envs"
	exitcode "github.com/TencentBlueKing/bk-ci/agent/src/pkg/exiterror"
	"github.com/pkg/errors"
	"golang.org/x/net/http/httpproxy"
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

var client = newClientWithProxy()

// newClient 替换客户端，重置连接池
func newClient() {
	client = newClientWithProxy()
}

// newClientWithProxy 创建带代理感知的 HTTP 客户端。
// Transport.Proxy 使用闭包函数，每次请求时实时从 envs.FetchEnv 读取
// HTTP_PROXY / HTTPS_PROXY，支持后台下发环境变量动态切换代理。
func newClientWithProxy() *http.Client {
	transport := http.DefaultTransport.(*http.Transport).Clone()
	transport.Proxy = proxyFromAgentEnv
	return &http.Client{Transport: transport}
}

// proxyFromAgentEnv 根据请求 scheme 解析代理配置。
// 优先读取 agent 环境变量，其次回退到 .agent.properties 中持久化的代理配置，
// 最后再 fallback 到标准库 http.ProxyFromEnvironment。
// 同时支持 NO_PROXY 排除规则。
func proxyFromAgentEnv(req *http.Request) (*url.URL, error) {
	httpProxy := fetchProxyEnv("HTTP_PROXY")
	httpsProxy := fetchProxyEnv("HTTPS_PROXY")
	noProxy := fetchProxyEnv("NO_PROXY")

	if httpProxy == "" && httpsProxy == "" {
		return http.ProxyFromEnvironment(req)
	}

	cfg := httpproxy.Config{
		HTTPProxy:  httpProxy,
		HTTPSProxy: httpsProxy,
		NoProxy:    noProxy,
	}

	proxyURL, err := cfg.ProxyFunc()(req.URL)
	if err != nil {
		logs.Errorf("resolve proxy for %s error: %s, fallback to ProxyFromEnvironment", req.URL.Host, err)
		return http.ProxyFromEnvironment(req)
	}

	if proxyURL != nil {
		logs.Infof("using proxy %s for %s", proxyURL.Redacted(), req.URL.Host)
	}
	return proxyURL, nil
}

// fetchProxyEnv 读取代理相关配置：优先查环境变量，其次回退到 .agent.properties 中持久化的代理配置。
func fetchProxyEnv(key string) string {
	if v, ok := envs.FetchEnv(key); ok && v != "" {
		return v
	}
	if v, ok := envs.FetchEnv(strings.ToLower(key)); ok && v != "" {
		return v
	}
	return fetchProxyFromConfig(key)
}

func fetchProxyFromConfig(key string) string {
	if config.GAgentConfig == nil {
		return ""
	}

	switch key {
	case config.KeyHTTPProxy:
		return strings.TrimSpace(config.GAgentConfig.HTTPProxy)
	case config.KeyHTTPSProxy:
		return strings.TrimSpace(config.GAgentConfig.HTTPSProxy)
	case config.KeyNOProxy:
		return strings.TrimSpace(config.GAgentConfig.NOProxy)
	default:
		return ""
	}
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
	enableExitTimeStr, ok := envs.FetchEnv(constant.DevopsAgentTimeoutExitTime)
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
