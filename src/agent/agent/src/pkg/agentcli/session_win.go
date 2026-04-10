//go:build windows
// +build windows

package agentcli

import (
	"fmt"
	"os"
	"os/exec"
	"path/filepath"
	"time"
	"unsafe"

	"golang.org/x/sys/windows"
	"golang.org/x/sys/windows/registry"
)

var (
	modadvapi32 = windows.NewLazySystemDLL("advapi32.dll")

	procLogonUserW             = modadvapi32.NewProc("LogonUserW")
	procLsaOpenPolicy          = modadvapi32.NewProc("LsaOpenPolicy")
	procLsaStorePrivateData    = modadvapi32.NewProc("LsaStorePrivateData")
	procLsaRetrievePrivateData = modadvapi32.NewProc("LsaRetrievePrivateData")
	procLsaFreeMemory          = modadvapi32.NewProc("LsaFreeMemory")
	procLsaClose               = modadvapi32.NewProc("LsaClose")
)

const (
	logon32LogonInteractive = 2
	logon32ProviderDefault  = 0
	policyCreateSecret      = 0x00000020
	policyGetPrivateInfo    = 0x00000004

	lsaKeyUser     = "BkCiSessionUser"
	lsaKeyPassword = "BkCiSessionPassword"
)

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

func enableSession(workDir, user, password string, autoLogon bool) error {
	printStep("============================================")
	printStep(msg(" BK-CI Agent Session Mode Configuration", " BK-CI Agent 会话模式配置"))
	printStep("============================================")

	if autoLogon {
		if err := validateCredentials(user, password); err != nil {
			return err
		}
	}

	prepareWorkDir(workDir)

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}

	cleanupLegacySchtasks(serviceName)
	if serviceExists(serviceName) {
		stopService(serviceName)
		deleteService(serviceName)
		time.Sleep(2 * time.Second)
	}

	daemonPath := fmt.Sprintf(`"%s\%s"`, workDir, daemonBinary())
	printStepf("%s", msgf("creating service %s", "创建服务 %s", serviceName))
	if out, err := runSc("create", serviceName, "binPath=", daemonPath, "start=", "auto"); err != nil {
		return cliErrorf("sc.exe create failed: %s (%v)", "sc.exe create 失败: %s (%v)", out, err)
	}

	configureSCMRecovery(serviceName)

	if autoLogon {
		if err := enableAutoLogon(user, password); err != nil {
			return cliErrorf("enable auto-logon failed: %v", "启用自动登录失败: %v", err)
		}
	}

	writeInstallType(workDir, "SESSION")

	printStepf("%s", msgf("starting service %s", "启动服务 %s", serviceName))
	if out, err := runSc("start", serviceName); err != nil {
		printWarn(msgf("service start may have failed: %s (%v)", "服务启动可能失败: %s (%v)", out, err))
	}
	time.Sleep(3 * time.Second)

	printStep("")
	printStep("============================================")
	printStep(msg(" Done", " 完成"))
	printStep("============================================")
	printStepf("%-13s %s", msg("Service", "服务"), serviceName)

	if autoLogon {
		printStepf("%-13s %s", msg("Session user", "会话用户"), user)
		printStepf("%-13s %s", msg("Auto-logon", "自动登录"), msg("enabled (every reboot)", "已启用 (每次重启)"))
		printStep("")
	} else {
		printStepf("%-13s %s", msg("Session user", "会话用户"), msg("(current logged-in user)", "(当前登录用户)"))
		printStepf("%-13s %s", msg("Auto-logon", "自动登录"), msg("not configured", "未配置"))
		printStep("")
	}
	for _, line := range configureSessionSummaryLines(user, autoLogon) {
		printStep(line)
	}
	return nil
}

func disableSession(workDir string) error {
	printStep(msg("Disabling session mode...", "正在禁用会话模式..."))
	removeSessionSecrets()
	removeAutoLogon()
	writeInstallType(workDir, "SERVICE")

	serviceName, err := getServiceName(workDir)
	if err != nil {
		return err
	}
	if serviceExists(serviceName) {
		stopService(serviceName)
		time.Sleep(time.Second)
		runSc("start", serviceName)
		time.Sleep(2 * time.Second)
	}
	printStep(msg("Session mode disabled. Agent will run in Session 0 unless a user is logged in.", "会话模式已禁用。除非有用户登录，否则 Agent 将运行在 Session 0。"))
	return nil
}

// ── Credential validation ────────────────────────────────────────────────

func validateCredentials(account, password string) error {
	user, domain := splitUserDomain(account)
	printStepf("%s", msgf("validating credentials for %s@%s ...", "正在校验凭据 %s@%s ...", user, domain))

	var token windows.Handle
	ret, _, err := procLogonUserW.Call(
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(user))),
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(domain))),
		uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(password))),
		uintptr(logon32LogonInteractive),
		uintptr(logon32ProviderDefault),
		uintptr(unsafe.Pointer(&token)),
	)
	if ret == 0 {
		code := err.(windows.Errno)
		msg := "unknown error"
		switch uint32(code) {
		case 1326:
			msg = "wrong username or password"
		case 1327:
			msg = "account restriction"
		case 1330:
			msg = "password expired"
		case 1331:
			msg = "account disabled"
		}
		return cliErrorf("credential validation failed: %s (Win32 %d)", "凭据校验失败: %s (Win32 %d)", msg, code)
	}
	windows.CloseHandle(token)
	printStep(msg("credentials verified OK", "凭据校验通过"))
	return nil
}

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

// ── LSA Secret operations ────────────────────────────────────────────────

