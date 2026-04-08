package agentcli

import (
	"fmt"
	"os"
	"path/filepath"
	"runtime"
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

// printUsageLocalized prints help in the detected language, filtered by current OS.
func printUsageLocalized() {
	isWin := runtime.GOOS == "windows"

	isMac := runtime.GOOS == "darwin"

	if useChinese {
		fmt.Print("用法: devopsAgent <命令> [选项]\n\n")
		fmt.Print("服务管理:\n")
		if isWin {
			fmt.Print(`  install [模式] [选项]  安装并启动 Agent 守护进程
    (默认)             安装为 Windows 服务
    session            安装为 Windows 服务 + 配置桌面会话访问
      --auto-logon 用户名 密码
                       配置 Windows 自动登录 (注销/重启后自动恢复)
    task               [已废弃] 安装为计划任务
`)
		} else if isMac {
			fmt.Print(`  install [模式]       安装并启动 Agent 守护进程
    (默认)             需要用户登录桌面, 直接启动进程
    background         无需登录, SSH/无头环境可用
`)
		} else {
			fmt.Print(`  install [模式]       安装并启动 Agent 守护进程
    (默认)             root: 系统级 systemd 服务 / 非 root: 直接启动
    service            安装为系统级 systemd 服务 (需要 root)
    user               安装为用户级 systemd 服务 (非 root, 注销后仍运行)
    direct             直接启动进程
`)
		}
		fmt.Print(`  uninstall            停止并卸载守护进程服务
  start                启动守护进程
  stop                 停止守护进程

  注: 模式相同时 install 不重复执行; 模式不同时自动先卸载再安装。

维护:
  repair               修复文件: 停止 → 重新解压依赖 → 重启
  reinstall            完全重装: 保留身份配置, 删除其余文件, 从服务端重新下载
    -y                 跳过二次确认
  status               显示运行状态 + 健康检查 (网络/磁盘/证书诊断)
`)
		fmt.Print(`
调试:
  debug [on|off]       切换调试模式 (修改后需重启 Agent 生效)
                       · 日志级别降低到 DEBUG, 输出更详细的运行信息
                       · 禁止自动升级, 方便本地调试
                       · Docker 构建结束后不自动删除容器, 便于排查问题

其他:
  version              打印版本号
    -f                 打印完整版本信息 (版本号 / Git Commit / 构建时间)
  -h, --help           显示此帮助
  (无参数)             正常运行 Agent
`)
	} else {
		fmt.Print("Usage: devopsAgent <command> [options]\n\n")
		fmt.Print("Service management:\n")
		if isWin {
			fmt.Print(`  install [mode] [options]  Install and start agent daemon
    (default)          Install as Windows service
    session            Install as service + configure desktop session access
      --auto-logon USER PASSWORD
                       Enable auto-logon (recovers after logoff/reboot)
    task               [deprecated] Install as scheduled task
`)
		} else if isMac {
			fmt.Print(`  install [mode]       Install and start agent daemon
    (default)          Requires user login session, direct process start
    background         No login needed, works over SSH and headless
`)
		} else {
			fmt.Print(`  install [mode]       Install and start agent daemon
    (default)          root: system systemd service / non-root: direct start
    service            Install as system-level systemd service (root)
    user               Install as user-level systemd service (non-root, survives logout)
    direct             Direct process start
`)
		}
		fmt.Print(`  uninstall            Stop and remove agent daemon service
  start                Start agent daemon
  stop                 Stop agent daemon

  Note: install skips if same mode; auto-uninstalls if switching modes.

Maintenance:
  repair               Repair files: stop -> re-extract dependencies -> restart
  reinstall            Full reinstall: keep identity, re-download everything from server
    -y                 Skip confirmation prompt
  status               Show status + health checks (network/disk/cert diagnostics)
`)
		fmt.Print(`
Debug:
  debug [on|off]       Toggle debug mode (restart agent to take effect)
                       Differences from normal mode:
                       · Log level set to DEBUG — more verbose output
                       · Auto-upgrade disabled — convenient for local debugging
                       · Docker containers kept after build — easier troubleshooting

Other:
  version              Print version
    -f                 Print full version info (version / Git commit / build time)
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
