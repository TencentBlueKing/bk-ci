//go:build windows

/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 Tencent.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package envs

import (
	"fmt"
	"strings"
	"sync"
	"syscall"
	"time"
	"unsafe"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

const (
	HKEY_CURRENT_USER  = 0x80000001
	HKEY_LOCAL_MACHINE = 0x80000002
	KEY_READ           = 0x20019
)

// regOpenKeyEx 打开注册表键
func regOpenKeyEx(hKey uintptr, subKey string, options uint32, samDesired uint32) (uintptr, error) {
	advapi32 := syscall.NewLazyDLL("advapi32.dll")
	regOpenKeyEx := advapi32.NewProc("RegOpenKeyExW")

	subKeyPtr, err := syscall.UTF16PtrFromString(subKey)
	if err != nil {
		return 0, err
	}

	var result uintptr
	ret, _, _ := regOpenKeyEx.Call(
		hKey,
		uintptr(unsafe.Pointer(subKeyPtr)),
		uintptr(options),
		uintptr(samDesired),
		uintptr(unsafe.Pointer(&result)),
	)

	if ret != 0 {
		return 0, fmt.Errorf("RegOpenKeyEx failed: %d", ret)
	}
	return result, nil
}

// regCloseKey 关闭注册表键
func regCloseKey(hKey uintptr) {
	advapi32 := syscall.NewLazyDLL("advapi32.dll")
	regCloseKey := advapi32.NewProc("RegCloseKey")
	regCloseKey.Call(hKey)
}

// regEnumValue 枚举注册表值
func regEnumValue(hKey uintptr, index uint32) (name string, value string, err error) {
	advapi32 := syscall.NewLazyDLL("advapi32.dll")
	regEnumValue := advapi32.NewProc("RegEnumValueW")

	nameBuffer := make([]uint16, 16384)
	nameLen := uint32(len(nameBuffer))

	dataBuffer := make([]uint16, 16384)
	dataLen := uint32(len(dataBuffer) * 2)

	var dataType uint32

	ret, _, _ := regEnumValue.Call(
		hKey,
		uintptr(index),
		uintptr(unsafe.Pointer(&nameBuffer[0])),
		uintptr(unsafe.Pointer(&nameLen)),
		0,
		uintptr(unsafe.Pointer(&dataType)),
		uintptr(unsafe.Pointer(&dataBuffer[0])),
		uintptr(unsafe.Pointer(&dataLen)),
	)

	if ret != 0 {
		return "", "", fmt.Errorf("enum failed: %d", ret)
	}

	name = syscall.UTF16ToString(nameBuffer)
	value = syscall.UTF16ToString(dataBuffer)

	return name, value, nil
}

type envValueSourceType string

const (
	User   envValueSourceType = "User"
	System envValueSourceType = "System"
	Merge  envValueSourceType = "Merge"
)

// envValue 环境变量的值和来源
type envValue struct {
	Value  string
	Source envValueSourceType
}

// envSnapshot 环境变量快照
type envSnapshot struct {
	User   map[string]string   // 用户环境变量(原始)
	System map[string]string   // 系统环境变量(原始)
	Merged map[string]envValue // 合并后的环境变量
}

// getAllEnvFromRegistry 读取所有环境变量并按Windows规则合并
func getAllEnvFromRegistry() envSnapshot {
	snapshot := envSnapshot{
		User:   make(map[string]string),
		System: make(map[string]string),
		Merged: make(map[string]envValue),
	}

	// 读取用户环境变量
	hKey, err := regOpenKeyEx(HKEY_CURRENT_USER, "Environment", 0, KEY_READ)
	if err == nil {
		defer regCloseKey(hKey)

		for i := uint32(0); ; i++ {
			name, value, err := regEnumValue(hKey, i)
			if err != nil {
				break
			}
			snapshot.User[name] = value
		}
	}

	// 读取系统环境变量
	hKey, err = regOpenKeyEx(HKEY_LOCAL_MACHINE, "SYSTEM\\CurrentControlSet\\Control\\Session Manager\\Environment", 0, KEY_READ)
	if err == nil {
		defer regCloseKey(hKey)

		for i := uint32(0); ; i++ {
			name, value, err := regEnumValue(hKey, i)
			if err != nil {
				break
			}
			snapshot.System[name] = value
		}
	}

	// 合并规则:
	// 1. 对于PATH/Path/path: 用户PATH + 系统PATH (用分号连接)
	// 2. 对于其他变量: 用户变量覆盖系统变量

	// 先加系统的
	for name, value := range snapshot.System {
		snapshot.Merged[name] = envValue{
			Value:  value,
			Source: System,
		}
	}

	// 再处理用户的
	for name, value := range snapshot.User {
		if strings.ToUpper(name) == "PATH" {
			// PATH特殊处理: 不区分大小写查找系统PATH并合并
			systemPath, systemPathKey := "", ""
			for sysKey, sysVal := range snapshot.System {
				if strings.ToUpper(sysKey) == "PATH" {
					systemPath = sysVal
					systemPathKey = sysKey
					break
				}
			}

			if systemPath == "" {
				snapshot.Merged[name] = envValue{Value: value, Source: User}
			} else {
				mergedPath := systemPath
				if !strings.HasSuffix(mergedPath, ";") {
					mergedPath += ";"
				}
				mergedPath += value

				// 删除系统阶段可能以不同大小写存入的PATH条目
				if systemPathKey != name {
					delete(snapshot.Merged, systemPathKey)
				}
				snapshot.Merged[name] = envValue{Value: mergedPath, Source: Merge}
			}
		} else {
			snapshot.Merged[name] = envValue{Value: value, Source: User}
		}
	}

	return snapshot
}

type EnvChangeOperationType string

const (
	New    EnvChangeOperationType = "New"
	Update EnvChangeOperationType = "Update"
	Delete EnvChangeOperationType = "Delete"
)

type EnvChange struct {
	Key       string
	OldValue  string
	NewValue  string
	Operation EnvChangeOperationType
	Source    envValueSourceType
}

type EnvPollingWatcher struct {
	interval     time.Duration
	stopCh       chan struct{}
	snapshotLock sync.RWMutex
	lastSnapshot envSnapshot
}

func NewEnvPollingWatcher(interval time.Duration) *EnvPollingWatcher {
	return &EnvPollingWatcher{
		interval:     interval,
		stopCh:       make(chan struct{}),
		snapshotLock: sync.RWMutex{},
	}
}

func (w *EnvPollingWatcher) Start() {
	w.snapshotLock.Lock()
	w.lastSnapshot = getAllEnvFromRegistry()
	mergedCount := len(w.lastSnapshot.Merged)
	w.snapshotLock.Unlock()
	logs.Infof("env polling init envs %d", mergedCount)
	go w.poll()
}

func (w *EnvPollingWatcher) poll() {
	ticker := time.NewTicker(w.interval)
	defer ticker.Stop()
	for {
		select {
		case <-w.stopCh:
			return
		case <-ticker.C:
			w.checkChanges()
		}
	}
}

func (w *EnvPollingWatcher) checkChanges() {
	currentSnapshot := getAllEnvFromRegistry()
	var changes []EnvChange
	// 检查新增和修改
	w.snapshotLock.RLock()
	for key, newEnvValue := range currentSnapshot.Merged {
		oldEnvValue, exists := w.lastSnapshot.Merged[key]
		if !exists {
			// 新增
			changes = append(changes, EnvChange{
				Key:       key,
				OldValue:  "",
				NewValue:  newEnvValue.Value,
				Operation: New,
				Source:    newEnvValue.Source,
			})
		} else if oldEnvValue.Value != newEnvValue.Value {
			changes = append(changes, EnvChange{
				Key:       key,
				OldValue:  oldEnvValue.Value,
				NewValue:  newEnvValue.Value,
				Operation: Update,
				Source:    newEnvValue.Source,
			})
		}
	}

	// 检查删除
	for key, oldEnvValue := range w.lastSnapshot.Merged {
		if _, exists := currentSnapshot.Merged[key]; !exists {
			// 删除
			changes = append(changes, EnvChange{
				Key:       key,
				OldValue:  oldEnvValue.Value,
				NewValue:  "",
				Operation: Delete,
				Source:    oldEnvValue.Source,
			})
		}
	}
	w.snapshotLock.RUnlock()

	// 如果有变化,触发回调
	if len(changes) > 0 {
		for _, change := range changes {
			logs.Infof("env polling change key=%s,op=%s,old=%s,new=%s,from=%s", change.Key, change.Operation, change.OldValue, change.NewValue, change.Source)
		}
		w.snapshotLock.Lock()
		w.lastSnapshot = currentSnapshot
		w.snapshotLock.Unlock()
	}
}

func (w *EnvPollingWatcher) Stop() {
	close(w.stopCh)
}

var watcher *EnvPollingWatcher = nil

func InitEnvPolling() {
	watcher = NewEnvPollingWatcher(3 * time.Second)
	watcher.Start()
}

func FetchEnvFromPolling() map[string]string {
	res := map[string]string{}
	if watcher != nil {
		watcher.snapshotLock.RLock()
		defer watcher.snapshotLock.RUnlock()
		for k, v := range watcher.lastSnapshot.Merged {
			res[k] = v.Value
		}
	}
	return res
}
