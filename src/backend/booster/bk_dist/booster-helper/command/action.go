/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at http://opensource.org/licenses/MIT
 *
 */

package command

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"net/http"
	"os"
	"sort"
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/compress"
	Types "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"

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
	data, err := doGetSetting(c, false)
	printData(data, c, err)
	return nil
}

func doGetSetting(c *commandCli.Context, selectorEnable bool) (*ProjectInfo, error) {
	projectID, err := getProjectID(c)
	if err != nil {
		return nil, err
	}
	var url string
	if projectID != "" {
		url = gatewayHost + GetProjectSettingURI + projectID
	} else {
		url = gatewayHost + GetProjectListURI
		if selectorEnable {
			url += ProjectSelector
		}
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
		fmt.Printf("project : (%s) decode error:%v \n", projectID, err)
		return nil, ErrDecode
	}
	if len(info.Setting) < 1 {
		fmt.Printf("project : (%s) not found \n", projectID)
		return nil, ErrProjectNotFound
	}

	return &info, nil
}

func getProjectID(c *commandCli.Context) (string, error) {
	if !c.IsSet(FlagProjectID) {
		return "", nil
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

	for _, info := range data.Setting {
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
		fmt.Printf("\n")
	}
}

func getCPUStats(c *commandCli.Context) error {
	runtimeDir := dcUtil.GetRuntimeDir()
	projectList, err := doGetSetting(c, true)
	if err != nil {
		return err
	}
	t := time.Now().Format("2006-01-02")
	filepath := runtimeDir + "/cpustats/cpu-stats-" + t
	f, _ := os.OpenFile(filepath, os.O_WRONLY|os.O_CREATE|os.O_SYNC|os.O_APPEND, 0755)

	os.Stdout = f
	defer f.Close()

	for _, info := range projectList.Setting {
		projectID := info.ProjectID
		cpustats, err := calculateCpuStats(c, projectID)
		if cpustats == nil || err != nil {
			continue
		}
	}
	printHeader()
	printCpuStats(c, projectNeedIncreaseCpu, "projects need to increase request_cpu:")
	printCpuStats(c, projectNeedDecreaseCpu, "projects need to decrease request_cpu:")
	printCpuStats(c, projectCpuReasonable, "projects' request_cpu reasonable:")
	return nil
}

func calculateCpuStats(c *commandCli.Context, projectID string) (*CpuStats, error) {
	tasklist, err := getTaskList(c, projectID)
	if err != nil {
		return nil, err
	}
	if tasklist == nil || len(tasklist.Tasks) == 0 {
		return nil, nil
	}

	stats := &CpuStats{
		projectID:      projectID,
		requestCpu:     tasklist.Tasks[0].RequestCPU,
		taskSum:        len(tasklist.Tasks),
		taskDistribute: make([][]TaskInfo, 6),
		validTaskSum:   0,
	}
	for _, task := range tasklist.Tasks {
		if task.Status != "finish" {
			continue
		}
		maxConcurrency, err := calculateConcurrency(task)
		if err != nil {
			continue
		}
		taskinfo := TaskInfo{
			taskID:         task.TaskID,
			maxConcurrency: maxConcurrency,
		}
		requestCpu := task.RequestCPU
		if requestCpu == 0 {
			continue
		}
		cpuUseLevel := int(5 * (maxConcurrency / requestCpu))
		if cpuUseLevel > 5 {
			cpuUseLevel = 5
		}
		stats.taskDistribute[cpuUseLevel] = append(stats.taskDistribute[cpuUseLevel], taskinfo)
	}
	for i := range stats.taskDistribute {
		stats.validTaskSum += len(stats.taskDistribute[i])
	}
	if float64(len(stats.taskDistribute[5])+len(stats.taskDistribute[4])) < float64(stats.validTaskSum)*0.2 && stats.validTaskSum > 10 {
		projectNeedDecreaseCpu = append(projectNeedDecreaseCpu, *stats)
	} else if float64(len(stats.taskDistribute[5])) >= float64(stats.validTaskSum)*0.5 && stats.validTaskSum > 10 {
		projectNeedIncreaseCpu = append(projectNeedIncreaseCpu, *stats)
	} else {
		projectCpuReasonable = append(projectCpuReasonable, *stats)
	}
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
		return nil, nil
	}
	return &tasklist, nil
}

func calculateConcurrency(task disttask.TableTask) (float64, error) {
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
	maxConcurrency := getMaxConcurrency(jobtime)
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

func getMaxConcurrency(jobtime []JobTime) float64 {
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

func printCpuStats(c *commandCli.Context, statsList []CpuStats, title string) {
	fmt.Println(title)
	for _, stats := range statsList {
		fmt.Printf("%-20s %s\n", "project_id: ", stats.projectID)
		fmt.Printf("%-20s %f\n", "request_cpu: ", stats.requestCpu)
		fmt.Printf("%-20s %d\n", "task_sum: ", stats.taskSum)
		fmt.Printf("%-20s %d\n", "valid_task_sum: ", stats.validTaskSum)
		fmt.Printf("%-20s%-10s%-10s%-10s%-10s%-10s%-10s\n", "cpu used percent", "0~20%", "20-40%", "40-60%", "60-80%", "80~100%", "over 100%")
		fmt.Printf("%-20s", "task num:")
		for _, taskinfo := range stats.taskDistribute {
			fmt.Printf("%-10d", len(taskinfo))
		}
		fmt.Printf("\n")
		if c.IsSet(FlagAllInfo) {
			for _, info := range stats.taskDistribute {
				showAllTask(info)
			}
		}
		fmt.Printf("\n")
	}
	fmt.Printf("\n")
}

func showAllTask(tasklist []TaskInfo) {
	for _, task := range tasklist {
		fmt.Printf("%-25s %f\n", task.taskID, task.maxConcurrency)
	}
	fmt.Printf("\n")
}

func printHeader() {
	fmt.Printf("%d projects need to increase request_cpu:\n", len(projectNeedIncreaseCpu))
	fmt.Printf("%d projects need to decrease request_cpu:\n", len(projectNeedDecreaseCpu))
	fmt.Printf("\n")
}
