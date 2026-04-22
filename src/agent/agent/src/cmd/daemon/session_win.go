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

	"golang.org/x/sys/windows"

	"github.com/TencentBlueKing/bk-ci/agent/src/pkg/common/logs"
)

var (
	modwtsapi32 = windows.NewLazySystemDLL("wtsapi32.dll")
	modkernel32 = windows.NewLazySystemDLL("kernel32.dll")
	modadvapi32 = windows.NewLazySystemDLL("advapi32.dll")
	moduserenv  = windows.NewLazySystemDLL("userenv.dll")

	procWTSEnumerateSessionsW        = modwtsapi32.NewProc("WTSEnumerateSessionsW")
	procWTSGetActiveConsoleSessionId = modkernel32.NewProc("WTSGetActiveConsoleSessionId")
	procWTSQueryUserToken            = modwtsapi32.NewProc("WTSQueryUserToken")
	procWTSFreeMemory                = modwtsapi32.NewProc("WTSFreeMemory")
	procDuplicateTokenEx             = modadvapi32.NewProc("DuplicateTokenEx")
	procCreateEnvironmentBlock       = moduserenv.NewProc("CreateEnvironmentBlock")
	procDestroyEnvironmentBlock      = moduserenv.NewProc("DestroyEnvironmentBlock")
	procCreateProcessAsUserW         = modadvapi32.NewProc("CreateProcessAsUserW")
	procLogonUserW                   = modadvapi32.NewProc("LogonUserW")
	procSetTokenInformation          = modadvapi32.NewProc("SetTokenInformation")
	procLsaOpenPolicy                = modadvapi32.NewProc("LsaOpenPolicy")
	procLsaRetrievePrivateData       = modadvapi32.NewProc("LsaRetrievePrivateData")
	procLsaFreeMemory                = modadvapi32.NewProc("LsaFreeMemory")
	procLsaClose                     = modadvapi32.NewProc("LsaClose")
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

	logon32LogonInteractive = 2
	logon32ProviderDefault  = 0
	tokenSessionId          = 12

	policyGetPrivateInformation uint32 = 0x00000004

	lsaSecretKeyUser     = "BkCiSessionUser"
	lsaSecretKeyPassword = "BkCiSessionPassword"

	// noSessionID is the sentinel value returned by WTSGetActiveConsoleSessionId
	// when no physical console session exists.
	noSessionID = 0xFFFFFFFF
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
	if sessionID == noSessionID {
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

// enableTcbPrivilege enables SeTcbPrivilege on the current process token.
// This is required by SetTokenInformation(TokenSessionId) and is only
// available when running as SYSTEM.
func enableTcbPrivilege() error {
	var token windows.Token
	err := windows.OpenProcessToken(
		windows.CurrentProcess(),
		windows.TOKEN_ADJUST_PRIVILEGES|windows.TOKEN_QUERY,
		&token,
	)
	if err != nil {
		return fmt.Errorf("OpenProcessToken: %w", err)
	}
	defer token.Close()

	var luid windows.LUID
	err = windows.LookupPrivilegeValue(nil, windows.StringToUTF16Ptr("SeTcbPrivilege"), &luid)
	if err != nil {
		return fmt.Errorf("LookupPrivilegeValue(SeTcbPrivilege): %w", err)
	}

	tp := windows.Tokenprivileges{
		PrivilegeCount: 1,
		Privileges: [1]windows.LUIDAndAttributes{
			{Luid: luid, Attributes: windows.SE_PRIVILEGE_ENABLED},
		},
	}
	err = windows.AdjustTokenPrivileges(token, false, &tp, 0, nil, nil)
	if err != nil {
		return fmt.Errorf("AdjustTokenPrivileges: %w", err)
	}
	return nil
}

// StartProcessWithLogon launches an executable in the console session using
// LogonUser credentials. This works even when no user is interactively logged
// in — the service (running as SYSTEM) uses LogonUser to obtain a token, sets
// its session to the physical console session via SetTokenInformation, then
// calls CreateProcessAsUser.
//
// Requirements:
//   - Service must run as SYSTEM (for SeTcbPrivilege)
//   - A physical console session must exist (machine has a display or virtual display)
func StartProcessWithLogon(account, password, appPath, cmdLine, workDir string) (*SessionProcessInfo, error) {
	user, domain := splitUserDomain(account)

	if err := enableTcbPrivilege(); err != nil {
		logs.WithError(err).Warn("enableTcbPrivilege failed, SetTokenInformation may fail")
	}

	var logonToken windows.Handle
	ret, _, err := procLogonUserW.Call(
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(user))),
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(domain))),
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(password))),
		uintptr(logon32LogonInteractive),
		uintptr(logon32ProviderDefault),
		uintptr(unsafe.Pointer(&logonToken)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("LogonUserW(%s): %w", user, err)
	}
	defer windows.CloseHandle(logonToken)

	var primaryToken windows.Token
	ret, _, err = procDuplicateTokenEx.Call(
		uintptr(logonToken),
		0,
		0,
		uintptr(securityImpersonation),
		uintptr(tokenPrimary),
		uintptr(unsafe.Pointer(&primaryToken)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("DuplicateTokenEx: %w", err)
	}
	defer primaryToken.Close()

	consoleSession, _, _ := procWTSGetActiveConsoleSessionId.Call()
	if consoleSession == noSessionID {
		return nil, fmt.Errorf("no console session available")
	}
	sid := uint32(consoleSession)

	ret, _, err = procSetTokenInformation.Call(
		uintptr(primaryToken),
		uintptr(tokenSessionId),
		uintptr(unsafe.Pointer(&sid)),
		unsafe.Sizeof(uint32(0)),
	)
	if ret == 0 {
		return nil, fmt.Errorf("SetTokenInformation(TokenSessionId=%d): %w", sid, err)
	}

	var envBlock uintptr
	ret, _, err = procCreateEnvironmentBlock.Call(
		uintptr(unsafe.Pointer(&envBlock)),
		uintptr(primaryToken),
		0,
	)
	if ret == 0 {
		return nil, fmt.Errorf("CreateEnvironmentBlock: %w", err)
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

	ret, _, err = procCreateProcessAsUserW.Call(
		uintptr(primaryToken),
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
		return nil, fmt.Errorf("CreateProcessAsUserW(logon): %w", err)
	}

	logs.Infof("launched process via LogonUser(%s) in console session %d, pid=%d", user, sid, pi.ProcessId)
	return &SessionProcessInfo{
		PID:           pi.ProcessId,
		ProcessHandle: pi.Process,
		ThreadHandle:  pi.Thread,
	}, nil
}

// ── LSA Secret read (for session credentials) ────────────────────────────

type lsaUnicodeString struct {
	Length        uint16
	MaximumLength uint16
	Buffer        *uint16
}

type lsaObjectAttributes struct {
	Length                   uint32
	RootDirectory            uintptr
	ObjectName               uintptr
	Attributes               uint32
	SecurityDescriptor       uintptr
	SecurityQualityOfService uintptr
}

// ReadLsaSecret reads a secret stored by LsaStorePrivateData (same API used
// by configure_session.ps1 and Sysinternals Autologon).
func ReadLsaSecret(keyName string) (string, error) {
	var attrs lsaObjectAttributes
	attrs.Length = uint32(unsafe.Sizeof(attrs))
	var systemName lsaUnicodeString
	var policyHandle uintptr

	ret, _, err := procLsaOpenPolicy.Call(
		uintptr(unsafe.Pointer(&systemName)),
		uintptr(unsafe.Pointer(&attrs)),
		uintptr(policyGetPrivateInformation),
		uintptr(unsafe.Pointer(&policyHandle)),
	)
	if ret != 0 {
		return "", fmt.Errorf("LsaOpenPolicy: NTSTATUS 0x%x: %w", ret, err)
	}
	defer procLsaClose.Call(policyHandle)

	key := lsaUnicodeString{
		Length:        uint16(len(keyName) * 2),
		MaximumLength: uint16((len(keyName) + 1) * 2),
		Buffer:        windows.StringToUTF16Ptr(keyName),
	}

	var privateData *lsaUnicodeString
	ret, _, err = procLsaRetrievePrivateData.Call(
		policyHandle,
		uintptr(unsafe.Pointer(&key)),
		uintptr(unsafe.Pointer(&privateData)),
	)
	if ret != 0 {
		return "", fmt.Errorf("LsaRetrievePrivateData(%s): NTSTATUS 0x%x: %w", keyName, ret, err)
	}
	if privateData == nil || privateData.Buffer == nil {
		return "", fmt.Errorf("LsaRetrievePrivateData(%s): empty result", keyName)
	}
	defer procLsaFreeMemory.Call(uintptr(unsafe.Pointer(privateData)))

	result := windows.UTF16PtrToString(privateData.Buffer)
	return result, nil
}

// ReadSessionCredentials reads session user/password from LSA Secret store.
// Returns empty strings if either value is not configured or unreadable.
func ReadSessionCredentials() (user, password string) {
	u, err := ReadLsaSecret(lsaSecretKeyUser)
	if err != nil {
		return "", ""
	}
	p, err := ReadLsaSecret(lsaSecretKeyPassword)
	if err != nil {
		return "", ""
	}
	return u, p
}

// splitUserDomain parses "DOMAIN\user" or "user@domain" into (user, domain).
// If no separator is found, domain defaults to "." (local machine).
func splitUserDomain(account string) (user, domain string) {
	if account == "" {
		return "", "."
	}
	for i := 0; i < len(account); i++ {
		if account[i] == '\\' {
			u := account[i+1:]
			if u == "" {
				return account, "."
			}
			return u, account[:i]
		}
	}
	for i := 0; i < len(account); i++ {
		if account[i] == '@' {
			return account[:i], account[i+1:]
		}
	}
	return account, "."
}
