//go:build windows
// +build windows

package command

import (
    "bytes"
    "fmt"
    "github.com/astaxie/beego/logs"
    "golang.org/x/sys/windows"
    "os"
    "os/exec"
    "strings"
    "unicode/utf16"
    "unsafe"
)

func RunCommand(command string, args []string, workDir string, envMap map[string]string) (output []byte, err error) {
    cmd := exec.Command(command)

    if len(args) > 0 {
        cmd.Args = append(cmd.Args, args...)
    }

    if workDir != "" {
        cmd.Dir = workDir
    }

    cmd.Env = os.Environ()
    if envMap != nil {
        for k, v := range envMap {
            cmd.Env = append(cmd.Env, fmt.Sprintf("%s=%s", k, v))
        }
    }

    logs.Info("cmd.Path: ", cmd.Path)
    logs.Info("cmd.Args: ", cmd.Args)
    logs.Info("cmd.workDir: ", cmd.Dir)

    outPut, err := cmd.CombinedOutput()
    logs.Info("output: ", string(outPut))
    if err != nil {
        return outPut, err
    }

    return outPut, nil
}

func StartProcess(command string, args []string, workDir string, envMap map[string]string, runUser string) (int, error) {
    commandLine := commandArgsConcat(command, args)
    logs.Info("windows commandLine:", commandLine)
    logs.Info("config user(only print):", runUser)
    logs.Info("workDir:", workDir)
    return StartProcessAsCurrentUser(commandLine, workDir, envMap)
}

func commandArgsConcat(command string, args []string) string {
    args_len := len(args)
    if args_len < 1 {
        return command
    }
    commandLine := command
    for i := 0; i < args_len; i++ {
        commandLine = strings.Join([]string{commandLine, args[i]}, " ")
    }
    return commandLine
}

var (
    modwtsapi32 *windows.LazyDLL = windows.NewLazySystemDLL("wtsapi32.dll")
    modkernel32 *windows.LazyDLL = windows.NewLazySystemDLL("kernel32.dll")
    modadvapi32 *windows.LazyDLL = windows.NewLazySystemDLL("advapi32.dll")
    moduserenv  *windows.LazyDLL = windows.NewLazySystemDLL("userenv.dll")

    procWTSEnumerateSessionsW        *windows.LazyProc = modwtsapi32.NewProc("WTSEnumerateSessionsW")
    procWTSGetActiveConsoleSessionId *windows.LazyProc = modkernel32.NewProc("WTSGetActiveConsoleSessionId")
    procWTSQueryUserToken            *windows.LazyProc = modwtsapi32.NewProc("WTSQueryUserToken")
    procDuplicateTokenEx             *windows.LazyProc = modadvapi32.NewProc("DuplicateTokenEx")
    procCreateEnvironmentBlock       *windows.LazyProc = moduserenv.NewProc("CreateEnvironmentBlock")
    procDestroyEnvironmentBlock      *windows.LazyProc = moduserenv.NewProc("DestroyEnvironmentBlock")
    procCreateProcessAsUser          *windows.LazyProc = modadvapi32.NewProc("CreateProcessAsUserW")
)

const (
    WTS_CURRENT_SERVER_HANDLE uintptr = 0
)

type HANDLE uintptr
type WTS_CONNECTSTATE_CLASS int

const (
    WTSActive WTS_CONNECTSTATE_CLASS = iota
    WTSConnected
    WTSConnectQuery
    WTSShadow
    WTSDisconnected
    WTSIdle
    WTSListen
    WTSReset
    WTSDown
    WTSInit
)

type SECURITY_IMPERSONATION_LEVEL int

const (
    SecurityAnonymous SECURITY_IMPERSONATION_LEVEL = iota
    SecurityIdentification
    SecurityImpersonation
    SecurityDelegation
)

type TOKEN_TYPE int

const (
    TokenPrimary TOKEN_TYPE = iota + 1
    TokenImpersonazion
)

type SW int

