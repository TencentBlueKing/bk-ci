/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package disttask

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"sort"
	"strconv"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/types"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonMySQL "github.com/Tencent/bk-ci/src/booster/common/mysql"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine"
	"github.com/Tencent/bk-ci/src/booster/server/pkg/engine/disttask"

	"github.com/emicklei/go-restful"
)

// ListClientVersion handle the http request for listing client version
func ListClientVersion(req *restful.Request, resp *restful.Response) {
	filePath := "./data/VersionList.txt"
	f, err := ioutil.ReadFile(filePath)
	if err != nil {
		blog.Error("List Version error:(%v)", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, Message: err.Error()})
	}
	s := string(f)
	if strings.HasSuffix(s, "\n") {
		s = s[:len(s)-1]
	}
	result := strings.Split(s, "\n")

	sort.Sort(sort.Reverse(sort.StringSlice(result)))
	api.ReturnRest(&api.RestResponse{Resp: resp, Data: result})
}

// ListWorkerImages handle the http request for listing worker images
func ListWorkerImages(req *restful.Request, resp *restful.Response) {
	args := req.Request.URL.Query()
	queueName := args.Get("queue_name")

	filePath := "./data/DisttaskWorkerList.json"

	result := commonTypes.WorkerImage{
		Mesos: make([]commonTypes.Image, 0, 100),
		K8s:   make([]commonTypes.Image, 0, 100),
	}
	data, _ := ioutil.ReadFile(filePath)
	err := json.Unmarshal([]byte(data), &result)
	if err != nil {
		api.ReturnRest(&api.RestResponse{Resp: resp, Message: err.Error()})
	}

	switch queueName {
	case "shenzhen", "shanghai", "chengdu", "tianjin":
		api.ReturnRest(&api.RestResponse{Resp: resp, Data: result.Mesos})
	case "K8S://gd":
		api.ReturnRest(&api.RestResponse{Resp: resp, Data: result.K8s})
	default:
		message := fmt.Sprintf("unknown queue name : %s", queueName)
		api.ReturnRest(&api.RestResponse{Resp: resp, Message: message})
	}
}

