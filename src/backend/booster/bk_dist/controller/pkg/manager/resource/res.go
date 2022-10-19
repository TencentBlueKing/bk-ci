/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package resource

import (
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	v2 "github.com/Tencent/bk-ci/src/booster/server/pkg/api/v2"
)

// Status save resource status
type Status int

// define file send status
const (
	ResourceInit Status = iota
	ResourceApplying
	ResourceApplySucceed
	ResourceApplyFailed
	ResourceReleasing
	ResourceReleaseSucceed
	ResourceReleaseFailed
	ResourceSpecified = 98 // specified directly by user, no relation with server
	ResourceUnknown   = 99
)

var (
	statusMap = map[Status]string{
		ResourceInit:           "init",
		ResourceApplying:       "applying",
		ResourceApplySucceed:   "applysucceed",
		ResourceApplyFailed:    "applyfailed",
		ResourceReleasing:      "releasing",
		ResourceReleaseSucceed: "releasesucceed",
		ResourceReleaseFailed:  "releasefailed",
		ResourceSpecified:      "specified",
		ResourceUnknown:        "unknown",
	}
)

// String return the string of FileSendStatus
func (f Status) String() string {
	if v, ok := statusMap[f]; ok {
		return v
	}

	return "unknown"
}

// Res describe the resource manager
type Res struct {
	taskid        string // we will get taskid before taskInfo
	status        Status
	taskInfo      *v2.RespTaskInfo
	heartbeatInfo []byte
	applyTime     time.Time
}

func (r *Res) heartbeatData() []byte {
	if r.heartbeatInfo != nil {
		return r.heartbeatInfo
	}

	codec.EncJSON(v2.ParamHeartbeat{
		TaskID: r.taskid,
		Type:   v2.HeartBeatPing.String(),
	}, &r.heartbeatInfo)

	return r.heartbeatInfo
}

func (r *Res) needHeartBeat() bool {
	return r.status == ResourceApplying || r.status == ResourceApplySucceed
}

func (r *Res) canRelease() bool {
	return r.status == ResourceApplying ||
		r.status == ResourceApplySucceed ||
		r.status == ResourceReleaseFailed ||
		r.status == ResourceApplyFailed
}

func (r *Res) isReleaseStatus() bool {
	return r.status == ResourceReleasing ||
		r.status == ResourceReleaseSucceed ||
		r.status == ResourceReleaseFailed
}
