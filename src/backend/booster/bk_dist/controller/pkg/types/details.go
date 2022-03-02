/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package types

import (
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
)

// WorkStatsDetail describe the work stats details
type WorkStatsDetail struct {
	CurrentTime      int64                       `json:"current_time"`
	WorkID           string                      `json:"work_id"`
	TaskID           string                      `json:"task_id"`
	Scene            string                      `json:"scene"`
	Status           string                      `json:"status"`
	Success          bool                        `json:"success"`
	RegisteredTime   int64                       `json:"registered_time"`
	UnregisteredTime int64                       `json:"unregistered_time"`
	StartTime        int64                       `json:"start_time"`
	EndTime          int64                       `json:"end_time"`
	JobRemoteOK      int                         `json:"job_remote_ok"`
	JobRemoteError   int                         `json:"job_remote_error"`
	JobLocalOK       int                         `json:"job_local_ok"`
	JobLocalError    int                         `json:"job_local_error"`
	Jobs             []*dcSDK.ControllerJobStats `json:"jobs"`
}

type WorkStatsDetailList []*WorkStatsDetail

// Len return the length
func (wsl WorkStatsDetailList) Len() int {
	return len(wsl)
}

// Less do the comparing
func (wsl WorkStatsDetailList) Less(i, j int) bool {
	return wsl[i].RegisteredTime > wsl[j].RegisteredTime
}

// Swap do the data swap
func (wsl WorkStatsDetailList) Swap(i, j int) {
	wsl[i], wsl[j] = wsl[j], wsl[i]
}
