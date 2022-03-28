/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"encoding/json"
	"fmt"
	"net"
	"net/http"

	"github.com/Tencent/bk-ci/src/booster/common"
	"github.com/Tencent/bk-ci/src/booster/common/http/httpclient"
	"github.com/Tencent/bk-ci/src/booster/common/types"
)

const (
	applyDistCCServerURI   = "v1/distcc/apply"
	inspectDistCCServerURI = "v1/distcc/task"
	distCCTaskDoneURI      = "v1/distcc/release"
	heartBeatURI           = "v1/distcc/heartbeat"
	cmakeConfigURI         = "v1/distcc/cmake"
)

var (
	ProdDistCCServerDomain = ""
	ProdDistCCServerPort   = ""
	ProdDistCCServerHost   = fmt.Sprintf("http://%s:%s/api", ProdDistCCServerDomain, ProdDistCCServerPort)

	TestDistCCServerDomain = ""
	TestDistCCServerPort   = ""
	TestDistCCServerHost   = fmt.Sprintf("http://%s:%s/api", TestDistCCServerDomain, TestDistCCServerPort)

	DistCCServerDomain = ""
	DistCCServerHost   = ""

	cli *httpclient.HTTPClient
)

func init() {
	cli = httpclient.NewHTTPClient()

	cli.SetHeader("Content-Type", "application/json")
	cli.SetHeader("Accept", "application/json")
}

func requestServer(method, uri string, data []byte) ([]byte, bool, error) {
	if err := checkDNS(); err != nil {
		return nil, false, err
	}

	uri = fmt.Sprintf("%s/%s", DistCCServerHost, uri)
	DebugPrintf("method(%s), uri(%s), data: %s\n", method, uri, string(data))

	var resp *httpclient.HttpResponse
	var err error

	switch method {
	case "GET":
		resp, err = cli.Get(uri, nil, data)

	case "POST":
		resp, err = cli.Post(uri, nil, data)

	case "DELETE":
		resp, err = cli.Delete(uri, nil, data)

	case "PUT":
		resp, err = cli.Put(uri, nil, data)

	default:
		err = fmt.Errorf("uri %s method %s is invalid", uri, method)
	}

	if err != nil {
		return nil, false, err
	}

	if resp.StatusCode != http.StatusOK {
		return nil, false, fmt.Errorf("%s", string(resp.Reply))
	}

	var apiResp *types.APIResponse
	err = json.Unmarshal(resp.Reply, &apiResp)
	if err != nil {
		return nil, true, fmt.Errorf("unmarshal request %s response %s error %s",
			uri, string(resp.Reply), err.Error())
	}

	if apiResp.Code != common.RestSuccess {
		return nil, true, fmt.Errorf(apiResp.Message)
	}

	by, err := json.Marshal(apiResp.Data)
	if err != nil {
		return nil, true, fmt.Errorf("marshal apiResp.Data error %s", err.Error())
	}

	return by, true, nil
}

func checkDNS() error {
	_, err := net.LookupIP(DistCCServerDomain)
	if err != nil {
		fmt.Printf("%v\n", err)
	}
	return err
}
