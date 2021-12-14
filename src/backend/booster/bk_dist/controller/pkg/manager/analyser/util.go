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
	"bytes"
	"os"
	"path/filepath"
	"regexp"
	"strings"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
)

var sourceFileLanguage = map[string]string{
	// C
	"c": "c",
	"i": "c",

	// C++
	"cc":  "c++",
	"cpp": "c++",
	"cxx": "c++",
	"C":   "c++",
	"CXX": "c++",

	// Objective C
	"m":  "objective-c",
	"mi": "objective-c",

	// Objective C++
	"mm":  "objective-c++",
	"M":   "objective-c++",
	"mii": "objective-c++",
}

var supportLanguage = map[string]bool{
	"c":             true,
	"c++":           true,
	"objective-c":   true,
	"objective-c++": true,
}

func saveNormPath(path string) string {
	if path == "." {
		return ""
	}

	for strings.HasPrefix(path, "./") {
		path = path[2:]
	}

	return strings.TrimRight(path, "/")
}

var systemSearchLocale = []string{
	"LANG=en_US.UTF-8",
	"LC_CTYPE=en_US.UTF-8",
	"LC_NUMERIC=en_US.UTF-8",
	"LC_TIME=en_US.UTF-8",
	"LC_COLLATE=en_US.UTF-8",
	"LC_MONETARY=en_US.UTF-8",
	"LC_MESSAGES=en_US.UTF-8",
	"LC_PAPER=en_US.UTF-8",
	"LC_NAME=en_US.UTF-8",
	"LC_ADDRESS=en_US.UTF-8",
	"LC_TELEPHONE=en_US.UTF-8",
	"LC_MEASUREMENT=en_US.UTF-8",
	"LC_IDENTIFICATION=en_US.UTF-8",
	"LC_ALL=en_US.UTF-8",
}

func getSystemSearch(compiler, sysRoot, language string) ([]string, error) {
	cmd := []string{"-x", language, "-v", "-c", "/dev/null", "-o", "/dev/null"}
	if sysRoot != "" {
		cmd = append([]string{"--sysroot=" + sysRoot}, cmd...)
	}

	sb := dcSyscall.Sandbox{
		Env: env.NewSandbox(append(os.Environ(), systemSearchLocale...)),
	}
	var out bytes.Buffer
	sb.Stderr = &out
	if _, err := sb.ExecCommand(compiler, cmd...); err != nil {
		return nil, err
	}

	match := ReSystemSearch.FindSubmatch(out.Bytes())
	if match == nil {
		return nil, ErrAnalyserSystemIncludeDirUnknown
	}
	rl := strings.Split(string(match[1]), "\n")
	for i := range rl {
		rl[i] = strings.TrimSpace(rl[i])
	}
	return rl, nil
}

func getFileDesc(f *dcFile.Info) *dcSDK.FileDesc {
	return &dcSDK.FileDesc{
		FilePath:           f.Path(),
		FileSize:           f.Size(),
		Lastmodifytime:     f.ModifyTime64(),
		Targetrelativepath: filepath.Dir(f.Path()),
		Filemode:           f.Mode32(),
	}
}

func uniqueSymlinkFileDesc(s []*dcSDK.FileDesc) []*dcSDK.FileDesc {
	r := make([]*dcSDK.FileDesc, 0, len(s))
	for _, a := range s {
		found := false
		for _, b := range r {
			if a.FilePath == b.FilePath && a.LinkTarget == b.LinkTarget {
				found = true
				break
			}
		}
		if found {
			continue
		}

		r = append(r, a)
	}

	return r
}

