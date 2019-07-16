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

package api

import (
	"pkg/config"
	"testing"
	"time"
)

func loadConfig() {
	config.LoadAgentConfig()
	config.LoadAgentEnv()
}

func Test_buildUrl_01(t *testing.T) {
	loadConfig()
	url := buildUrl("/abc")
	t.Log("url: ", url)
}

func Test_CheckUpgrade_01(t *testing.T) {
	loadConfig()
	data, err := CheckUpgrade()
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("data: ", data)
}

func Test_AgentStartup_01(t *testing.T) {
	loadConfig()
	data, err := AgentStartup()
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("data: ", data)
}

func Test_GetAgentStatus_01(t *testing.T) {
	loadConfig()
	data, err := GetAgentStatus()
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("data: ", data)
}

func Test_GetBuild_01(t *testing.T) {
	loadConfig()
	data, err := GetBuild()
	if err != nil {
		t.Error("err: ", err.Error())
		return
	}
	t.Log("data: ", data)
}
