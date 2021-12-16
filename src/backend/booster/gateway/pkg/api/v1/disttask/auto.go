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
	"net/url"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	commonTypes "github.com/Tencent/bk-ci/src/booster/common/types"
	"github.com/Tencent/bk-ci/src/booster/gateway/pkg/api"

	"github.com/emicklei/go-restful"
)

const (
	autoScenePathKey = "auto_scene"
)

// AutoListTask 自动从url中获取并补全scene, 然后list task
func AutoListTask(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoListTask with scene(%s)", scene)
	ensureQueryProjectID(req, scene)
	ListTask(req, resp)
}

// AutoListWorkStats 自动从url中获取并补全scene, 然后list stats
func AutoListWorkStats(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoListWorkStats with scene(%s)", scene)
	ensureQueryProjectID(req, scene)
	ListWorkStats(req, resp)
}

// AutoListProject 自动从url中获取并补全scene, 然后list project
func AutoListProject(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoListProject with scene(%s)", scene)
	ensureQueryProjectID(req, scene)
	ListProject(req, resp)
}

// AutoUpdateProject 自动从url中获取并补全scene, 然后update project
func AutoUpdateProject(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoUpdateProject with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	updateProject(req, resp)
}

// AutoDeleteProject 自动从url中获取并补全scene, 然后delete project
func AutoDeleteProject(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoDeleteProject with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	deleteProject(req, resp)
}

// AutoDeleteProject 自动从url中获取并补全scene, 然后delete project
func AutoListWhitelist(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoListWhitelist with scene(%s)", scene)
	ensureQueryProjectID(req, scene)
	ListWhitelist(req, resp)
}

// AutoUpdateWhitelist 自动从url中获取并补全scene, 然后update whitelist
func AutoUpdateWhitelist(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoUpdateWhitelist with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	UpdateWhitelist(req, resp)
}

// AutoDeleteWhitelist 自动从url中获取并补全scene, 然后delete whitelist
func AutoDeleteWhitelist(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoDeleteWhitelist with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	DeleteWhitelist(req, resp)
}

// AutoListWorker 自动从url中获取并补全scene, 然后list worker
func AutoListWorker(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoListWorker with scene(%s)", scene)
	ensureQueryProjectID(req, scene)
	ListWorker(req, resp)
}

// AutoUpdateWorker 自动从url中获取并补全scene, 然后update worker
func AutoUpdateWorker(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoUpdateWorker with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	updateWorker(req, resp)
}

// AutoDeleteWorker 自动从url中获取并补全scene, 然后delete worker
func AutoDeleteWorker(req *restful.Request, resp *restful.Response) {
	scene := getScene(req)
	if scene == "" {
		blog.Errorf("get scene from path failed: empty scene")
		api.ReturnRest(&api.RestResponse{Resp: resp, ErrCode: commonTypes.ServerErrInvalidParam,
			Message: "empty scene"})
		return
	}
	blog.Infof("request to AutoDeleteWorker with scene(%s)", scene)
	ensurePutOrDeleteProjectID(req, scene)
	deleteWorker(req, resp)
}

func getScene(req *restful.Request) string {
	autoScene := req.PathParameter(autoScenePathKey)
	return strings.TrimPrefix(autoScene, "disttask-")
}

func ensureQueryProjectID(req *restful.Request, scene string) {
	raw, _ := url.ParseQuery(req.Request.URL.RawQuery)

	if projectID := req.QueryParameter(api.QueryProjectIDKey); projectID != "" {
		raw[api.QueryProjectIDKey] = []string{commonTypes.GetProjectIDWithScene(projectID, scene)}
	}

	raw[querySceneKey] = []string{scene}

	req.Request.URL.RawQuery = raw.Encode()
	_ = req.Request.ParseForm()
}

func ensurePutOrDeleteProjectID(req *restful.Request, scene string) {
	req.SetAttribute(api.QueryProjectIDKey, req.PathParameter(api.QueryProjectIDKey))
	req.SetAttribute(queryWorkerVersionKey, req.PathParameter(queryWorkerVersionKey))
	req.SetAttribute(querySceneKey, scene)
}