// parseMacroFunctionArgs 接受一个原字符串, 以及symbol函数结束的位置, 解析并返回这个symbol函数的所有入参, 以及结尾位置
// 例如给定 expr = "myFunction(a, b, c, d) something-others", symbol为"myFunction" = expr[0:10], 则symbolEnd = 10
// 将返回入参列表[a, b, c, d]和symbol函数结束的位置, symbol函数为"myFunction(a, b, c, d)" = expr[0:22], 则结束位置 = 22
func parseMacroFunctionArgs(expr string, symbolEnd int) ([]string, int) {
	// symbol函数如果有参数, 则symbolEnd位置必定为开括号'('
	if symbolEnd >= len(expr) || expr[symbolEnd] != '(' {
		return nil, symbolEnd
	}

	// 记录开括号的次数, 当最外层括号闭合后, 则symbol函数结束
	openParenthesis := 0

	// 记录上一次出现的逗号位置, 用于标定参数在字符串中的区间
	lastComma := symbolEnd

	// 记录当前位置是否在引号内, 若在引号内则不判定任何括号
	insideQuotes := false

	end := 0
	args := make([]string, 0, 10)
	for i := symbolEnd; i < len(expr); i++ {
		s := expr[i]
		ls := expr[i-1]

		if insideQuotes {
			if s == '"' && ls != '\\' {
				// 闭合引号
				insideQuotes = false
			}
			continue
		}

		switch s {
		case ',':

			// 只解析第一层括号的参数, 更深层的参数解析交给后续递归解析去做
			if openParenthesis == 1 {
				if arg := strings.TrimSpace(expr[lastComma+1 : i]); arg != "" {
					args = append(args, arg)
				}
				lastComma = i
			}
		case '(':
			openParenthesis++
		case ')':
			openParenthesis--
			if openParenthesis == 0 {
				if arg := strings.TrimSpace(expr[lastComma+1 : i]); arg != "" {
					args = append(args, arg)
				}
				end = i + 1
				break
			}
		case '"':

			// 开引号, 进入引号区间
			if ls != '\\' {
				insideQuotes = true
			}
		}
	}

	// 没有匹配到完整的函数入参
	if openParenthesis != 0 {
		return nil, symbolEnd
	}

	return args, end
}

func prepend(prefix string, s []string) []string {
	r := make([]string, 0, len(s))
	for i := range s {
		r = append(r, prefix+s[i])
	}
	return r
}

func updateDisabledMap(s map[string]bool, p string) map[string]bool {
	r := make(map[string]bool)
	for k := range s {
		r[k] = true
	}

	r[p] = true
	return r
}

func replaceSymbol(base, old, new string) string {
	re, _ := regexp.Compile(`\b` + regexp.QuoteMeta(old) + `\b`)
	if re == nil {
		return strings.ReplaceAll(base, old, new)
	}
	return re.ReplaceAllString(base, strings.ReplaceAll(new, "\\", "\\\\"))
}

func realExpansion(expansion string) string {
	return ReSinglePound.ReplaceAllString(ReDoublePound.ReplaceAllString(expansion, ""), "\"$1\"")
}

func uniqueStringSlice(s []string) []string {
	r := make([]string, 0, len(s))
	m := make(map[string]bool)

	for i := range s {
		if _, ok := m[s[i]]; ok {
			continue
		}

		r = append(r, s[i])
		m[s[i]] = true
	}

	return r
}

type includeType int

const (
	includeTypeDefault includeType = iota
	includeTypeQuote
	includeTypeAngle
)

func getRealIncludeFromExpr(expr string) (includeType, string) {
	r := ReIncludes.FindStringSubmatch(expr)

	for i, d := range r {
		switch ReIncludes.SubexpNames()[i] {
		case "quote":
			if d != "" {
				return includeTypeQuote, d
			}
		case "angle":
			if d != "" {
				return includeTypeAngle, d
			}
		}
	}

	return includeTypeDefault, expr
}

func findAllSymbol(expr string) []string {
	return ReMacroSymbol.FindAllString(expr, -1)
}

func getFirstWorld(s string) []int {
	first := -1
	for i := 0; i < len(s); i++ {
		if isWorld(s[i]) {
			if first == -1 {
				first = i
			}
			continue
		}

		if first != -1 {
			return []int{first, i}
		}
	}

	if first != -1 {
		return []int{first, len(s)}
	}

	return nil
}

func isWorld(s byte) bool {
	return 'A' <= s && s <= 'Z' || 'a' <= s && s <= 'z' || '0' <= s && s <= '9' || s == '_'
}

func isExprBegin(s byte) bool {
	return s == '"' || s == '<' || isWorld(s)
}
