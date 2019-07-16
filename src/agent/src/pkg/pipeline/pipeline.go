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

package pipeline

import (
	"bytes"
	"errors"
	"fmt"
	"github.com/astaxie/beego/logs"
	"io/ioutil"
	"os"
	"pkg/api"
	"pkg/config"
	"pkg/util"
	"pkg/util/command"
	"pkg/util/systemutil"
	"strings"
	"time"
)

func Start() {
	time.Sleep(10 * time.Second)
	for {
		runPipeline()
		time.Sleep(30 * time.Second)
	}
}

func parseAndRunCommandPipeline(pipelineData interface{}) (err error) {
	pipeline := new(CommandPipeline)
	err = util.ParseJsonToData(pipelineData, pipeline)
	if err != nil {
		return errors.New("parse command pipeline failed")
	}

	api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusExecuting, "Executing"))

	if strings.TrimSpace(pipeline.Command) == "" {
		api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, ""))
		return nil
	}

	lines := strings.Split(pipeline.Command, "\n")
	if len(lines) == 0 {
		api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, ""))
		return nil
	}

	buffer := bytes.Buffer{}
	if strings.HasPrefix(strings.TrimSpace(lines[0]), "#!") {
		buffer.WriteString(lines[0] + "\n")
		buffer.WriteString("cd " + systemutil.GetWorkDir() + "\n")
	} else {
		buffer.WriteString("#!/bin/bash\n\n")
		buffer.WriteString("cd " + systemutil.GetWorkDir() + "\n")
		buffer.WriteString(lines[0] + "\n")
	}

	for i := 1; i < len(lines); i++ {
		buffer.WriteString(lines[i] + "\n")
	}
	scriptContent := buffer.String()
	logs.Info("scriptContent:", scriptContent)

	scriptFile := fmt.Sprintf("%s/devops_pipeline_%s_%s.sh", config.GetAgentWorkdir(), pipeline.SeqId, pipeline.Type)
	err = ioutil.WriteFile(scriptFile, []byte(scriptContent), 0777)
	if err != nil {
		api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "write pipeline script file failed: "+err.Error()))
		return errors.New("write pipeline script file failed: " + err.Error())
	}
	defer os.Remove(scriptFile)

	output, err := command.RunCommand(scriptFile, []string{} /*args*/, config.GetAgentWorkdir(), nil)
	if err != nil {
		api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusFailure, "run pipeline failed: "+err.Error()))
		return errors.New("run pipeline failed: " + err.Error())
	}

	outputStr := string(output)
	logs.Info("script output: ", string(output))

	api.UpdatePipelineStatus(api.NewPipelineResponse(pipeline.SeqId, StatusSuccess, outputStr))
	return nil
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

	if pipelineData["type"] == COMMAND {
		err = parseAndRunCommandPipeline(pipelineData)
	} else {
		logs.Warn("not support pipeline: type: ", pipelineData["type"])
		return
	}

	if err != nil {
		logs.Error("run pipeline failed: ", err.Error())
	} else {
		logs.Info("run pipeline done")
	}
}
