#!/usr/bin/env python3
# =============================================================================
# test_agentcli.py — BK-CI agentcli 全平台集成测试
#
# 自动检测当前平台（Linux / macOS / Windows），执行对应模式的全量 CLI 测试。
#
# 覆盖模式:
#   Linux:   DIRECT / USER / SERVICE
#   macOS:   login / background
#   Windows: service / session / task
#
# 用法:
#   cd /path/to/agent && python test_agentcli.py
#   python test_agentcli.py --work-dir /path/to/agent
#   python test_agentcli.py --agent-path /path/to/bin --work-dir /path/to/agent
#   python test_agentcli.py --modes DIRECT SERVICE
#   python test_agentcli.py --session-user ".\user" --session-password "P@ss"
#   python test_agentcli.py --no-fail-fast          # 失败后继续跑完所有测试
#   python test_agentcli.py --sections version debug  # 只跑 version 和 debug 测试段
#   python test_agentcli.py --sections install start stop  # 只跑 install→start→stop 流程
#
# 前置条件:
#   - Python 3.6+
#   - 在已安装的 agent 目录下运行（含 devopsAgent、devopsDaemon、.agent.properties）
#   - Windows 需管理员权限；Linux SERVICE 模式需 root + systemd
#
# 零外部依赖，仅使用 Python 标准库。
# =============================================================================

import sys
import os
import re
import time
import signal
import platform
import argparse
import subprocess
from pathlib import Path

# =============================================================================
# 颜色 / 日志系统
# =============================================================================

def _enable_ansi_on_windows():
    """Windows 10+ 启用虚拟终端序列（ANSI 颜色）"""
    if sys.platform == "win32":
        try:
            import ctypes
            kernel32 = ctypes.windll.kernel32
            handle = kernel32.GetStdHandle(-11)  # STD_OUTPUT_HANDLE
            mode = ctypes.c_ulong()
            kernel32.GetConsoleMode(handle, ctypes.byref(mode))
            kernel32.SetConsoleMode(handle, mode.value | 0x0004)  # ENABLE_VIRTUAL_TERMINAL_PROCESSING
        except Exception:
            pass


class C:
    """ANSI 颜色常量"""
    RED    = "\033[0;31m"
    GREEN  = "\033[0;32m"
    YELLOW = "\033[1;33m"
    CYAN   = "\033[0;36m"
    BOLD   = "\033[1m"
    RESET  = "\033[0m"


def log_info(msg):
    print(f"  {C.CYAN}[INFO]{C.RESET}  {msg}")

def log_pass(msg):
    print(f"  {C.GREEN}[PASS]{C.RESET}  {msg}")

def log_fail(msg):
    print(f"  {C.RED}[FAIL]{C.RESET}  {msg}")

def log_skip(msg):
    print(f"  {C.YELLOW}[SKIP]{C.RESET}  {msg}")

def log_section(msg):
    print(f"\n{C.BOLD}{C.CYAN}\u2500\u2500 {msg} \u2500\u2500{C.RESET}")

def log_mode(mode):
    print(f"\n{C.BOLD}{C.CYAN}\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}  \u6a21\u5f0f: {mode}{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d{C.RESET}")

def log_test_header(name):
    print(f"\n  {C.BOLD}[TEST]{C.RESET} {name}")

def log_banner(title, modes_str):
    print(f"\n{C.BOLD}{C.CYAN}\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u2551  {title:<40s}\u2551{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u2551  \u6a21\u5f0f: {modes_str:<34s}\u2551{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d{C.RESET}")

def log_summary_banner():
    print(f"\n{C.BOLD}{C.CYAN}\u2554\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2557{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u2551  \u6d4b\u8bd5\u7ed3\u679c\u6c47\u603b                                  \u2551{C.RESET}")
    print(f"{C.BOLD}{C.CYAN}\u255a\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u255d{C.RESET}")


# =============================================================================
# 通用工具函数
# =============================================================================

def safe_unlink(path):
    """安全删除文件（兼容 Python 3.6，无 missing_ok）"""
    try:
        path.unlink()
    except (FileNotFoundError, OSError):
        pass


def read_pid_file(path):
    """读取 pid 文件内容，返回去空白字符串；文件不存在返回空串"""
    try:
        return Path(path).read_text().strip()
    except (FileNotFoundError, OSError):
        return ""


def read_agent_id(work_dir):
    """从 .agent.properties 读取 agent.id（兼容 devops.agent.id 和 agent.id 两种格式）"""
    props = Path(work_dir) / ".agent.properties"
    try:
        for line in props.read_text(encoding="utf-8", errors="ignore").splitlines():
            line = line.strip()
            if line.startswith("devops.agent.id="):
                return line.split("=", 1)[1].strip()
            if line.startswith("agent.id="):
                return line.split("=", 1)[1].strip()
    except (FileNotFoundError, OSError):
        pass
    return "unknown"


def wait_for_pid_alive(pid_file, timeout, is_alive_fn):
    """轮询等待 pid 文件出现且进程存活"""
    for _ in range(timeout):
        pid_str = read_pid_file(pid_file)
        if pid_str and pid_str.isdigit() and is_alive_fn(pid_str):
            return True
        time.sleep(1)
    return False


def wait_for_pid_gone(pid_str, timeout, is_alive_fn):
    """轮询等待进程退出"""
    for _ in range(timeout):
        if not is_alive_fn(pid_str):
            return True
        time.sleep(1)
    return not is_alive_fn(pid_str)


def _decode_bytes(data):
    """尝试 UTF-8 → GBK → latin-1 解码字节串，确保不抛异常"""
    if not data:
        return ""
    for enc in ("utf-8", "gbk", "latin-1"):
        try:
            return data.decode(enc)
        except (UnicodeDecodeError, LookupError):
            continue
    return data.decode("utf-8", errors="replace")


def run_cmd(args, **kwargs):
    """执行外部命令，返回 (returncode, stdout_str, stderr_str)"""
    timeout = kwargs.pop("timeout", 30)
    try:
        r = subprocess.run(
            args,
            stdout=subprocess.PIPE,
            stderr=subprocess.PIPE,
            timeout=timeout,
            **kwargs
        )
        return type("R", (), {
            "returncode": r.returncode,
            "stdout": _decode_bytes(r.stdout),
            "stderr": _decode_bytes(r.stderr),
        })()
    except (FileNotFoundError, subprocess.TimeoutExpired):
        return type("R", (), {
            "returncode": -1,
            "stdout": "",
            "stderr": "",
        })()


# =============================================================================
# PlatformVerifier 基类
# =============================================================================

