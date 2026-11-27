"""Exercise the publisher and native updater in a temporary Agent installation."""
import contextlib
from functools import partial
from http.server import SimpleHTTPRequestHandler, ThreadingHTTPServer
import io
import os
from pathlib import Path
import platform
import shutil
import subprocess
import sys
import tarfile
import tempfile
import threading
import time
import unittest
import zipfile


TOOLS = Path(__file__).resolve().parents[1] / "manual-upgrade"
WIN = os.name == "nt"
SYSTEM = "windows" if WIN else platform.system().lower()
ARCH = {"x86_64": "amd64", "aarch64": "arm64", "loongarch64": "loong64",
        "x86": "386"}.get(platform.machine().lower(), platform.machine().lower())
TARGET = f"{SYSTEM}-{ARCH}"


class ArchivedReleaseTest(unittest.TestCase):
    def test_pack_selected_releases_without_build_tools(self):
        with tempfile.TemporaryDirectory() as tmp:
            work = Path(tmp)
            for target, binary in (("windows-amd64", "devopsAgent.exe"),
                                   ("linux-loong64", "devopsAgent")):
                for version in ("v1", "v2"):
                    source = work / "signed releases" / version / target
                    source.mkdir(parents=True)
                    payload = b"\x00\xff\r\n" + version.encode() + bytes(range(256))
                    (source / binary).write_bytes(payload)
                    (source / (binary + ".sig")).write_bytes(b"\xff\x00signature")
                    out = work / "publish" / version
                    subprocess.run([sys.executable, str(TOOLS / "pack.py"), "--source", str(source),
                                    "--output", str(out), "--target", target], cwd=work,
                                   env=dict(os.environ, PATH=""), check=True, stdout=subprocess.PIPE)
                # Read both after publication to catch accidental replacement across versions.
                for version in ("v1", "v2"):
                    source = work / "signed releases" / version / target
                    out = work / "publish" / version
                    expected = {binary: b"\x00\xff\r\n" + version.encode() + bytes(range(256)),
                                binary + ".sig": b"\xff\x00signature"}
                    if target.startswith("windows"):
                        with zipfile.ZipFile(out / (target + ".zip")) as archive:
                            actual = {name: archive.read(name) for name in expected}
                    else:
                        with tarfile.open(out / (target + ".tar.gz")) as archive:
                            actual = {name: archive.extractfile(name).read() for name in expected}
                    self.assertEqual(actual, expected)
                    self.assertEqual({name: (source / name).read_bytes() for name in expected}, expected)


class UpgradeTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.fixture = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.fixture.cleanup)
        path = Path(cls.fixture.name)
        if WIN:
            cls.exe = path / "fixture.exe"
            # Two real, isolated processes. No machine-wide service registration.
            code = """
using System;
using System.Diagnostics;
using System.IO;
using System.Threading;
class Fixture {
    static void Main(string[] args) {
        if (args.Length > 0 && args[0] == "status") {
            Console.WriteLine("fixture status ok");
            return;
        }
        var process = Process.GetCurrentProcess();
        var exe = process.MainModule.FileName;
        var root = Path.GetDirectoryName(exe);
        // Multiple fixture instances can publish the same PID file concurrently.
        for (int attempt = 0; ; attempt++) {
            try { File.WriteAllText(exe + ".testpid", process.Id.ToString()); break; }
            catch (IOException) { if (attempt == 5) throw; Thread.Sleep(20); }
        }
        if (Path.GetFileName(exe) == "devopsDaemon.exe") {
            Process.Start(new ProcessStartInfo(Path.Combine(root, "devopsAgent.exe")) {
                WorkingDirectory = root, UseShellExecute = false, CreateNoWindow = true
            });
        }
        Thread.Sleep(180000);
    }
}"""
            subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                            "Add-Type -TypeDefinition $env:FIXTURE_CS -OutputAssembly $env:FIXTURE_EXE "
                            "-OutputType ConsoleApplication -ErrorAction Stop"],
                           env=dict(os.environ, FIXTURE_CS=code, FIXTURE_EXE=str(cls.exe)), check=True)
        else:
            cls.exe = Path(sys.executable).resolve()
            (path / "sitecustomize.py").write_text(
                "import os, sys, subprocess, time\nfrom pathlib import Path\n"
                "exe = Path(sys.executable)\n"
                "if 'status' in sys.argv[1:]:\n"
                "    print('fixture status ok')\n"
                "elif exe.name in ('devopsDaemon', 'devopsAgent'):\n"
                "    Path(str(exe) + '.testpid').write_text(str(os.getpid()))\n"
                "    if exe.name == 'devopsDaemon': subprocess.Popen([str(exe.with_name('devopsAgent'))])\n"
                "    time.sleep(180)\n"
            )

    def stop_fixture(self):
        if WIN:
            subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                            "Get-Process devopsDaemon,devopsAgent -ErrorAction SilentlyContinue | "
                            "Where-Object { $_.Path -and [IO.Path]::GetDirectoryName($_.Path) -eq $env:FIXTURE_DIR } | "
                            "ForEach-Object { $_.Kill(); $_.WaitForExit(5000) | Out-Null }"],
                           env=dict(os.environ, FIXTURE_DIR=str(self.root)), check=False)
        else:
            for name in ("devopsDaemon", "devopsAgent"):
                file = self.root / (name + ".testpid")
                if file.exists():
                    pid = int(file.read_text())
                    exe = Path(f"/proc/{pid}/exe")
                    if SYSTEM == "linux":
                        matches = exe.exists() and exe.resolve() == self.root / name
                    else:
                        out = subprocess.run(["ps", "-ww", "-p", str(pid), "-o", "comm="], stdout=subprocess.PIPE)
                        matches = os.fsdecode(out.stdout).strip() == str(self.root / name)
                    if matches:
                        os.kill(pid, 15)

    def setUp(self):
        tmp = tempfile.TemporaryDirectory()
        self.addCleanup(tmp.cleanup)
        self.work = Path(tmp.name).resolve()
        self.root = self.work / "old agent"
        self.root.mkdir()
        self.addCleanup(self.stop_fixture)
        self.env = os.environ.copy()
        if not WIN:
            self.env.update(PYTHONPATH=self.fixture.name, PYTHONHOME=sys.base_prefix)
        self.daemon = self.root / ("devopsDaemon.exe" if WIN else "devopsDaemon")
        self.agent = self.root / ("devopsAgent.exe" if WIN else "devopsAgent")
        for dest in (self.daemon, self.agent):
            shutil.copy2(self.exe, dest)
        self.props = f"devops.agent.id=bkci-fixture-{self.work.name}\n".encode()
        (self.root / ".agent.properties").write_bytes(self.props)
        (self.root / "workspace").mkdir()
        (self.root / "workspace" / "build").write_bytes(b"user data")
        (self.root / "worker-agent.jar").write_bytes(b"old worker")
        self.payload = self.work / "payload"
        (self.payload / "extras").mkdir(parents=True)
        (self.payload / "extras" / "tool").write_bytes(b"added")
        (self.payload / "worker-agent.jar").write_bytes(b"new worker")
        self.out = self.work / "release"
        self.pkg = self.out / (TARGET + (".zip" if WIN else ".tar.gz"))
        self.pack()

    def pack(self, success=True):
        res = subprocess.run([sys.executable, str(TOOLS / "pack.py"), TARGET,
                              str(self.payload), str(self.out)], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
        self.assertEqual(res.returncode == 0, success, res.stdout)

    def run_script(self, action, package=None, success=True, env=None, cwd=None):
        if WIN:
            ps = shutil.which("powershell.exe") or shutil.which("pwsh")
            cmd = [ps, "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", str(TOOLS / "upgrade.ps1")]
        else:
            script = "upgrade_macos.sh" if SYSTEM == "darwin" else "upgrade.sh"
            cmd = ["bash", str(TOOLS / script)]
        args = cmd + ([action] if action != "upgrade" else []) + [str(self.root)]
        if action == "upgrade":
            args.append(str(package or self.pkg))
        res = subprocess.run(args, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT, env=env or self.env, cwd=cwd, timeout=90)
        self.assertEqual(res.returncode == 0, success, res.stdout)
        return res.stdout

    def instance_count(self, name):
        out = subprocess.run(["pgrep", "-x", name], stdout=subprocess.PIPE, check=False).stdout
        count = 0
        for raw in out.split():
            try:
                if Path(f"/proc/{int(raw)}/exe").resolve() == self.root / name:
                    count += 1
            except (OSError, ValueError):
                pass
        return count

    def original(self):
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"old worker")
        self.assertFalse((self.root / "extras").exists())
        self.assertEqual((self.root / "workspace" / "build").read_bytes(), b"user data")
        self.assertEqual((self.root / ".agent.properties").read_bytes(), self.props)

    def test_upgrade_skips_agent_cli(self):
        output = self.run_script("upgrade")
        self.assertNotIn(b"fixture status ok", output)

    def test_http_apply_and_rollback(self):
        handler = partial(SimpleHTTPRequestHandler, directory=str(self.out))
        with ThreadingHTTPServer(("127.0.0.1", 0), handler) as server:
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            try:
                self.run_script("upgrade", f"http://127.0.0.1:{server.server_port}/{self.pkg.name}")
            finally:
                server.shutdown()
                thread.join()
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
        self.assertEqual((self.root / "extras/tool").read_bytes(), b"added")
        self.assertTrue(Path(str(self.agent) + '.testpid').exists())
        self.run_script("rollback")
        self.original()

    def test_manual_agent_without_runtime_pid_is_replaced(self):
        proc = subprocess.Popen([str(self.agent)], cwd=self.work, env=self.env)
        try:
            pidfile = Path(str(self.agent) + ".testpid")
            for _ in range(100):
                if pidfile.exists() and pidfile.read_text() == str(proc.pid):
                    break
                time.sleep(0.05)
            self.assertEqual(pidfile.read_text(), str(proc.pid))
            output = self.run_script("upgrade")
            self.assertIn(str(proc.pid).encode(), output)
            self.assertIsNotNone(proc.poll())
            self.assertNotEqual(pidfile.read_text(), str(proc.pid))
        finally:
            if proc.poll() is None:
                proc.kill()
            proc.wait()

    @unittest.skipUnless(SYSTEM == "linux", "running executable moved into backup")
    def test_surviving_old_agent_prevents_second_start(self):
        shutil.copy2(self.agent, self.payload / "devopsAgent")
        self.pack()
        shutil.copy2(shutil.which("sleep"), self.agent)
        proc = subprocess.Popen(["sh", "-c", 'trap "" TERM; exec "$1" 90', "sh", str(self.agent)])
        try:
            output = self.run_script("upgrade")
            self.assertIn("跳过启动".encode(), output)
            self.assertIn(str(proc.pid).encode(), output)
            self.assertIsNone(proc.poll())
            self.assertFalse(Path(str(self.daemon) + ".testpid").exists())
            self.assertEqual(self.agent.read_bytes(), self.exe.read_bytes())
        finally:
            proc.kill()
            proc.wait()

    def test_unattended_repeat_upgrade_and_restart(self):
        self.run_script("upgrade")
        old_pids = [Path(str(p) + '.testpid').read_text() for p in (self.daemon, self.agent)]
        (self.payload / "extras/tool").write_bytes(b"v2")
        self.pack()
        self.run_script("upgrade")
        for p, old in zip((self.daemon, self.agent), old_pids):
            self.assertNotEqual(Path(str(p) + '.testpid').read_text(), old)
        self.assertEqual((self.root / "extras/tool").read_bytes(), b"v2")
        self.run_script("rollback")
        self.assertEqual((self.root / "extras/tool").read_bytes(), b"added")

    @unittest.skipUnless(WIN, "Windows manual upgrade lock")
    def test_pending_record_and_active_lock_have_distinct_errors(self):
        state = self.root / ".bkci-script-upgrade"
        pending = state / "pending"
        pending.mkdir(parents=True)

        def failure_log():
            before = set(state.glob("run-*.log"))
            self.run_script("upgrade", success=False)
            log, = set(state.glob("run-*.log")) - before
            return log.read_text(encoding="utf-8")

        with (state / "running.lock").open("wb"):
            self.assertIn("失败 [获取手动升级锁]", failure_log())
        message = failure_log()
        self.assertIn("失败 [检查上次升级记录]", message)
        self.assertIn("rollback", message)
        self.assertNotIn("confirm", message)
        (pending / "applied").touch()
        self.assertIn("confirm", failure_log())
        self.assertTrue((pending / "applied").exists())

    def test_recover_interrupted_replace(self):
        self.run_script("upgrade")
        pending = self.root / ".bkci-script-upgrade/pending"
        # Recreate the journal state between backing up and installing the JAR.
        (pending / "applied").unlink()
        (pending / "complete").unlink()
        (self.root / "worker-agent.jar").rename(pending / "new/worker-agent.jar")
        self.run_script("confirm", success=False)
        self.run_script("rollback")
        self.original()

    def _systemctl_fixture(self):
        (self.root / ".install_type").write_text("USER\n")
        systemctl = self.work / "systemctl"
        systemctl.write_text(
            '#!/bin/sh\ncase " $* " in\n'
            '*" show "*) printf "WorkingDirectory=%s\\n" "$UNIT_DIR";;\n'
            '*" stop "*) echo "$*" >> "$UNIT_LOG";;\n'
            '*" start "*) echo "$*" >> "$UNIT_LOG"; '
            '(cd "$UNIT_DIR" && exec ./devopsDaemon) </dev/null >/dev/null 2>&1 & ;;\nesac\n'
        )
        systemctl.chmod(0o755)
        trace = self.work / "unit-log"
        return trace, dict(self.env, PATH=str(self.work) + os.pathsep + self.env["PATH"],
                           UNIT_LOG=str(trace))

    @unittest.skipUnless(SYSTEM == "linux", "systemd controller")
    def test_unconfirmed_systemd_service_warns_and_uses_processes(self):
        trace, env = self._systemctl_fixture()
        env["UNIT_DIR"] = str(self.work)
        output = self.run_script("upgrade", env=env)
        self.assertIn(b"WorkingDirectory", output)
        self.assertFalse(trace.exists())
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
        self.run_script("rollback", env=env)
        self.original()

    @unittest.skipUnless(SYSTEM == "linux", "systemd controller")
    def test_systemd_user_service_uses_registered_directory(self):
        trace, env = self._systemctl_fixture()
        env["UNIT_DIR"] = str(self.root)
        self.run_script("upgrade", env=env)
        self.assertEqual([line.split()[:3] for line in trace.read_text().splitlines()],
                         [["--user", "--no-ask-password", "stop"], ["--user", "--no-ask-password", "start"]])
        self.assertTrue(Path(str(self.agent) + ".testpid").exists())

    @unittest.skipUnless(SYSTEM in ("linux", "darwin"), "macOS upgrade script")
    def test_macos_hook_with_stale_and_live_pid(self):
        env = self.env.copy()
        if SYSTEM == "linux":
            # Exercise the Darwin script in Linux CI with macOS command output.
            (self.work / "uname").write_text('#!/bin/sh\ncase "$1" in -m) echo arm64;; *) echo Darwin;; esac\n')
            (self.work / "ps").write_text('#!/bin/sh\nif [ "$1" = -ww ]; then exec readlink "/proc/$3/exe"; fi\n'
                                          'exec /bin/ps "$@"\n')
            for name in ("uname", "ps"):
                (self.work / name).chmod(0o755)
            env["PATH"] = str(self.work) + os.pathsep + env["PATH"]
        target = "darwin-arm64" if SYSTEM == "linux" else TARGET
        subprocess.run([sys.executable, str(TOOLS / "pack.py"), target, str(self.payload), str(self.out)],
                       check=True, stdout=subprocess.PIPE)
        (self.root / "runtime").mkdir()
        pidfile = self.root / "runtime/daemon.pid"
        exited = subprocess.Popen(["true"])
        exited.wait()
        pidfile.write_text(str(exited.pid))
        (self.root / "stop.sh").write_text("touch stop-hook-ran\nexit 2\n")
        (self.root / "start.sh").write_text("nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n")
        cmd = ["bash", str(TOOLS / "upgrade_macos.sh"), str(self.root), str(self.out / (target + ".tar.gz"))]
        result = subprocess.run(cmd, env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, timeout=60)
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertTrue(Path(str(self.agent) + ".testpid").exists())
        self.assertFalse((self.root / "stop-hook-ran").exists())
        pidfile.write_text(str(os.getpid()))
        result = subprocess.run(cmd, env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE, timeout=60)
        self.assertEqual(result.returncode, 0, result.stdout)
        self.assertIn(str(os.getpid()).encode(), result.stdout)
        self.assertFalse((self.root / "stop-hook-ran").exists())

    @unittest.skipUnless(SYSTEM == "linux", "install directory hooks")
    def test_hook_rejects_pid_from_another_installation(self):
        other = self.work / "another agent"
        other.mkdir()
        shutil.copy2(shutil.which("sleep"), other / "devopsAgent")
        proc = subprocess.Popen([str(other / "devopsAgent"), "120"], cwd=other)
        try:
            (self.root / "runtime").mkdir()
            pidfile = self.root / "runtime/agent.pid"
            pidfile.write_text(str(proc.pid))
            (self.root / "stop.sh").write_text('touch hook-ran\n')
            (self.root / "start.sh").write_text('nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n')
            self.run_script("upgrade", cwd=other)
            self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
            self.assertFalse((self.root / "hook-ran").exists())
            self.assertIsNone(proc.poll())
            pidfile.unlink()
            self.run_script("upgrade", cwd=other)
            self.assertTrue((self.root / "hook-ran").exists())
            self.assertEqual(self.instance_count("devopsAgent"), 1)
            self.assertIsNone(proc.poll())
        finally:
            proc.terminate()
            proc.wait(timeout=5)

    @unittest.skipUnless(SYSTEM == "linux", "install directory hooks")
    def test_install_dir_stop_start_scripts_take_precedence(self):
        trace = self.work / "hooks-log"
        (self.root / "stop.sh").write_text(
            '#!/bin/sh\n'
            '[ "$PWD" = "$HOOK_DIR" ] || exit 4\n'
            'echo stop >> "$HOOK_TRACE"\n'
            'for f in "$HOOK_DIR"/devops*.testpid; do\n'
            '  [ -f "$f" ] && kill -TERM "$(cat "$f")" 2>/dev/null || :\n'
            'done\n'
        )
        (self.root / "start.sh").write_text(
            '#!/bin/sh\n'
            'echo start >> "$HOOK_TRACE"\n'
            '[ "$PWD" = "$HOOK_DIR" ] || exit 4\n'
            'nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n'
            'nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n'
        )
        env = dict(self.env, HOOK_TRACE=str(trace), HOOK_DIR=str(self.root))
        self.run_script("upgrade", env=env)
        self.assertEqual(trace.read_text().split(), ["stop", "start"])
        self.assertEqual(self.instance_count("devopsDaemon"), 2)
        self.assertEqual(self.instance_count("devopsAgent"), 2)
        self.assertTrue(Path(str(self.agent) + ".testpid").exists())
        (self.root / "start.sh").write_text('nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n'
                                          'echo start >> "$HOOK_TRACE"\n')
        self.run_script("rollback", env=env)
        self.assertEqual(trace.read_text().split(), ["stop", "start", "stop", "start"])

    @unittest.skipUnless(SYSTEM == "linux", "install directory hooks")
    def test_stop_script_failure_allows_upgrade(self):
        trace = self.work / "hooks-fail-log"
        (self.root / "stop.sh").write_text('#!/bin/sh\necho stop >> "$HOOK_TRACE"\nexit 3\n')
        (self.root / "start.sh").write_text('echo start >> "$HOOK_TRACE"\n'
                                           'nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n')
        env = dict(self.env, HOOK_TRACE=str(trace))
        output = self.run_script("upgrade", env=env)
        self.assertIn(b"stop.sh", output)
        self.assertEqual(trace.read_text().split(), ["stop", "start"])
        self.assertEqual(self.instance_count("devopsAgent"), 1)
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")

    @unittest.skipUnless(SYSTEM == "linux", "install directory hooks")
    def test_stuck_stop_hook_cannot_block_install(self):
        (self.root / "stop.sh").write_text("exec sleep 120\n")
        (self.root / "start.sh").write_text("nohup ./devopsDaemon </dev/null >/dev/null 2>&1 &\n")
        output = self.run_script("upgrade")
        self.assertIn("超过 30 秒".encode(), output)
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")

    @unittest.skipUnless(WIN, "Windows service controller")
    def test_windows_skips_service_start_when_agent_reappears(self):
        harness = self.work / "respawn.ps1"
        harness.write_text(r"""
function Get-Service {
    param($Name, $ErrorAction)
    if ((Test-Path (Join-Path $env:TEST_ROOT '.bkci-script-upgrade/pending/complete')) -and
        !(Test-Path $env:TEST_TRACE)) {
        $p = Start-Process (Join-Path $env:TEST_ROOT 'devopsAgent.exe') -PassThru -WindowStyle Hidden
        Set-Content $env:TEST_TRACE $p.Id
    }
    $svc = [pscustomobject]@{ Status='Stopped' }
    $svc | Add-Member ScriptMethod WaitForStatus { param($status, $duration) }
    $svc | Add-Member ScriptMethod Start { Add-Content $env:TEST_TRACE 'unexpected-start' }
    $svc
}
function Get-ItemProperty { param($LiteralPath) [pscustomobject]@{ ImagePath=$env:TEST_DAEMON } }
& $env:TEST_SCRIPT $env:TEST_ROOT $env:TEST_PACKAGE
exit $LASTEXITCODE
""")
        trace = self.work / "respawn-log"
        env = dict(self.env, TEST_ROOT=str(self.root), TEST_DAEMON=str(self.daemon),
                   TEST_TRACE=str(trace), TEST_SCRIPT=str(TOOLS / "upgrade.ps1"), TEST_PACKAGE=str(self.pkg))
        result = subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy",
                                 "Bypass", "-File", str(harness)], env=env, stdout=subprocess.PIPE,
                                stderr=subprocess.STDOUT, timeout=60)
        self.assertEqual(result.returncode, 0, result.stdout)
        pid = int(trace.read_text().strip())
        self.assertIn(str(pid).encode(), result.stdout)
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")

    @unittest.skipUnless(WIN, "Windows service controller")
    def test_windows_service_uses_original_registration(self):
        # Supply a service controller in this PowerShell process only. The
        # daemon and agent it starts are real executables under the test root.
        harness = self.work / "service-test.ps1"
        harness.write_text(r"""
function Get-Service {
    param($Name, $ErrorAction)
    $svc = [pscustomobject]@{ Status='Running' }
    if (!(Get-Process devopsDaemon -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $env:TEST_DAEMON })) {
        $svc.Status = 'Stopped'
    }
    $svc | Add-Member ScriptMethod Stop { Add-Content $env:TEST_TRACE 'stop' }
    $svc | Add-Member ScriptMethod WaitForStatus { param($status, $duration) }
    $svc | Add-Member ScriptMethod Start {
        Add-Content $env:TEST_TRACE 'start'
        Start-Process $env:TEST_DAEMON -WorkingDirectory $env:TEST_ROOT -WindowStyle Hidden
    }
    $svc
}
function Get-ItemProperty { param($LiteralPath) [pscustomobject]@{ ImagePath=$env:TEST_IMAGE } }
& $env:TEST_SCRIPT $env:TEST_ROOT $env:TEST_PACKAGE
exit $LASTEXITCODE
""")
        trace = self.work / "service-log"
        cmd = ["powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", str(harness)]
        env = dict(self.env, TEST_DAEMON=str(self.daemon), TEST_ROOT=str(self.root), TEST_TRACE=str(trace),
                   TEST_SCRIPT=str(TOOLS / "upgrade.ps1"), TEST_PACKAGE=str(self.pkg),
                   TEST_IMAGE=str(self.work / self.daemon.name))
        def invoke():
            return subprocess.run(cmd, env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                                  stderr=subprocess.STDOUT, timeout=90)
        self.assertEqual(invoke().returncode, 0)
        self.assertFalse(trace.exists())
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
        env["TEST_IMAGE"] = str(self.daemon)
        for _ in range(2):
            res = invoke()
            self.assertEqual(res.returncode, 0, res.stdout)
        self.assertEqual(trace.read_text().split(), ["start", "stop", "start"])

    @unittest.skipUnless(WIN, "Windows duplicate process cleanup")
    def test_windows_duplicate_processes_are_preserved(self):
        harness = self.work / "duplicate-test.ps1"
        harness.write_text(r"""
function Get-Service {
    param($Name, $ErrorAction)
    $svc = [pscustomobject]@{ Status='Stopped' }
    if (Get-Process devopsDaemon -ErrorAction SilentlyContinue | Where-Object { $_.Path -eq $env:TEST_DAEMON }) {
        $svc.Status = 'Running'
    }
    $svc | Add-Member ScriptMethod Stop { Add-Content $env:TEST_TRACE 'stop' }
    $svc | Add-Member ScriptMethod WaitForStatus { param($status, $duration) }
    $svc | Add-Member ScriptMethod Start {
        Add-Content $env:TEST_TRACE 'start'
        Start-Process $env:TEST_DAEMON -WorkingDirectory $env:TEST_ROOT -WindowStyle Hidden
        Start-Process $env:TEST_DAEMON -WorkingDirectory $env:TEST_ROOT -WindowStyle Hidden
    }
    $svc
}
function Get-ItemProperty { param($LiteralPath) [pscustomobject]@{ ImagePath=$env:TEST_IMAGE } }
& $env:TEST_SCRIPT upgrade $env:TEST_ROOT $env:TEST_PACKAGE
exit $LASTEXITCODE
""")
        trace = self.work / "duplicate-log"
        env = dict(self.env, TEST_DAEMON=str(self.daemon), TEST_ROOT=str(self.root), TEST_TRACE=str(trace),
                   TEST_SCRIPT=str(TOOLS / "upgrade.ps1"), TEST_PACKAGE=str(self.pkg), TEST_IMAGE=str(self.daemon))
        cmd = ["powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-File", str(harness)]
        res = subprocess.run(cmd, env=env, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                             stderr=subprocess.STDOUT, timeout=90)
        self.assertEqual(res.returncode, 0, res.stdout)
        self.assertIn(b"PID", res.stdout)
        for name in ("devopsDaemon", "devopsAgent"):
            count = subprocess.run(
                ["powershell.exe", "-NoProfile", "-NonInteractive", "-Command",
                 "@(Get-Process -Name $env:NAME -ErrorAction SilentlyContinue | Where-Object { "
                 "$_.Path -and $_.Path -eq (Join-Path $env:TEST_ROOT ($env:NAME + '.exe')) }).Count"],
                env=dict(env, NAME=name), stdout=subprocess.PIPE, stderr=subprocess.STDOUT).stdout.strip()
            self.assertEqual(count, b"2", (name, count))

    @unittest.skipUnless(WIN, "Windows legacy scheduled task")
    def test_legacy_task_starts_detached_daemon(self):
        # Exercise the task action shape (including a space in the VBS path)
        # without installing a scheduled task on the developer's machine.
        harness = self.work / "task-test.ps1"
        harness.write_text(r"""
function Get-Service { param($Name, $ErrorAction) }
function New-Object {
    param($ComObject)
    $scheduler = [pscustomobject]@{}
    $scheduler | Add-Member ScriptMethod Connect {}
    $scheduler | Add-Member ScriptMethod GetFolder { param($path) $global:fakeFolder }
    $scheduler
}
$global:fakeAction = [pscustomobject]@{ Path='cscript.exe'; Arguments=('"'+(Join-Path $env:TEST_ROOT 'devopsctl.vbs')+'"') }
$actions = [pscustomobject]@{ Count=1 }
$actions | Add-Member ScriptMethod Item { param($index) $global:fakeAction }
$global:fakeTask = [pscustomobject]@{ Definition=[pscustomobject]@{ Actions=$actions }; State=3 }
$global:fakeTask | Add-Member ScriptMethod Run { param($parameters)
    Add-Content $env:TEST_TRACE 'run'
    Start-Process (Join-Path $env:TEST_ROOT 'devopsDaemon.exe') -WorkingDirectory $env:TEST_ROOT -WindowStyle Hidden
}
$global:fakeFolder = [pscustomobject]@{}
$global:fakeFolder | Add-Member ScriptMethod GetTask { param($name) $global:fakeTask }
& $env:TEST_SCRIPT $env:TEST_ROOT $env:TEST_PACKAGE
exit $LASTEXITCODE
""")
        (self.root / ".install_type").write_text("TASK")
        trace = self.work / "task-log"
        env = dict(self.env, TEST_ROOT=str(self.root), TEST_TRACE=str(trace),
                   TEST_SCRIPT=str(TOOLS / "upgrade.ps1"), TEST_PACKAGE=str(self.pkg))
        for _ in range(2):
            res = subprocess.run(["powershell.exe", "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass",
                                  "-File", str(harness)], env=env, stdin=subprocess.DEVNULL,
                                 stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=90)
            self.assertEqual(res.returncode, 0, res.stdout)
        self.assertEqual(trace.read_text().split(), ["run", "run"])

    def test_darwin_package_layout(self):
        (self.payload / "devopsAgent").write_bytes(b"program")
        subprocess.run([sys.executable, str(TOOLS / "pack.py"), "darwin-arm64",
                        str(self.payload), str(self.out)], check=True, stdout=subprocess.PIPE)
        with tarfile.open(self.out / "darwin-arm64.tar.gz") as tar:
            self.assertEqual(tar.extractfile("bkci-package-target").read(), b"darwin-arm64\n")
            self.assertEqual(tar.getmember("devopsAgent").mode, 0o755)
            self.assertEqual(tar.extractfile("extras/tool").read(), b"added")

    @unittest.skipUnless(SYSTEM == "linux", "exercises Linux uname selection")
    def test_install_archived_package_for_special_linux_arch(self):
        uname = self.work / "uname"
        for arch, target in (("loongarch64", "linux-loong64"), ("riscv64", "linux-riscv64")):
            with self.subTest(arch=arch):
                uname.write_text(f'#!/bin/sh\nif [ "$1" = "-m" ]; then echo {arch}; else /bin/uname "$@"; fi\n')
                uname.chmod(0o755)
                env = dict(self.env, PATH=str(self.work) + os.pathsep + os.environ["PATH"])
                subprocess.run([sys.executable, str(TOOLS / "pack.py"), "--source", str(self.payload),
                                "--target", "linux-" + arch, "--output", str(self.out)],
                               check=True, stdout=subprocess.PIPE)
                self.run_script("upgrade", self.out / (target + ".tar.gz"), env=env)
                self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
                self.run_script("rollback", env=env)
                self.original()

    def test_protected_data(self):
        (self.payload / ".agent.properties").write_bytes(b"bad")
        self.pack(success=False)
        self.original()
        marker = "bkci-package-target"
        for name, target in (("workspace/build", TARGET), ("../outside", TARGET),
                             ("worker-agent.jar", "wrong-platform")):
            with self.subTest(name=name):
                if WIN:
                    with zipfile.ZipFile(self.pkg, "w") as z:
                        z.writestr(marker, target)
                        z.writestr(name, "bad")
                else:
                    with tarfile.open(self.pkg, "w:gz") as tar:
                        for path, data in ((marker, target.encode()), (name, b"bad")):
                            info = tarfile.TarInfo(path)
                            info.size = len(data)
                            tar.addfile(info, io.BytesIO(data))
                self.run_script("upgrade", success=False)
                self.original()

    def test_write_failure_reverts_previous_files(self):
        env = self.env.copy()
        if WIN:
            # The held JAR cannot be renamed. The added file precedes it in the ZIP.
            hold = (self.root / "worker-agent.jar").open("rb")
        else:
            hold = contextlib.nullcontext()
            shim = self.work / "mv"
            shim.write_text("#!/bin/sh\ncase \"$1\" in */pending/new/worker-agent.jar) exit 31;; esac\n"
                            "exec /bin/mv \"$@\"\n")
            shim.chmod(0o755)
            env["PATH"] = str(self.work) + os.pathsep + env["PATH"]
        with hold:
            self.run_script("upgrade", success=False, env=env)
        self.original()
        self.assertFalse((self.root / ".bkci-script-upgrade/pending").exists())

    def test_failed_start_keeps_installed_files(self):
        (self.payload / self.daemon.name).write_bytes(b'not an executable')
        self.pack()
        self.run_script('upgrade')
        self.assertEqual(self.daemon.read_bytes(), b'not an executable')
        self.assertEqual((self.root / "worker-agent.jar").read_bytes(), b"new worker")
        self.assertTrue((self.root / ".bkci-script-upgrade/pending/complete").exists())


if __name__ == "__main__":
    unittest.main(verbosity=2)
