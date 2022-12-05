/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cl

import (
	"bufio"
	"crypto/md5"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonUtil "github.com/Tencent/bk-ci/src/booster/common/util"

	"github.com/google/shlex"
	"github.com/saintfish/chardet"
	"golang.org/x/text/encoding/unicode"
)

func hasSpace(s string) bool {
	if s == "" {
		return false
	}

	for _, v := range s {
		if v == ' ' {
			return true
		}
	}

	return false
}

// func parseArgument(data string) ([]string, []string, error) {
// 	options := make([]string, 0, 0)
// 	sources := make([]string, 0, 0)
// 	curstr := make([]byte, 0, 0)
// 	i := 0
// 	for ; i < len(data); i++ {
// 		c := data[i]
// 		if c != ' ' && c != '\r' && c != '\n' {
// 			curstr = []byte{}
// 			inQuotes := 0
// 			for ; i < len(data); i++ {
// 				curChar := data[i]
// 				curIsQuote := 0
// 				if curChar == '"' {
// 					curIsQuote = 1
// 				}
// 				if curIsQuote == 1 {
// 					inQuotes = inQuotes ^ 1
// 				}

// 				if (curChar == ' ' || curChar == '\r' || curChar == '\n') && inQuotes == 0 {
// 					break
// 				}

// 				curstr = append(curstr, curChar)
// 			}

// 			// ignore /MP
// 			if strings.HasPrefix(string(curstr), "/MP") {
// 				continue
// 			}

// 			// ignore /Zm
// 			// if strings.HasPrefix(string(curstr), "/Zm") {
// 			// 	continue
// 			// }

// 			// deal /Fd
// 			if strings.HasPrefix(string(curstr), "/Fd") {
// 				if options[len(options)-1] != "/FS" {
// 					options = append(options, "/FS")
// 				}
// 			}

// 			// ignore .pdb
// 			if strings.Index(string(curstr), ".pdb") != -1 {
// 				// curstr = []byte("/Z7")
// 				continue
// 			}

// 			// ignore /Zi and /ZI
// 			if strings.HasPrefix(string(curstr), "/Zi") ||
// 				strings.HasPrefix(string(curstr), "/ZI") {
// 				continue
// 			}

// 			s := strings.Trim(string(curstr), "\"")
// 			if isSourceFile(s) {
// 				sources = append(sources, s)
// 			} else {
// 				if string(curstr) == "/we4668" {
// 					options = append(options, "/wd4668") // for ue4
// 				} else {
// 					// !!! here is maybe unnecesary !!!
// 					if !hasSpace(string(curstr)) {
// 						options = append(options, strings.Replace(string(curstr), "\"", "", -1))
// 					} else {
// 						options = append(options, string(curstr))
// 					}
// 				}
// 			}
// 		}
// 	}

// 	return options, sources, nil
// }

func checkCharset(rawBytes []byte) (string, error) {
	detector := chardet.NewTextDetector()
	charset, err := detector.DetectBest(rawBytes)
	if err != nil {
		return "", err
	}

	// fmt.Printf("Charset:%s\n", charset.Charset)
	// fmt.Printf("Language:%s\n", charset.Language)

	return charset.Charset, nil
}

func checkResponseFileCharset(f string) (string, error) {
	data, err := ioutil.ReadFile(f)
	if err != nil {
		return "", err
	}

	return checkCharset(data)
}

func readBom(filename string) (string, error) {
	f, err := os.Open(filename)
	if err != nil {
		_, _ = fmt.Fprintln(os.Stderr, err)
		//os.Exit(1)
		return "", err
	}
	defer func() {
		_ = f.Close()
	}()

	//dec := unicode.UTF16(unicode.LittleEndian, unicode.IgnoreBOM).NewDecoder()
	dec := unicode.UTF16(unicode.LittleEndian, unicode.UseBOM).NewDecoder()
	scn := bufio.NewScanner(dec.Reader(f))
	data := ""
	for scn.Scan() {
		data = data + scn.Text()
	}
	if err := scn.Err(); err != nil {
		return "", err
	}

	return data, nil
}

func readUtf8(filename string) (string, error) {
	data, err := ioutil.ReadFile(filename)
	if err != nil {
		return "", err
	}

	return string(data), nil
}

