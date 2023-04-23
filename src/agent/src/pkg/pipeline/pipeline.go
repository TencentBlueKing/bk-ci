//go:build !out
// +build !out

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

package pipeline

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"strings"
	"time"

	"github.com/pkg/errors"

	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/api"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/logs"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/command"
	"github.com/TencentBlueKing/bk-ci/src/agent/src/pkg/util/systemutil"
)

func Start() {
	defer func() {
		if err := recover(); err != nil {
			logs.Error("agent pipeline panic: ", err)
		}
	}()

	time.Sleep(10 * time.Second)
	for {
		runPipeline()
		time.Sleep(30 * time.Second)
	}
}

func runPipeline() {
	logs.Info("start run pipeline")

	result, err := api.GetAgentPipeline()
	if err != nil {
		logs.Error("get pipeline failed: ", err.Error())
		return
	}
	if result.IsNotOk() {
		logs.Error("get pipeline failed, message: ", result.Message)
		return
	}
	if result.Data == nil {
		logs.Info("no pipeline to run, skip")
		return
	}

	pipelineData, ok := result.Data.(map[string]interface{})
	if !ok {
		logs.Error("parse pipeline failed, invalid pipeline")
		return
	}

	if pipelineData["type"] != COMMAND {
		logs.Warn("not support pipeline: type: ", pipelineData["type"])
		return
	}

	pipeline, lines, err := parsePipelineData(pipelineData)
	if err != nil {
		logs.Error("run pipeline parsePipelineData error: ", err)
	}

	if systemutil.IsWindows() {
		err = runCommandPipelineWindows(pipeline, lines)
	} else {
		err = runCommandPipeline(pipeline, lines)
	}

	if err != nil {
		logs.Error("run pipeline failed: ", err)
	} else {
		logs.Info("run pipeline done")
	}
}

func parsePipelineData(pipelineData interface{}) (*CommandPipeline, []string, error) {
	pipeline := new(CommandPipeline)
	err := util.ParseJsonToData(pipelineData, pipeline)
	if err != nil {
		return nil, nil, errors.New("parse command pipeline failed")
	}

	_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusExecuting, "Executing"))

	if strings.TrimSpace(pipeline.Command) == "" {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, ""))
		return nil, nil, nil
	}

	lines := strings.Split(pipeline.Command, "\n")
	if len(lines) == 0 {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, ""))
		return nil, nil, nil
	}

	return pipeline, lines, nil
}

func runCommandPipeline(pipeline *CommandPipeline, lines []string) (err error) {
	buffer := bytes.Buffer{}
	if strings.HasPrefix(strings.TrimSpace(lines[0]), "#!") {
		buffer.WriteString(lines[0] + "\n")
		buffer.WriteString("set -e\n")
		buffer.WriteString("cd " + systemutil.GetWorkDir() + "\n")
	} else {
		buffer.WriteString("#!/bin/bash\n\n")
		buffer.WriteString("set -e\n")
		buffer.WriteString("cd " + systemutil.GetWorkDir() + "\n")
		buffer.WriteString(lines[0] + "\n")
	}

	for i := 1; i < len(lines); i++ {
		buffer.WriteString(lines[i] + "\n")
	}
	scriptContent := buffer.String()
	logs.Info("scriptContent:", scriptContent)

	scriptFile := fmt.Sprintf("%s/devops_pipeline_%s_%s.sh", systemutil.GetWorkDir(), pipeline.SeqId, pipeline.Type)
	err = ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "write pipeline script file failed: "+err.Error()))
		return errors.Wrap(err, "write pipeline script file failed")
	}
	defer os.Remove(scriptFile)

	output, err := command.RunCommand(scriptFile, []string{} /*args*/, systemutil.GetWorkDir(), nil)
	if err != nil {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "run pipeline failed: "+err.Error()+"\noutput: "+string(output)))
		return errors.Wrap(err, "run pipeline failed")
	}
	_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusSuccess, string(output)))
	return nil
}

func runCommandPipelineWindows(pipeline *CommandPipeline, lines []string) error {
	buffer := bytes.Buffer{}

	buffer.WriteString("@echo off\n")  // 关闭打印命令名称
	buffer.WriteString("chcp 65001\n") // UTF-8中文
	buffer.WriteString("cd " + systemutil.GetWorkDir() + "\n")

	for i := 0; i < len(lines); i++ {
		buffer.WriteString(lines[i] + "\n")
	}
	scriptContent := buffer.String()
	logs.Info("scriptContent:", scriptContent)

	scriptFile := fmt.Sprintf("%s/devops_pipeline_%s_%s.bat", systemutil.GetWorkDir(), pipeline.SeqId, pipeline.Type)
	err := ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "write pipeline script file failed: "+err.Error()))
		return errors.Wrap(err, "write pipeline script file failed")
	}
	defer os.Remove(scriptFile)

	output, err := command.RunCommand(scriptFile, []string{} /*args*/, systemutil.GetWorkDir(), nil)
	if err != nil {
		_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "run pipeline failed: "+err.Error()+"\noutput: "+string(output)))
		return errors.Wrap(err, "run pipeline failed")
	}
	_, _ = api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusSuccess, string(output)))

	return nil
}
