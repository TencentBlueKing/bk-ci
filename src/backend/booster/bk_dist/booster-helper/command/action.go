/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"build-booster/bk_dist/common/types"
	"build-booster/common/blog"
	Types "build-booster/common/types"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"strings"

	commandCli "github.com/urfave/cli"
)

// Action return command actions
func Action(c *commandCli.Context) error {
	return action(c)
}

func action(c *commandCli.Context) error {
	switch c.Command.Name {
	case CommandGetConfig:
		return getConfig(c)
	default:
		return fmt.Errorf("unknown command[%s]", c.Command.Name)
	}
}

func getConfig(c *commandCli.Context) error {
	data, err := doGetSetting(c)
	printData(data, c, err)
	return nil
}

func doGetSetting(c *commandCli.Context) (*ProjectInfo, error) {
	if !c.IsSet(FlagProjectID) {
		return nil, ErrProjectidMissed
	}
	projectID := c.String(FlagProjectID)

	if !c.IsSet(FlagBoosterType) {
		return nil, ErrBoosterTypeMissed
	}
	if types.GetBoosterType(c.String(FlagBoosterType)) == types.BoosterUnknown {
		return nil, ErrBoosterTypeWrong
	}

	scene := c.String(FlagBoosterType)
	projectID = Types.GetProjectIDWithScene(projectID, scene)

	var url string
	if !c.IsSet(FlagUseTestAdderss) {
		url = fmt.Sprint(ProdBuildBoosterGatewayHost, GetProjectSettingURI, projectID)
	} else {
		url = fmt.Sprint(TestBuildBoosterGatewayHost, GetProjectSettingURI, projectID)
	}

	resp, err := http.Get(url)
	if err != nil {
		blog.Errorf("get failed :%v", err)
		return nil, ErrGetFailed
	}
	res, _ := ioutil.ReadAll(resp.Body)

	var info ProjectInfo
	err = json.Unmarshal(res, &info)
	if err != nil {
		return nil, ErrDecode
	}
	if len(info.Setting) < 1 {
		return nil, ErrProjectNotFound
	}

	return &info, nil
}

func printData(data *ProjectInfo, c *commandCli.Context, err error) {
	if err != nil {
		fmt.Println(err)
		return
	}
	if data == nil || len(data.Setting) < 1 {
		fmt.Println("unknown error!")
		return
	}

	info := data.Setting[0]
	fmt.Printf("%-20s %s\n", "project_name: ", info.ProjectName)
	fmt.Printf("%-20s %s\n", "project_id: ", info.ProjectID)
	var Os string = "Unknown"
	var Compiler string = "Unknown"
	worker_version := info.WorkerVersion
	pos := strings.Index(worker_version, "-")
	if pos != -1 {
		Os = worker_version[:pos]
		Compiler = worker_version[pos+1:]
	}
	fmt.Printf("%-20s %s\n", "Os: ", Os)
	fmt.Printf("%-20s %s\n", "Compiler: ", Compiler)
	if c.IsSet(FlagAllInfo) {
		fmt.Printf("%-20s %s\n", "worker_version: ", info.WorkerVersion)
		fmt.Printf("%-20s %s\n", "queue: ", info.QueueName)
		fmt.Printf("%-20s %s\n", "engine: ", info.EngineName)
		fmt.Printf("%-20s %d\n", "priority: ", info.Priority)
	}
}
