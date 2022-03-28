/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package httpserver

import (
	"github.com/emicklei/go-restful"
)

// Action restful action struct.
type Action struct {
	Verb    string               // Verb identifying the action ("GET", "POST", "WATCH", PROXY", etc).
	Path    string               // The path of the action
	Params  []*restful.Parameter // List of parameters associated with the action.
	Handler restful.RouteFunction
}

// NewAction get a new action.
func NewAction(verb, path string, params []*restful.Parameter, handler restful.RouteFunction) *Action {
	return &Action{
		Verb:    verb,
		Path:    path,
		Params:  params,
		Handler: handler,
	}
}
