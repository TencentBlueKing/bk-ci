package api

import (
	"github.com/Tencent/bk-ci/src/booster/common/http/httpserver"
)

const (
	prefix    = "/api/"
	VersionV1 = "v1"

	PathV1 = prefix + VersionV1
)

// Action describe the http action
type Action httpserver.Action

var apiV1actions = make([]*httpserver.Action, 0, 100)

// Register a handler into v1 actions
// means all the URL of these handlers are start with PathV1
func RegisterV1Action(action Action) {
	apiV1actions = append(apiV1actions, httpserver.NewAction(action.Verb, action.Path, action.Params, action.Handler))
}

// Get V1 actions
func GetAPIV1Action() []*httpserver.Action {
	return apiV1actions
}

// InitActionsFunc init all actions registered in initFunc
func InitActionsFunc() error {
	for _, f := range initFunc {
		if err := f(); err != nil {
			return err
		}
	}
	return nil
}

var initFunc = make([]func() error, 0, 10)

// called by actions for registering some daemon functions
// and these functions will be called after flag-init and server-start
func RegisterInitFunc(f func() error) {
	initFunc = append(initFunc, f)
}