// return compile options and source files
func readResponse(f, dir string) (string, error) {
	newf := f
	if !dcFile.Stat(newf).Exist() {
		// try with dir
		tempf, _ := filepath.Abs(filepath.Join(dir, newf))
		if !dcFile.Stat(tempf).Exist() {
			return "", fmt.Errorf("%s or %s dose not exist", newf, tempf)
		} else {
			newf = tempf
		}
	}

	charset, err := checkResponseFileCharset(newf)
	if err != nil {
		return "", err
	}

	data := ""
	if charset == "UTF-16LE" {
		data, err = readBom(newf)
	} else {
		data, err = readUtf8(newf)
	}
	if err != nil {
		return "", err
	}

	if data == "" {
		return "", fmt.Errorf("%s is empty", newf)
	}

	return data, nil
}

// replace which next is not in nextExcludes
func replaceWithNextExclude(s string, old byte, new string, nextExcludes []byte) string {
	if s == "" {
		return ""
	}

	if len(nextExcludes) == 0 {
		return strings.Replace(s, string(old), new, -1)
	}

	targetslice := make([]byte, 0, 0)
	nextexclude := false
	totallen := len(s)
	for i := 0; i < totallen; i++ {
		c := s[i]
		if c == old {
			nextexclude = false
			if i < totallen-1 {
				next := s[i+1]
				for _, e := range nextExcludes {
					if next == e {
						nextexclude = true
						break
					}
				}
			}
			if nextexclude {
				targetslice = append(targetslice, c)
				targetslice = append(targetslice, s[i+1])
				i++
			} else {
				targetslice = append(targetslice, []byte(new)...)
			}
		} else {
			targetslice = append(targetslice, c)
		}
	}

	return string(targetslice)
}

// ensure compiler exist in args.
func ensureCompilerRaw(args []string, workdir string) (string, []string, bool, string, string, string, error) {
	responseFile := ""
	sourcedependfile := ""
	objectfile := ""
	pchfile := ""
	showinclude := false
	if len(args) == 0 {
		blog.Warnf("cl: ensure compiler got empty arg")
		return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, ErrorMissingOption
	}

	if args[0] == "/" || args[0] == "@" || isSourceFile(args[0]) || isObjectFile(args[0]) {
		return responseFile, append([]string{defaultCompiler}, args...), showinclude, sourcedependfile, objectfile, pchfile, nil
	}

	if !strings.HasSuffix(args[0], defaultCompiler) {
		return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, fmt.Errorf("not supported cmd %s", args[0])
	}

	for _, v := range args {
		if strings.HasPrefix(v, "@") {
			responseFile = strings.Trim(v[1:], "\"")

			data := ""
			if responseFile != "" {
				var err error
				data, err = readResponse(responseFile, workdir)
				if err != nil {
					blog.Infof("cl: failed to read response file:%s,err:%v", responseFile, err)
					return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, err
				}
			}
			// options, sources, err := parseArgument(data)
			options, err := shlex.Split(replaceWithNextExclude(string(data), '\\', "\\\\", []byte{'"'}))
			if err != nil {
				blog.Infof("cl: failed to parse response file:%s,err:%v", responseFile, err)
				return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, err
			}

			args = []string{args[0]}
			args = append(args, options...)

		} else if v == "/showIncludes" {
			showinclude = true
		}
	}

	// if showinclude {
	// 	args = append(args, "/showIncludes")
	// }

	for i := range args {
		if args[i] == "/sourceDependencies" && i+1 <= len(args)-1 {
			sourcedependfile = args[i+1]
			continue
		} else if strings.HasPrefix(args[i], "/Fo") {
			if len(args[i]) > 3 {
				objectfile = args[i][3:]
				continue
			}

			i++
			if i >= len(args) {
				blog.Warnf("cl: scan args: no output file found after /Fo")
				return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, ErrorMissingOption
			}
			objectfile = args[i]
		} else if strings.HasPrefix(args[i], "/Fp") {
			if len(args[i]) > 3 {
				pchfile = args[i][3:]
				continue
			}

			i++
			if i >= len(args) {
				blog.Warnf("cl: scan args: no output file found after /Fo")
				return responseFile, nil, showinclude, sourcedependfile, objectfile, pchfile, ErrorMissingOption
			}
			pchfile = args[i]
		}
	}

	// TODO : deal with "/sourceDependencies"

	if responseFile != "" && !filepath.IsAbs(responseFile) {
		responseFile, _ = filepath.Abs(filepath.Join(workdir, responseFile))
	}

	if sourcedependfile != "" && !filepath.IsAbs(sourcedependfile) {
		sourcedependfile, _ = filepath.Abs(filepath.Join(workdir, sourcedependfile))
	}

	if objectfile != "" && !filepath.IsAbs(objectfile) {
		objectfile, _ = filepath.Abs(filepath.Join(workdir, objectfile))
	}

	if pchfile != "" && !filepath.IsAbs(pchfile) {
		pchfile, _ = filepath.Abs(filepath.Join(workdir, pchfile))
	}

	return responseFile, args, showinclude, sourcedependfile, objectfile, pchfile, nil
}

