/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package api

import (
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
)

const (
	prefix    = "/api/"
	versionV1 = "v1"

	// PathV1 describe the prefix of version 1 http action.
	PathV1 = prefix + versionV1
)

// InitActionsFunc will call all the functions in initFunc
func InitActionsFunc() error {
	for _, f := range initFunc {
		if err := f(); err != nil {
			return err
		}
	}
	return nil
}

var initFunc = make([]func() error, 0, 10)

// RegisterInitFunc called by actions for registering some daemon functions
// and these functions will be called after flag-init and server-start
func RegisterInitFunc(f func() error) {
	initFunc = append(initFunc, f)
}

// Action describe the http handler action.
type Action httpserver.Action

var (
	apiV1actions = make([]*httpserver.Action, 0, 100)
)

// RegisterV1Action register a handler into v1 actions
// means all the URL of these handlers are start with PathV1
func RegisterV1Action(action Action) {
	apiV1actions = append(apiV1actions, httpserver.NewAction(action.Verb, action.Path, action.Params, action.Handler))
}

// GetAPIV1Action get V1 actions
func GetAPIV1Action() []*httpserver.Action {
	return apiV1actions
}
