/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

/*
 *
 * 解析clang-cl的参数
 */

package cmd

import (
	"testing"
	"fmt"
)

func TestParseMacro(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"-D_WIN32_", 
		"/D_WIN32_",
		"-D",
		"_WIN32_",
	}
	cmd , _ := Parse(argv)
	if cmd.Program != "clang-cl.exe" {
		t.Errorf("Parse() program error")
	}

	if len(cmd.Macros) != 3 {
		t.Errorf("Parse() macros length error")
	}

	if cmd.Macros[0] != "-D_WIN32_" {
		t.Errorf("Parse() macros error")
	}

	if cmd.Macros[1] != "/D_WIN32_" {
		t.Errorf("Parse() macros error")
	}

	if cmd.Macros[2] != "-D _WIN32_" {
		t.Errorf("Parse() macros error")
	}
}


func TestParseObj(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"-o",
		"test.obj", 
	}
	cmd , _ := Parse(argv)

	if cmd.Obj != "test.obj" {
		t.Errorf("Parse() obj error")
	}
}


func TestParseWinObj(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"/Fotest.obj",
	}
	cmd , _ := Parse(argv)

	if cmd.Obj != "test.obj" {
		t.Errorf("Parse() obj error")
	}
}


func TestParseWarnings(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"/Wall",
	}
	cmd , _ := Parse(argv)

	if cmd.Warnings[0] != "/Wall" {
		t.Errorf("Parse() warnings error")
	}
}


func TestParseOpts(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"/O1",
		"/O2",
		"/O3",
	}
	cmd , _ := Parse(argv)

	if cmd.Opts[2] != "/O3" {
		t.Errorf("Parse() opts error")
	}
}


func TestParseRuntimes(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"/MT",
		"/MTd",
		"/O3",
	}
	cmd , _ := Parse(argv)

	if cmd.Runtimes[1] != "/MTd" {
		t.Errorf("Parse() runtimes error")
	}
}

func TestParseStd(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"/std:c++11",	}
	cmd , _ := Parse(argv)

	if cmd.Std != "/std:c++11" {
		t.Errorf("Parse() std error")
	}
}


func TestParseSrcs(t *testing.T) {
	argv := []string {
		"clang-cl.exe", 
		"a.cpp",	}
	cmd , _ := Parse(argv)

	if cmd.Srcs[0] != "a.cpp" {
		t.Errorf("Parse() srcs error")
	}
}


func TestParseIncludes(t *testing.T) {
    argv := []string {
		"clang-cl.exe", 
		"-imsvcC:\\abc",
		"a.cpp",
	}
	cmd , _ := Parse(argv)
	if cmd.Srcs[0] != "a.cpp" {
		t.Errorf("Parse() srcs error")
	}

	if cmd.Includes[0] != "-imsvcC:\\abc" {
		t.Errorf("Parse() includes error")
	}
}

func TestRenderPreprocess(t *testing.T) {
    argv := []string {
		"clang-cl.exe", 
		"-imsvcC:\\abc",
		"-D", 
		"WIN32",
		"a.cpp",
		"b.cpp",
	}
	cmd , _ := Parse(argv)
	args := cmd.RenderToPreprocess()
	fmt.Printf("%+v", args)
	if args[1] != "/E" {
		t.Errorf("render to preprocess error")

	}
}


func TestRenderToServerSide(t *testing.T) {
    argv := []string {
		"clang-cl.exe", 
		"-imsvcC:\\abc",
		"-D", 
		"WIN32",
		"a.cpp",
		"b.cpp",
		"-otest.obj",
	}
	cmd , _ := Parse(argv)
	args := cmd.RenderToServerSide("tmp.cc")
	fmt.Printf("%+v", args)
	if args[3] != "-c" {
		t.Errorf("render to server error")

	}
}

