/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package analyser

import (
	"fmt"
	"os"
	"path/filepath"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// ParseCommand 解析任务的命令, 获得命令参数相关信息
func (a *Analyser) ParseCommand(currentDir string, args []string, environ *env.Sandbox) (*ParseStat, error) {
	if len(args) < 2 {
		return nil, fmt.Errorf("command too few arguments")
	}

	ps := &ParseStat{}
	ps.Compiler = args[0]

	// 若为绝对路径, 则无需处理
	if !filepath.IsAbs(ps.Compiler) {

		// 若为相对路径, 则在当前目录基础上获取绝对路径
		if filepath.Base(ps.Compiler) != ps.Compiler {
			ps.Compiler = filepath.Join(currentDir, ps.Compiler)
		} else {

			// 若为单独的命令, 则在PATH中找到绝对路径
			bPath := os.Getenv("PATH")
			if environ != nil {
				bPath = environ.GetOriginEnv("PATH")
			}
			ps.Compiler, _ = util.CheckExecutableWithPath(ps.Compiler, bPath, "")
		}
	}

	for i := 1; i < len(args); i++ {
		arg := args[i]

		// 所有不是flag的参数, 先统一过滤掉
		// 其中 - 是标准输入文件
		if !strings.HasPrefix(arg, "-") || arg == "-" {

			// 忽略参数在引号内的情况 TODO: 是否考虑加强?
			if !strings.HasPrefix(arg, "\"-") {

				// 这些非flag, 都被认为是文件
				ps.FileNames = append(ps.FileNames, arg)
			}
			continue
		}

		// 检测是否为1个字母的flag, 如-D, -I
		if action, ok := OptionOneLetter[arg[:2]]; ok {
			flag := arg[:2]

			// 若flag和参数是分离的, 如-I /usr/lib/include, 则取下一位
			// 若flag参数是一体的, 如-I/usr/lib/include, 则直接获取
			if arg = arg[2:]; len(arg) == 0 {
				i += 1

				if i >= len(args) {
					return nil, fmt.Errorf("no argument found for option %s", flag)
				}
				arg = args[i]
			}

			if err := action(ps, arg); err != nil {
				return nil, err
			}
			continue
		}

		// 检测是否为多个字母构成, 且带有分离的指定参数的flag, 如-MF foo
		if action, ok := OptionTwoWords[arg]; ok {
			i += 1

			if i >= len(args) {
				return nil, fmt.Errorf("no argument found for option %s", arg)
			}
			arg = args[i]

			if err := action(ps, arg); err != nil {
				return nil, err
			}
			continue
		}

		// 检测是否为多个字母构成, 且带有=连接指定参数的flag, 如--sysroot=/mumble
		if idx := strings.Index(arg, "="); idx > -1 {
			if action, ok := OptionAppearingAsAssignments[arg[:idx]]; ok {
				if err := action(ps, arg[idx:]); err != nil {
					return nil, err
				}

				continue
			}
		}

		// 检测是否为多个字母构成, 但没有参数的flag, 如-nostdinc
		if action, ok := OptionOneWord[arg]; ok {
			if err := action(ps, arg); err != nil {
				return nil, err
			}

			continue
		}

		// 检测是否为多个字母构成, 且带有合并的指定参数的flag, 如-MFfoo
		// 最后处理这部分是因为这部分最慢
		foundAction := false
		for k, action := range OptionMaybeTwoWords {
			if action != nil && strings.HasPrefix(arg, k) {
				if err := action(ps, arg[len(k):]); err != nil {
					return nil, err
				}

				foundAction = true
				break
			}
		}
		if foundAction {
			continue
		}

		continue
	}

	for _, item := range ps.IDirs {
		if item == "-" {
			return nil, fmt.Errorf("got argument -I-, use -iquote instead")
		}
	}

	if len(ps.FileNames) != 1 {
		return nil, fmt.Errorf("could not lacate filename from: %v", ps.FileNames)
	}

	sourceFile := ps.FileNames[0]
	if ps.OutputFile != "" {
		ps.SourceFilePrefix = ps.OutputFile[len(ps.OutputFile)-len(filepath.Ext(ps.OutputFile)):]
	} else {
		ps.SourceFilePrefix = sourceFile[len(sourceFile)-len(filepath.Ext(sourceFile)):]
	}
	ps.SourceFilePrefix = filepath.Join(currentDir, ps.SourceFilePrefix)

	// 没有指定-x, 或指定了-x none
	if ps.Language == "" || ps.Language == "none" {
		if language, ok := sourceFileLanguage[filepath.Ext(sourceFile)[1:]]; ok {
			ps.Language = language
		}
	}

	if _, ok := supportLanguage[ps.Language]; !ok {
		return nil, fmt.Errorf("no support langauge %s", ps.Language)
	}

	//sysroot := ps.IncludeSysRoot()
	// Compiler-Defaults create symlink from tmp to system dir

	// TODO: get or create files index

	systemDirList, err := a.fileCache.GetSystemDir(ps.Compiler, ps.IncludeSysRoot(), ps.Language)
	if err != nil {
		return nil, err
	}
	ps.SystemDirList = a.fileCache.GetDirectoryFileList(systemDirList)

	ps.AngleDirList = append(ps.AngleDirList, a.fileCache.GetDirectoryFileList(ps.IDirs)...)
	ps.AngleDirList = append(ps.AngleDirList, a.fileCache.GetDirectoryFileList(ps.BeforeSystemDirs)...)
	// makeup angle dir list
	if !ps.NoDistinc {
		ps.AngleDirList = append(ps.AngleDirList, ps.SystemDirList...)
		ps.AngleDirList = append(ps.AngleDirList, a.fileCache.GetDirectoryFileList(systemDirList)...)
	}
	ps.AngleDirList = append(ps.AngleDirList, a.fileCache.GetDirectoryFileList(ps.AfterSystemDirs)...)

	// makeup quote dir list
	ps.QuoteDirList = append(a.fileCache.GetDirectoryFileList(ps.QuoteDirs), ps.AngleDirList...)

	ps.IncludeFileList = a.fileCache.GetRelativeFileList(ps.IncludeFiles)
	ps.SourceFile = a.fileCache.GetRelativeFile(sourceFile)

	blog.Debugf("analyser: [%s] parse command got sourceFile(%s) with index(%d) string(%s)",
		a.id, sourceFile, ps.SourceFile.Index(), ps.SourceFile.String())
	blog.Debugf("analyser: [%s] parse command got quoteDirList: %v", a.id, ps.QuoteDirList)
	blog.Debugf("analyser: [%s] parse command got angleDirList: %v", a.id, ps.AngleDirList)
	return ps, nil
}

// FlagHandleFunc 用来规范当解析到每个参数时对应的处理函数
type FlagHandleFunc func(stat *ParseStat, arg string) error

// FlagHandleNothing 代表该参数无需任何处理
var FlagHandleNothing = func(stat *ParseStat, arg string) error {
	return nil
}

// 只有1个字母的flag的处理规则
var OptionOneLetter = map[string]FlagHandleFunc{
	"-D": func(stat *ParseStat, arg string) error {
		stat.DOpts = append(stat.DOpts, strings.Split(arg, "="))
		return nil
	},
	"-I": func(stat *ParseStat, arg string) error {
		stat.IDirs = append(stat.IDirs, arg)
		return nil
	},
	"-o": func(stat *ParseStat, arg string) error {
		stat.OutputFile = arg
		return nil
	},
	"-x": func(stat *ParseStat, arg string) error {
		stat.Language = arg
		return nil
	},

	// 为了正确地解析, 我们需要囊括所有由两部分组成的参数, 比如 "gcc -L foo", 虽然我们不需要做什么
	// 但不能漏过这些参数, 否则foo会被解析为文件
	"-U": FlagHandleNothing,
	"-A": FlagHandleNothing,
	"-l": FlagHandleNothing,
	"-F": func(stat *ParseStat, arg string) error {
		dirs, err := filepath.Glob(filepath.Join(arg, "*", "Headers"))
		if err != nil {
			return err
		}

		stat.IDirs = append(stat.IDirs, dirs...)
		return nil
	},
	"-u": FlagHandleNothing,
	"-L": FlagHandleNothing,
	"-B": FlagHandleNothing,
	"-V": FlagHandleNothing,
	"-b": FlagHandleNothing,
}

// 参数含多个字母, 且可能分为两个部分(参数, 值), 也可能合在一起的处理规则, 如：
// -MF a.d
// -MFfoo
var OptionMaybeTwoWords = map[string]FlagHandleFunc{
	"-MF":     FlagHandleNothing,
	"-MT":     FlagHandleNothing,
	"-MQ":     FlagHandleNothing,
	"-arch":   FlagHandleNothing,
	"-target": FlagHandleNothing,
	"-include": func(stat *ParseStat, arg string) error {
		stat.IncludeFiles = append(stat.IncludeFiles, arg)
		return nil
	},
	"-imacros": func(stat *ParseStat, arg string) error {
		stat.IncludeFiles = append(stat.IncludeFiles, arg)
		return nil
	},
	"-idirafter": func(stat *ParseStat, arg string) error {
		stat.AfterSystemDirs = append(stat.AfterSystemDirs, arg)
		return nil
	},
	"-iprefix": func(stat *ParseStat, arg string) error {
		stat.IPrefix = arg
		return nil
	},
	"-iwithprefixbefore": func(stat *ParseStat, arg string) error {
		stat.IDirs = append(stat.IDirs, filepath.Join(stat.IPrefix, arg))
		return nil
	},
	"-isysroot": func(stat *ParseStat, arg string) error {
		stat.SysRoot = arg
		return nil
	},
	"-imultilib": func(stat *ParseStat, arg string) error {
		return fmt.Errorf("argument -imultilib is no implement")
	},
	"-isystem": func(stat *ParseStat, arg string) error {
		stat.BeforeSystemDirs = append(stat.BeforeSystemDirs, arg)
		return nil
	},
	"-iquote": func(stat *ParseStat, arg string) error {
		stat.QuoteDirs = append(stat.QuoteDirs, arg)
		return nil
	},
}

// 参数含多个字母, 且一定是分为两个部分(参数, 值)的处理规则
var OptionAlwaysTwoWords = map[string]FlagHandleFunc{
	"-Xpreprocessor": func(stat *ParseStat, arg string) error {
		return fmt.Errorf("argument -Xpreprocessor is no implement")
	},

	// 为了正确地解析, 我们需要囊括所有由两部分组成的参数, 比如 "gcc -L foo", 虽然我们不需要做什么
	// 但不能漏过这些参数, 否则foo会被解析为文件
	"-aux-info":   FlagHandleNothing,
	"--param":     FlagHandleNothing,
	"-Xassembler": FlagHandleNothing,
	"-Xlinker":    FlagHandleNothing,
}

// 含多个字母, 且可能分为两个部分(参数, 值), 由OptionMaybeTwoWords和OptionAlwaysTwoWords组成
var OptionTwoWords = map[string]FlagHandleFunc{}

// 含多个字母, 且包含=号连接参数和值的处理规则
var OptionAppearingAsAssignments = map[string]FlagHandleFunc{
	"--sysroot": func(stat *ParseStat, arg string) error {
		stat.SysRoot = arg
		return nil
	},
}

// 含多个字母, 但只有参数, 没有值的处理规则
var OptionOneWord = map[string]FlagHandleFunc{
	"-undef": FlagHandleNothing,
	"-nostdinc": func(stat *ParseStat, arg string) error {
		stat.NoDistinc = true
		return nil
	},
}

// ParseStat provide the result of a single task's ParseCommand
type ParseStat struct {
	Compiler         string
	NoDistinc        bool
	FileNames        []string
	QuoteDirs        []string
	IncludeFiles     []string
	IDirs            []string
	BeforeSystemDirs []string
	AfterSystemDirs  []string

	Language   string
	ISysRoot   string
	SysRoot    string
	OutputFile string
	IPrefix    string
	DOpts      [][]string

	SourceFilePrefix string

	SourceFile      *File
	QuoteDirList    []*File
	AngleDirList    []*File
	SystemDirList   []*File
	IncludeFileList []*File
}

// IncludeSysRoot get the sys root of this task
func (ps *ParseStat) IncludeSysRoot() string {
	if ps.ISysRoot != "" {
		return ps.ISysRoot
	}

	return ps.SysRoot
}

func init() {
	// 合并两个two-words的配置
	for k, v := range OptionMaybeTwoWords {
		OptionTwoWords[k] = v
	}
	for k, v := range OptionAlwaysTwoWords {
		OptionTwoWords[k] = v
	}
}
