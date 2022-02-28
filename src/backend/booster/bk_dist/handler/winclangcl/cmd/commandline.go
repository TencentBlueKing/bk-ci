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
    "fmt"
    "strings"
)

type Commandline struct {
    
    Program string
    Srcs []string
    // *.cc *.cpp *.c
    FileType string
    // /Fo -o
    Obj string
    // -I ./ /I./ -I./ -msvc /FI
    Includes []string
    // -D /D
    Macros []string
    // /MT /MD
    Runtimes []string
    //O1 O2 O3
    Opts []string
    // /std:c++11
    Std string
    // /W
    Warnings []string
    // /Yc
    Pchs []string
    // -target
    Target string

    Others []string

    Unsupported []string
}

func Parse(argv []string) (Commandline, error) {
    cmd := Commandline{}
    if len(argv) == 0 {
        err := fmt.Errorf("argv is empty")
        return cmd, err
    }
    cmd.Program = argv[0]
    if (!strings.HasSuffix(cmd.Program, "clang-cl.exe") && 
        !strings.HasSuffix(cmd.Program, "clang-cl")) {
            err := fmt.Errorf("%s is not supported", cmd.Program)
            return cmd, err
        }
    i := 1
    for i < len(argv) {
        if(argv[i] == "-I" || argv[i] == "/I") {
            inc := argv[i] + " " + argv[i+1]
            cmd.Includes = append(cmd.Includes, inc)
            i = i + 1
            goto next
        }
        if(strings.HasPrefix(argv[i], "-I") || strings.HasPrefix(argv[i], "/I")) {
            cmd.Includes = append(cmd.Includes, argv[i])
            goto next
        }

        if(strings.HasPrefix(argv[i], "-imsvc")) {
            cmd.Includes = append(cmd.Includes, argv[i])
            goto next
        }

        if(strings.HasPrefix(argv[i], "/FI")) {
            cmd.Includes = append(cmd.Includes, argv[i])
            goto next
        }


        // /D _WIN_32
        if(argv[i] == "/D" || argv[i] == "-D") {
            macro := argv[i] + " " + argv[i+1]
            cmd.Macros = append(cmd.Macros, macro)
            i = i + 1
            goto next
        }
        // /D_WIN_32
        if(strings.HasPrefix(argv[i], "/D") || strings.HasPrefix(argv[i], "-D")) {
            cmd.Macros = append(cmd.Macros, argv[i])
            goto next
        }
        if(argv[i] == "-o"){
            cmd.Obj = argv[i+1]
            i = i + 1
            goto next
        }
        if(strings.HasPrefix(argv[i], "-o")) {
            cmd.Obj = argv[i][2:]
            goto next
        }
        if(strings.HasPrefix(argv[i], "/Fo")) {
            cmd.Obj = argv[i][3:]
            goto next
        }
        if(strings.HasPrefix(argv[i], "/W")) {
            cmd.Warnings = append(cmd.Warnings, argv[i])
            goto next
        }
        if(strings.HasPrefix(argv[i], "/M")) {
            cmd.Runtimes = append(cmd.Runtimes, argv[i])
            goto next
        }
        if(strings.HasPrefix(argv[i], "/O")) {
            cmd.Opts = append(cmd.Opts, argv[i])
            goto next
        }
        if(strings.HasPrefix(argv[i], "/O")) {
            cmd.Opts = append(cmd.Opts, argv[i])
            goto next
        }
        if(strings.HasPrefix(argv[i], "/std:")) {
            cmd.Std = argv[i]
            goto next
        }

        if(strings.HasPrefix(argv[i], "--target=")) {
            cmd.Target = argv[i]
            goto next
        }

        if( strings.HasSuffix(argv[i], ".cc")) {
            cmd.Srcs = append(cmd.Srcs, argv[i])
            cmd.FileType = "cc"
            goto next
        }

        if( strings.HasSuffix(argv[i], ".c")) {
            cmd.Srcs = append(cmd.Srcs, argv[i])
            cmd.FileType = "c"
            goto next
        }

        if( strings.HasSuffix(argv[i], ".cpp")) {
            cmd.Srcs = append(cmd.Srcs, argv[i])
            cmd.FileType = "cpp"
            goto next
        }

        if( strings.HasPrefix(argv[i], "/Fp") || 
            strings.HasPrefix(argv[i], "/Yu") || 
            strings.HasPrefix(argv[i], "/Yc")) {
            cmd.Pchs = append(cmd.Pchs, argv[i])
            goto next
        }
        if(argv[i] == "-fcolor-diagnostics") {
            cmd.Unsupported = append(cmd.Unsupported, argv[i])
            goto next
        }

        if(argv[i] == "-Werror") {
            cmd.Unsupported = append(cmd.Unsupported, argv[i])
            goto next
        }

        cmd.Others = append(cmd.Others, argv[i])
        
        next:
            i = i + 1

    }

    return cmd, nil
}

//输出服务端编译命令参数列表
func (cmd *Commandline)RenderToServerSide(src string) []string{
    result := []string{}
    result = append(result, cmd.Program)
    result = append(result, "--target=x86_64-pc-windows-msvc")
    result = append(result, "--driver-mode=cl")
    result = append(result, "-c")
    result = append(result, src)
    result = append(result, "-o")
    result = append(result, cmd.Obj)
    result = append(result, cmd.Runtimes...)
    result = append(result, cmd.Std)
    result = append(result, cmd.Warnings...)
    result = append(result, cmd.Opts...)
    result = append(result, cmd.Others...)
    result = append(result, "/WX-")
    return result
}

// 输出预处理的命令
func (cmd *Commandline)RenderToPreprocess() []string{
    result := []string{}
    result = append(result, cmd.Program)
    result = append(result, "/E")
    result = append(result, cmd.Srcs...)
    result = append(result, cmd.Includes...)
    result = append(result, cmd.Macros...)
    result = append(result, cmd.Runtimes...)
    result = append(result, cmd.Std)
    result = append(result, cmd.Warnings...)
    result = append(result, cmd.Opts...)
    result = append(result, cmd.Others...)
    return result
}


