/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
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
	"encoding/json"
	"errors"
	"github.com/astaxie/beego/logs"
	"io"
	"io/ioutil"
	"net/http"
	"net/url"
	"reflect"
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
	Body   []byte
	Status int
	Error  error
}

func IsSuccess(status int) bool {
	return status >= 200 && status < 400
}

func NewHttpClient() *HttpClient {
	return &HttpClient{
		client:    http.DefaultClient,
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

func (r *HttpClient) Body(body interface{}) *HttpClient {
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

	logs.Info("body: ", string(data))
	return r
}

func (r *HttpClient) Execute() *HttpResult {
	result := new(HttpResult)
	defer func() {
		if err := recover(); err != nil {
			logs.Error("http request err: ", err)
			result.Error = errors.New("http request err")
		}
	}()

	req, err := http.NewRequest(r.method, r.url, r.body)
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
		result.Error = err
		return result
	}
	defer resp.Body.Close()
	body, err := ioutil.ReadAll(resp.Body)
	if err != nil {
		result.Error = err
		return result
	}

	result.Body = body
	result.Status = resp.StatusCode
	logs.Info("http status: ", resp.Status)
	logs.Info("http respBody: ", string(body))
	return result
}

func (r *HttpResult) Into(obj interface{}) error {
	// TODO 怎么在golang用泛型将result转成结构化数据？
	if nil != r.Error {
		return r.Error
	}

	err := json.Unmarshal(r.Body, obj)
	if nil != err {
		return err
	}

	return nil
}