const (
    SW_HIDE            SW = 0
    SW_SHOWNORMAL         = 1
    SW_NORMAL             = 1
    SW_SHOWMINIMIZED      = 2
    SW_SHOWMAXIMIZED      = 3
    SW_MAXIMIZE           = 3
    SW_SHOWNOACTIVATE     = 4
    SW_SHOW               = 5
    SW_MINIMIZE           = 6
    SW_SHOWMINNOACTIVE    = 7
    SW_SHOWNA             = 8
    SW_RESTORE            = 9
    SW_SHOWDEFAULT        = 10
    SW_MAX                = 1
)

type WTS_SESSION_INFO struct {
    SessionID      windows.Handle
    WinStationName *uint16
    State          WTS_CONNECTSTATE_CLASS
}

const (
    CREATE_UNICODE_ENVIRONMENT uint32 = 0x00000400
    CREATE_NO_WINDOW                  = 0x08000000
    CREATE_NEW_CONSOLE                = 0x00000010
)

func GetCurrentUserSessionId() (windows.Handle, error) {
    sessionList, err := WTSEnumerateSessions()
    if err != nil {
        return 0xFFFFFFFF, fmt.Errorf("get current user session token: %s", err)
    }

    for i := range sessionList {
        if sessionList[i].State == WTSActive {
            return sessionList[i].SessionID, nil
        }
    }

    if sessionId, _, err := procWTSGetActiveConsoleSessionId.Call(); sessionId == 0xFFFFFFFF {
        return 0xFFFFFFFF, fmt.Errorf("call native WTSGetActiveConsoleSessionId: %s", err)
    } else {
        return windows.Handle(sessionId), nil
    }
}

func WTSEnumerateSessions() ([]*WTS_SESSION_INFO, error) {
    var (
        sessionInformation windows.Handle      = windows.Handle(0)
        sessionCount       int                 = 0
        sessionList        []*WTS_SESSION_INFO = make([]*WTS_SESSION_INFO, 0)
    )

    /*
       see microsoft windows/win32/api/wtsapi32/nf-wtsapi32-wtsenumeratesessionsw
    */
    if returnCode, _, err := procWTSEnumerateSessionsW.Call(
        WTS_CURRENT_SERVER_HANDLE,                    //  A handle to the RD Session Host server.
        0,                                            // This parameter is reserved. It must be zero.
        1,                                            // The version of the enumeration request. This parameter must be 1.
        uintptr(unsafe.Pointer(&sessionInformation)), // To free the returned buffer, call the WTSFreeMemory function?
        uintptr(unsafe.Pointer(&sessionCount))); returnCode == 0 {
        return nil, fmt.Errorf("call native WTSEnumerateSessionsW: %s", err)
    }

    structSize := unsafe.Sizeof(WTS_SESSION_INFO{})
    current := uintptr(sessionInformation)
    for i := 0; i < sessionCount; i++ {
        sessionList = append(sessionList, (*WTS_SESSION_INFO)(unsafe.Pointer(current)))
        current += structSize
    }

    return sessionList, nil
}

func DuplicateUserTokenFromSessionID(sessionId windows.Handle) (windows.Token, error) {
    var (
        impersonationToken windows.Handle = 0
        userToken          windows.Token  = 0
    )

    if returnCode, _, err := procWTSQueryUserToken.Call(
        uintptr(sessionId),
        uintptr(unsafe.Pointer(&impersonationToken))); returnCode == 0 {
        return 0xFFFFFFFF, fmt.Errorf("call native WTSQueryUserToken: %s", err)
    }

    if returnCode, _, err := procDuplicateTokenEx.Call(
        uintptr(impersonationToken),
        0,
        0,
        uintptr(SecurityImpersonation),
        uintptr(TokenPrimary),
        uintptr(unsafe.Pointer(&userToken))); returnCode == 0 {
        return 0xFFFFFFFF, fmt.Errorf("call native DuplicateTokenEx: %s", err)
    }

    if err := windows.CloseHandle(impersonationToken); err != nil {
        return 0xFFFFFFFF, fmt.Errorf("close windows handle used for token duplication: %s", err)
    }

    return userToken, nil
}

