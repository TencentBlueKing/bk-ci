/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pkg

import (
	"fmt"
	"os"
	"os/exec"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/common/codec"
	"github.com/Tencent/bk-ci/src/booster/common/types"

	commandCli "github.com/urfave/cli"
)

// CMakeProcess do cmake process
// add CMAKE_C_COMPILER and CMAKE_CXX_COMPILER to specify the compiler in CMakeFiles
func CMakeProcess(c *commandCli.Context) error {
	initProcess(c)
	return cmakeProcess(c)
}

func cmakeProcess(c *commandCli.Context) error {
	if c.Bool(FlagClang) {
		Compiler = CompilerClang
	}

	projectID := c.String(FlagProjectID)
	if projectID == "" {
		fmt.Printf("%s must be specified", FlagProjectID)
	}

	config := requestCMakeConfig(projectID)

	raw := fmt.Sprintf("cmake %s %s", config.Args, c.String("args"))
	fmt.Printf("exec command: %s\n", raw)
	cmd := exec.Command("/bin/bash", "-c", raw)
	dir, _ := os.Getwd()
	cmd.Dir = dir
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err := cmd.Run()

	saveExitCode(err)
	if err != nil {
		return err
	}

	fmt.Printf("\n* %s done *\n", ClientCMake.Name())
	return nil
}

func requestCMakeConfig(projectID string) *types.CMakeArgs {
	data, ok, err := requestServer("GET",
		fmt.Sprintf("%s?project_id=%s", cmakeConfigURI, projectID), nil)
	if ok && err != nil {
		fmt.Printf("request server failed: %v, degraded to local compiling.\n", err)
		ok = false
	} else if !ok {
		fmt.Printf("failed to connect to server, degraded to local compiling.\n")
	}

	var args types.CMakeArgs
	if ok {
		if err = codec.DecJSON(data, &args); err == nil {
			// replace the compiler path as absolute path
			var compilerPath, compilerPlusPath, compilerAbsPath, compilerPlusAbsPath string
			ccacheAbsPath := getAbsolutePath("ccache")
			distccAbsPath := getAbsolutePath("distcc")

			switch Compiler {
			case CompilerGcc:
				compilerPath = "gcc"
				compilerPlusPath = "g++"
				compilerAbsPath = getAbsolutePath("gcc")
				compilerPlusAbsPath = getAbsolutePath("g++")
			case CompilerClang:
				compilerPath = "clang"
				compilerPlusPath = "clang++"
				compilerAbsPath = getAbsolutePath("clang")
				compilerPlusAbsPath = getAbsolutePath("clang++")

				args.Args = strings.ReplaceAll(args.Args,
					"-DCMAKE_C_COMPILER_ARG1='gcc'", "-DCMAKE_C_COMPILER_ARG1='clang'")
				args.Args = strings.ReplaceAll(args.Args,
					"-DCMAKE_CXX_COMPILER_ARG1='g++'", "-DCMAKE_CXX_COMPILER_ARG1='clang++'")
				args.Args = strings.ReplaceAll(args.Args,
					"-DCMAKE_C_COMPILER_ARG1='distcc gcc'", "-DCMAKE_C_COMPILER_ARG1='distcc clang'")
				args.Args = strings.ReplaceAll(args.Args,
					"-DCMAKE_CXX_COMPILER_ARG1='distcc g++'", "-DCMAKE_CXX_COMPILER_ARG1='distcc clang++'")
			}

			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_C_COMPILER='ccache'", fmt.Sprintf("-DCMAKE_C_COMPILER='%s'", ccacheAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_CXX_COMPILER='ccache'", fmt.Sprintf("-DCMAKE_CXX_COMPILER='%s'", ccacheAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_C_COMPILER='distcc'", fmt.Sprintf("-DCMAKE_C_COMPILER='%s'", distccAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_CXX_COMPILER='distcc'", fmt.Sprintf("-DCMAKE_CXX_COMPILER='%s'", distccAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_C_COMPILER_ARG1='distcc ",
				fmt.Sprintf("-DCMAKE_C_COMPILER_ARG1='%s ", distccAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				"-DCMAKE_CXX_COMPILER_ARG1='distcc ",
				fmt.Sprintf("-DCMAKE_CXX_COMPILER_ARG1='%s ", distccAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				fmt.Sprintf("-DCMAKE_C_COMPILER='%s'", compilerPath),
				fmt.Sprintf("-DCMAKE_C_COMPILER='%s'", compilerAbsPath))
			args.Args = strings.ReplaceAll(args.Args,
				fmt.Sprintf("-DCMAKE_CXX_COMPILER='%s'", compilerPlusPath),
				fmt.Sprintf("-DCMAKE_CXX_COMPILER='%s'", compilerPlusAbsPath))

			return &args
		}
		fmt.Printf("decode server data failed(%s), degraded to local compiling.\n", string(data))
	}

	// cmake local
	args.Args = "."
	switch Compiler {
	case CompilerGcc:
		args.Args = fmt.Sprintf("-DCMAKE_C_COMPILER='%s' -DCMAKE_CXX_COMPILER='%s'",
			getAbsolutePath("gcc"), getAbsolutePath("g++"))
	case CompilerClang:
		args.Args = fmt.Sprintf("-DCMAKE_C_COMPILER='%s' -DCMAKE_CXX_COMPILER='%s'",
			getAbsolutePath("clang"), getAbsolutePath("clang++"))
	}

	return &args
}

func getAbsolutePath(relative string) string {
	out, err := exec.Command("/bin/bash", "-c", fmt.Sprintf("which %s", relative)).Output()
	if err != nil {
		return relative
	}

	return strings.Replace(string(out), "\n", "", -1)
}