// ListTask handle the http request for listing task with conditions.
func ListTask(req *restful.Request, resp *restful.Response) {
	opts, err := getListOptions(req, "TASK")
	if err != nil {
		blog.Errorf("list task get options failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	taskList, length, err := defaultMySQL.ListTask(opts)
	if err != nil {
		blog.Errorf("list task failed opts(%v): %v", opts, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrListTaskFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: taskList, Extra: map[string]interface{}{"length": length}})
}

// ListWorkStats handle the http request for listing work stats with conditions.
func ListWorkStats(req *restful.Request, resp *restful.Response) {
	opts, err := getListOptions(req, "WORK_STATS")
	if err != nil {
		blog.Errorf("list work stats get options failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	wsList, length, err := defaultMySQL.ListWorkStats(opts)
	if err != nil {
		blog.Errorf("list work stats failed opts(%v): %v", opts, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrListWorkStatsFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: wsList, Extra: map[string]interface{}{"length": length}})
}

// ListProject handle the http request for listing project with conditions.
func ListProject(req *restful.Request, resp *restful.Response) {
	opts, err := getListOptions(req, "PROJECT")
	if err != nil {
		blog.Errorf("list project get options failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	projectList, length, err := defaultMySQL.ListProject(opts)
	if err != nil {
		blog.Errorf("list project failed opts(%v): %v", opts, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrListProjectFailed,
			Message: err.Error()})
		return
	}

	var result []map[string]interface{}
	for _, project := range projectList {
		item := make(map[string]interface{}, 1000)
		for k, v := range wrapMap(project.TableProjectSetting) {
			item[k] = v
		}
		for k, v := range wrapMap(project.TableProjectInfo) {
			item[k] = v
		}
		result = append(result, item)
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: result, Extra: map[string]interface{}{"length": length}})
}

// UpdateProject handle the http request for updating project with some fields.
func UpdateProject(req *restful.Request, resp *restful.Response) {
	req.SetAttribute(api.QueryProjectIDKey, req.PathParameter(api.QueryProjectIDKey))
	req.SetAttribute(querySceneKey, req.PathParameter(querySceneKey))
	updateProject(req, resp)
}

func updateProject(req *restful.Request, resp *restful.Response) {
	var projectType UpdateProjectType
	rawBody, err := ioutil.ReadAll(req.Request.Body)
	if err != nil {
		blog.Errorf("update project read data from request body failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if err = projectType.Load(rawBody); err != nil {
		blog.Errorf("update project load data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if projectType.Operator == "" {
		blog.Errorf("update project failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	projectID, _ := req.Attribute(api.QueryProjectIDKey).(string)
	scene, _ := req.Attribute(querySceneKey).(string)
	if scene != "" {
		projectID = commonTypes.GetProjectIDWithScene(projectID, scene)
		projectType.Data.Scene = scene
		projectType.RawData["project_id"] = projectID
		projectType.RawData["scene"] = scene
	}
	projectType.Data.ProjectID = projectID
	projectType.RawData["project_id"] = projectID

	if err := projectType.CheckData(); err != nil {
		blog.Errorf("update project failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	var record []byte
	_ = codec.EncJSON(projectType.RawData, &record)
	blog.Infof("receive a project update: ID(%s) Operator(%s) Data: %s",
		projectID, projectType.Operator, string(record))
	if err := defaultMySQL.CreateOrUpdateProjectSetting(
		&projectType.Data.TableProjectSetting, projectType.RawData); err != nil {
		blog.Errorf("update project failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrUpdateProjectFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// DeleteProject handle the http request for deleting project.
func DeleteProject(req *restful.Request, resp *restful.Response) {
	req.SetAttribute(api.QueryProjectIDKey, req.PathParameter(api.QueryProjectIDKey))
	req.SetAttribute(querySceneKey, req.PathParameter(querySceneKey))
	deleteProject(req, resp)
}

func deleteProject(req *restful.Request, resp *restful.Response) {
	var projectType DeleteProjectType
	if err := codec.DecJSONReader(req.Request.Body, &projectType); err != nil {
		blog.Errorf("delete project get data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if projectType.Operator == "" {
		blog.Errorf("delete project failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	projectID, _ := req.Attribute(api.QueryProjectIDKey).(string)
	scene, _ := req.Attribute(querySceneKey).(string)
	if scene != "" {
		projectID = commonTypes.GetProjectIDWithScene(projectID, scene)
	}

	blog.Infof("receive a project delete: ID(%s) Operator(%s)", projectID, projectType.Operator)
	if err := defaultMySQL.DeleteProjectSetting(projectID); err != nil {
		blog.Errorf("delete project failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrDeleteProjectFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// ListWhitelist handle the http request for listing whitelist with conditions.
func ListWhitelist(req *restful.Request, resp *restful.Response) {
	opts, err := getListOptions(req, "WHITELIST")
	if err != nil {
		blog.Errorf("list whitelist get options failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	whitelistList, _, err := defaultMySQL.ListWhitelist(opts)
	if err != nil {
		blog.Errorf("list whitelist failed opts(%v): %v", opts, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrListWhiteListFailed,
			Message: err.Error()})
		return
	}
	for _, wl := range whitelistList {
		if wl.ProjectID == engine.WhiteListAllProjectID {
			wl.ProjectID = ""
		}
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: whitelistList})
}

// UpdateWhitelist handle the http request for updating whitelist with full fields.
func UpdateWhitelist(req *restful.Request, resp *restful.Response) {
	var whitelistType UpdateWhitelistType
	if err := codec.DecJSONReader(req.Request.Body, &whitelistType); err != nil {
		blog.Errorf("update whitelist get data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if whitelistType.Operator == "" {
		blog.Errorf("update whitelist failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	scene, _ := req.Attribute(querySceneKey).(string)

	whiteList := whitelistType.Data
	for _, wl := range whiteList {
		wl.ProjectID = commonTypes.GetProjectIDWithScene(wl.ProjectID, scene)

		if err := wl.CheckData(); err != nil {
			blog.Errorf("update whitelist check data failed: %v", err)
			api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
				Message: err.Error()})
			return
		}
	}

	var record []byte
	_ = codec.EncJSON(whiteList, &record)
	blog.Infof("receive a whitelist update: Operator(%s) Data: %s", whitelistType.Operator, record)

	if err := defaultMySQL.PutWhitelist(whiteList); err != nil {
		blog.Errorf("update whitelist failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrUpdateWhiteListFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// DeleteWhitelist handle the http request for deleting whitelist.
func DeleteWhitelist(req *restful.Request, resp *restful.Response) {
	var whitelistType DeleteWhitelistType
	if err := codec.DecJSONReader(req.Request.Body, &whitelistType); err != nil {
		blog.Errorf("delete whitelist get data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if whitelistType.Operator == "" {
		blog.Errorf("delete whitelist failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	scene, _ := req.Attribute(querySceneKey).(string)

	keys := whitelistType.Data
	for _, key := range keys {
		key.ProjectID = commonTypes.GetProjectIDWithScene(key.ProjectID, scene)
		if key.ProjectID == "" {
			key.ProjectID = engine.WhiteListAllProjectID
		}
	}

	var record []byte
	_ = codec.EncJSON(keys, &record)
	blog.Infof("receive a whitelist delete: Operator(%s) Data: %s", whitelistType.Operator, record)

	if err := defaultMySQL.DeleteWhitelist(keys); err != nil {
		blog.Errorf("delete whitelist failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrDeleteWhiteListFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// ListWorker handle the http request for listing worker with conditions.
func ListWorker(req *restful.Request, resp *restful.Response) {
	opts, err := getListOptions(req, "WORKER")
	if err != nil {
		blog.Errorf("list worker get options failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	workerList, _, err := defaultMySQL.ListWorker(opts)
	if err != nil {
		blog.Errorf("list worker failed opts(%v): %v", opts, err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrListWorkerFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp, Data: workerList})
}

// UpdateWorker handle the http request for updating worker with full fields.
func UpdateWorker(req *restful.Request, resp *restful.Response) {
	req.SetAttribute(queryWorkerVersionKey, req.PathParameter(queryWorkerVersionKey))
	req.SetAttribute(querySceneKey, req.PathParameter(querySceneKey))
	updateWorker(req, resp)
}

func updateWorker(req *restful.Request, resp *restful.Response) {
	var workerType UpdateWorkerType
	if err := codec.DecJSONReader(req.Request.Body, &workerType); err != nil {
		blog.Errorf("update worker get data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if workerType.Operator == "" {
		blog.Errorf("update worker failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	worker := workerType.Data
	worker.WorkerVersion, _ = req.Attribute(queryWorkerVersionKey).(string)
	worker.Scene, _ = req.Attribute(querySceneKey).(string)
	if err := worker.CheckData(); err != nil {
		blog.Errorf("update worker failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	blog.Infof("receive a worker update: Operator(%s) workerVersion(%s)",
		workerType.Operator, worker.WorkerVersion)

	if err := defaultMySQL.PutWorker(&worker); err != nil {
		blog.Errorf("update worker failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrUpdateWorkerFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

// DeleteWorker handle the http request for deleting worker.
func DeleteWorker(req *restful.Request, resp *restful.Response) {
	req.SetAttribute(queryWorkerVersionKey, req.PathParameter(queryWorkerVersionKey))
	req.SetAttribute(querySceneKey, req.PathParameter(querySceneKey))
	deleteWorker(req, resp)
}

func deleteWorker(req *restful.Request, resp *restful.Response) {
	var workerType DeleteWorkerType
	if err := codec.DecJSONReader(req.Request.Body, &workerType); err != nil {
		blog.Errorf("delete worker get data failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam, Message: err.Error()})
		return
	}

	if workerType.Operator == "" {
		blog.Errorf("delete worker failed: operator not specific")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrOperatorNoSpecific,
			Message: "operator no specific"})
		return
	}

	workerVersion, _ := req.Attribute(queryWorkerVersionKey).(string)
	scene, _ := req.Attribute(querySceneKey).(string)
	blog.Infof("receive a worker delete: workerVersion(%s) scene(%s) Operator(%s)",
		workerVersion, scene, workerType.Operator)
	if err := defaultMySQL.DeleteWorker(workerVersion, scene); err != nil {
		blog.Errorf("delete worker failed: %v", err)
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrDeleteWorkerFailed,
			Message: err.Error()})
		return
	}

	api.ReturnRest(&api.RestResponse{Resp: resp})
}

func getListOptions(req *restful.Request, resource string) (commonMySQL.ListOptions, error) {
	opts := commonMySQL.NewListOptions()

	value := req.Request.URL.Query()

	// set offset of list options, default is 0
	offset, _ := strconv.Atoi(value.Get(api.QueryOffsetKey))
	opts.Offset(offset)

	// set limit of list options, default is 1000
	limit, _ := strconv.Atoi(value.Get(api.QueryLimitKey))
	if limit == 0 {
		limit = 1000
	}
	opts.Limit(limit)

	// set selector of list options
	selector := value.Get(api.QuerySelectorKey)
	if selector != "" {
		opts.Select(strings.Split(selector, api.MultiSeparator))
	}

	// set order of list options
	order := value.Get(api.QueryOrderKey)
	if order != "" {
		opts.Order(strings.Split(order, api.MultiSeparator))
	}

	// set other query conditions
	for k, vl := range value {
		if len(vl) == 0 {
			continue
		}
		rv := vl[0]

		switch strings.ToUpper(resource) {
		case "TASK":
			if _, ok := listTaskInKey[k]; ok {
				v, err := parseType(k, rv, true)
				if err != nil {
					return opts, err
				}
				opts.In(k, v)
				continue
			}
			if _, ok := listTaskGtKey[k]; ok {
				v, err := parseType(k, rv, false)
				if err != nil {
					return opts, err
				}

				if _, ok = api.OriginKey[k]; !ok {
					continue
				}
				opts.Gt(api.OriginKey[k], v)
				continue
			}
			if _, ok := listTaskLtKey[k]; ok {
				v, err := parseType(k, rv, false)
				if err != nil {
					return opts, err
				}

				if _, ok = api.OriginKey[k]; !ok {
					continue
				}
				opts.Lt(api.OriginKey[k], v)
				continue
			}
		case "PROJECT":
			if _, ok := listProjectInKey[k]; ok {
				v, err := parseType(k, rv, true)
				if err != nil {
					return opts, err
				}
				opts.In(k, v)
				continue
			}
		case "WHITELIST":
			if _, ok := listWhitelistInKey[k]; ok {
				v, err := parseType(k, rv, true)
				if err != nil {
					return opts, err
				}
				opts.In(k, v)
				continue
			}
		case "WORKER":
			if _, ok := listWorkerInKey[k]; ok {
				v, err := parseType(k, rv, true)
				if err != nil {
					return opts, err
				}
				opts.In(k, v)
				continue
			}
		case "WORK_STATS":
			if _, ok := listWorkStatsInKey[k]; ok {
				v, err := parseType(k, rv, true)
				if err != nil {
					return opts, err
				}
				opts.In(k, v)
				continue
			}
		default:
			break
		}
	}
	return opts, nil
}

func parseType(k string, v string, isList bool) (interface{}, error) {
	if _, ok := intKey[k]; ok {
		if !isList {
			return strconv.Atoi(v)
		}
		r := make([]int, 0, 100)
		for _, vi := range strings.Split(v, api.MultiSeparator) {
			item, err := strconv.Atoi(vi)
			if err != nil {
				return nil, err
			}

			r = append(r, item)
		}
		return r, nil
	}

	if _, ok := int64Key[k]; ok {
		if !isList {
			return strconv.ParseInt(v, 10, 64)
		}
		r := make([]int64, 0, 100)
		for _, vi := range strings.Split(v, api.MultiSeparator) {
			item, err := strconv.ParseInt(vi, 10, 64)
			if err != nil {
				return nil, err
			}

			r = append(r, item)
		}
		return r, nil
	}

	if _, ok := boolKey[k]; ok {
		return v == "true", nil
	}

	if _, ok := float64Key[k]; ok {
		if !isList {
			return strconv.ParseFloat(v, 64)
		}
		r := make([]float64, 0, 100)
		for _, vi := range strings.Split(v, api.MultiSeparator) {
			item, err := strconv.ParseFloat(vi, 64)
			if err != nil {
				return nil, err
			}

			r = append(r, item)
		}
		return r, nil
	}

	if !isList {
		return v, nil
	}
	return strings.Split(v, api.MultiSeparator), nil
}

// OperatorType describe the http request's operator.
type OperatorType struct {
	Operator string `json:"operator"`
}

// UpdateProjectSettingShell get project settings and add some extra message
type UpdateProjectSettingShell struct {
	disttask.TableProjectSetting

	CCacheEnable bool `json:"ccache_enabled"`
}

// UpdateProjectType describe the param of http request to update project.
type UpdateProjectType struct {
	OperatorType
	Data    UpdateProjectSettingShell `json:"data"`
	RawData map[string]interface{}    `json:"-"`
}

// Load UpdateProjectType from http body bytes
func (upt *UpdateProjectType) Load(rawBody []byte) error {
	if err := codec.DecJSON(rawBody, upt); err != nil {
		return err
	}

	var rawMap map[string]interface{}
	_ = codec.DecJSON(rawBody, &rawMap)
	data := rawMap["data"]
	if data == nil {
		return fmt.Errorf("no data specified, nothing to do")
	}

	var tmp []byte
	_ = codec.EncJSON(data, &tmp)
	_ = codec.DecJSON(tmp, &upt.RawData)

	return nil
}

// CheckData check if the data is valid.
func (upt *UpdateProjectType) CheckData() error {
	// check project_id can not be empty
	if upt.RawData["project_id"] == "" {
		return fmt.Errorf("projectID empty")
	}

	if _, ok := upt.RawData["request_cpu"].(string); ok {
		delete(upt.RawData, "request_cpu")
	}

	if upt.Data.RequestCPU > 0 {
		upt.RawData["request_cpu"] = upt.Data.RequestCPU
	}

	if _, ok := upt.RawData["least_cpu"].(string); ok {
		delete(upt.RawData, "least_cpu")
	}

	if upt.Data.LeastCPU > 0 {
		upt.RawData["least_cpu"] = upt.Data.LeastCPU
	}

	switch upt.Data.Scene {
	case types.BoosterCC.String():

		// 兼容ccache_enabled在upsert project时打平的需求
		if v, ok := upt.RawData["ccache_enabled"]; ok {
			if t, ok := v.(bool); ok {
				extra := fmt.Sprintf("{\"ccache_enabled\":%t}", t)
				upt.Data.Extra = extra
				upt.RawData["extra"] = extra
			}

			delete(upt.RawData, "ccache_enabled")
		}
	}

	upt.Data.EngineName = disttask.EngineName
	upt.RawData["engine_name"] = disttask.EngineName
	return nil
}

// DeleteProjectType describe the param of http request to delete project.
type DeleteProjectType struct {
	OperatorType
}

// UpdateWhitelistType describe the param of http request to update whitelist.
type UpdateWhitelistType struct {
	OperatorType
	Data []*disttask.TableWhitelist `json:"data"`
}

// DeleteWhitelistType describe the param of http request to delete whitelist.
type DeleteWhitelistType struct {
	OperatorType
	Data []*engine.WhiteListKey `json:"data"`
}

// UpdateWorkerType describe the param of http request to update worker.
type UpdateWorkerType struct {
	OperatorType
	Data disttask.TableWorker `json:"data"`
}

// DeleteWorkerType describe the param of http request to delete worker.
type DeleteWorkerType struct {
	OperatorType
}

func wrapMap(source interface{}) map[string]interface{} {
	var tmp []byte
	_ = codec.EncJSON(source, &tmp)

	data := make(map[string]interface{}, 1000)
	_ = codec.DecJSON(tmp, &data)
	return data
}