// ensure compiler exist in args.
func ensureCompiler(args []string, workdir string) (string, []string, bool, error) {
	responseFile := ""
	if len(args) == 0 {
		blog.Warnf("cl: ensure compiler got empty arg")
		return responseFile, nil, false, ErrorMissingOption
	}

	if args[0] == "/" || args[0] == "@" || isSourceFile(args[0]) || isObjectFile(args[0]) {
		return responseFile, append([]string{defaultCompiler}, args...), false, nil
	}

	if !strings.HasSuffix(args[0], defaultCompiler) {
		return responseFile, nil, false, fmt.Errorf("not supported cmd %s", args[0])
	}

	showinclude := false
	for _, v := range args {
		if strings.HasPrefix(v, "@") {
			responseFile = strings.Trim(v[1:], "\"")

			data := ""
			if responseFile != "" {
				var err error
				data, err = readResponse(responseFile, workdir)
				if err != nil {
					blog.Infof("cl: failed to read response file:%s,err:%v", responseFile, err)
					return responseFile, nil, showinclude, err
				}
			}
			// options, sources, err := parseArgument(data)
			options, err := shlex.Split(replaceWithNextExclude(string(data), '\\', "\\\\", []byte{'"'}))
			if err != nil {
				blog.Infof("cl: failed to parse response file:%s,err:%v", responseFile, err)
				return responseFile, nil, showinclude, err
			}

			for i := range options {
				if options[i] == "/we4668" {
					options[i] = "/wd4668" // for ue4
					break
				}
			}

			// if len(sources) != 1 {
			// 	err := fmt.Errorf("cl: do not support multi source files")
			// 	blog.Infof("%v", err)
			// 	return responseFile, nil, showinclude, err
			// }

			args = []string{args[0]}
			args = append(args, options...)
			// args = append(args, sources[0])
		} else if v == "/showIncludes" {
			showinclude = true
		}
	}

	if showinclude {
		args = append(args, "/showIncludes")
	}

	return responseFile, args, showinclude, nil
}

var (
	sourceFileExt = map[string]bool{
		// begin with i
		".i":  true,
		".ii": true,

		// begin with c
		".c":   true,
		".cc":  true,
		".cpp": true,
		".cxx": true,
		".cp":  true,
		".c++": true,

		// begin with C
		".C": true,

		// begin with m
		".m":   true,
		".mm":  true,
		".mi":  true,
		".mii": true,

		// begin with M
		".M": true,
	}

	preprocessedFileExt = map[string]bool{
		// begin with i
		".i":  true,
		".ii": true,

		// begin with m
		".mi":  true,
		".mii": true,
	}

	preprocessedExtensionMap = map[string]string{
		// extension .i
		".i": ".i",
		".c": ".i",

		// extension .ii
		".cc":  ".ii",
		".cpp": ".ii",
		".cxx": ".ii",
		".cp":  ".ii",
		".c++": ".ii",
		".C":   ".ii",
		".ii":  ".ii",

		// extension .mi
		".mi": ".mi",
		".m":  ".mi",

		// extension .mii
		".mii": ".mii",
		".mm":  ".mii",

		// extension .s
		".s": ".s",
		".S": ".s",
	}

	// skip options and skip its value in the next index
	skipLocalOptionsWithValue = map[string]bool{
		"/D":                  true, // Defines constants and macros.
		"-D":                  true, // same with /D, supported by vs 2022
		"/I":                  true, // Searches a directory for include files.
		"-I":                  true, // same with /I, supported by vs 2022
		"/U":                  true, // Removes a predefined macro.
		"/FI":                 true,
		"/Yu":                 true,
		"/sourceDependencies": true,
		"/external:I":         true, //specify compiler diagnostic behavior for certain header files
		"-external:I":         true, //specify compiler diagnostic behavior for certain header files
	}

	// skip options without value
	skipLocalOptions = map[string]bool{
		// "/MD":  true, // Creates a multithreaded DLL using MSVCRT.lib.
		// "/MDd": true, // Creates a debug multithreaded DLL using MSVCRTD.lib.
		// "/MP":  true, // Compiles multiple source files by using multiple processes.
		"/showIncludes": true,
	}

	// skip options start with flags
	skipLocalOptionStartWith = map[string]bool{
		"/D":          true,
		"/I":          true,
		"-D":          true,
		"-I":          true,
		"/U":          true,
		"/Fd":         true,
		"/FI":         true, // Preprocesses the specified include file.
		"/Fp":         true, // Preprocesses the specified include file.
		"/Yu":         true, // Uses a precompiled header file during build.
		"/Zm":         true, // Specifies the precompiled header memory allocation limit.
		"/external:W": true, //specify compiler diagnostic behavior for certain header files
		"-external:W": true, //specify compiler diagnostic behavior for certain header files
	}
)

