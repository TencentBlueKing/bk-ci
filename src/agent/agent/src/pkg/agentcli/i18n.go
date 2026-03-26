package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

var useChinese bool

// initLang detects system language. Called once at the start of Run().
func initLang(workDir string) {
	lang := os.Getenv("LANG")
	if lang == "" {
		lang = os.Getenv("LC_ALL")
	}
	if lang == "" {
		lang = os.Getenv("LANGUAGE")
	}

	if lang == "" {
		if v, err := readProperty(workDir, "devops.language"); err == nil {
			lang = v
		}
	}

	if lang == "" {
		lang = detectPlatformLang()
	}

	useChinese = strings.HasPrefix(strings.ToLower(lang), "zh")
}

// msg returns the localized message: Chinese if detected, English otherwise.
func msg(en, zh string) string {
	if useChinese {
		return zh
	}
	return en
}

// msgf returns a formatted localized message.
func msgf(en, zh string, a ...interface{}) string {
	if useChinese {
		return fmt.Sprintf(zh, a...)
	}
	return fmt.Sprintf(en, a...)
}

// printUsageLocalized prints help in the detected language.
func printUsageLocalized() {
	if useChinese {
		fmt.Print(`用法: devopsAgent <命令> [选项]

服务管理:
  install              安装并启动 Agent 守护进程服务
  uninstall            停止并卸载守护进程服务
  start                启动守护进程
  stop                 停止守护进程

维护:
  repair               修复文件: 停止 → 重新解压依赖 → 重启
  reinstall             完全重装: 保留身份配置, 删除其余文件, 从服务端重新下载
    -y                 跳过二次确认
  status               显示当前运行模式和配置状态

会话模式 (仅 Windows):
  configure-session    配置桌面会话访问
    --user 用户名      Windows 登录账号 (可选)
    --password 密码    账号密码 (指定 --user 时必填)
    --auto-logon       配置 Windows 自动登录
    --disable          取消会话模式

其他:
  version              打印版本号
  fullVersion          打印完整版本信息
  -h, --help           显示此帮助
  (无参数)             正常运行 Agent
`)
	} else {
		fmt.Print(`Usage: devopsAgent <command> [options]

Service management:
  install              Install and start agent daemon service
  uninstall            Stop and remove agent daemon service
  start                Start agent daemon
  stop                 Stop agent daemon

Maintenance:
  repair               Repair files: stop -> re-extract dependencies -> restart
  reinstall            Full reinstall: keep identity, re-download everything from server
    -y                 Skip confirmation prompt
  status               Show current running mode and configuration

Session mode (Windows only):
  configure-session    Configure desktop session access
    --user USER        Windows logon account (optional)
    --password PASS    Password (required with --user)
    --auto-logon       Enable Windows auto-logon on reboot
    --disable          Revert to plain service mode

Other:
  version              Print version
  fullVersion          Print full version info
  -h, --help           Show this help
  (no command)         Run agent (normal mode)
`)
	}
}

// tryReadLang attempts to read language from .agent.properties without error.
func tryReadLang(workDir string) string {
	propsPath := filepath.Join(workDir, ".agent.properties")
	data, err := os.ReadFile(propsPath)
	if err != nil {
		return ""
	}
	for _, line := range strings.Split(string(data), "\n") {
		line = strings.TrimSpace(line)
		if strings.HasPrefix(line, "devops.language=") {
			return strings.TrimPrefix(line, "devops.language=")
		}
	}
	return ""
}