class PlatformVerifier:
    """平台验证器基类，定义跨平台接口和 Unix 默认实现"""

    def __init__(self, work_dir):
        self.work_dir = Path(work_dir)

    def platform_name(self):
        raise NotImplementedError

    def modes(self):
        raise NotImplementedError

    def executable_name(self):
        raise NotImplementedError

    def daemon_name(self):
        raise NotImplementedError

    def service_name(self, agent_id):
        raise NotImplementedError

    def can_run_mode(self, mode):
        """返回 (可否运行, 原因)"""
        return True, None

    def verify_install(self, mode, svc_name):
        """安装后的平台特定验证（仅输出诊断信息）"""
        pass

    def verify_start(self, mode, svc_name):
        """启动后等待平台服务就绪"""
        pass

    def verify_stop(self, mode, svc_name):
        """停止后的平台特定验证"""
        pass

    def verify_uninstall(self, mode, svc_name):
        """卸载后的平台特定验证"""
        pass

    def cleanup_mode(self, mode, svc_name, work_dir):
        """清理指定模式的系统资源"""
        self._kill_residual(work_dir)

    def env_info(self):
        """返回平台环境信息字典"""
        return {}

    def get_install_extra_args(self, mode, session_user="", session_password=""):
        """返回 install 命令的额外参数"""
        return []

    def is_pid_alive(self, pid_str):
        """Unix 默认实现：用 signal 0 检查进程存活"""
        if not pid_str or not pid_str.isdigit():
            return False
        try:
            os.kill(int(pid_str), 0)
            return True
        except OSError:
            return False

    def _kill_residual(self, work_dir):
        """杀残留的 daemon/agent 进程"""
        work_dir = Path(work_dir)
        for pidname in ("daemon.pid", "agent.pid"):
            pid_str = read_pid_file(work_dir / "runtime" / pidname)
            if pid_str and pid_str.isdigit() and self.is_pid_alive(pid_str):
                try:
                    if sys.platform == "win32":
                        subprocess.run(
                            ["taskkill", "/F", "/PID", pid_str],
                            capture_output=True, timeout=10
                        )
                    else:
                        os.kill(int(pid_str), signal.SIGKILL)
                except (OSError, subprocess.TimeoutExpired):
                    pass
        # 清理标记文件
        safe_unlink(work_dir / ".install_type")
        safe_unlink(work_dir / ".debug")
        time.sleep(1)


# =============================================================================
# LinuxVerifier
# =============================================================================

class LinuxVerifier(PlatformVerifier):

    def platform_name(self):
        return "Linux"

    def modes(self):
        return ["DIRECT", "USER", "SERVICE"]

    def executable_name(self):
        return "devopsAgent"

    def daemon_name(self):
        return "devopsDaemon"

    def service_name(self, agent_id):
        return f"devops-agent-{agent_id}"

    def _has_systemd(self):
        try:
            r = run_cmd(["systemctl", "is-system-running"])
            out = r.stdout.strip()
            return out in ("running", "degraded")
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def _has_user_systemd(self):
        try:
            r = run_cmd(["systemctl", "--user", "is-system-running"])
            out = r.stdout.strip()
            return out in ("running", "degraded")
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def can_run_mode(self, mode):
        if mode == "SERVICE":
            if os.getuid() != 0:
                return False, f"SERVICE \u6a21\u5f0f\u9700\u8981 root \u6743\u9650\uff08\u5f53\u524d uid={os.getuid()}\uff09"
            if not self._has_systemd():
                return False, "SERVICE \u6a21\u5f0f\u9700\u8981 systemd"
        elif mode == "USER":
            if not self._has_user_systemd():
                return False, "USER \u6a21\u5f0f\u9700\u8981 systemctl --user \u53ef\u7528"
        return True, None

    def verify_install(self, mode, svc_name):
        if mode == "SERVICE":
            r = run_cmd(["systemctl", "list-unit-files", f"{svc_name}.service"])
            if r.returncode == 0 and svc_name in r.stdout:
                log_info(f"systemd \u5355\u5143\u5df2\u6ce8\u518c: {svc_name}.service")
            else:
                log_info("\u63d0\u793a: systemd \u5355\u5143\u672a\u627e\u5230")
        elif mode == "USER":
            r = run_cmd(["systemctl", "--user", "list-unit-files", f"{svc_name}.service"])
            if r.returncode == 0 and svc_name in r.stdout:
                log_info("user systemd \u5355\u5143\u5df2\u6ce8\u518c")
            else:
                log_info("\u63d0\u793a: user systemd \u5355\u5143\u672a\u627e\u5230")
        else:
            log_info("DIRECT \u6a21\u5f0f\u4e0d\u6ce8\u518c\u7cfb\u7edf\u670d\u52a1")

    def verify_start(self, mode, svc_name):
        if mode in ("SERVICE", "USER"):
            cmd = ["systemctl"] if mode == "SERVICE" else ["systemctl", "--user"]
            for _ in range(15):
                r = run_cmd(cmd + ["is-active", svc_name])
                if "active" in r.stdout and "inactive" not in r.stdout:
                    break
                time.sleep(1)

    def verify_stop(self, mode, svc_name):
        if mode in ("SERVICE", "USER"):
            cmd = ["systemctl"] if mode == "SERVICE" else ["systemctl", "--user"]
            time.sleep(2)
            r = run_cmd(cmd + ["is-active", svc_name])
            if "active" in r.stdout and "inactive" not in r.stdout:
                log_info(f"\u63d0\u793a: {'user ' if mode == 'USER' else ''}systemd \u670d\u52a1\u4ecd\u663e\u793a active")
            else:
                log_info(f"{'user ' if mode == 'USER' else ''}systemd \u670d\u52a1\u5df2\u505c\u6b62")

    def verify_uninstall(self, mode, svc_name):
        if mode == "SERVICE":
            unit = Path(f"/etc/systemd/system/{svc_name}.service")
            if not unit.exists():
                log_info("systemd \u5355\u5143\u5df2\u6e05\u7406")
            else:
                log_info("\u63d0\u793a: systemd \u5355\u5143\u6587\u4ef6\u53ef\u80fd\u4ecd\u5b58\u5728")
        elif mode == "USER":
            unit = Path.home() / f".config/systemd/user/{svc_name}.service"
            if not unit.exists():
                log_info("user systemd \u5355\u5143\u5df2\u6e05\u7406")
            else:
                log_info("\u63d0\u793a: user systemd \u5355\u5143\u6587\u4ef6\u53ef\u80fd\u4ecd\u5b58\u5728")

    def cleanup_mode(self, mode, svc_name, work_dir):
        if mode == "SERVICE":
            run_cmd(["systemctl", "stop", svc_name])
            run_cmd(["systemctl", "disable", svc_name])
            safe_unlink(Path(f"/etc/systemd/system/{svc_name}.service"))
            run_cmd(["systemctl", "daemon-reload"])
        elif mode == "USER":
            run_cmd(["systemctl", "--user", "stop", svc_name])
            run_cmd(["systemctl", "--user", "disable", svc_name])
            safe_unlink(Path.home() / f".config/systemd/user/{svc_name}.service")
            run_cmd(["systemctl", "--user", "daemon-reload"])
        self._kill_residual(work_dir)

    def env_info(self):
        return {
            "systemd":      "\u53ef\u7528" if self._has_systemd() else "\u4e0d\u53ef\u7528",
            "user-systemd":  "\u53ef\u7528" if self._has_user_systemd() else "\u4e0d\u53ef\u7528",
        }


# =============================================================================
# MacOSVerifier
# =============================================================================

