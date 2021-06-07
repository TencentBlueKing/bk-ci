/*
Copyright 2017 Authors All rights reserved
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
