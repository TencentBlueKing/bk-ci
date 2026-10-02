#![allow(dead_code)]
use std::{path::PathBuf, process::Command, sync::OnceLock};
pub struct Fixture {
    pub java: PathBuf,
    pub jar: PathBuf,
    _dir: tempfile::TempDir,
}
pub fn fixture() -> &'static Fixture {
    static FIXTURE: OnceLock<Fixture> = OnceLock::new();
    FIXTURE.get_or_init(|| {
        let jdk = PathBuf::from(
            std::env::var_os("BK_CI_TEST_JDK").expect("set BK_CI_TEST_JDK to a JDK 17+ directory"),
        );
        let suffix = if cfg!(windows) { ".exe" } else { "" };
        let dir = tempfile::Builder::new()
            .prefix("bkci java fixture ")
            .tempdir()
            .unwrap();
        let source = dir.path().join("WorkerProbe.java");
        std::fs::write(&source, include_str!("WorkerProbe.java")).unwrap();
        let mut javac = Command::new(jdk.join("bin").join(format!("javac{suffix}")));
        javac
            .arg("-encoding")
            .arg("UTF-8")
            .arg("-d")
            .arg(dir.path())
            .arg(&source);
        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            javac.creation_flags(0x08000000);
        }
        let output = javac.output().unwrap();
        assert!(
            output.status.success(),
            "javac failed: {}",
            String::from_utf8_lossy(&output.stderr)
        );
        let jar = dir.path().join("worker probe.jar");
        let mut jar_cmd = Command::new(jdk.join("bin").join(format!("jar{suffix}")));
        jar_cmd
            .args(["--create", "--file"])
            .arg(&jar)
            .args(["--main-class", "WorkerProbe", "-C"])
            .arg(dir.path())
            .arg("WorkerProbe.class");
        #[cfg(windows)]
        {
            use std::os::windows::process::CommandExt;
            jar_cmd.creation_flags(0x08000000);
        }
        let output = jar_cmd.output().unwrap();
        assert!(
            output.status.success(),
            "jar failed: {}",
            String::from_utf8_lossy(&output.stderr)
        );
        Fixture {
            java: jdk.join("bin").join(format!("java{suffix}")),
            jar,
            _dir: dir,
        }
    })
}