func isSourceFile(filename string) bool {
	if _, ok := sourceFileExt[filepath.Ext(filename)]; ok {
		return true
	}

	return false
}

func isObjectFile(filename string) bool {
	return filepath.Ext(filename) == ".obj"
}

// check if the given file is already preprocessed
func isPreprocessedFile(filename string) bool {
	if _, ok := preprocessedFileExt[filepath.Ext(filename)]; ok {
		return true
	}

	return false
}

// get a input file's extension and return the extension should be after preprocessed.
func getPreprocessedExt(inputFile string) string {
	inputExt := filepath.Ext(inputFile)
	if _, ok := preprocessedExtensionMap[inputExt]; !ok {
		return ""
	}

	return preprocessedExtensionMap[inputExt]
}

func stripLocalArgs(args []string) []string {
	r := make([]string, 0, len(args))

	// skip through argv, copying all arguments but skipping ones that ought to be omitted
	for index := 0; index < len(args); index++ {
		arg := args[index]

		// skip the options and its value in next index.
		if _, ok := skipLocalOptionsWithValue[arg]; ok {
			index++
			continue
		}

		// skip the options that with value together
		if func() bool {
			for key := range skipLocalOptionStartWith {
				if strings.HasPrefix(arg, key) {
					return true
				}
			}
			return false
		}() {
			continue
		}

		// skip the options without value
		if _, ok := skipLocalOptions[arg]; ok {
			continue
		}

		r = append(r, arg)
	}

	r[0] = filepath.Base(r[0])
	return r
}

type ccArgs struct {
	inputFile           string
	outputFile          string
	args                []string
	specifiedSourceType bool
}

