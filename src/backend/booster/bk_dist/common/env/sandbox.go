/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package env

import (
	"runtime"
	"strings"
)

// NewSandbox get a new sandbox
func NewSandbox(env []string) *Sandbox {
	s := &Sandbox{
		env:      env,
		envIndex: make(map[string]int),
	}
	s.copyEnv()
	return s
}

// Sandbox provide a handler to handle the environments variables
type Sandbox struct {
	env []string

	envIndex map[string]int
}

func (sb *Sandbox) copyEnv() {
	for i, s := range sb.env {
		for j := 0; j < len(s); j++ {
			if s[j] == '=' {
				key := ensureKey(s[:j])

				if _, ok := sb.envIndex[key]; !ok {
					sb.envIndex[key] = i // first mention of key
				} else {
					// Clear duplicate keys. This permits Unsetenv to
					// safely delete only the first item without
					// worrying about unshadowing a later one,
					// which might be a security problem.

					// TODO: 在unix下, 为了方便unset操作, 但sandbox当前不包含unset操作, 暂时不需要.
					// 如果一定要加, 需要保证windows下不被执行, 否则会影响Source()中的数据, 导致子进程执行失败.
					// sb.env[i] = ""
				}
				break
			}
		}
	}
}

// Source return the source env, generated as a slice, each item is the format as "a=b"
func (sb *Sandbox) Source() []string {
	return sb.env
}

// IsSet check the bool key is set in current environments
func (sb *Sandbox) IsSet(key string) bool {
	return sb.GetEnv(key) != ""
}

// GetEnv receive the key and get the real key in bk-dist environment, then return the value
func (sb *Sandbox) GetEnv(key string) string {
	return sb.GetOriginEnv(GetEnvKey(key))
}

// GetOriginEnv receive the key and return the value
func (sb *Sandbox) GetOriginEnv(key string) string {
	key = ensureKey(key)

	if len(key) == 0 {
		return ""
	}

	i, ok := sb.envIndex[key]
	if !ok {
		return ""
	}
	s := sb.env[i]
	for i := 0; i < len(s); i++ {
		if s[i] == '=' {
			return s[i+1:]
		}
	}
	return ""
}

func ensureKey(key string) string {
	if runtime.GOOS == "windows" {
		return strings.ToUpper(key)
	}

	return key
}
