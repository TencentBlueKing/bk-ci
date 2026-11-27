"""将指定目录里的已签名文件或历史 Agent 产物按安装布局直接归档。只依赖 Python 标准库。"""
import argparse
import os
from pathlib import Path
import re
import stat
import sys
import tarfile
import tempfile
import zipfile
from io import BytesIO


MARKER = "bkci-package-target"
PROTECTED = {"workspace", "runtime", "logs", "tmp", "upgrade", "config", "conf", "cert", "data", MARKER}
PROGRAMS = {"devopsAgent", "devopsDaemon", "upgrader", "devopsCollector"}


def target_name(value):
    if value in ("darwin-amd64", "darwin-arm64", "windows-amd64", "windows-386"):
        return value
    match = re.fullmatch(r"linux-([a-z0-9][a-z0-9_]*)", value)
    if match:
        arch = match.group(1)
        aliases = {"x86_64": "amd64", "aarch64": "arm64", "loongarch64": "loong64"}
        return "linux-" + aliases.get(arch, arch)
    raise argparse.ArgumentTypeError(f"不支持的平台 {value!r}，请填写 linux-<架构>、darwin-amd64/arm64 或 windows-amd64/386")


def contents(source, target):
    files = []
    seen = set()
    for path in sorted(source.rglob("*")):
        name = path.relative_to(source).as_posix()
        info = path.lstat()
        if stat.S_ISLNK(info.st_mode) or getattr(info, "st_file_attributes", 0) & 0x400:
            raise ValueError(f"链接不能打包：{name}")
        for part in name.split("/"):
            if (not re.fullmatch(r"[A-Za-z0-9_+-][A-Za-z0-9_.+-]*", part)
                    or part.endswith(".") or re.match(r"(?i)^(CON|NUL|PRN|AUX|COM[0-9]|LPT[0-9])(\.|$)", part)):
                raise ValueError(f"不支持的路径：{name}")
        if name.lower().split("/")[0] in PROTECTED or name.lower().endswith((".properties", ".pid", ".lock")):
            raise ValueError(f"源目录含有用户数据或保留路径：{name}；请使用干净的增量目录")
        if name.lower() in seen:
            raise ValueError(f"路径大小写冲突：{name}")
        seen.add(name.lower())
        if stat.S_ISDIR(info.st_mode):
            continue
        if not stat.S_ISREG(info.st_mode):
            raise ValueError(f"只支持普通文件：{name}")
        if (target.startswith("windows") and name in PROGRAMS
                or not target.startswith("windows") and name.endswith(".exe") and name[:-4] in PROGRAMS):
            raise ValueError(f"{name} 与目标 {target} 不匹配")
        files.append((path, name))
    if not files:
        raise ValueError("增量目录没有文件")
    return files


def pack(source, target, out):
    source, out = source.resolve(), out.resolve()
    if not source.is_dir():
        raise ValueError(f"找不到输入产物目录：{source}")
    if source == out or source in out.parents:
        raise ValueError(f"输出目录 {out} 应位于输入产物目录 {source} 以外")
    print(f"输入目录：{source}\n目标平台：{target}")
    files = contents(source, target)
    out.mkdir(parents=True, exist_ok=True)
    windows = target.startswith("windows-")
    dest = out / (target + (".zip" if windows else ".tar.gz"))
    with tempfile.TemporaryDirectory(dir=out) as tmp:
        archive = Path(tmp) / dest.name
        if windows:
            with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
                z.writestr(MARKER, target + "\n")
                for path, name in files:
                    z.write(path, name)
        else:
            with tarfile.open(archive, "w:gz", dereference=True) as tar:
                data = (target + "\n").encode("ascii")
                item = tarfile.TarInfo(MARKER)
                item.size = len(data)
                tar.addfile(item, BytesIO(data))
                for path, name in files:
                    item = tar.gettarinfo(str(path), arcname=name)
                    item.mode = 0o755 if (item.mode & 0o111 or name in PROGRAMS or name.endswith(".sh")) else 0o644
                    with path.open("rb") as stream:
                        tar.addfile(item, stream)
        os.replace(archive, dest)
    print(f"{dest} ({len(files)} 个升级文件)")


if __name__ == "__main__":
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--source", required=True, type=Path, help="已有产物目录；其中的相对路径即安装路径")
    p.add_argument("--target", required=True, type=target_name, help="目标机器的平台，例如 linux-loong64")
    p.add_argument("--output", required=True, type=Path, help="该版本的输出目录，例如 releases/v1")
    argv = sys.argv[1:]
    # Preserve the original positional invocation: pack.py TARGET SOURCE OUT.
    if len(argv) == 3 and not argv[0].startswith("-"):
        argv = ["--target", argv[0], "--source", argv[1], "--output", argv[2]]
    args = p.parse_args(argv)
    try:
        pack(args.source, args.target, args.output)
    except (OSError, ValueError, tarfile.TarError, zipfile.BadZipFile) as err:
        sys.exit(f"打包失败：{err}")
