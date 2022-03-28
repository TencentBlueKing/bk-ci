/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

type ClientType string

var (
	hookMode     = false
	cacheEnabled *bool
	hasSetCache  = false

	ClientMake  ClientType = "bk-make"
	ClientCMake ClientType = "bk-cmake"
	ClientBazel ClientType = "bk-bazel"
	ClientBlade ClientType = "bk-blade"
	ClientNinja ClientType = "bk-ninja"
)

const (
	ClientMakeUsage  = "blueking make client, replaced of make"
	ClientCMakeUsage = "blueking cmake client, replaced of cmake"
	ClientBazelUsage = "blueking bazel client, replaced of bazel"
	ClientBladeUsage = "blueking blade client, replaced of blade"
	ClientNinjaUsage = "blueking ninja client, replaced of ninja"
)

// Name return the string name of ClientType
func (ct ClientType) Name() string {
	switch ct {
	case ClientMake:
		return string(ct)
	case ClientCMake:
		return string(ct)
	case ClientBazel:
		return string(ct)
	case ClientBlade:
		return string(ct)
	case ClientNinja:
		return string(ct)
	}
	return "unknown"
}

// Usage return the usage message of ClientType
func (ct ClientType) Usage() string {
	switch ct {
	case ClientMake:
		return ClientMakeUsage
	case ClientCMake:
		return ClientCMakeUsage
	case ClientBazel:
		return ClientBazelUsage
	case ClientBlade:
		return ClientBladeUsage
	case ClientNinja:
		return ClientNinjaUsage
	}
	return "unknown"
}

var (
	Connected = true
	Compiler  = CompilerGcc

	TotalWaitServerSecs = 20
	PrintEveryTimes     = 5
	SleepSecsPerWait    = 1
)

type CompilerType string

const (
	CompilerGcc   CompilerType = "gcc"
	CompilerClang CompilerType = "clang"
)
