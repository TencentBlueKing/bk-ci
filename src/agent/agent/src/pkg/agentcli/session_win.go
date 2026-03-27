//go:build windows
// +build windows

package agentcli

import (
	"flag"
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

func handleConfigureSession(workDir string, args []string) error {
	fs := flag.NewFlagSet("configure-session", flag.ExitOnError)
	userFlag := fs.String("user", "", "Windows logon account")
	passFlag := fs.String("password", "", "Account password")
	autoLogon := fs.Bool("auto-logon", false, "Enable Windows auto-logon on reboot")
	disable := fs.Bool("disable", false, "Disable session mode")
	fs.Parse(args)

	if *disable {
		return disableSession(workDir)
	}

	if *autoLogon && *userFlag == "" {
		return fmt.Errorf("--auto-logon requires --user and --password")
	}
	if *userFlag != "" && *passFlag == "" {
		return fmt.Errorf("--password is required when --user is specified")
	}

	return enableSession(workDir, *userFlag, *passFlag, *autoLogon)
}

func enableSession(workDir, user, password string, autoLogon bool) error {
	printStep("============================================")
	printStep(" BK-CI Agent Session Mode Configuration")
	printStep("============================================")

	hasCredentials := user != ""

	if hasCredentials {
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
	printStepf("creating service %s", serviceName)
	if out, err := runSc("create", serviceName, "binPath=", daemonPath, "start=", "auto"); err != nil {
		return fmt.Errorf("sc.exe create: %s (%w)", out, err)
	}

	if hasCredentials {
		storeLsaSecret(lsaKeyUser, user)
		storeLsaSecret(lsaKeyPassword, password)
		printStep("session credentials stored in LSA Secret (encrypted)")
	}

	if autoLogon {
		if err := enableAutoLogon(user, password); err != nil {
			return fmt.Errorf("enable auto-logon: %w", err)
		}
	}

	writeInstallType(workDir, "SESSION")

	printStepf("starting service %s", serviceName)
	runSc("start", serviceName)
	time.Sleep(3 * time.Second)

	printStep("")
	printStep("============================================")
	printStep(" Done")
	printStep("============================================")
	printStepf("Service      : %s", serviceName)

	if autoLogon {
		printStepf("Session user : %s", user)
		printStep("LogonUser    : enabled")
		printStep("Auto-logon   : enabled (every reboot)")
		printStep("")
		printStep("The agent is active in your current session NOW.")
		printStepf("On future reboots Windows auto-logs in as %s.", user)
		printStep("If the password changes, re-run with the new password.")
	} else if hasCredentials {
		printStepf("Session user : %s", user)
		printStep("LogonUser    : enabled")
		printStep("Auto-logon   : not configured")
		printStep("")
		printStep("The agent is active in your current session NOW.")
		printStep("When no user is logged in, daemon uses LogonUser fallback.")
		printStep("To also auto-logon on reboot, add --auto-logon.")
		printStep("If the password changes, re-run with the new password.")
	} else {
		printStep("Session user : (current logged-in user)")
		printStep("LogonUser    : not configured")
		printStep("Auto-logon   : not configured")
		printStep("")
		printStep("The agent is active in your current session NOW.")
		printStep("When no user is logged in, agent falls back to Session 0.")
		printStep("To enable fallback, add --user and --password.")
	}
	return nil
}

func disableSession(workDir string) error {
	printStep("Disabling session mode...")
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
	printStep("Session mode disabled. Agent will run in Session 0 unless a user is logged in.")
	return nil
}

// ── Credential validation ────────────────────────────────────────────────

func validateCredentials(account, password string) error {
	user, domain := splitUserDomain(account)
	printStepf("validating credentials for %s@%s ...", user, domain)

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
		return fmt.Errorf("credential validation failed: %s (Win32 %d)", msg, code)
	}
	windows.CloseHandle(token)
	printStep("credentials verified OK")
	return nil
}

func splitUserDomain(account string) (user, domain string) {
	for i := 0; i < len(account); i++ {
		if account[i] == '\\' {
			return account[i+1:], account[:i]
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
		return 0, fmt.Errorf("LsaOpenPolicy: NTSTATUS 0x%x: %w", ret, err)
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
		return fmt.Errorf("LsaStorePrivateData(%s): NTSTATUS 0x%x: %w", key, ret, callErr)
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
	printStep("session credentials removed from LSA Secret")
}

// ── Auto-logon (registry + LSA) ──────────────────────────────────────────

func enableAutoLogon(account, password string) error {
	user, domain := splitUserDomain(account)
	printStepf("configuring Windows auto-logon: user=%s, domain=%s", user, domain)

	k, _, err := registry.CreateKey(
		registry.LOCAL_MACHINE,
		`SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon`,
		registry.SET_VALUE,
	)
	if err != nil {
		return fmt.Errorf("open Winlogon key: %w", err)
	}
	defer k.Close()

	k.SetStringValue("AutoAdminLogon", "1")
	k.SetStringValue("DefaultUserName", user)
	k.SetStringValue("DefaultDomainName", domain)
	k.DeleteValue("DefaultPassword")
	k.DeleteValue("AutoLogonCount")

	if err := storeLsaSecret("DefaultPassword", password); err != nil {
		return fmt.Errorf("store auto-logon password: %w", err)
	}
	printStep("auto-logon password stored via LSA Secret (encrypted)")

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

	printStep("auto-logon configured (activates on next reboot)")
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
	printStep("Windows auto-logon disabled")
}

// ── Install type marker ──────────────────────────────────────────────────

const installTypeFile = ".install_type"

func writeInstallType(workDir, mode string) {
	p := filepath.Join(workDir, installTypeFile)
	if err := os.WriteFile(p, []byte(mode), 0644); err != nil {
		printWarn(fmt.Sprintf("failed to write %s: %v", installTypeFile, err))
		return
	}
	printStepf("install type set to %s", mode)
}

// ── sc.exe helper ────────────────────────────────────────────────────────

func runSc(args ...string) (string, error) {
	out, err := exec.Command("sc.exe", args...).CombinedOutput()
	return string(out), err
}
