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
	"io"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
)

// BoosterConfig describe the whole data that distribute workers booster should hold.
type BoosterConfig struct {
	// Task type decide what kind of this task is going to be launched.
	// It matters the type of executor's handler.
	Type BoosterType

	// ProjectID define the project which influences the remote workers settings.
	ProjectID string

	// BuildID provides a custom key to be associated with the task.
	BuildID string

	// Use BatchMode for multi boosters use the same workID.
	BatchMode bool

	// Args describe the actual args to be executed after initialization.
	Args string

	Cmd string

	Works BoosterWorks

	Transport BoosterTransport

	Controller sdk.ControllerConfig
}

// BoosterWorks describe the works data
type BoosterWorks struct {
	Degraded bool

	Stdout io.Writer
	Stderr io.Writer

	// RunDir describe the current command running dir.
	RunDir string

	// User describe the current user.
	User string

	CommandPath       string
	LimitPerWorker    int
	Jobs              int
	MaxJobs           int
	MaxDegradedJobs   int
	MaxLocalTotalJobs int
	MaxLocalPreJobs   int
	MaxLocalExeJobs   int
	MaxLocalPostJobs  int
	SupportDirectives bool
	GlobalSlots       bool

	ExecutorLogLevel string

	Environments map[string]string

	HookPreloadLibPath string
	HookConfigPath     string
	HookMode           bool
	NoLocal            bool
	Local              bool
	WorkerSideCache    bool
	LocalRecord        bool

	Bazel      bool
	BazelPlus  bool
	Bazel4Plus bool
	Launcher   bool

	Preload           sdk.PreloadConfig
	PreloadContent    string
	PreloadContentRaw string

	AdditionFiles []string

	WorkerList []string
	CheckMd5   bool

	OutputEnvJSONFile   []string
	OutputEnvSourceFile []string
	CommitSuicide       bool
	ToolChainJSONFile   string

	IOTimeoutSecs int

	Pump                 bool
	PumpDisableMacro     bool
	PumpIncludeSysHeader bool
	PumpCheck            bool
	PumpCache            bool
	PumpCacheDir         string
	PumpCacheSizeMaxMB   int32
	PumpCacheRemoveAll   bool
	PumpBlackList        []string
	PumpMinActionNum     int32

	ForceLocalList []string

	NoWork bool

	WriteMemroy bool

	IdleKeepSecs int
}

// BoosterTransport describe the transport data to controller
type BoosterTransport struct {
	ServerDomain           string
	ServerHost             string
	Timeout                time.Duration
	HeartBeatTick          time.Duration
	InspectTaskTick        time.Duration
	TaskPreparingTimeout   time.Duration
	PrintTaskInfoEveryTime int
	CommitSuicideCheckTick time.Duration
}
