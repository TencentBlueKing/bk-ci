/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"build-booster/bk_dist/common/sdk"
	"build-booster/bk_dist/common/types"
	"build-booster/common/blog"
	"build-booster/common/compress"
	Types "build-booster/common/types"
	"build-booster/server/pkg/engine/disttask"
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"sort"
	"strconv"
	"strings"
	"time"

	commandCli "github.com/urfave/cli"
)

// Action return command actions
func Action(c *commandCli.Context) error {
	if !c.IsSet(FlagUseTestAdderss) {
		gatewayHost = ProdBuildBoosterGatewayHost
	} else {
		gatewayHost = TestBuildBoosterGatewayHost
	}
	return action(c)
}

func action(c *commandCli.Context) error {
	switch c.Command.Name {
	case CommandGetConfig:
		return getConfig(c)
	case CommandGetWorkStats:
		return getCPUStats(c)
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
	projectID, err := getProjectId(c)
	if err != nil {
		return nil, err
	}

	url := fmt.Sprint(gatewayHost, GetProjectSettingURI, projectID)

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

func getProjectId(c *commandCli.Context) (string, error) {
	if !c.IsSet(FlagProjectID) {
		return "", ErrProjectidMissed
	}
	projectID := c.String(FlagProjectID)

	if !c.IsSet(FlagBoosterType) {
		return "", ErrBoosterTypeMissed
	}
	if types.GetBoosterType(c.String(FlagBoosterType)) == types.BoosterUnknown {
		return "", ErrBoosterTypeWrong
	}
	scene := c.String(FlagBoosterType)
	return Types.GetProjectIDWithScene(projectID, scene), nil
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
	workerVersion := info.WorkerVersion
	pos := strings.Index(workerVersion, "-")
	if pos != -1 {
		Os = workerVersion[:pos]
		Compiler = workerVersion[pos+1:]
	}
	fmt.Printf("%-20s %s\n", "Os: ", Os)
	fmt.Printf("%-20s %s\n", "Compiler: ", Compiler)
	if c.IsSet(FlagAllInfo) {
		fmt.Printf("%-20s %s\n", "workerVersion: ", info.WorkerVersion)
		fmt.Printf("%-20s %s\n", "queue: ", info.QueueName)
		fmt.Printf("%-20s %s\n", "engine: ", info.EngineName)
		fmt.Printf("%-20s %d\n", "priority: ", info.Priority)
	}
}

func getCPUStats(c *commandCli.Context) error {
	projectID, err := getProjectId(c)
	if err != nil {
		return err
	}

	cpustats, err := getCpuStats(c, projectID)
	if err != nil {
		return err
	}
	printCpuStats(c, cpustats)
	return nil
}

func getCpuStats(c *commandCli.Context, projectID string) (*CpuStats, error) {
	tasklist, err := getTaskList(c, projectID)
	if err != nil {
		return nil, err
	}

	lowScope, err1 := strconv.ParseFloat(c.String(FlagLowScope), 64)
	upScope, err2 := strconv.ParseFloat(c.String(FlagUpScope), 64)
	if err1 != nil || err2 != nil || lowScope >= upScope {
		fmt.Println(ErrScopeFormatWrong)
		upScope = DefaultUpScope
		lowScope = DefaultLowScope
	}

	stats := &CpuStats{
		projectID:  projectID,
		requestCpu: tasklist.Tasks[0].RequestCPU,
		taskSum:    len(tasklist.Tasks),
		upScope:    upScope,
		lowScope:   lowScope,
	}
	for _, task := range tasklist.Tasks {
		if task.Status != "finish" {
			continue
		}
		maxConcurrency, err := caculateConcurrency(task, upScope, lowScope)
		if err != nil {
			continue
		}
		taskinfo := TaskInfo{
			taskID:         task.TaskID,
			maxConcurrency: maxConcurrency,
		}
		requestCpu := task.RequestCPU
		if maxConcurrency < requestCpu*lowScope {
			stats.excessTask = append(stats.excessTask, taskinfo)
		} else if maxConcurrency > requestCpu*upScope {
			stats.notEnoughTask = append(stats.notEnoughTask, taskinfo)
		} else {
			stats.reasonableTask = append(stats.reasonableTask, taskinfo)
		}
	}
	stats.validTaskSum = len(stats.excessTask) + len(stats.notEnoughTask) + len(stats.reasonableTask)
	return stats, nil
}

func getTaskList(c *commandCli.Context, projectID string) (*TaskList, error) {
	day := 0
	if c.IsSet(FlagDay) {
		day, _ = strconv.Atoi(c.String(FlagDay))
	}
	if day <= 0 {
		day = 1
	}
	if day >= 7 {
		day = 7
	}
	t := time.Now().Add(-time.Hour * 24 * time.Duration(day)).Unix()

	url := gatewayHost + GetTaskInfoURI + projectID + "&create_time_left=" + strconv.FormatInt(t, 10) + TaskSelector
	fmt.Printf("url:%s,day: %d\n", url, day)
	resp, err := http.Get(url)
	if err != nil {
		blog.Errorf("get failed :%v", err)
		return nil, ErrGetFailed
	}
	res, _ := ioutil.ReadAll(resp.Body)

	var tasklist TaskList
	err = json.Unmarshal(res, &tasklist)

	if err != nil {
		fmt.Println(err)
		return nil, ErrDecode
	}
	if len(tasklist.Tasks) == 0 {
		fmt.Printf("no task in this project in recent %d day \n", day)
		return nil, nil
	}
	return &tasklist, nil
}

func caculateConcurrency(task disttask.TableTask, upScope float64, lowScope float64) (float64, error) {
	worklist, err := getWorkList(task.TaskID)
	if err != nil || task.CPUTotal != task.RequestCPU || len(worklist) == 0 || !worklist[0].Success {
		return 0, ErrNotInStats
	}

	var jobtime []JobTime
	for _, work := range worklist {
		jobStatsData := string(compress.ToSourceCode(work.JobStats))
		var jobstats []sdk.ControllerJobStats
		err = json.Unmarshal([]byte(jobStatsData), &jobstats)
		if err != nil {
			fmt.Printf("err in work(%s),err:%v\n", work.WorkID, err)
		}
		for _, job := range jobstats {
			if job.RemoteWorkLockTime.Unix() >= task.StartTime && job.RemoteWorkUnlockTime.Unix() >= job.RemoteWorkLockTime.Unix() {
				jobtime = append(jobtime, JobTime{
					startTime: job.RemoteWorkLockTime.Unix() - task.StartTime,
					endTime:   job.RemoteWorkUnlockTime.Unix() - task.StartTime,
				})
			}
		}
	}

	maxConcurrency := getMaxccy(jobtime)
	return maxConcurrency, nil
}

func getWorkList(taskID string) ([]disttask.TableWorkStats, error) {
	url := gatewayHost + GetProjectWorkerStatsURI + taskID + "&selector=job_stats,success"

	resp, err := http.Get(url)
	if err != nil {
		blog.Errorf("get failed :%v", err)
		return nil, ErrGetFailed
	}
	res, _ := ioutil.ReadAll(resp.Body)

	var worklist WorkStats
	err = json.Unmarshal(res, &worklist)
	if err != nil {
		return nil, ErrDecode
	}
	if len(worklist.Works) == 0 {
		return nil, ErrNoWork
	}
	return worklist.Works, nil
}

func getMaxccy(jobtime []JobTime) float64 {
	maxConcurrency := 0
	var startTime, endTime []int64
	for _, t := range jobtime {
		startTime = append(startTime, t.startTime)
		endTime = append(endTime, t.endTime)
	}
	sort.Slice(startTime, func(i, j int) bool { return startTime[i] < startTime[j] })
	sort.Slice(endTime, func(i, j int) bool { return endTime[i] < endTime[j] })
	jobSum := 0
	var i, j int = 0, 0
	for i < len(jobtime) || j < len(jobtime) {
		if i == len(jobtime) {
			j++
			jobSum--
		} else if j == len(jobtime) {
			i++
			jobSum++
		} else {
			if startTime[i] < endTime[j] {
				i++
				jobSum++
			} else if startTime[i] > endTime[j] {
				j++
				jobSum--
			} else {
				i++
				j++
			}
		}
		if jobSum > maxConcurrency {
			maxConcurrency = jobSum
		}
	}
	return float64(maxConcurrency)
}

func printCpuStats(c *commandCli.Context, stats *CpuStats) {
	fmt.Printf("%-25s %s\n", "project_id: ", stats.projectID)
	fmt.Printf("%-25s %f\n", "request_cpu: ", stats.requestCpu)
	fmt.Printf("%-25s %d\n", "task_sum: ", stats.taskSum)
	fmt.Printf("%-25s %d\n", "valid_task_sum: ", stats.validTaskSum)
	fmt.Printf("%-25s %f\n", "upScope is set : ", stats.upScope)
	fmt.Printf("%-25s %f\n", "lowScope is set : ", stats.lowScope)
	fmt.Printf("%-25s %d\n", "cpu reasonable task_num: ", len(stats.reasonableTask))
	if c.IsSet(FlagAllInfo) {
		showAllTask(stats.reasonableTask)
	}
	fmt.Printf("%-25s %d\n", "too much cpu task_num: ", len(stats.excessTask))
	if c.IsSet(FlagAllInfo) {
		showAllTask(stats.excessTask)
	}
	fmt.Printf("%-25s %d\n", "too little cpu task_num: ", len(stats.notEnoughTask))
	if c.IsSet(FlagAllInfo) {
		showAllTask(stats.notEnoughTask)
	}
}

func showAllTask(tasklist []TaskInfo) {
	for _, task := range tasklist {
		fmt.Printf("%-25s %f\n", task.taskID, task.maxConcurrency)
	}
	fmt.Printf("\n")
}
