/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package normal

import (
	"time"
)

const (
	taskIDRandomLength = 5
	taskIDFormat       = "%s-%s-%d%s"

	clientHeartBeatTickTime = 5 * time.Second

	layerCleanTimeAfterReleased = 2 * time.Hour
	layerCleanLockTimeGap       = 10 * time.Minute
	layerCleanReleasedTaskGap   = 10 * time.Minute

	selectorSelectSleepTime     = 200 * time.Millisecond
	selectorLogQueueStatGapTime = 10 * time.Second

	trackerCheckGapTime = 1 * time.Second
	trackerTrackGapTime = 1 * time.Second

	keeperHealthCheckGapTime  = 10 * time.Second
	keeperFirstStartGraceTime = 1 * time.Minute
	keeperInitTimeout         = 20 * time.Second
	keeperStartingTimeout     = 120 * time.Second

	cleanerReleaseCheckGapTime = 5 * time.Second

	defaultMaxParallel = 10

	messageNoEnoughAvailableWorkers = "task launch failed, no enough available running workers."
	messageDegradeTask              = "busy compiling now, no enough resource left, degrade to local compiling."
	messageStagingTimeout           = "task staging timeout."
	messageTaskInit                 = "task is initializing."
	messageTaskStaging              = "task is staging."
	messageTaskStarting             = "task is starting, the workers will be launched soon."
	messageTaskRunning              = "task is running successfully, ready for working."
	messageHeartBeatTimeoutAndLost  = "client heartbeat timeout and lost."
	messageTaskStartingTimeout      = "task starting timeout."
	messageTaskInitTimeout          = "task init timeout."
	messageTaskFinishSuccessfully   = "task finish successfully."
	messageTaskCanceledByClient     = "task failed, canceled by client."
)
