/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package command

// ClientType define client type
type ClientType string

// define vars
var (
	ClientBKUBTTool ClientType = "bk-ubt-tool"

	ControllerScheme = "http"
	ControllerIP     = "127.0.0.1"
	ControllerPort   = 30117
)

// const vars
const (
	ClientBKUBTToolUsage = "BlueKing ubt tool"
)

// Name return client name
func (ct ClientType) Name() string {
	switch ct {
	case ClientBKUBTTool:
		return string(ct)
	}
	return "unknown"
}

// Usage return client usage
func (ct ClientType) Usage() string {
	switch ct {
	case ClientBKUBTTool:
		return ClientBKUBTToolUsage
	}
	return "unknown"
}
