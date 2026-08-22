from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from bkci_agent_sdk import (
    DOCKER_DATA_DIR,
    DockerBuildOptions,
    ImagePullPolicy,
    WorkerBuildOptions,
    build_docker_create_args,
    build_worker_env,
    parse_worker_version,
    registry_from_image,
    resolve_java_bin,
    resolve_latest_java,
    run_docker_build,
    run_worker_build,
)


def build_info(*, docker=True):
    value = {
        "projectId": "project",
        "buildId": "build",
        "vmSeqId": "1",
        "pipelineId": "pipeline",
        "workspace": "",
    }
    if docker:
        value["dockerBuildInfo"] = {
            "agentId": "agent",
            "secretKey": "secret",
            "image": "registry.example.com/team/image:1",
            "credential": {"user": "", "password": ""},
            "options": {"volumes": ["/host:/guest"], "privileged": True},
            "imagePullPolicy": ImagePullPolicy.IF_NOT_PRESENT.value,
        }
    return value


class FakeDockerRunner:
    def __init__(self, exit_code=0) -> None:
        self.exit_code = exit_code
        self.calls = []

    async def image_exists(self, image):
        self.calls.append(("inspect", image))
        return True

    async def pull_image(self, *args):
        self.calls.append(("pull", *args))
        return ""

    async def create_container(self, args):
        self.calls.append(("create", args))
        return "container-id"

    async def start_container(self, container_id):
        self.calls.append(("start", container_id))

    async def wait_container(self, container_id):
        self.calls.append(("wait", container_id))
        return self.exit_code

    async def container_logs(self, container_id):
        return "container failed"

    async def remove_container(self, container_id):
        self.calls.append(("remove", container_id))