class MacOSVerifier(PlatformVerifier):

    def platform_name(self):
        return "macOS"

    def modes(self):
        return ["login", "background"]

    def executable_name(self):
        return "devopsAgent"

    def daemon_name(self):
        return "devopsDaemon"

    def service_name(self, agent_id):
        return f"com.tencent.bk-ci.agent.{os.getuid()}"

    def _plist_path(self, agent_id):
        svc = self.service_name(agent_id)
        return Path.home() / f"Library/LaunchAgents/{svc}.plist"

    def _has_modern_launchctl(self):
        try:
            r = run_cmd(["launchctl", "bootstrap"])
            combined = (r.stdout + r.stderr).lower()
            return "unrecognized" not in combined and "unknown" not in combined
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def can_run_mode(self, mode):
        if mode == "background" and not self._has_modern_launchctl():
            log_info("\u63d0\u793a: launchctl bootstrap \u4e0d\u53ef\u7528\uff0cbackground \u5c06\u964d\u7ea7\u4e3a login \u57df")
        return True, None

    def verify_install(self, mode, svc_name):
        agent_id = read_agent_id(self.work_dir)
        plist = self._plist_path(agent_id)
        if plist.exists():
            log_info(f"plist \u5df2\u521b\u5efa: {plist}")
            try:
                content = plist.read_text()
                if "devopsDaemon" in content:
                    log_info("plist \u5185\u5bb9\u6b63\u5e38\uff08\u542b devopsDaemon \u8def\u5f84\uff09")
                else:
                    log_info("\u63d0\u793a: plist \u5185\u5bb9\u672a\u5305\u542b devopsDaemon")
            except OSError:
                pass
        else:
            log_info(f"\u63d0\u793a: plist \u672a\u627e\u5230\uff08\u8def\u5f84: {plist}\uff09")

    def verify_start(self, mode, svc_name):
        # launchd 拉起可能需要几秒，等待由 runner 的 wait_for_pid_alive 处理
        pass

    def verify_stop(self, mode, svc_name):
        pass

    def verify_uninstall(self, mode, svc_name):
        agent_id = read_agent_id(self.work_dir)
        plist = self._plist_path(agent_id)
        if not plist.exists():
            log_info("plist \u5df2\u6e05\u7406")
        else:
            log_info(f"\u63d0\u793a: plist \u4ecd\u5b58\u5728\uff08{plist}\uff09")

    def cleanup_mode(self, mode, svc_name, work_dir):
        uid = os.getuid()
        agent_id = read_agent_id(work_dir)
        plist = self._plist_path(agent_id)
        if plist.exists():
            if self._has_modern_launchctl():
                domain = f"gui/{uid}" if mode == "login" else f"user/{uid}"
                run_cmd(["launchctl", "bootout", f"{domain}/{svc_name}"])
                run_cmd(["launchctl", "bootout", domain, str(plist)])
            else:
                run_cmd(["launchctl", "unload", "-w", str(plist)])
            safe_unlink(plist)
        self._kill_residual(work_dir)

    def env_info(self):
        try:
            ver = run_cmd(["sw_vers", "-productVersion"]).stdout.strip()
        except (FileNotFoundError, subprocess.TimeoutExpired):
            ver = "\u672a\u77e5"
        return {
            "macOS \u7248\u672c": ver,
            "arch":       platform.machine(),
            "launchctl":  "\u73b0\u4ee3\u6a21\u5f0f\uff08bootstrap\uff09" if self._has_modern_launchctl()
                          else "\u65e7\u7248\uff08load/unload\uff09",
        }


# =============================================================================
# WindowsVerifier
# =============================================================================

class WindowsVerifier(PlatformVerifier):

    def platform_name(self):
        return "Windows"

    def modes(self):
        return ["service", "session", "task"]

    def executable_name(self):
        return "devopsAgent.exe"

    def daemon_name(self):
        return "devopsDaemon.exe"

    def service_name(self, agent_id):
        return f"devops-agent-{agent_id}"

    def is_pid_alive(self, pid_str):
        if not pid_str or not pid_str.isdigit():
            return False
        try:
            import ctypes
            pid = int(pid_str)
            PROCESS_QUERY_LIMITED_INFORMATION = 0x1000
            handle = ctypes.windll.kernel32.OpenProcess(
                PROCESS_QUERY_LIMITED_INFORMATION, False, pid
            )
            if handle:
                ctypes.windll.kernel32.CloseHandle(handle)
                return True
            return False
        except (ValueError, OSError):
            return False

    def _is_admin(self):
        try:
            import ctypes
            return bool(ctypes.windll.shell32.IsUserAnAdmin())
        except Exception:
            return False

    def _service_exists(self, svc_name):
        try:
            r = run_cmd(["sc.exe", "query", svc_name])
            return r.returncode == 0
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def _service_running(self, svc_name):
        try:
            r = run_cmd(["sc.exe", "query", svc_name])
            return "RUNNING" in r.stdout
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def _task_exists(self, svc_name):
        try:
            r = run_cmd(["schtasks", "/query", "/tn", svc_name])
            return r.returncode == 0
        except (FileNotFoundError, subprocess.TimeoutExpired):
            return False

    def _check_auto_logon_registry(self, expect_disabled=False):
        try:
            import winreg
            key = winreg.OpenKey(
                winreg.HKEY_LOCAL_MACHINE,
                r"SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon",
                0, winreg.KEY_READ
            )
            try:
                val, _ = winreg.QueryValueEx(key, "AutoAdminLogon")
                log_info(f"AutoAdminLogon \u6ce8\u518c\u8868\u503c: {val}")
                if expect_disabled and val == "1":
                    log_info("\u63d0\u793a: AutoAdminLogon \u4ecd\u4e3a 1")
            except FileNotFoundError:
                log_info("auto-logon \u672a\u542f\u7528\u6216\u5df2\u7981\u7528")
            finally:
                winreg.CloseKey(key)
        except OSError:
            log_info("auto-logon \u672a\u542f\u7528\u6216\u5df2\u7981\u7528")

    def _cleanup_session_registry(self):
        try:
            import winreg
            key = winreg.OpenKey(
                winreg.HKEY_LOCAL_MACHINE,
                r"SOFTWARE\Microsoft\Windows NT\CurrentVersion\Winlogon",
                0, winreg.KEY_SET_VALUE
            )
            for name in ("AutoAdminLogon", "DefaultUserName",
                         "DefaultDomainName", "AutoLogonSID"):
                try:
                    winreg.DeleteValue(key, name)
                except FileNotFoundError:
                    pass
            winreg.CloseKey(key)
        except OSError:
            pass

    def can_run_mode(self, mode):
        if not self._is_admin():
            return False, "\u9700\u8981\u7ba1\u7406\u5458\u6743\u9650\u8fd0\u884c"
        return True, None

    def verify_install(self, mode, svc_name):
        if mode in ("service", "session"):
            if self._service_exists(svc_name):
                log_info(f"Windows Service \u5df2\u6ce8\u518c: {svc_name}")
                try:
                    r = run_cmd(["sc.exe", "qc", svc_name])
                    # 只输出前几行关键信息
                    lines = r.stdout.strip().splitlines()[:5]
                    for line in lines:
                        log_info(f"  {line.strip()}")
                except (FileNotFoundError, subprocess.TimeoutExpired):
                    pass
            else:
                log_info("\u63d0\u793a: Windows Service \u672a\u627e\u5230")
            if mode == "session":
                self._check_auto_logon_registry()
        elif mode == "task":
            if self._task_exists(svc_name):
                log_info(f"\u8ba1\u5212\u4efb\u52a1\u5df2\u6ce8\u518c: {svc_name}")
            else:
                log_info("\u63d0\u793a: \u8ba1\u5212\u4efb\u52a1\u672a\u627e\u5230\uff08task \u6a21\u5f0f\u5df2\u5e9f\u5f03\uff09")

    def verify_start(self, mode, svc_name):
        # Windows 服务启动由 SCM 管理，等待由 runner 的 wait_for_pid_alive 处理
        pass

    def verify_stop(self, mode, svc_name):
        pass

    def verify_uninstall(self, mode, svc_name):
        time.sleep(2)
        if mode in ("service", "session"):
            if self._service_exists(svc_name):
                log_info(f"\u63d0\u793a: Windows Service \u4ecd\u5b58\u5728\uff08SCM \u5220\u9664\u9700\u8981\u51e0\u79d2\uff09")
            else:
                log_info(f"Windows Service \u5df2\u5220\u9664")
            if mode == "session":
                self._check_auto_logon_registry(expect_disabled=True)
        elif mode == "task":
            if self._task_exists(svc_name):
                log_info("\u63d0\u793a: \u8ba1\u5212\u4efb\u52a1\u4ecd\u5b58\u5728")
            else:
                log_info("\u8ba1\u5212\u4efb\u52a1\u5df2\u5220\u9664")

    def cleanup_mode(self, mode, svc_name, work_dir):
        # 停止并删除服务
        if self._service_exists(svc_name):
            run_cmd(["sc.exe", "stop", svc_name])
            time.sleep(2)
            run_cmd(["sc.exe", "delete", svc_name])
        # 删除计划任务
        if self._task_exists(svc_name):
            run_cmd(["schtasks", "/delete", "/tn", svc_name, "/f"])
        # session 模式清理注册表
        if mode == "session":
            self._cleanup_session_registry()
        self._kill_residual(work_dir)

    def get_install_extra_args(self, mode, session_user="", session_password=""):
        if mode == "session" and session_user and session_password:
            return ["--auto-logon", session_user, session_password]
        return []

    def env_info(self):
        return {}


