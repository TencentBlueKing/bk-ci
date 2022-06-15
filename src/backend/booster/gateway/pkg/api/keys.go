/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package api

const (
	// multiSeparator is used for split keys from string
	MultiSeparator = ","

	QueryTaskIDKey          = "task_id"
	QueryProjectIDKey       = "project_id"
	QueryBuildIDKey         = "build_id"
	QueryStatusKey          = "status"
	QueryPriorityKey        = "priority"
	QueryCreateTimeLeftKey  = "create_time_left"
	QueryCreateTimeRightKey = "create_time_right"
	QueryCreateTimeKey      = "create_time"
	QueryStartTimeLeftKey   = "start_time_left"
	QueryStartTimeRightKey  = "start_time_right"
	QueryStartTimeKey       = "start_time"
	QueryEndTimeLeftKey     = "end_time_left"
	QueryEndTimeRightKey    = "end_time_right"
	QueryEndTimeKey         = "end_time"
	QueryProjectNameKey     = "project_name"
	QueryIPKey              = "ip"
	QueryStageTimeout       = "stage_timeout"
	QueryReleased           = "released"

	QueryOffsetKey   = "offset"
	QueryLimitKey    = "limit"
	QuerySelectorKey = "selector"
	QueryOrderKey    = "order"
)

var (
	// GroupListTaskInKey implements the keys that can be filtered with "In" during task list
	GroupListTaskInKey = map[string]bool{
		QueryTaskIDKey:    true,
		QueryProjectIDKey: true,
		QueryBuildIDKey:   true,
		QueryStatusKey:    true,
		QueryPriorityKey:  true,
		QueryReleased:     true,
	}

	// GroupListTaskGtKey implements the keys that can be filtered with "Gt" during task list
	GroupListTaskGtKey = map[string]bool{
		QueryCreateTimeLeftKey: true,
		QueryStartTimeLeftKey:  true,
		QueryEndTimeLeftKey:    true,
	}

	// GroupListTaskLtKey implements the keys that can be filtered with "Lt" during task list
	GroupListTaskLtKey = map[string]bool{
		QueryStartTimeRightKey:  true,
		QueryCreateTimeRightKey: true,
		QueryEndTimeRightKey:    true,
	}

	// GroupListProjectInKey implements the keys that can be filtered with "In" during project list
	GroupListProjectInKey = map[string]bool{
		QueryProjectIDKey:   true,
		QueryProjectNameKey: true,
		QueryPriorityKey:    true,
		QueryStageTimeout:   true,
	}

	// GroupListWhitelistInKey implements the keys that can be filtered with "In" during whitelist list
	GroupListWhitelistInKey = map[string]bool{
		QueryProjectIDKey: true,
		QueryIPKey:        true,
	}

	// GroupIntKey implements the int keys
	GroupIntKey = map[string]bool{
		QueryPriorityKey:  true,
		QueryStageTimeout: true,
	}

	// GroupInt64Key implements the int64 keys
	GroupInt64Key = map[string]bool{
		QueryCreateTimeLeftKey:  true,
		QueryStartTimeLeftKey:   true,
		QueryEndTimeLeftKey:     true,
		QueryStartTimeRightKey:  true,
		QueryCreateTimeRightKey: true,
		QueryEndTimeRightKey:    true,
	}

	// GroupBoolKey implements the bool keys
	GroupBoolKey = map[string]bool{
		QueryReleased: true,
	}

	// GroupFloat64Key implements the float64 keys
	GroupFloat64Key = map[string]bool{}

	// OriginKey implements the lt and gt origin key
	OriginKey = map[string]string{
		QueryCreateTimeLeftKey:  QueryCreateTimeKey,
		QueryCreateTimeRightKey: QueryCreateTimeKey,
		QueryStartTimeLeftKey:   QueryStartTimeKey,
		QueryStartTimeRightKey:  QueryStartTimeKey,
		QueryEndTimeLeftKey:     QueryEndTimeKey,
		QueryEndTimeRightKey:    QueryEndTimeKey,
	}
)

// WithBasicGroup combine the basic keys and the extra keys. Return a new key groups.
func WithBasicGroup(basic map[string]bool, extra map[string]bool) map[string]bool {
	r := make(map[string]bool, 100)
	for k, v := range basic {
		r[k] = v
	}
	for k, v := range extra {
		r[k] = v
	}

	return r
}