// fetch current process environment
func fetchProcessEnviron(userToken windows.Token) (env []string, e error) {

    var envInfo windows.Handle

    if returnCode, _, err := procCreateEnvironmentBlock.Call(
        uintptr(unsafe.Pointer(&envInfo)),
        uintptr(userToken),
        0); returnCode == 0 {
        return nil, fmt.Errorf("create environment: %s", err)
    }

    defer procDestroyEnvironmentBlock.Call(uintptr(envInfo))
    blockPtr := uintptr(unsafe.Pointer(envInfo))
    for {
        entry := windows.UTF16PtrToString((*uint16)(unsafe.Pointer(blockPtr)))
        if len(entry) == 0 {
            break
        }
        env = append(env, entry)
        blockPtr += 2 * (uintptr(len(entry)) + 1)
    }
    return env, nil
}

func appendEnvironmentBlock(userToken windows.Token, envMap map[string]string) (uintptr, error) {

    var endByte = []byte{0}

    var buffer bytes.Buffer

    cpEnvStrings, err := fetchProcessEnviron(userToken)
    if err != nil {
        return 0, err
    }

    logs.Info("current process env:", cpEnvStrings)

    if len(cpEnvStrings) > 0 {
        for _, cpEnvString := range cpEnvStrings {
            // add current process env
            buffer.WriteString(cpEnvString)
            buffer.Write(endByte)
        }
    }

    if len(envMap) == 0 {
        logs.Info("no append env")
        buffer.Write(endByte)
        return uintptr(unsafe.Pointer(&utf16.Encode([]rune(buffer.String()))[0])), nil
    }
tryNext:
    for k, v := range envMap {
        for i := 0; i < len(k); i++ {
            if k[i] == '=' || k[i] == 0 {
                logs.Warn("Skip bad env key: ", k)
                goto tryNext
            }
        }
        // add env
        buffer.WriteString(k)
        buffer.WriteString("=")
        buffer.WriteString(v)
        buffer.Write(endByte)
    }
    // end flag
    buffer.Write(endByte)

    logs.Info("value:", buffer.String())
    return uintptr(unsafe.Pointer(&utf16.Encode([]rune(buffer.String()))[0])), nil
}

func StartProcessAsCurrentUser(cmdLine, workDir string, envMap map[string]string) (int, error) {
    var (
        sessionId windows.Handle
        userToken windows.Token

        startupInfo windows.StartupInfo
        processInfo windows.ProcessInformation

        lpApplicationName   uintptr = 0
        lpCommandLine       uintptr = 0
        lpWorkingDir        uintptr = 0
        lpEnv               uintptr = 0
        lpProcessAttributes uintptr = 0
        lpThreadAttributes  uintptr = 0
        bInheritHandles     uintptr = 0
        err                 error
    )

    if sessionId, err = GetCurrentUserSessionId(); err != nil {
        return -1, err
    }

    if userToken, err = DuplicateUserTokenFromSessionID(sessionId); err != nil {
        return -1, fmt.Errorf("get duplicate user token for current user session: %s", err)
    }

    lpEnv, err = appendEnvironmentBlock(userToken, envMap)
    if err != nil {
        return -1, fmt.Errorf("append env block: %s", err)
    }


    creationFlags := CREATE_UNICODE_ENVIRONMENT | CREATE_NO_WINDOW //  | CREATE_NEW_CONSOLE
    startupInfo.ShowWindow = SW_SHOW
    startupInfo.Desktop = windows.StringToUTF16Ptr("winsta0\\default")

    if len(cmdLine) > 0 {
        lpCommandLine = uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(cmdLine)))
    }
    if len(workDir) > 0 {
        lpWorkingDir = uintptr(unsafe.Pointer(windows.StringToUTF16Ptr(workDir)))
    }

    if returnCode, _, err := procCreateProcessAsUser.Call(
        uintptr(userToken),
        lpApplicationName, // lpApplicationName 允许为空，此处统一使用lpCommandLine
        lpCommandLine,
        lpProcessAttributes,
        lpThreadAttributes,
        bInheritHandles,
        uintptr(creationFlags),
        lpEnv,
        lpWorkingDir,
        uintptr(unsafe.Pointer(&startupInfo)),
        uintptr(unsafe.Pointer(&processInfo)),
    ); returnCode == 0 {
        return -1, fmt.Errorf("create process as user: %s", err)
    }

    return int(processInfo.ProcessId), nil
}
