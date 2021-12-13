/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package v1

import (
	"strconv"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/compress"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/dashboard/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"

	"github.com/emicklei/go-restful"
)

// ListTask handle the http request for listing tasks with conditions.
func ListTask(req *restful.Request, resp *restful.Response) {
	projectID := req.QueryParameter(pathQueryKeyProjectID)
	user := req.QueryParameter(pathQueryKeyUser)
	sourceIP := req.QueryParameter(pathQueryKeySourceIP)
	offset, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyOffset))
	limit, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyLimit))
	day, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyDay))

	if projectID == "" {
		blog.Errorf("api: list task with empty projectID")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam,
			Message: api.ServerErrInvalidParam.String()})
		return
	}

	if limit <= 0 {
		limit = 5
	}
	if limit > 100 {
		limit = 100
	}
	if day <= 0 {
		day = 1
	}
	if day > 7 {
		day = 7
	}
	opts := commonMySQL.NewListOptions()
	opts.Offset(offset)
	opts.Limit(limit)
	opts.Equal("project_id", projectID)
	opts.Gt("create_time", time.Now().Add(-time.Hour*24*time.Duration(day)).Unix())
	if user != "" {
		opts.In("user", strings.Split(user, ","))
	}
	if sourceIP != "" {
		opts.In("source_ip", strings.Split(sourceIP, ","))
	}
	opts.Order([]string{"-create_time"})
	opts.Select([]string{"task_id", "create_time", "start_time", "end_time", "user", "source_ip", "status"})

	workList, total, err := defaultMySQL.ListTask(opts)
	if err != nil {
		blog.Errorf("api: get work with projectID(%s) offset(%d) limit(%d) failed: %v",
			projectID, offset, limit, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkFailed, Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: workList, Extra: map[string]interface{}{"total": total}})
}

// GetTask handle the http request for getting task with conditions.
func GetTask(req *restful.Request, resp *restful.Response) {
	taskID := req.PathParameter(pathParamKeyTaskID)

	if taskID == "" {
		blog.Errorf("api: get task with empty task_id")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam,
			Message: api.ServerErrInvalidParam.String()})
		return
	}

	task, err := defaultMySQL.GetTask(taskID)
	if err != nil {
		blog.Errorf("api: get task(%s) failed: %v", taskID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetTaskFailed, Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: task})
}

// ListWorks handle the http request for listing works with conditions.
func ListWorks(req *restful.Request, resp *restful.Response) {
	projectID := req.QueryParameter(pathQueryKeyProjectID)
	offset, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyOffset))
	limit, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyLimit))
	day, _ := strconv.Atoi(req.QueryParameter(pathQueryKeyDay))

	if projectID == "" {
		blog.Errorf("api: list work with empty projectID")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam,
			Message: api.ServerErrInvalidParam.String()})
		return
	}

	if limit <= 0 {
		limit = 5
	}
	if limit > 100 {
		limit = 100
	}
	if day <= 0 {
		day = 1
	}
	if day > 7 {
		day = 7
	}
	opts := commonMySQL.NewListOptions()
	opts.Offset(offset)
	opts.Limit(limit)
	opts.Equal("project_id", projectID)
	opts.Gt("registered_time", time.Now().Add(-time.Hour*24*time.Duration(day)).UnixNano())
	opts.Order([]string{"-registered_time"})
	opts.Select([]string{"work_id", "registered_time", "start_time", "end_time", "success"})

	workList, total, err := defaultMySQL.ListWorkStats(opts)
	if err != nil {
		blog.Errorf("api: get work with projectID(%s) offset(%d) limit(%d) failed: %v",
			projectID, offset, limit, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkFailed, Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: workList, Extra: map[string]interface{}{"total": total}})
}

// GetWorkStats handle the http request for getting work with conditions.
func GetWorkStats(req *restful.Request, resp *restful.Response) {
	taskID := req.QueryParameter(pathQueryKeyTaskID)
	workID := req.QueryParameter(pathQueryKeyWorkID)
	decodeJob := req.QueryParameter(pathQueryKeyDecodeJob) != ""

	if taskID == "" && workID == "" {
		blog.Errorf("api: get work stats with empty task_id and work_id")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrInvalidParam,
			Message: api.ServerErrInvalidParam.String()})
		return
	}

	opts := commonMySQL.NewListOptions()
	opts.Limit(1)
	if taskID != "" {
		opts.Equal("task_id", taskID)
	}
	if workID != "" {
		opts.Equal("work_id", workID)
	}
	workList, _, err := defaultMySQL.ListWorkStats(opts)
	if err != nil {
		blog.Errorf("api: get work(%s)(%s) failed: %v", taskID, workID, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkFailed, Message: err.Error()})
		return
	}

	if len(workList) == 0 {
		blog.Errorf("api: get work(%s)(%s) failed: get empty list from db", taskID, workID)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: api.ServerErrGetWorkFailed, Message: "work not found"})
		return
	}

	data := &WorkStats{TableWorkStats: *workList[0]}
	if decodeJob {
		data.JobStatsData = string(compress.ToSourceCode(data.JobStats))
		data.JobStats = ""
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: data})
}

// WorkStats describe the stats info return to web
type WorkStats struct {
	disttask.TableWorkStats

	JobStatsData string `json:"job_stats_data"`
}