func lsaOpenPolicy(access uint32) (uintptr, error) {
	var attrs lsaObjectAttributes
	attrs.Length = uint32(unsafe.Sizeof(attrs))
	var systemName lsaUnicodeString
	var handle uintptr

	ret, _, err := procLsaOpenPolicy.Call(
		uintptr(unsafe.Pointer(&systemName)),
		uintptr(unsafe.Pointer(&attrs)),
		uintptr(access),
		uintptr(unsafe.Pointer(&handle)),
	)
	if ret != 0 {
		return 0, cliErrorf("LsaOpenPolicy failed: NTSTATUS 0x%x: %v", "LsaOpenPolicy 失败: NTSTATUS 0x%x: %v", ret, err)
	}
	return handle, nil
}

func toLsaString(s string) lsaUnicodeString {
	return lsaUnicodeString{
		Length:        uint16(len(s) * 2),
		MaximumLength: uint16((len(s) + 1) * 2),
		Buffer:        windows.StringToUTF16Ptr(s),
	}
}

func storeLsaSecret(key, value string) error {
	handle, err := lsaOpenPolicy(policyCreateSecret)
	if err != nil {
		return err
	}
	defer procLsaClose.Call(handle)

	k := toLsaString(key)
	v := toLsaString(value)
	ret, _, callErr := procLsaStorePrivateData.Call(
		handle,
		uintptr(unsafe.Pointer(&k)),
		uintptr(unsafe.Pointer(&v)),
	)
	if ret != 0 {
		return cliErrorf("LsaStorePrivateData(%s) failed: NTSTATUS 0x%x: %v", "LsaStorePrivateData(%s) 失败: NTSTATUS 0x%x: %v", key, ret, callErr)
	}
	return nil
}

func deleteLsaSecret(key string) {
	handle, err := lsaOpenPolicy(policyCreateSecret)
	if err != nil {
		return
	}
	defer procLsaClose.Call(handle)

	k := toLsaString(key)
	procLsaStorePrivateData.Call(handle, uintptr(unsafe.Pointer(&k)), 0)
}

func removeSessionSecrets() {
	deleteLsaSecret(lsaKeyUser)
	deleteLsaSecret(lsaKeyPassword)
	printStep(msg("session credentials removed from LSA Secret", "会话凭据已从 LSA Secret 中移除"))
}

// ── Auto-logon (registry + LSA) ──────────────────────────────────────────

func enableAutoLogon(account, password string) error {
	user, domain := splitUserDomain(account)
	printStepf("%s", msgf("configuring Windows auto-logon: user=%s, domain=%s", "正在配置 Windows 自动登录: 用户=%s, 域=%s", user, domain))

	k, _, err := registry.CreateKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon`,
		registry.SET_VALUE,
	)
	if err != nil {
		return cliErrorf("open Winlogon key failed: %v", "打开 Winlogon 注册表键失败: %v", err)
	}
	defer k.Close()

	if err := k.SetStringValue("AutoAdminLogon", "1"); err != nil {
		return cliErrorf("set AutoAdminLogon failed: %v", "设置 AutoAdminLogon 失败: %v", err)
	}
	if err := k.SetStringValue("DefaultUserName", user); err != nil {
		return cliErrorf("set DefaultUserName failed: %v", "设置 DefaultUserName 失败: %v", err)
	}
	if err := k.SetStringValue("DefaultDomainName", domain); err != nil {
		return cliErrorf("set DefaultDomainName failed: %v", "设置 DefaultDomainName 失败: %v", err)
	}
	_ = k.DeleteValue("DefaultPassword")
	_ = k.DeleteValue("AutoLogonCount")

	if err := storeLsaSecret("DefaultPassword", password); err != nil {
		return cliErrorf("store auto-logon password failed: %v", "存储自动登录密码失败: %v", err)
	}
	printStep(msg("auto-logon password stored via LSA Secret (encrypted)", "自动登录密码已通过 LSA Secret 加密存储"))

	pk, _, _ := registry.CreateKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Policies\Microsoft\Power\PowerSettings\0e796bdb-100d-47d6-a2d5-f7d2daa51f51`,
		registry.SET_VALUE,
	)
	if pk != 0 {
		pk.SetDWordValue("DCSettingIndex", 0)
		pk.SetDWordValue("ACSettingIndex", 0)
		pk.Close()
	}

	printStep(msg("auto-logon configured (activates on next reboot)", "自动登录已配置 (下次重启生效)"))
	return nil
}

func removeAutoLogon() {
	k, err := registry.OpenKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon`,
		registry.SET_VALUE,
	)
	if err != nil {
		return
	}
	defer k.Close()

	k.SetStringValue("AutoAdminLogon", "0")
	k.DeleteValue("DefaultPassword")
	deleteLsaSecret("DefaultPassword")
	printStep(msg("Windows auto-logon disabled", "Windows 自动登录已禁用"))
}

// ── Install type marker ──────────────────────────────────────────────────

const installTypeFile = ".install_type"

func writeInstallType(workDir, mode string) {
	p := filepath.Join(workDir, installTypeFile)
	if err := os.WriteFile(p, []byte(mode), 0600); err != nil {
		printWarn(msgf("failed to write %s: %v", "写入 %s 失败: %v", installTypeFile, err))
		return
	}
	printStepf("%s", msgf("install type set to %s", "安装类型已设置为 %s", mode))
}

// ── sc.exe helper ────────────────────────────────────────────────────────

func runSc(args ...string) (string, error) {
	out, err := exec.Command("sc.exe", args...).CombinedOutput()
	return string(out), err
}