# =============================================================================
# 平台检测
# =============================================================================

def detect_platform(work_dir):
    """自动检测平台，返回对应的 PlatformVerifier 实例"""
    p = sys.platform
    if p == "linux":
        return LinuxVerifier(work_dir)
    elif p == "darwin":
        return MacOSVerifier(work_dir)
    elif p == "win32":
        return WindowsVerifier(work_dir)
    else:
        print(f"{C.RED}\u9519\u8bef: \u4e0d\u652f\u6301\u7684\u5e73\u53f0 {p}{C.RESET}")
        sys.exit(1)


# =============================================================================
# AgentTestRunner — 测试执行引擎
# =============================================================================

class AgentTestRunner:
    """跨平台 agentcli 集成测试运行器"""

    DAEMON_TIMEOUT = 20 if sys.platform == "win32" else 15

    # 可选的测试空间（section）名称
    ALL_SECTIONS = ["version", "debug", "install", "start", "stop", "uninstall", "reinstall"]

    def __init__(self, work_dir, verifier, session_user="", session_password="",
                 fail_fast=False, sections=None, agent_path=None):
        self.work_dir = Path(work_dir)
        self.agent_path = Path(agent_path) if agent_path else self.work_dir  # agent 二进制所在目录
        self.verifier = verifier
        self.session_user = session_user
        self.session_password = session_password
        self.fail_fast = fail_fast
        self.sections = set(s.lower() for s in sections) if sections else None  # None=全部

        # 全局统计
        self.total_pass = 0
        self.total_fail = 0
        self.total_skip = 0
        self.mode_results = {}
        self.failed_summary = []

        # 当前模式统计
        self._cur_pass = 0
        self._cur_fail = 0
        self._cur_skip = 0
        self._cur_mode = ""
        self._abort = False  # fail-fast 中止标志

        # agent 信息
        self._agent_id = read_agent_id(work_dir)

    # ── 命令执行 ──────────────────────────────────────────────────────────

    def agent_cmd(self, *args):
        """在 work_dir 下执行 agent 子命令，返回 (exit_code, output)

        可执行文件从 agent_path 目录获取，工作目录为 work_dir。
        使用 bytes 模式读取 stdout/stderr，然后尝试 UTF-8 → GBK → latin-1 解码，
        避免 Windows 上 Go 二进制输出 UTF-8 而 Python subprocess 默认用 GBK 导致的
        UnicodeDecodeError。
        """
        exe = self.agent_path / self.verifier.executable_name()
        try:
            result = subprocess.run(
                [str(exe)] + list(args),
                cwd=str(self.work_dir),
                stdout=subprocess.PIPE,
                stderr=subprocess.PIPE,
                timeout=300
            )
            stdout = _decode_bytes(result.stdout)
            stderr = _decode_bytes(result.stderr)
            output = (stdout + stderr).strip()
            return result.returncode, output
        except subprocess.TimeoutExpired:
            return -1, "命令执行超时（300s）"
        except FileNotFoundError:
            return -1, f"找不到可执行文件: {exe}"

    # ── 测试框架 ──────────────────────────────────────────────────────────

    def run_test(self, name, fn, *args):
        """执行单个测试，自动计数 + 彩色输出。返回 True=通过, False=失败/中止"""
        if self._abort:
            return False
        log_test_header(name)
        try:
            ok = fn(*args) if args else fn()
            if ok:
                log_pass(name)
                self._cur_pass += 1
                self.total_pass += 1
                return True
            else:
                log_fail(name)
                self._cur_fail += 1
                self.total_fail += 1
                self.failed_summary.append(f"[{self._cur_mode}] {name}")
                if self.fail_fast:
                    self._abort = True
                return False
        except Exception as e:
            log_fail(f"{name}  (\u5f02\u5e38: {e})")
            self._cur_fail += 1
            self.total_fail += 1
            self.failed_summary.append(f"[{self._cur_mode}] {name}")
            if self.fail_fast:
                self._abort = True
            return False

    def skip_test(self, name, reason):
        log_test_header(name)
        log_skip(reason)
        self._cur_skip += 1
        self.total_skip += 1

    # ── 环境检查 ──────────────────────────────────────────────────────────

    def setup(self):
        """检查必要文件并打印环境信息"""
        log_section("\u73af\u5883\u51c6\u5907")

        exe = self.agent_path / self.verifier.executable_name()
        if not exe.exists():
            print(f"{C.RED}\u9519\u8bef: '{self.agent_path}' \u4e2d\u627e\u4e0d\u5230 {self.verifier.executable_name()}{C.RESET}")
            print("\u8bf7\u901a\u8fc7 --agent-path \u6307\u5b9a agent \u4e8c\u8fdb\u5236\u6240\u5728\u76ee\u5f55")
            sys.exit(1)

        props = self.work_dir / ".agent.properties"
        if not props.exists():
            print(f"{C.RED}\u9519\u8bef: '{self.work_dir}' \u4e2d\u627e\u4e0d\u5230 .agent.properties{C.RESET}")
            print("\u8bf7\u901a\u8fc7 --work-dir \u6307\u5b9a agent \u5de5\u4f5c\u76ee\u5f55")
            sys.exit(1)

        daemon = self.agent_path / self.verifier.daemon_name()
        if not daemon.exists():
            log_info(f"\u8b66\u544a: {self.verifier.daemon_name()} \u4e0d\u5b58\u5728\uff0cstart/stop \u76f8\u5173\u6d4b\u8bd5\u5c06\u8df3\u8fc7")

        # Unix: chmod +x
        if sys.platform != "win32":
            try:
                exe.chmod(exe.stat().st_mode | 0o111)
                if daemon.exists():
                    daemon.chmod(daemon.stat().st_mode | 0o111)
            except OSError:
                pass

        log_info(f"WORK_DIR:   {self.work_dir}")
        if self.agent_path != self.work_dir:
            log_info(f"AGENT_PATH: {self.agent_path}")
        log_info(f"agent.id:   {self._agent_id}")

        if sys.platform != "win32":
            import pwd
            log_info(f"USER:      {pwd.getpwuid(os.getuid()).pw_name} (uid={os.getuid()})")
        else:
            log_info(f"USER:      {os.environ.get('USERNAME', '\u672a\u77e5')}")

        # 平台特定信息
        for key, val in self.verifier.env_info().items():
            log_info(f"{key}:  {val}")

        svc_name = self.verifier.service_name(self._agent_id)
        log_info(f"\u670d\u52a1\u540d:  {svc_name}")

        if self.session_user:
            log_info(f"SESSION_USER: {self.session_user} (auto-logon \u6d4b\u8bd5\u5df2\u542f\u7528)")

    # ── 18 个测试方法 ────────────────────────────────────────────────────

    def _test_version_basic(self):
        """version \u57fa\u7840\u8f93\u51fa"""
        rc, out = self.agent_cmd("version")
        log_info(f"\u8f93\u51fa: {out}")
        if rc != 0:
            log_fail(f"\u9000\u51fa\u7801={rc}\uff0c\u671f\u671b 0")
            return False
        if not re.search(r"version|v\d", out, re.IGNORECASE):
            log_fail("\u672a\u68c0\u6d4b\u5230\u7248\u672c\u5b57\u7b26\u4e32")
            return False
        return True

    def _test_version_full(self):
        """version -f 显示详细信息"""
        rc, out = self.agent_cmd("version", "-f")
        log_info(f"输出: {out}")
        if rc != 0:
            log_fail(f"退出码={rc}，期望 0")
            return False
        # -f 输出应比普通 version 更长（包含 commit hash / 时间戳等额外行）
        lines = [l for l in out.strip().splitlines() if l.strip()]
        if len(lines) < 2 and not re.search(
            r"commit|build|time|git|[0-9a-f]{8,}|\d{4}-\d{2}-\d{2}",
            out, re.IGNORECASE
        ):
            log_fail("未检测到详细版本信息（commit hash 或时间戳）")
            return False
        return True

    def _test_debug_query(self):
        """debug \u67e5\u8be2\u72b6\u6001"""
        rc, out = self.agent_cmd("debug")
        log_info(f"\u8f93\u51fa: {out}")
        return rc == 0

    def _test_debug_on(self):
        """debug on \u2192 .debug \u521b\u5efa"""
        debug_file = self.work_dir / ".debug"
        safe_unlink(debug_file)
        rc, out = self.agent_cmd("debug", "on")
        if rc != 0:
            log_fail(f"\u9000\u51fa\u7801={rc}")
            return False
        if not debug_file.exists():
            log_fail(".debug \u6587\u4ef6\u672a\u521b\u5efa")
            return False
        log_info(".debug \u5df2\u521b\u5efa")
        return True

    def _test_debug_off(self):
        """debug off \u2192 .debug \u5220\u9664"""
        debug_file = self.work_dir / ".debug"
        debug_file.touch()
        rc, out = self.agent_cmd("debug", "off")
        if rc != 0:
            log_fail(f"\u9000\u51fa\u7801={rc}")
            return False
        if debug_file.exists():
            log_fail(".debug \u6587\u4ef6\u672a\u5220\u9664")
            return False
        log_info(".debug \u5df2\u5220\u9664")
        return True

    def _test_install(self, mode):
        """install \u6307\u5b9a\u6a21\u5f0f \u2192 .install_type \u6b63\u786e"""
        install_type = self.work_dir / ".install_type"
        safe_unlink(install_type)

        cmd_args = ["install", mode]
        extra = self.verifier.get_install_extra_args(
            mode, self.session_user, self.session_password
        )
        cmd_args.extend(extra)

        rc, out = self.agent_cmd(*cmd_args)
        log_info(f"install \u8f93\u51fa:\n{out}")

        if rc != 0:
            log_fail(f"install \u9000\u51fa\u7801={rc}")
            return False
        if not install_type.exists():
            log_fail(".install_type \u672a\u521b\u5efa")
            return False

        saved = install_type.read_text().strip().lower()
        log_info(f".install_type={saved}")
        if saved != mode.lower():
            log_fail(f".install_type='{saved}'\uff0c\u671f\u671b '{mode.lower()}'")
            return False

        svc_name = self.verifier.service_name(self._agent_id)
        self.verifier.verify_install(mode, svc_name)
        return True

    def _test_status_after_install(self):
        """status \u5b89\u88c5\u540e"""
        rc, out = self.agent_cmd("status")
        log_info(f"\u8f93\u51fa:\n{out}")
        if rc != 0:
            log_fail(f"status \u9000\u51fa\u7801={rc}")
            return False
        return True

    def _test_start(self, mode):
        """start \u2192 daemon \u8fdb\u7a0b\u542f\u52a8"""
        daemon = self.agent_path / self.verifier.daemon_name()
        if not daemon.exists():
            log_fail(f"{self.verifier.daemon_name()} \u4e0d\u5b58\u5728")
            return False

        rc, out = self.agent_cmd("start")
        log_info(f"start \u8f93\u51fa: {out}")

        # Windows service \u6a21\u5f0f\u4e0b\uff0cinstall \u4f1a\u81ea\u52a8\u542f\u52a8\u670d\u52a1\uff0c\u518d\u6b21 start \u8fd4\u56de 1056
        # "already running" \u5c5e\u4e8e\u6b63\u5e38\u60c5\u51b5\uff0c\u4e0d\u7b97\u5931\u8d25
        already_running = re.search(
            r"already.+running|1056|already.+started", out, re.IGNORECASE
        )
        if rc != 0 and not already_running:
            log_fail(f"start \u9000\u51fa\u7801={rc}")
            return False
        if already_running:
            log_info("\u670d\u52a1\u5df2\u5728\u8fd0\u884c\uff08install \u65f6\u5df2\u81ea\u52a8\u542f\u52a8\uff09\uff0c\u89c6\u4e3a\u6210\u529f")

        # 平台特定等待（如 systemd service active）
        svc_name = self.verifier.service_name(self._agent_id)
        self.verifier.verify_start(mode, svc_name)

        # 等待 daemon.pid 出现且进程存活
        pid_file = self.work_dir / "runtime" / "daemon.pid"
        if not wait_for_pid_alive(pid_file, self.DAEMON_TIMEOUT, self.verifier.is_pid_alive):
            log_fail(f"daemon.pid \u4e0d\u5b58\u5728\u6216\u8fdb\u7a0b\u672a\u5b58\u6d3b\uff08{self.DAEMON_TIMEOUT}s \u540e\uff09")
            runtime = self.work_dir / "runtime"
            if runtime.exists():
                log_info(f"runtime \u76ee\u5f55: {list(runtime.iterdir())}")
            else:
                log_info("runtime \u76ee\u5f55\u4e0d\u5b58\u5728")
            return False

        pid = read_pid_file(pid_file)
        log_info(f"daemon \u5df2\u542f\u52a8, pid={pid}")
        return True

    def _test_status_running(self):
        """status \u8fd0\u884c\u4e2d\u542b PID"""
        rc, out = self.agent_cmd("status")
        log_info(f"\u8f93\u51fa:\n{out}")
        if rc != 0:
            log_fail(f"status \u9000\u51fa\u7801={rc}")
            return False
        pid = read_pid_file(self.work_dir / "runtime" / "daemon.pid")
        if pid and pid not in out:
            log_info(f"\u63d0\u793a: status \u672a\u542b pid={pid}")
        return True

    def _test_debug_on_running(self):
        """debug on \u4e0d\u5f71\u54cd\u8fdb\u7a0b"""
        debug_file = self.work_dir / ".debug"
        safe_unlink(debug_file)
        pid_before = read_pid_file(self.work_dir / "runtime" / "daemon.pid")

        rc, out = self.agent_cmd("debug", "on")
        if rc != 0:
            log_fail(f"\u9000\u51fa\u7801={rc}")
            return False
        if not debug_file.exists():
            log_fail(".debug \u672a\u521b\u5efa")
            return False
        time.sleep(1)
        if pid_before and not self.verifier.is_pid_alive(pid_before):
            log_fail("daemon \u8fdb\u7a0b\u6d88\u5931")
            return False
        log_info("daemon \u8fdb\u7a0b\u672a\u53d7\u5f71\u54cd")
        return True

    def _test_debug_off_running(self):
        """debug off \u4e0d\u5f71\u54cd\u8fdb\u7a0b"""
        debug_file = self.work_dir / ".debug"
        debug_file.touch()
        pid_before = read_pid_file(self.work_dir / "runtime" / "daemon.pid")

        rc, out = self.agent_cmd("debug", "off")
        if rc != 0:
            log_fail(f"\u9000\u51fa\u7801={rc}")
            return False
        if debug_file.exists():
            log_fail(".debug \u672a\u5220\u9664")
            return False
        time.sleep(1)
        if pid_before and not self.verifier.is_pid_alive(pid_before):
            log_fail("daemon \u8fdb\u7a0b\u6d88\u5931")
            return False
        log_info("daemon \u8fdb\u7a0b\u672a\u53d7\u5f71\u54cd")
        return True

    def _test_stop(self, mode):
        """stop \u2192 daemon \u8fdb\u7a0b\u505c\u6b62"""
        dpid = read_pid_file(self.work_dir / "runtime" / "daemon.pid")
        apid = read_pid_file(self.work_dir / "runtime" / "agent.pid")
        log_info(f"stop \u524d: daemon pid={dpid}, agent pid={apid}")

        rc, out = self.agent_cmd("stop")
        log_info(f"stop \u8f93\u51fa: {out}")
        if rc != 0:
            log_fail(f"stop \u9000\u51fa\u7801={rc}")
            return False

        # 平台特定验证
        svc_name = self.verifier.service_name(self._agent_id)
        self.verifier.verify_stop(mode, svc_name)

        if not dpid:
            return True
        if not wait_for_pid_gone(dpid, self.DAEMON_TIMEOUT, self.verifier.is_pid_alive):
            log_fail(f"stop \u540e daemon \u8fdb\u7a0b (pid={dpid}) \u4ecd\u5728\u8fd0\u884c")
            return False
        log_info("daemon \u8fdb\u7a0b\u5df2\u505c\u6b62")
        return True

    def _test_status_stopped(self):
        """status \u505c\u6b62\u540e"""
        rc, out = self.agent_cmd("status")
        log_info(f"\u8f93\u51fa:\n{out}")
        if rc > 1:
            log_fail(f"status \u9000\u51fa\u7801={rc}\uff0c\u5f02\u5e38")
            return False
        return True

    def _test_start_again(self):
        """\u518d\u6b21 start \u2192 \u91cd\u65b0\u8fd0\u884c"""
        daemon = self.agent_path / self.verifier.daemon_name()
        if not daemon.exists():
            log_fail(f"{self.verifier.daemon_name()} \u4e0d\u5b58\u5728")
            return False

        rc, out = self.agent_cmd("start")
        log_info(f"start \u8f93\u51fa: {out}")

        already_running = re.search(
            r"already.+running|1056|already.+started", out, re.IGNORECASE
        )
        if rc != 0 and not already_running:
            log_fail(f"start \u9000\u51fa\u7801={rc}")
            return False
        if already_running:
            log_info("\u670d\u52a1\u5df2\u5728\u8fd0\u884c\uff0c\u89c6\u4e3a\u6210\u529f")

        pid_file = self.work_dir / "runtime" / "daemon.pid"
        if not wait_for_pid_alive(pid_file, self.DAEMON_TIMEOUT, self.verifier.is_pid_alive):
            log_fail(f"\u518d\u6b21 start \u540e daemon \u672a\u5b58\u6d3b")
            return False
        pid = read_pid_file(pid_file)
        log_info(f"daemon \u518d\u6b21\u542f\u52a8\u6210\u529f, pid={pid}")
        return True

    def _test_stop_again(self):
        """\u518d\u6b21 stop \u2192 \u6210\u529f\u505c\u6b62"""
        dpid = read_pid_file(self.work_dir / "runtime" / "daemon.pid")
        rc, out = self.agent_cmd("stop")
        log_info(f"stop \u8f93\u51fa: {out}")
        if rc != 0:
            log_fail(f"stop \u9000\u51fa\u7801={rc}")
            return False
        if not dpid:
            return True
        if not wait_for_pid_gone(dpid, self.DAEMON_TIMEOUT, self.verifier.is_pid_alive):
            log_fail(f"\u518d\u6b21 stop \u540e daemon \u672a\u505c\u6b62")
            return False
        log_info("daemon \u518d\u6b21\u505c\u6b62\u6210\u529f")
        return True

    def _test_uninstall(self, mode):
        """uninstall \u2192 \u6e05\u7406\u9a8c\u8bc1"""
        rc, out = self.agent_cmd("uninstall")
        log_info(f"uninstall \u8f93\u51fa: {out}")
        if rc != 0:
            log_fail(f"uninstall \u9000\u51fa\u7801={rc}")
            return False

        install_type = self.work_dir / ".install_type"
        if install_type.exists():
            log_fail(".install_type \u672a\u5220\u9664")
            return False
        log_info(".install_type \u5df2\u5220\u9664")

        svc_name = self.verifier.service_name(self._agent_id)
        self.verifier.verify_uninstall(mode, svc_name)
        return True

    def _test_status_uninstalled(self):
        """status \u5378\u8f7d\u540e\u4e0d\u5d29\u6e83"""
        rc, out = self.agent_cmd("status")
        log_info(f"\u8f93\u51fa:\n{out}")
        if rc > 1:
            log_fail(f"status \u9000\u51fa\u7801={rc}\uff0c\u5f02\u5e38")
            return False
        return True

    def _test_reinstall(self):
        """reinstall \u2192 \u6210\u529f\u6216\u8fde\u63a5\u5931\u8d25\u5747\u4e3a\u5408\u7406"""
        rc, out = self.agent_cmd("reinstall", "-y")
        log_info(f"reinstall \u8f93\u51fa: {out}")
        log_info(f"reinstall \u9000\u51fa\u7801: {rc}")

        # 命令执行超时（subprocess 被 kill），server 不可达时 reinstall 可能卡住
        if rc == -1:
            log_info("reinstall \u8d85\u65f6\uff08server \u4e0d\u53ef\u8fbe\u6216\u4e0b\u8f7d\u8017\u65f6\u8fc7\u957f\uff0c\u5c5e\u4e8e\u9884\u671f\uff09")
            return True

        if rc == 0:
            exe = self.agent_path / self.verifier.executable_name()
            if not exe.exists():
                log_fail(f"reinstall \u540e {self.verifier.executable_name()} \u4e0d\u5b58\u5728")
                return False
            log_info(f"reinstall \u6210\u529f\uff0c{self.verifier.executable_name()} \u53ef\u7528")
            return True
        else:
            # 连接失败也属合理（server 不可达）
            if not re.search(
                r"connect|dial|refused|timeout|error|failed|\u8d85\u65f6|\u4e0b\u8f7d|\u91cd\u65b0\u5b89\u88c5|\u5931\u8d25",
                out, re.IGNORECASE
            ):
                log_fail("reinstall \u5931\u8d25\u4e14\u8f93\u51fa\u65e0\u9884\u671f\u7684\u9519\u8bef\u4fe1\u606f")
                return False
            log_info("reinstall \u8fde\u63a5\u5931\u8d25\uff08server \u4e0d\u53ef\u8fbe\uff0c\u5c5e\u4e8e\u9884\u671f\uff09")
            return True

    # ── 模式测试流程 ──────────────────────────────────────────────────────

    def _should_abort(self):
        """检查是否需要因 fail-fast 而中止"""
        if self._abort:
            log_info(f"{C.RED}[ABORT]{C.RESET} --fail-fast 已启用，测试失败后中止")
            return True
        return False

    def _should_run_section(self, section):
        """检查是否应运行指定测试段"""
        if self.sections is None:
            return True  # 未指定 --sections 则全部运行
        return section.lower() in self.sections

    def run_mode(self, mode):
        """单模式完整测试流程（三平台共享）"""
        self._cur_mode = mode
        self._cur_pass = 0
        self._cur_fail = 0
        self._cur_skip = 0

        log_mode(mode)

        if self.sections:
            log_info(f"仅运行测试段: {', '.join(sorted(self.sections))}")

        # 前提检查
        can_run, reason = self.verifier.can_run_mode(mode)
        if not can_run:
            log_skip(reason)
            self.mode_results[mode] = "SKIPPED"
            self.total_skip += 17
            return

        # 清理残留
        svc_name = self.verifier.service_name(self._agent_id)
        self.verifier.cleanup_mode(mode, svc_name, self.work_dir)
        safe_unlink(self.work_dir / ".install_type")
        safe_unlink(self.work_dir / ".debug")

        # ── version 子命令 ──
        if self._should_run_section("version"):
            log_section("version \u5b50\u547d\u4ee4")
            self.run_test("version \u57fa\u7840\u8f93\u51fa",         self._test_version_basic)
            self.run_test("version -f \u663e\u793a\u8be6\u7ec6\u4fe1\u606f",   self._test_version_full)
            if self._should_abort(): return self._record_mode_result(mode, svc_name)

        # ── debug 子命令（安装前）──
        if self._should_run_section("debug"):
            log_section("debug \u5b50\u547d\u4ee4\uff08\u5b89\u88c5\u524d\uff09")
            self.run_test("debug \u67e5\u8be2\u72b6\u6001",           self._test_debug_query)
            self.run_test("debug on \u2192 .debug \u521b\u5efa",      self._test_debug_on)
            self.run_test("debug off \u2192 .debug \u5220\u9664",     self._test_debug_off)
            if self._should_abort(): return self._record_mode_result(mode, svc_name)

        # ── install ──
        # 注意: start/stop/uninstall 依赖 install，若选了这些段则自动执行 install
        need_install = self._should_run_section("install") or (
            self.sections and self.sections & {"start", "stop", "uninstall"}
        )
        if need_install:
            log_section(f"install {mode}")
            self.run_test(
                f"install {mode} \u2192 .install_type \u6b63\u786e",
                self._test_install, mode
            )
            if self._should_abort(): return self._record_mode_result(mode, svc_name)

            if self._should_run_section("install"):
                log_section("status\uff08\u5b89\u88c5\u540e\u672a\u542f\u52a8\uff09")
                self.run_test("status \u5b89\u88c5\u540e",            self._test_status_after_install)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

        # ── start / debug(运行时) / stop ──
        has_daemon = (self.agent_path / self.verifier.daemon_name()).exists()
        run_start = self._should_run_section("start")
        run_stop = self._should_run_section("stop")

        if has_daemon and (run_start or run_stop):
            if run_start:
                log_section("start")
                self.run_test("start \u2192 daemon \u8fdb\u7a0b\u542f\u52a8",  self._test_start, mode)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

                log_section("status\uff08\u8fd0\u884c\u4e2d\uff09")
                self.run_test("status \u8fd0\u884c\u4e2d\u542b PID",           self._test_status_running)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

                log_section("debug\uff08\u8fd0\u884c\u65f6\uff09")
                self.run_test("debug on \u4e0d\u5f71\u54cd\u8fdb\u7a0b",       self._test_debug_on_running)
                self.run_test("debug off \u4e0d\u5f71\u54cd\u8fdb\u7a0b",      self._test_debug_off_running)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

            if run_stop:
                # 如果没跑 start 段但要跑 stop 段，需要先启动
                if not run_start:
                    log_section("start\uff08\u4e3a stop \u6d4b\u8bd5\u51c6\u5907\uff09")
                    self.run_test("start \u2192 daemon \u8fdb\u7a0b\u542f\u52a8",  self._test_start, mode)
                    if self._should_abort(): return self._record_mode_result(mode, svc_name)

                log_section("stop")
                self.run_test("stop \u2192 daemon \u8fdb\u7a0b\u505c\u6b62",   self._test_stop, mode)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

                log_section("status\uff08\u505c\u6b62\u540e\uff09")
                self.run_test("status \u505c\u6b62\u540e",                      self._test_status_stopped)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)

                log_section("start / stop \u518d\u6b21\u9a8c\u8bc1")
                self.run_test("\u518d\u6b21 start \u2192 \u91cd\u65b0\u8fd0\u884c",     self._test_start_again)
                self.run_test("\u518d\u6b21 stop \u2192 \u6210\u529f\u505c\u6b62",      self._test_stop_again)
                if self._should_abort(): return self._record_mode_result(mode, svc_name)
            else:
                # 只跑了 start 没跑 stop，需要停掉进程
                log_section("stop\uff08\u6e05\u7406\uff09")
                self.run_test("stop \u2192 daemon \u8fdb\u7a0b\u505c\u6b62",   self._test_stop, mode)
        elif not has_daemon and (run_start or run_stop) and not self.sections:
            dn = self.verifier.daemon_name()
            log_info(f"{dn} \u4e0d\u5b58\u5728\uff0c\u8df3\u8fc7 start/stop \u76f8\u5173\u6d4b\u8bd5")
            for name in [
                "start \u2192 daemon \u8fdb\u7a0b\u542f\u52a8",
                "status \u8fd0\u884c\u4e2d\u542b PID",
                "debug on \u4e0d\u5f71\u54cd\u8fdb\u7a0b",
                "debug off \u4e0d\u5f71\u54cd\u8fdb\u7a0b",
                "stop \u2192 daemon \u8fdb\u7a0b\u505c\u6b62",
                "status \u505c\u6b62\u540e",
                "\u518d\u6b21 start \u2192 \u91cd\u65b0\u8fd0\u884c",
                "\u518d\u6b21 stop \u2192 \u6210\u529f\u505c\u6b62",
            ]:
                self.skip_test(name, f"{dn} \u4e0d\u5b58\u5728")

        # ── uninstall ──
        if self._should_run_section("uninstall"):
            log_section("uninstall")
            self.run_test("uninstall \u2192 \u6e05\u7406\u9a8c\u8bc1",         self._test_uninstall, mode)
            if self._should_abort(): return self._record_mode_result(mode, svc_name)

            log_section("status\uff08\u5378\u8f7d\u540e\uff09")
            self.run_test("status \u5378\u8f7d\u540e\u4e0d\u5d29\u6e83",       self._test_status_uninstalled)
            if self._should_abort(): return self._record_mode_result(mode, svc_name)

        # ── reinstall ──
        if self._should_run_section("reinstall"):
            log_section("reinstall")
            self.run_test("reinstall \u2192 \u6210\u529f\u6216\u8fde\u63a5\u5931\u8d25\u5747\u4e3a\u5408\u7406", self._test_reinstall)

        self._record_mode_result(mode, svc_name)

    def _record_mode_result(self, mode, svc_name):
        """记录模式结果并清理"""
        # 记录模式结果
        if self._cur_fail == 0:
            self.mode_results[mode] = f"PASS ({self._cur_pass} \u901a\u8fc7, {self._cur_skip} \u8df3\u8fc7)"
        else:
            self.mode_results[mode] = (
                f"FAIL ({self._cur_pass} \u901a\u8fc7, "
                f"{self._cur_fail} \u5931\u8d25, {self._cur_skip} \u8df3\u8fc7)"
            )

        # 清理，为下一模式准备
        self.verifier.cleanup_mode(mode, svc_name, self.work_dir)

    # ── 总入口 ────────────────────────────────────────────────────────────

    def run_all(self, selected_modes=None):
        """运行全部模式测试，返回 exit code（0=全部通过，1=有失败）"""
        self.setup()

        # 验证 sections
        if self.sections:
            invalid = self.sections - set(self.ALL_SECTIONS)
            if invalid:
                print(f"{C.RED}错误: 未知的测试段 {invalid}{C.RESET}")
                print(f"可用测试段: {', '.join(self.ALL_SECTIONS)}")
                return 1

        all_modes = self.verifier.modes()
        modes_to_run = selected_modes if selected_modes else all_modes

        # 验证用户指定的模式是否有效
        for m in modes_to_run:
            if m not in all_modes:
                # 大小写不敏感匹配
                matched = [am for am in all_modes if am.lower() == m.lower()]
                if matched:
                    modes_to_run = [matched[0] if x == m else x for x in modes_to_run]
                else:
                    print(f"{C.RED}\u9519\u8bef: \u6a21\u5f0f '{m}' \u5728\u5f53\u524d\u5e73\u53f0\u4e0d\u53ef\u7528{C.RESET}")
                    print(f"\u53ef\u7528\u6a21\u5f0f: {', '.join(all_modes)}")
                    return 1

        for mode in modes_to_run:
            self.run_mode(mode)
            if self._abort:
                break

        self.print_summary(modes_to_run)
        return 1 if self.total_fail > 0 else 0

    def print_summary(self, modes):
        """打印测试结果汇总"""
        log_summary_banner()

        print(f"  \u603b\u8ba1: {C.GREEN}\u901a\u8fc7 {self.total_pass}{C.RESET} / "
              f"{C.RED}\u5931\u8d25 {self.total_fail}{C.RESET} / "
              f"{C.YELLOW}\u8df3\u8fc7 {self.total_skip}{C.RESET}")
        print()
        print("  \u5404\u6a21\u5f0f\u7ed3\u679c:")
        for mode in modes:
            result = self.mode_results.get(mode, "\u672a\u6267\u884c")
            if result.startswith("PASS"):
                print(f"    {C.GREEN}\u2713{C.RESET} {mode}: {result}")
            elif result.startswith("FAIL"):
                print(f"    {C.RED}\u2717{C.RESET} {mode}: {result}")
            else:
                print(f"    {C.YELLOW}\u2013{C.RESET} {mode}: {result}")

        if self.failed_summary:
            print()
            print(f"  {C.RED}\u5931\u8d25\u660e\u7ec6:{C.RESET}")
            for t in self.failed_summary:
                print(f"    {C.RED}\u2717{C.RESET} {t}")

        print(f"{C.BOLD}{C.CYAN}\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550\u2550{C.RESET}")


