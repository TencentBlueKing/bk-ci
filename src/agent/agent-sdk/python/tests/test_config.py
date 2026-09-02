from __future__ import annotations

from pathlib import Path
import tempfile
import unittest

from bkci_agent_sdk import AgentConfig, AuthHeader, parse_properties


class ConfigTests(unittest.TestCase):
    def test_parse_properties_and_defaults(self) -> None:
        values = parse_properties(
            """
            # comment
            ; another comment
            devops.project.id = project
            value.with.equals=a=b
            ignored
            """
        )
        self.assertEqual(values["devops.project.id"], "project")
        self.assertEqual(values["value.with.equals"], "a=b")

    def test_load_properties_and_auth_headers(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / ".agent.properties"
            path.write_text(
                "\n".join(
                    [
                        "devops.project.id=p",
                        "devops.agent.id=a",
                        "devops.agent.secret.key=s",
                        "landun.gateway=example.test",
                        "devops.parallel.task.count=7",
                        "devops.docker.enable=yes",
                    ]
                ),
                encoding="utf-8",
            )
            config = AgentConfig.from_properties_file(path)
        self.assertEqual(config.get_gateway(), "http://example.test")
        self.assertEqual(config.parallel_task_count, 7)
        self.assertTrue(config.enable_docker_build)
        self.assertEqual(config.docker_parallel_task_count, 4)
        self.assertEqual(
            config.get_auth_header_map()[AuthHeader.SECRET_KEY],
            "s",
        )

    def test_required_values_are_validated(self) -> None:
        with self.assertRaisesRegex(ValueError, "project_id"):
            AgentConfig(gateway="gateway", project_id="", agent_id="a", secret_key="s")

    def test_from_registry(self) -> None:
        config = AgentConfig.from_registry(
            {
                "projectId": "p",
                "agentId": "a",
                "secretKey": "s",
                "gateway": "g",
                "parallelTaskCount": 2,
            }
        )
        self.assertEqual(config.project_id, "p")
        self.assertEqual(config.parallel_task_count, 2)

        defaulted = AgentConfig.from_registry(
            {
                "projectId": "p",
                "agentId": "a",
                "secretKey": "s",
                "gateway": "g",
                "parallelTaskCount": None,
                "dockerParallelTaskCount": None,
            }
        )
        self.assertEqual(defaulted.parallel_task_count, 4)


if __name__ == "__main__":
    unittest.main()
