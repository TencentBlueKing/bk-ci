//go:build linux || darwin

package main

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"
)

const version = "1.3.0"

func printUsage() {
	exe := filepath.Base(os.Args[0])
	fmt.Printf("%s v%s\n\n", exe, version)
	fmt.Println("`agent-util` 是一个放在 `other-utils/process-tree` 下的诊断工具集。")
	fmt.Println("通过不同子命令提供两类能力：")
	fmt.Println("  - tree:        Windows 进程树/阻塞诊断（当前平台不支持）")
	fmt.Println("  - shell-check: Unix login shell 启动链路检测")
	fmt.Println()
	fmt.Println("USAGE:")
	fmt.Printf("  %s shell-check [OPTIONS]\n", exe)
	fmt.Printf("  %s tree [OPTIONS]\n", exe)
	fmt.Println()
	fmt.Println("SUBCOMMANDS:")
	fmt.Println("  shell-check    复现 agent 的两层脚本 + login shell 启动方式，检测 rc/profile 问题")
	fmt.Println("  tree           Windows 进程树诊断（Linux/macOS 下会提示不支持）")
	fmt.Println()
	fmt.Println("EXAMPLES:")
	fmt.Printf("  %s shell-check\n", exe)
	fmt.Printf("  %s shell-check -shell /bin/zsh -timeout 12s -verbose\n", exe)
	fmt.Printf("  %s tree -buildid <build-id>\n", exe)
}

func main() {
	args := os.Args[1:]
	if len(args) == 0 {
		printUsage()
		return
	}

	switch args[0] {
	case "shell-check":
		os.Exit(runShellCheckCommand(args[1:]))
	case "tree":
		fmt.Fprintln(os.Stderr, "tree 子命令仅支持 Windows 平台")
		os.Exit(1)
	case "help", "-h", "--help":
		printUsage()
		return
	default:
		if strings.HasPrefix(args[0], "-") {
			os.Exit(runShellCheckCommand(args))
		}
		fmt.Fprintf(os.Stderr, "未知子命令: %s\n\n", args[0])
		printUsage()
		os.Exit(2)
	}
}