// scanArgs receive the complete compiling args, and the first item should always be a compiler name.
func scanArgs(args []string) (*ccArgs, error) {
	blog.Debugf("cl: scanning arguments: %v", args)

	if len(args) == 0 || strings.HasPrefix(args[0], "/") {
		blog.Warnf("cl: scan args: unrecognized option: %s", args[0])
		return nil, ErrorUnrecognizedOption
	}

	r := new(ccArgs)
	seenOptionC := false
	seenOptionO := false
	seenInputFile := false
	for index := 0; index < len(args); index++ {
		arg := args[index]

		if strings.HasPrefix(arg, "/") {
			switch arg {
			case "/E", "/EP", "/P":
				// pre-process should be run locally.
				blog.Warnf("cl: scan args: %s call for cpp must be local", arg)
				return nil, ErrorNotSupportE

			case "/Yc":
				// Creates a precompiled header file, should be run locally
				blog.Warnf("cl: scan args: /Yc call for cpp must be local")
				return nil, ErrorNotSupportYc

			case "/c":
				seenOptionC = true
				continue

			case "/TC", "/Tc", "/TP", "/Tp":
				r.specifiedSourceType = true
				continue
			}

			if strings.HasPrefix(arg, "/Yc") {
				// Creates a precompiled header file, should be run locally
				blog.Warnf("cl: scan args: /Yc call for cpp must be local")
				return nil, ErrorNotSupportYcStart
			}

			// if strings.HasPrefix(arg, "/Zm") {
			// 	continue
			// }

			if strings.HasPrefix(arg, "/Fo") {
				// /Fo should always appear once.
				if seenOptionO {
					blog.Warnf("cl: scan args: multi /Fo found in args")
					return nil, ErrorInvalidOption
				}
				seenOptionO = true

				// if /Fo just a prefix, the output file is also in this index, then skip the /Fo.
				if len(arg) > 3 {
					r.outputFile = arg[3:]
					continue
				}

				// if file name is in the next index, then take it.
				// Whatever follows must be the output file
				index++
				if index >= len(args) {
					blog.Warnf("cl: scan args: no output file found after /Fo")
					return nil, ErrorMissingOption
				}
				r.outputFile = args[index]
				continue
			}
			continue
		} else if strings.HasPrefix(arg, "-") {
			switch arg {
			case "-c":
				seenOptionC = true
				continue
			}
		}

		// if this is not start with /, then it maybe a file.
		if isSourceFile(arg) {
			if seenInputFile {
				blog.Warnf("cl: scan args: multi input file found in args")
				return nil, ErrorInvalidOption
			}
			seenInputFile = true

			r.inputFile = arg
			continue
		}

		// if this file is end with .obj, it must be the output file.
		if strings.HasSuffix(arg, ".obj") {
			if seenOptionO {
				blog.Warnf("cl: scan args: multi /Fo found in args")
				return nil, ErrorInvalidOption
			}
			seenOptionO = true
			r.outputFile = args[index]
		}
	}

	if !seenOptionC {
		blog.Warnf("cl: scan args: no /c or -c found, compiler apparently called not for compile")
		return nil, ErrorMissingOption
	}

	if !seenInputFile {
		blog.Warnf("cl: scan args: no visible input file")
		return nil, ErrorMissingOption
	}

	if !seenOptionO {
		var outputFile string
		var err error

		// preprocessing" rather than "stop after compilation."
		if seenOptionC {
			if outputFile, err = outputFromSource(r.inputFile, ".obj"); err != nil {
				return nil, err
			}
		}

		blog.Infof("cl: no visible output file, going to add \"/Fo %s\" at end", outputFile)
		args = append(args, "/Fo", outputFile)
		r.outputFile = outputFile
	}

	if r.outputFile == "" {
		blog.Warnf("cl: output to stdout, running locally")
		return nil, ErrorNotSupportOutputStdout
	}

	if !strings.HasSuffix(r.outputFile, ".obj") {
		f, err := outputFromSource(r.inputFile, ".obj")
		if err != nil {
			return nil, err
		}
		r.outputFile = filepath.Join(r.outputFile, f)

		for i := range args {
			if strings.HasPrefix(args[i], "/Fo") {
				args[i] = "/Fo" + r.outputFile
			}
		}
	}

	r.args = args
	blog.Debugf("cl: success to scan arguments: %s, input file %s, output file %s",
		r.args, r.inputFile, r.outputFile)
	return r, nil
}

func outputFromSource(filename, ext string) (string, error) {
	if len(filepath.Base(filename)) < 3 {
		blog.Warnf("cl: outputFromSource failed: source file %s is bogus", filename)
		return "", ErrorInvalidOption
	}

	return strings.TrimSuffix(filepath.Base(filename), filepath.Ext(filename)) + ext, nil
}

// Create a file inside the temporary directory and register it for
// later cleanup, and return its name.
//
// The file will be reopened later, possibly in a child.  But we know
// that it exists with appropriately tight permissions.
func makeTmpFile(tmpDir, prefix, ext string) (string, error) {
	stat, err := os.Stat(tmpDir)
	if err != nil {
		blog.Errorf("cl: can not access tmp dir \"%s\": %s", tmpDir, err)
		return "", err
	}
	if !stat.IsDir() || stat.Mode()&0555 == 0 {
		blog.Errorf("cl: can not access tmp dir \"%s\": is not a dir or could not be write or execute.", tmpDir)
		return "", ErrorFileInvalid
	}

	var target string
	pid := os.Getpid()
	for i := 0; i < 3; i++ {
		target = filepath.Join(tmpDir,
			fmt.Sprintf("%s_%d_%s_%d%s",
				prefix, pid, commonUtil.RandomString(8), time.Now().UnixNano(), ext))

		f, err := os.Create(target)
		if err != nil {
			blog.Errorf("cl: failed to create tmp file \"%s\": %s", target, err)
			continue
		}

		if err = f.Close(); err != nil {
			blog.Errorf("cl: failed to close tmp file \"%s\": %s", target, err)
			return "", err
		}

		blog.Infof("cl: success to make tmp file \"%s\"", target)
		return target, nil
	}

	return "", fmt.Errorf("cl: create tmp file failed: %s", target)
}

