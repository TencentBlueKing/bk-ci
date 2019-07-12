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
	"encoding/json"
	"errors"
	"github.com/astaxie/beego/logs"
	"io"
	"io/ioutil"
	"net/http"
	"os"
	"pkg/config"
	"pkg/util/fileutil"
)

type DevopsResult struct {
	Data    interface{} `json:"data"`
	Status  int64       `json:"status"`
	Message string      `json:"message"`
}

func (d *DevopsResult) IsOk() bool {
	return d.Status == 0
}

func (d *DevopsResult) IsNotOk() bool {
	return d.Status != 0
}

type AgentResult struct {
	DevopsResult
	AgentStatus string `json:"agentStatus"`
}

func (a *AgentResult) IsAgentDelete() bool {
	if a.AgentStatus == "" {
		return false
	}
	return a.AgentStatus == config.AgentStatusDelete
}

func (r *HttpResult) IntoDevopsResult() (*DevopsResult, error) {
	if nil != r.Error {
		return nil, r.Error
	}

	result := new(DevopsResult)
	err := json.Unmarshal(r.Body, result)
	if nil != err {
		logs.Error("parse result error: ", err.Error())
		return nil, errors.New("parse result error")
	} else {
		return result, nil
	}
}

func (r *HttpResult) IntoAgentResult() (*AgentResult, error) {
	if nil != r.Error {
		return nil, r.Error
	}

	result := new(AgentResult)
	err := json.Unmarshal(r.Body, result)
	if nil != err {
		logs.Error("parse result error: ", err.Error())
		return nil, errors.New("parse result error")
	} else {
		return result, nil
	}
}

func DownloadUpgradeFile(url string, headers map[string]string, filepath string) (md5 string, err error) {
	oldFileMd5, err := fileutil.GetFileMd5(filepath)
	if err != nil {
		logs.Error("check file md5 failed", err)
		return "", errors.New("check file md5 failed")
	}
	if oldFileMd5 != "" {
		url = url + "&eTag=" + oldFileMd5
	}

	req, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return "", err
	}

	//header
	for k, v := range headers {
		req.Header.Set(k, v)
	}

	resp, err := http.DefaultClient.Do(req)
	defer func() {
		if resp != nil && resp.Body != nil {
			resp.Body.Close()
		}
	}()
	if err != nil {
		logs.Error("download upgrade file failed", err)
		return "", errors.New("download upgrade file failed")
	}

	if !(resp.StatusCode >= 200 && resp.StatusCode < 300) {
		if resp.StatusCode == http.StatusNotFound {
			return "", errors.New("file not found")
		}
		if resp.StatusCode == http.StatusNotModified {
			return oldFileMd5, nil
		}
		body, _ := ioutil.ReadAll(resp.Body)
		logs.Error("download upgrade file failed, status: " + resp.Status + ", responseBody: " + string(body))
		return "", errors.New("download upgrade file failed")
	}

	err = writeToFile(filepath, resp.Body)
	if err != nil {
		logs.Error("download upgrade file failed", err)
		return "", errors.New("download upgrade file failed")
	}

	fileMd5, err := fileutil.GetFileMd5(filepath)
	logs.Info("download file md5: ", fileMd5)
	if err != nil {
		logs.Error("check file md5 failed", err)
		return "", errors.New("check file md5 failed")
	}

	checksumMd5 := resp.Header.Get("X-Checksum-Md5")
	logs.Info("checksum md5: ", checksumMd5)
	if len(checksumMd5) > 0 && checksumMd5 != fileMd5 {
		return "", errors.New("file md5 not match")
	}

	return fileMd5, err
}

func writeToFile(file string, content io.Reader) error {
	out, err := os.Create(file)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, content)
	if err != nil {
		logs.Error("save file failed", err)
		return err
	}
	return nil
}
