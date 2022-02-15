/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package httpclient

import (
	"bytes"
	"crypto/tls"
	"io/ioutil"
	"net"
	"net/http"
	"time"

	http2 "github.com/Tencent/bk-ci/src/booster/common/http"
	"github.com/Tencent/bk-ci/src/booster/common/ssl"
)

// HttpResponse define the information of the http response
type HttpResponse struct {
	Reply      []byte
	StatusCode int
	Status     string
	Header     http.Header
}

// HTTPClient define a http client for request.
type HTTPClient struct {
	caFile   string
	certFile string
	keyFile  string
	header   map[string]string
	httpCli  *http.Client
}

// NewHTTPClient get a new HTTPClient.
func NewHTTPClient() *HTTPClient {
	return &HTTPClient{
		httpCli: &http.Client{},
		header:  make(map[string]string),
	}
}

// GetClient return the original http.Client.
func (client *HTTPClient) GetClient() *http.Client {
	return client.httpCli
}

// SetTLSNoVerity set client TLS insecure skip verify
func (client *HTTPClient) SetTLSNoVerity() error {
	tlsConf := ssl.ClientTSLConfNoVerity()

	trans := client.NewTransPort()
	trans.TLSClientConfig = tlsConf
	client.httpCli.Transport = trans

	return nil
}

// SetTLSVerityServer set server TLS.
func (client *HTTPClient) SetTLSVerityServer(caFile string) error {
	client.caFile = caFile

	// load ca cert
	tlsConf, err := ssl.ClientTSLConfVerityServer(caFile)
	if err != nil {
		return err
	}

	client.SetTLSVerityConfig(tlsConf)

	return nil
}

// SetTLSVerity set client TLS with certs files.
func (client *HTTPClient) SetTLSVerity(caFile, certFile, keyFile, passwd string) error {
	client.caFile = caFile
	client.certFile = certFile
	client.keyFile = keyFile

	// load cert
	tlsConf, err := ssl.ClientTSLConfVerity(caFile, certFile, keyFile, passwd)
	if err != nil {
		return err
	}

	client.SetTLSVerityConfig(tlsConf)

	return nil
}

// SetTLSVerityConfig set client TLS with tls.Config.
func (client *HTTPClient) SetTLSVerityConfig(tlsConf *tls.Config) {
	trans := client.NewTransPort()
	trans.TLSClientConfig = tlsConf
	client.httpCli.Transport = trans
}

// NewTransPort get a new http transport.
func (client *HTTPClient) NewTransPort() *http.Transport {
	return &http.Transport{
		TLSHandshakeTimeout: 5 * time.Second,
		Dial: (&net.Dialer{
			Timeout:   5 * time.Second,
			KeepAlive: 30 * time.Second,
		}).Dial,
		ResponseHeaderTimeout: 30 * time.Second,
	}
}

// SetTimeout set the client timeout.
func (client *HTTPClient) SetTimeOut(timeOut time.Duration) {
	client.httpCli.Timeout = timeOut
}

// SetHeader set header for the http client。
// Note：if the header is the same with the parameter(header) which is specified
// in the function GET, POST, PUT,DELETE,Patch and so on. this set header is ignore in the call
func (client *HTTPClient) SetHeader(key, value string) {
	client.header[key] = value
}

// SetBatchHeader batch set header for the http client。
// Note：if the header is the same with the parameter(header) which is specified
// in the function GET, POST, PUT,DELETE,Patch and so on. this set header is ignore in the call
func (client *HTTPClient) SetBatchHeader(headerSet []*http2.HeaderSet) {
	if headerSet == nil {
		return
	}
	for _, header := range headerSet {
		client.header[header.Key] = header.Value
	}
}

// GET do the get request and return body.
func (client *HTTPClient) GET(url string, header http.Header, data []byte) ([]byte, error) {
	return client.Request(url, "GET", header, data)
}

// POST do the post request and return body.
func (client *HTTPClient) POST(url string, header http.Header, data []byte) ([]byte, error) {
	return client.Request(url, "POST", header, data)
}

// DELETE do the delete request and return body.
func (client *HTTPClient) DELETE(url string, header http.Header, data []byte) ([]byte, error) {
	return client.Request(url, "DELETE", header, data)
}

// PUT do the put request and return body.
func (client *HTTPClient) PUT(url string, header http.Header, data []byte) ([]byte, error) {
	return client.Request(url, "PUT", header, data)
}

// PATCH do the patch request and return body.
func (client *HTTPClient) PATCH(url string, header http.Header, data []byte) ([]byte, error) {
	return client.Request(url, "PATCH", header, data)
}

// Get do the get request and return HttpResponse.
func (client *HTTPClient) Get(url string, header http.Header, data []byte) (*HttpResponse, error) {
	return client.RequestEx(url, "GET", header, data)
}

// Post do the post request and return HttpResponse.
func (client *HTTPClient) Post(url string, header http.Header, data []byte) (*HttpResponse, error) {
	return client.RequestEx(url, "POST", header, data)
}

// Delete do the delete request and return HttpResponse.
func (client *HTTPClient) Delete(url string, header http.Header, data []byte) (*HttpResponse, error) {
	return client.RequestEx(url, "DELETE", header, data)
}

// Put do the put request and return HttpResponse.
func (client *HTTPClient) Put(url string, header http.Header, data []byte) (*HttpResponse, error) {
	return client.RequestEx(url, "PUT", header, data)
}

// Patch do the patch request and return HttpResponse.
func (client *HTTPClient) Patch(url string, header http.Header, data []byte) (*HttpResponse, error) {
	return client.RequestEx(url, "PATCH", header, data)
}

// Request do request and return body.
func (client *HTTPClient) Request(url, method string, header http.Header, data []byte) ([]byte, error) {
	rsp, err := client.RequestEx(url, method, header, data)
	if err != nil {
		return nil, err
	}

	return rsp.Reply, nil
}

// RequestEx do request and return HttpResponse.
func (client *HTTPClient) RequestEx(url, method string, header http.Header, data []byte) (*HttpResponse, error) {
	var req *http.Request
	var errReq error
	httpRsp := &HttpResponse{
		Reply:      nil,
		StatusCode: http.StatusInternalServerError,
		Status:     "Internal Server Error",
	}

	if data != nil {
		req, errReq = http.NewRequest(method, url, bytes.NewReader(data))
	} else {
		req, errReq = http.NewRequest(method, url, nil)
	}

	if errReq != nil {
		return httpRsp, errReq
	}

	req.Close = true

	if header != nil {
		req.Header = header
	}

	for key, value := range client.header {
		if req.Header.Get(key) != "" {
			continue
		}
		req.Header.Set(key, value)
	}

	rsp, err := client.httpCli.Do(req)
	if err != nil {
		return httpRsp, err
	}

	defer func() {
		_ = rsp.Body.Close()
	}()

	httpRsp.Status = rsp.Status
	httpRsp.StatusCode = rsp.StatusCode
	httpRsp.Header = rsp.Header

	rpy, err := ioutil.ReadAll(rsp.Body)
	if err != nil {
		return httpRsp, err
	}

	httpRsp.Reply = rpy
	return httpRsp, nil
}