func getPumpIncludeFile(tmpDir, prefix, ext string, args []string) (string, error) {
	fullarg := strings.Join(args, " ")
	md5str := md5.Sum([]byte(fullarg))
	target := filepath.Join(tmpDir, fmt.Sprintf("%s_%x%s", prefix, md5str, ext))

	return target, nil
}

func createFile(target string) error {
	for i := 0; i < 3; i++ {
		f, err := os.Create(target)
		if err != nil {
			blog.Errorf("cl: failed to create tmp file \"%s\": %s", target, err)
			continue
		}

		if err = f.Close(); err != nil {
			blog.Errorf("cl: failed to close tmp file \"%s\": %s", target, err)
			return err
		}

		blog.Infof("cl: success to make tmp file \"%s\"", target)
		return nil
	}

	return fmt.Errorf("cl: create tmp file failed: %s", target)
}

// only genegerate file name, do not create really
func makeTmpFileName(tmpDir, prefix, ext string) string {
	pid := os.Getpid()

	return filepath.Join(tmpDir,
		fmt.Sprintf("%s_%d_%s_%d%s",
			prefix, pid, commonUtil.RandomString(8), time.Now().UnixNano(), ext))
}

// Remove "/Fo" options from argument list.
func stripDashO(args []string) []string {
	r := make([]string, 0, len(args))

	for index := 0; index < len(args); index++ {
		arg := args[index]

		if arg == "/Fo" {
			index++
			continue
		}

		if strings.HasPrefix(arg, "/Fo") {
			continue
		}

		r = append(r, arg)
	}

	return r
}

// Used to change "/c" to "/E", so that we get preprocessed
// source.
func setActionOptionE(args []string) ([]string, error) {
	r := make([]string, 0, len(args))

	found := false
	for _, arg := range args {
		if arg == "/c" {
			found = true
			r = append(r, "/E")
			continue
		} else if arg == "-c" {
			found = true
			r = append(r, "/E")
			continue
		}

		r = append(r, arg)
	}

	if !found {
		blog.Warnf("cl: failed to find /c or -c")
		return nil, ErrorMissingOption
	}

	return r, nil
}

func getPreloadConfig(configPath string) (*dcSDK.PreloadConfig, error) {
	f, err := os.Open(configPath)
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = f.Close()
	}()

	var pConfig dcSDK.PreloadConfig
	if err = codec.DecJSONReader(f, &pConfig); err != nil {
		return nil, err
	}

	return &pConfig, nil
}

