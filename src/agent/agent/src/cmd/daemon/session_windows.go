//go:build windows
// +build windows

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

package main

import (
	"fmt"
	"unsafe"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"

	"golang.org/x/sys/windows"
)

var (
	modwtsapi32 = windows.NewLazySystemDLL("wtsapi32.dll")
	modkernel32 = windows.NewLazySystemDLL("kernel32.dll")
	modadvapi32 = windows.NewLazySystemDLL("advapi32.dll")
	moduserenv  = windows.NewLazySystemDLL("userenv.dll")

	procWTSEnumerateSessionsW       = modwtsapi32.NewProc("WTSEnumerateSessionsW")
	procWTSGetActiveConsoleSessionId = modkernel32.NewProc("WTSGetActiveConsoleSessionId")
	procWTSQueryUserToken           = modwtsapi32.NewProc("WTSQueryUserToken")
	procWTSFreeMemory               = modwtsapi32.NewProc("WTSFreeMemory")
	procDuplicateTokenEx            = modadvapi32.NewProc("DuplicateTokenEx")
	procCreateEnvironmentBlock      = moduserenv.NewProc("CreateEnvironmentBlock")
	procDestroyEnvironmentBlock     = moduserenv.NewProc("DestroyEnvironmentBlock")
	procCreateProcessAsUserW        = modadvapi32.NewProc("CreateProcessAsUserW")
)

const (
	wtsCurrentServerHandle uintptr = 0
)

type wtsConnectStateClass int

const (
	wtsActive wtsConnectStateClass = iota
	wtsConnected
	wtsConnectQuery
	wtsShadow
	wtsDisconnected
	wtsIdle
	wtsListen
	wtsReset
	wtsDown
	wtsInit
)

const (
	securityImpersonation = 2
	tokenPrimary          = 1
)

const (
	createUnicodeEnvironment uint32 = 0x00000400
	createNoWindow           uint32 = 0x08000000
)

type wtsSessionInfo struct {
	SessionID      uint32
	WinStationName *uint16
	State          wtsConnectStateClass
}

// SessionProcessInfo holds the result of a CreateProcessAsUser call.
type SessionProcessInfo struct {
	PID           uint32
	ProcessHandle windows.Handle
	ThreadHandle  windows.Handle
}

// Close releases the thread handle. The caller manages the process handle lifetime.
func (p *SessionProcessInfo) Close() {
	if p.ThreadHandle != 0 {
		windows.CloseHandle(p.ThreadHandle)
		p.ThreadHandle = 0
	}
}

// GetActiveSessionID returns the session ID of the first active user session,
// falling back to WTSGetActiveConsoleSessionId if no WTSActive session is
// found via enumeration.
func GetActiveSessionID() (uint32, error) {
	sessions, err := enumerateSessions()
	if err == nil {
		for _, s := range sessions {
			if s.State == wtsActive {
				logs.Infof("found active WTS session %d", s.SessionID)
				return s.SessionID, nil
			}
		}
	}

	sessionID, _, callErr := procWTSGetActiveConsoleSessionId.Call()
	if sessionID == 0xFFFFFFFF {
		return 0, fmt.Errorf("no active user session: WTSGetActiveConsoleSessionId: %w", callErr)
	}
	logs.Infof("using console session %d from WTSGetActiveConsoleSessionId", sessionID)
	return uint32(sessionID), nil
}

func enumerateSessions() ([]wtsSessionInfo, error) {
	var (
		pSessionInfo unsafe.Pointer
		count        uint32
	)
	ret, _, err := procWTSEnumerateSessionsW.Call(
		wtsCurrentServerHandle, 0, 1,
		uintptr(unsafe.Pointer(&pSessionInfo)),
		uintptr(unsafe.Pointer(&count)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("WTSEnumerateSessionsW: %w", err)
	}
	defer procWTSFreeMemory.Call(uintptr(pSessionInfo))

	sessions := unsafe.Slice((*wtsSessionInfo)(pSessionInfo), count)
	result := make([]wtsSessionInfo, count)
	copy(result, sessions)
	return result, nil
}

func duplicateUserToken(sessionID uint32) (windows.Token, error) {
	var impersonationToken windows.Handle
	ret, _, err := procWTSQueryUserToken.Call(
		uintptr(sessionID),
		uintptr(unsafe.Pointer(&impersonationToken)),
	)
	if ret == 0 {
		return 0, fmt.Errorf("WTSQueryUserToken(session=%d): %w", sessionID, err)
	}

	var userToken windows.Token
	ret, _, err = procDuplicateTokenEx.Call(
		uintptr(impersonationToken),
		0,
		0,
		uintptr(securityImpersonation),
		uintptr(tokenPrimary),
		uintptr(unsafe.Pointer(&userToken)),
	)
	windows.CloseHandle(impersonationToken)
	if ret == 0 {
		return 0, fmt.Errorf("DuplicateTokenEx: %w", err)
	}
	return userToken, nil
}

// StartProcessAsUser launches an executable in the active user's session
// using WTS APIs. The daemon service runs in Session 0, so this is required
// to give the agent (and its child build processes) access to the user's
// desktop, clipboard, and other session-bound resources.
func StartProcessAsUser(appPath, cmdLine, workDir string) (*SessionProcessInfo, error) {
	sessionID, err := GetActiveSessionID()
	if err != nil {
		return nil, fmt.Errorf("get active session: %w", err)
	}

	userToken, err := duplicateUserToken(sessionID)
	if err != nil {
		return nil, fmt.Errorf("duplicate user token for session %d: %w", sessionID, err)
	}
	defer userToken.Close()

	var envBlock uintptr
	ret, _, callErr := procCreateEnvironmentBlock.Call(
		uintptr(unsafe.Pointer(&envBlock)),
		uintptr(userToken),
		0,
	)
	if ret == 0 {
		return nil, fmt.Errorf("CreateEnvironmentBlock: %w", callErr)
	}
	defer procDestroyEnvironmentBlock.Call(envBlock)

	si := windows.StartupInfo{
		Cb:      uint32(unsafe.Sizeof(windows.StartupInfo{})),
		Desktop: windows.StringToUTF16Ptr("winsta0\\default"),
	}
	var pi windows.ProcessInformation

	creationFlags := createUnicodeEnvironment | createNoWindow

	var cmdLinePtr *uint16
	if cmdLine != "" {
		cmdLinePtr = windows.StringToUTF16Ptr(cmdLine)
	}
	var workDirPtr *uint16
	if workDir != "" {
		workDirPtr = windows.StringToUTF16Ptr(workDir)
	}

	ret, _, callErr = procCreateProcessAsUserW.Call(
		uintptr(userToken),
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(appPath))),
		uintptr(unsafe.Pointer(cmdLinePtr)),
		0, 0, 0,
		uintptr(creationFlags),
		envBlock,
		uintptr(unsafe.Pointer(workDirPtr)),
		uintptr(unsafe.Pointer(&si)),
		uintptr(unsafe.Pointer(&pi)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("CreateProcessAsUserW: %w", callErr)
	}

	logs.Infof("launched agent as user in session %d, pid=%d, app=%s", sessionID, pi.ProcessId, appPath)
	return &SessionProcessInfo{
		PID:           pi.ProcessId,
		ProcessHandle: pi.Process,
		ThreadHandle:  pi.Thread,
	}, nil
}