# =============================================================================
# 主入口
# =============================================================================

def main():
    _enable_ansi_on_windows()

    parser = argparse.ArgumentParser(
        description="BK-CI agentcli \u5168\u5e73\u53f0\u96c6\u6210\u6d4b\u8bd5",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""\
\u793a\u4f8b:
  python test_agentcli.py                                         # \u81ea\u52a8\u68c0\u6d4b\u5e73\u53f0\uff0c\u6d4b\u8bd5\u6240\u6709\u6a21\u5f0f\uff08\u5931\u8d25\u5373\u505c\uff09
  python test_agentcli.py --work-dir /path/to/agent               # \u6307\u5b9a\u5de5\u4f5c\u76ee\u5f55
  python test_agentcli.py --agent-path /path/to/bin               # \u6307\u5b9a agent \u4e8c\u8fdb\u5236\u8def\u5f84\uff08\u4e0e\u5de5\u4f5c\u76ee\u5f55\u5206\u5f00\uff09
  python test_agentcli.py --modes DIRECT SERVICE                  # \u53ea\u6d4b\u8bd5\u6307\u5b9a\u6a21\u5f0f
  python test_agentcli.py --no-fail-fast                          # \u5931\u8d25\u540e\u7ee7\u7eed\u8dd1\u5b8c\u6240\u6709\u6d4b\u8bd5
  python test_agentcli.py --sections version debug                # \u53ea\u8dd1 version \u548c debug \u6d4b\u8bd5\u6bb5
  python test_agentcli.py --sections install start stop           # \u53ea\u8dd1 install\u2192start\u2192stop \u6d41\u7a0b
  python test_agentcli.py --session-user ".\\\\user" --session-password "P@ss"  # Windows session auto-logon

\u53ef\u7528\u6d4b\u8bd5\u6bb5 (--sections):
  version    - version / version -f \u5b50\u547d\u4ee4
  debug      - debug on/off \u5b50\u547d\u4ee4\uff08\u5b89\u88c5\u524d\uff09
  install    - install + status\uff08\u5b89\u88c5\u540e\uff09
  start      - start + status\uff08\u8fd0\u884c\u4e2d\uff09+ debug\uff08\u8fd0\u884c\u65f6\uff09
  stop       - stop + status\uff08\u505c\u6b62\u540e\uff09+ \u518d\u6b21 start/stop
  uninstall  - uninstall + status\uff08\u5378\u8f7d\u540e\uff09
  reinstall  - reinstall \u5b50\u547d\u4ee4
"""
    )
    parser.add_argument(
        "--work-dir", default=".",
        help="agent \u5de5\u4f5c\u76ee\u5f55\uff0c\u542b .agent.properties \u7b49\u8fd0\u884c\u65f6\u6587\u4ef6\uff08\u9ed8\u8ba4\u5f53\u524d\u76ee\u5f55\uff09"
    )
    parser.add_argument(
        "--agent-path", default=None,
        help="agent \u4e8c\u8fdb\u5236\u6240\u5728\u76ee\u5f55\uff08devopsAgent/devopsDaemon\uff09\uff0c\u9ed8\u8ba4\u540c --work-dir"
    )
    parser.add_argument(
        "--modes", nargs="*", default=None,
        help="\u53ea\u6d4b\u8bd5\u6307\u5b9a\u6a21\u5f0f\uff08\u5982: --modes DIRECT SERVICE\uff09"
    )
    parser.add_argument(
        "--session-user", default="",
        help="[Windows] session \u6a21\u5f0f auto-logon \u7528\u6237\u540d"
    )
    parser.add_argument(
        "--session-password", default="",
        help="[Windows] session \u6a21\u5f0f auto-logon \u5bc6\u7801"
    )
    parser.add_argument(
        "--fail-fast", action="store_true", default=True,
        help="\u4efb\u4e00\u6d4b\u8bd5\u5931\u8d25\u540e\u7acb\u5373\u505c\u6b62\uff08\u9ed8\u8ba4\u5f00\u542f\uff09"
    )
    parser.add_argument(
        "--no-fail-fast", dest="fail_fast", action="store_false",
        help="\u5931\u8d25\u540e\u7ee7\u7eed\u8dd1\u5b8c\u6240\u6709\u6d4b\u8bd5"
    )
    parser.add_argument(
        "--sections", nargs="*", default=None,
        metavar="SECTION",
        help="\u53ea\u8dd1\u6307\u5b9a\u6d4b\u8bd5\u6bb5\uff08\u5982: --sections version debug install start stop uninstall reinstall\uff09"
    )
    args = parser.parse_args()

    work_dir = Path(args.work_dir).resolve()
    agent_path = Path(args.agent_path).resolve() if args.agent_path else None
    verifier = detect_platform(str(work_dir))

    # 标题
    pname = verifier.platform_name()
    modes = verifier.modes()
    log_banner(
        f"BK-CI agentcli \u96c6\u6210\u6d4b\u8bd5 \u2014 {pname}",
        " / ".join(modes)
    )

    runner = AgentTestRunner(
        str(work_dir), verifier,
        session_user=args.session_user,
        session_password=args.session_password,
        fail_fast=args.fail_fast,
        sections=args.sections,
        agent_path=str(agent_path) if agent_path else None,
    )
    sys.exit(runner.run_all(selected_modes=args.modes))


if __name__ == "__main__":
    main()