func saveResultFile(rf *dcSDK.FileDesc, dir string) error {
	fp := rf.FilePath
	data := rf.Buffer
	blog.Debugf("cl: ready save file [%s]", fp)
	if fp == "" {
		blog.Warnf("cl: file [%s] path is empty!", fp)
		return fmt.Errorf("file path is empty")
	}

	f, err := os.Create(fp)
	if err != nil {
		if !filepath.IsAbs(fp) && dir != "" {
			newfp, _ := filepath.Abs(filepath.Join(dir, fp))
			f, err = os.Create(newfp)
			if err != nil {
				blog.Errorf("cl: create file %s or %s error: [%s]", fp, newfp, err.Error())
				return err
			}
		} else {
			blog.Errorf("cl: create file %s error: [%s]", fp, err.Error())
			return err
		}
	}
	defer func() {
		_ = f.Close()
	}()

	if rf.CompressedSize > 0 {
		switch rf.Compresstype {
		case protocol.CompressNone:
			// allocTime = time.Now().Local().UnixNano()
			// compressTime = allocTime
			_, err := f.Write(data)
			if err != nil {
				blog.Errorf("save file [%s] error: [%s]", fp, err.Error())
				return err
			}
			break
		// case protocol.CompressLZO:
		// 	// decompress with lzox1 firstly
		// 	outdata, err := golzo.Decompress1X(bytes.NewReader(data), int(rf.CompressedSize), 0)
		// 	if err != nil {
		// 		blog.Errorf("cl: decompress file %s error: [%s]", fp, err.Error())
		// 		return err
		// 	}
		// 	outlen := len(string(outdata))
		// 	blog.Debugf("cl: decompressed file %s with lzo1x, from [%d] to [%d]", fp, rf.CompressedSize, outlen)
		// 	if outlen != int(rf.FileSize) {
		// 		err := fmt.Errorf("cl: decompressed size %d, expected size %d", outlen, rf.FileSize)
		// 		blog.Errorf("cl: decompress error: [%v]", err)
		// 		return err
		// 	}

		// 	_, err = f.Write(outdata)
		// 	if err != nil {
		// 		blog.Errorf("cl: save file [%s] error: [%v]", fp, err)
		// 		return err
		// 	}
		// 	break
		case protocol.CompressLZ4:
			// decompress with lz4 firstly
			dst := make([]byte, rf.FileSize)
			if dst == nil {
				err := fmt.Errorf("failed to alloc [%d] size buffer", rf.FileSize)
				blog.Errorf("%v", err)
				return err
			}

			// allocTime = time.Now().Local().UnixNano()
			outdata, err := dcUtil.Lz4Uncompress(data, dst)
			if err != nil {
				blog.Errorf("cl: decompress [%s] error: [%s], data len:[%d], buffer len:[%d], filesize:[%d]",
					fp, err.Error(), len(data), len(dst), rf.FileSize)
				return err
			}
			// compressTime = time.Now().Local().UnixNano()
			// outlen := len(string(outdata))
			outlen := len(outdata)
			blog.Debugf("cl: decompressed file %s with lz4, from [%d] to [%d]", fp, rf.CompressedSize, outlen)
			if outlen != int(rf.FileSize) {
				err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.FileSize)
				blog.Errorf("cl: decompress error: [%v]", err)
				return err
			}

			_, err = f.Write(outdata)
			if err != nil {
				blog.Errorf("cl: save file [%s] error: [%v]", fp, err)
				return err
			}
			break
		default:
			return fmt.Errorf("cl: unknown compress type [%s]", rf.Compresstype)
		}
	}

	blog.Debugf("cl: succeed to save file [%s]", fp)
	return nil
}

// EscapeArg and MakeCmdLine copied from exec_windows.go

// EscapeArg rewrites command line argument s as prescribed
// in https://msdn.microsoft.com/en-us/library/ms880421.
// This function returns "" (2 double quotes) if s is empty.
// Alternatively, these transformations are done:
// - every back slash (\) is doubled, but only if immediately
//   followed by double quote (");
// - every double quote (") is escaped by back slash (\);
// - finally, s is wrapped with double quotes (arg -> "arg"),
//   but only if there is space or tab inside s.
func EscapeArg(s string) string {
	if len(s) == 0 {
		return "\"\""
	}
	n := len(s)
	hasSpace := false
	for i := 0; i < len(s); i++ {
		switch s[i] {
		case '"', '\\':
			n++
		case ' ', '\t':
			hasSpace = true
		}
	}
	if hasSpace {
		n += 2
	}
	if n == len(s) {
		return s
	}

	qs := make([]byte, n)
	j := 0
	if hasSpace {
		qs[j] = '"'
		j++
	}
	slashes := 0
	for i := 0; i < len(s); i++ {
		switch s[i] {
		default:
			slashes = 0
			qs[j] = s[i]
		case '\\':
			slashes++
			qs[j] = s[i]
		case '"':
			for ; slashes > 0; slashes-- {
				qs[j] = '\\'
				j++
			}
			qs[j] = '\\'
			j++
			qs[j] = s[i]
		}
		j++
	}
	if hasSpace {
		for ; slashes > 0; slashes-- {
			qs[j] = '\\'
			j++
		}
		qs[j] = '"'
		j++
	}
	return string(qs[:j])
}

// EscapeArg and MakeCmdLine copied from exec_windows.go

// MakeCmdLine builds a command line out of args by escaping "special"
// characters and joining the arguments with spaces.
func MakeCmdLine(args []string) string {
	var s string
	for _, v := range args {
		if s != "" {
			s += " "
		}
		s += EscapeArg(v)
	}
	return s
}
