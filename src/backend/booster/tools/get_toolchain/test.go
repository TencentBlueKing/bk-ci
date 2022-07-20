package main

import (
	"encoding/json"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
)

var (
	targetDir = ""
	t         = OneToolChain{}
)

type File struct {
	LocalFullPath      string `json:"local_full_path"`
	RemoteRelativePath string `json:"remote_relative_path"`
}

type OneToolChain struct {
	ToolKey                string `json:"tool_key"`
	ToolName               string `json:"tool_name"`
	ToolLocalFullPath      string `json:"tool_local_full_path"`
	ToolRemoteRelativePath string `json:"tool_remote_relative_path"`
	Files                  []File `json:"files"`
}

type ToolChain struct {
	ToolChains []OneToolChain `json:"toolchains"`
}

func fileter(path string, info os.FileInfo, err error) error {
	if err != nil {
		fmt.Println(err)
		return err
	}

	ext := filepath.Ext(path)
	base := filepath.Base(path)
	// fmt.Printf("dir: %v: name: %s  ext:%s\n", info.IsDir(), path, filepath.Ext(path))
	if ext == ".exe" {
		// fmt.Printf("got exe: %s\n", path)
		if base == "cl.exe" {
			t.ToolKey = path
			t.ToolName = base
			t.ToolLocalFullPath = path
			t.ToolRemoteRelativePath = filepath.Dir(path)
		} else if base == "mspdbsrv.exe" {
			t.Files = append(t.Files, File{
				LocalFullPath:      path,
				RemoteRelativePath: filepath.Dir(path),
			})
		}
	} else if ext == ".dll" {
		// fmt.Printf("got dll: %s\n", path)
		if base == "clui.dll" {
			t.Files = append(t.Files, File{
				LocalFullPath:      path,
				RemoteRelativePath: filepath.Dir(path),
			})
		} else {
			fmt.Printf("filepath.Dir(path):%s,targetDir:%s\n", filepath.Dir(path), targetDir)
			if filepath.Dir(path) == targetDir {
				t.Files = append(t.Files, File{
					LocalFullPath:      path,
					RemoteRelativePath: filepath.Dir(path),
				})
			}
		}
	}

	return nil
}

func getToolChain(dir, f string) {
	err := filepath.Walk(dir, fileter)

	if err != nil {
		fmt.Println(err)
	}

	fullT := ToolChain{}
	fullT.ToolChains = append(fullT.ToolChains, t)

	b, err := json.Marshal(fullT)
	if err != nil {
		fmt.Println("error:", err)
	}

	// fmt.Printf("%s\n", string(b))
	jsonstr := string(b)
	jsonstr = strings.ReplaceAll(jsonstr, "\\\\", "/")
	fmt.Printf("%s\n", jsonstr)

	ioutil.WriteFile(f, []byte(jsonstr), os.ModePerm)
}

func main() {
	if len(os.Args) != 3 {
		fmt.Printf("usage: %s target_dir output_json_file\n", os.Args[0])
		fmt.Printf("for example: %s C:\\PROGRA~1\\MIB055~1\\2022\\COMMUN~1\\VC\\Tools\\MSVC\\1431~1.311\\bin\\Hostx64\\x64 cl_toolchain.json\n", os.Args[0])
		os.Exit(1)
	}

	targetDir = os.Args[1]
	getToolChain(os.Args[1], os.Args[2])
}