class WorkerDockerTests(unittest.IsolatedAsyncioTestCase):
    def test_version_parsing(self) -> None:
        self.assertEqual(parse_worker_version("warning line\nv1.2.3-beta.4\n"), "v1.2.3-beta.4")
        self.assertEqual(parse_worker_version("v1.2.3-RELEASE"), "v1.2.3-RELEASE")
        self.assertEqual(parse_worker_version("v1.2.3-1.4"), "")
        self.assertEqual(parse_worker_version("version 1.2.3"), "")

    def test_java_resolution_and_worker_env(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            java = root / "jdk17" / "bin" / "java"
            java.parent.mkdir(parents=True)
            java.touch()
            self.assertEqual(resolve_java_bin(root / "jdk17", "linux"), str(java))
            self.assertEqual(resolve_latest_java(root / "jdk17", None, "linux"), str(java))
            options = WorkerBuildOptions(
                build_info=build_info(docker=False),
                work_dir=root,
                worker_jar_path=root / "worker-agent.jar",
                gateway="http://gateway",
                agent_version="agent-v",
                worker_version="worker-v",
                jdk17_path=java,
                extra_env={"CUSTOM": "value"},
            )
            env = build_worker_env(options)
            self.assertEqual(env["DEVOPS_BUILD_ID"], "build")
            self.assertEqual(env["DEVOPS_AGENT_JDK_17_PATH"], str(java))
            self.assertEqual(env["CUSTOM"], "value")

    async def test_missing_worker_jar_is_reported(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            result = await run_worker_build(
                WorkerBuildOptions(
                    build_info=build_info(docker=False),
                    work_dir=directory,
                    worker_jar_path=Path(directory) / "missing.jar",
                    gateway="gateway",
                    agent_version="a",
                    worker_version="w",
                )
            )
        self.assertFalse(result.success)
        self.assertIn("worker jar missing", result.message)

    async def test_worker_process_success_and_temporary_file_cleanup(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            java = root / "java"
            java.write_text(
                """#!/bin/sh
for arg in "$@"; do
  case "$arg" in
    -Ddevops.agent.error.file=*) error_file=${arg#*=} ;;
  esac
done
: > "$error_file"
exit 0
""",
                encoding="utf-8",
            )
            java.chmod(0o755)
            worker_jar = root / "worker-agent.jar"
            worker_jar.write_bytes(b"placeholder")
            result = await run_worker_build(
                WorkerBuildOptions(
                    build_info=build_info(docker=False),
                    work_dir=root,
                    worker_jar_path=worker_jar,
                    gateway="http://gateway",
                    agent_version="agent",
                    worker_version="worker",
                    jdk17_path=java,
                )
            )
            self.assertTrue(result.success, result.message)
            self.assertTrue((root / ".agent.properties").exists())
            self.assertEqual(list(root.glob("devops_agent_*.sh")), [])
            self.assertEqual(list((root / "build_tmp").glob("*_build_msg.log")), [])

    async def test_docker_create_arguments(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            options = DockerBuildOptions(
                build_info=build_info(),
                work_dir=root,
                worker_jar_path=root / "worker.jar",
                docker_init_script_path=root / "init.sh",
                gateway="gateway",
                project_id="project",
                platform="linux",
            )
            args = await build_docker_create_args("container", "image:1", options)
            self.assertEqual(args[:2], ["--name", "container"])
            self.assertIn("--privileged", args)
            self.assertIn("bridge", args)
            self.assertIn("devops_agent_secret_key=secret", args)
            self.assertTrue(any(DOCKER_DATA_DIR in value for value in args))
            self.assertEqual(args[-5:], ["--entrypoint", "/bin/sh", "image:1", "-c", "/data/init.sh"])

    async def test_docker_build_uses_local_image_and_cleans_container(self) -> None:
        runner = FakeDockerRunner()
        with tempfile.TemporaryDirectory() as directory:
            result = await run_docker_build(
                runner,
                DockerBuildOptions(
                    build_info=build_info(),
                    work_dir=directory,
                    worker_jar_path=Path(directory) / "worker.jar",
                    docker_init_script_path=Path(directory) / "init.sh",
                    gateway="gateway",
                    project_id="project",
                    rand_suffix=lambda: "fixed",
                ),
            )
        self.assertTrue(result.success)
        self.assertFalse(any(call[0] == "pull" for call in runner.calls))
        self.assertIn(("remove", "container-id"), runner.calls)

    async def test_docker_build_accepts_pull_policy_enum(self) -> None:
        runner = FakeDockerRunner()
        info = build_info()
        info["dockerBuildInfo"]["imagePullPolicy"] = ImagePullPolicy.IF_NOT_PRESENT
        with tempfile.TemporaryDirectory() as directory:
            result = await run_docker_build(
                runner,
                DockerBuildOptions(
                    build_info=info,
                    work_dir=directory,
                    worker_jar_path=Path(directory) / "worker.jar",
                    docker_init_script_path=Path(directory) / "init.sh",
                    gateway="gateway",
                    project_id="project",
                ),
            )
        self.assertTrue(result.success)
        self.assertFalse(any(call[0] == "pull" for call in runner.calls))

    async def test_docker_failure_uses_container_log(self) -> None:
        runner = FakeDockerRunner(exit_code=7)
        with tempfile.TemporaryDirectory() as directory:
            result = await run_docker_build(
                runner,
                DockerBuildOptions(
                    build_info=build_info(),
                    work_dir=directory,
                    worker_jar_path=Path(directory) / "worker.jar",
                    docker_init_script_path=Path(directory) / "init.sh",
                    gateway="gateway",
                    project_id="project",
                    rand_suffix=lambda: "fixed",
                ),
            )
        self.assertFalse(result.success)
        self.assertIn("exit code 7: container failed", result.message)

    def test_registry_inference(self) -> None:
        self.assertEqual(registry_from_image("registry.example.com/team/image:1"), "registry.example.com")
        self.assertEqual(registry_from_image("ubuntu:latest"), "")
        self.assertEqual(registry_from_image("localhost:5000/image"), "localhost:5000")


if __name__ == "__main__":
    unittest.main()
