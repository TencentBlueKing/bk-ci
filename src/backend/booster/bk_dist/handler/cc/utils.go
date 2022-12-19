/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cc

import (
	"bytes"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"
	dcSyscall "github.com/Tencent/bk-ci/src/booster/bk_dist/common/syscall"

	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
	commonUtil "github.com/Tencent/bk-ci/src/booster/common/util"

	"github.com/google/shlex"
)

func getEnv(n string) string {
	return os.Getenv(n)
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

func expandOptions(sandbox *syscall.Sandbox, args []string) ([]string, error) {
	newArgs := make([]string, 0, 100)
	for _, arg := range args {
		if strings.HasPrefix(arg, "@") {
			fPath := arg[1:]
			if !filepath.IsAbs(fPath) {
				fPath = filepath.Join(sandbox.Dir, fPath)
			}
			f, err := os.OpenFile(fPath, os.O_RDONLY, 0644)
			if err != nil {
				blog.Errorf("cc: expand options check file(%s) err: %v", arg, err)
				return nil, err
			}
			options, err := ioutil.ReadAll(f)
			_ = f.Close()
			if err != nil {
				blog.Errorf("cc: expand options read file(%s) err: %v", arg, err)
				return nil, err
			}

			rspoptions, _ := shlex.Split(replaceWithNextExclude(string(options), '\\', "\\\\", []byte{'"'}))
			newArgs = append(newArgs, rspoptions...)
			continue
		}

		newArgs = append(newArgs, arg)
	}

	return newArgs, nil
}

// ensure compiler exist in args.
// change "executor -c foo.c" -> "cc -c foo.c"
func ensureCompiler(args []string) ([]string, error) {
	if len(args) == 0 {
		blog.Warnf("cc: ensure compiler got empty arg")
		return nil, ErrorMissingOption
	}

	if args[0] == "-" || isSourceFile(args[0]) || isObjectFile(args[0]) {
		return append([]string{defaultCompiler}, args...), nil
	}

	return args, nil
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
		"-D":                 true,
		"-I":                 true,
		"-U":                 true,
		"-L":                 true,
		"-l":                 true,
		"-MF":                true,
		"-MT":                true,
		"-MQ":                true,
		"-include":           true,
		"-imacros":           true,
		"-iprefix":           true,
		"-iwithprefix":       true,
		"-isystem":           true,
		"-iwithprefixbefore": true,
		"-idirafter":         true,
	}

	// skip options without value
	skipLocalOptions = map[string]bool{
		"-undef":      true,
		"-nostdinc":   true,
		"-nostdinc++": true,
		"-MD":         true,
		"-MMD":        true,
		"-MG":         true,
		"-MP":         true,
	}

	// skip options start with flags
	skipLocalOptionStartWith = map[string]bool{
		"-Wp,":     true,
		"-Wl,":     true,
		"-D":       true,
		"-I":       true,
		"-U":       true,
		"-L":       true,
		"-l":       true,
		"-MF":      true,
		"-MT":      true,
		"-MQ":      true,
		"-isystem": true,
	}
)

func isSourceFile(filename string) bool {
	if _, ok := sourceFileExt[filepath.Ext(filename)]; ok {
		return true
	}

	return false
}

func isObjectFile(filename string) bool {
	return filepath.Ext(filename) == ".o"
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

// Strip arguments like -D and -I from a command line, because they do
// not need to be passed across the wire.  This covers options for
// both the preprocess and link phases, since they should never happen
// remotely.
//
// In the case where we inadvertently do cause preprocessing to happen
// remotely, it is possible that omitting these options will make
// failure more obvious and avoid false success.
//
// Giving -L on a compile-only command line is a bit wierd, but it is
// observed to happen in Makefiles that are not strict about CFLAGS vs
// LDFLAGS, etc.
//
// NOTE: gcc-3.2's manual in the "preprocessor options" section
// describes some options, such as -d, that only take effect when
// passed directly to cpp.  When given to gcc they have different
// meanings.
//
// The value stored in '*out_argv' is malloc'd, but the arguments that
// are pointed to by that array are aliased with the values pointed
// to by 'from'.  The caller is responsible for calling free() on
// '*out_argv'.
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

// convert any -Wp options into regular gcc options.
func expandPreprocessorOptions(args []string) ([]string, error) {
	r := make([]string, 0, len(args)*2)
	for _, arg := range args {
		if !strings.HasPrefix(arg, "-Wp,") {
			r = append(r, arg)
			continue
		}

		options, err := copyExtraArgs(arg)
		if err != nil {
			return nil, err
		}

		r = append(r, options...)
	}

	return r, nil
}

// Convert a "-Wp,..." option into one or more regular gcc options.
// Copy the resulting gcc options to dest_argv, which should be pre-allocated by the caller.
func copyExtraArgs(option string) ([]string, error) {
	args := strings.Split(option, ",")
	if len(args) == 0 || args[0] != "-Wp" {
		return nil, fmt.Errorf("handle copy extra args(%s) failed: %v", option, ErrorMissingOption)
	}

	r := make([]string, 0, len(args))

	// skip first arg "-Wp"
	for index := 1; index < len(args); index++ {
		arg := args[index]

		r = append(r, arg)
		if arg == "-MD" || arg == "-MMD" {
			// if there is no args after -MD or -MMD, the args must be missing filename.
			if index == len(args)-1 {
				return nil, fmt.Errorf("'-Wp,-MD' or '-Wp,-MMD' option is missing filename argument")
			}

			r = append(r, "-MF")
		}
	}

	return r, nil
}

type ccArgs struct {
	inputFile          string
	outputFile         string
	additionOutputFile []string
	mfOutputFile       []string
	args               []string
}

// scanArgs receive the complete compiling args, and the first item should always be a compiler name.
func scanArgs(args []string, sandbox *dcSyscall.Sandbox) (*ccArgs, error) {
	blog.Debugf("cc: scanning arguments: %v", args)

	if len(args) == 0 || strings.HasPrefix(args[0], "-") {
		blog.Warnf("cc: scan args: unrecognized option: %s", args[0])
		return nil, ErrorUnrecognizedOption
	}

	r := new(ccArgs)
	seenOptionS := false
	seenOptionC := false
	seenOptionO := false
	seenInputFile := false
	seenGcov := false
	seenFprofileDir := false
	seenGsplitDwarf := false

	for index := 0; index < len(args); index++ {
		arg := args[index]

		if strings.HasPrefix(arg, "-") {
			switch arg {
			case "-E":
				// pre-process should be run locally.
				blog.Warnf("cc: scan args: -E call for cpp must be local")
				return nil, ErrorNotSupportE

			case "-MD", "-MMD":
				// These two generate dependencies as a side effect.  They
				// should work with the way we call cpp.
				continue

			case "-MG", "-MP":
				// These just modify the behaviour of other -M* options and do
				// nothing by themselves.
				continue

			case "-MF", "-MT", "-MQ":
				// As above but with extra argument.
				index++
				if arg == "-MF" && index < len(args) {
					r.mfOutputFile = append(r.mfOutputFile, args[index])
				}
				continue

			case "-march=native":
				blog.Warnf("cc: scan args: -march=native generates code for local machine; must be local")
				return nil, ErrorNotSupportMarchNative

			case "-mtune=native":
				blog.Warnf("cc: scan args: -mtune=native optimizes for local machine; must be local")
				return nil, ErrorNotSupportMtuneNative

			case "-ftest-coverage", "--coverage", "-coverage":
				seenGcov = true
				continue

			case "-frepo":
				blog.Warnf("cc: scan args: compiler will emit .rpo files; must be local")
				return nil, ErrorNotSupportFrepo

			case "-S":
				seenOptionS = true
				continue

			case "-c":
				seenOptionC = true
				continue
			}

			// ++ by tomtian 20201127,for example: -MF/data/.../XX.cpp.d
			if strings.HasPrefix(arg, "-MF") {
				continue
			}

			if strings.HasPrefix(arg, "-fprofile-dir") {
				seenFprofileDir = true
				continue
			}

			if strings.HasPrefix(arg, "-gsplit-dwarf") {
				seenGsplitDwarf = true
			}

			// --

			// -M(anything else) causes the preprocessor to
			// produce a list of make-style dependencies on
			// header files, either to stdout or to a local file.
			// It implies -E, so only the preprocessor is run,
			// not the compiler. There would be no point trying
			// to distribute it even if we could.
			if strings.HasPrefix(arg, "-M") {
				blog.Warnf("cc: scan args: %s implies -E (maybe) and must be local", arg)
				return nil, ErrorNotSupportM
			}

			// Look for assembler options that would produce output
			// files and must be local.
			// Writing listings to stdout could be supported but it might
			// be hard to parse reliably.
			if strings.HasPrefix(arg, "-Wa") {
				if strings.Contains(arg, ",-a") || strings.Contains(arg, "--MD") {
					blog.Warnf("cc: scan args: %s must be local", arg)
					return nil, ErrorNotSupportWa
				}
				continue
			}

			if strings.HasPrefix(arg, "-specs=") {
				blog.Warnf("cc: scan args: %s must be local", arg)
				return nil, ErrorNotSupportSpecs
			}

			if strings.HasPrefix(arg, "-x") {
				index++
				if index >= len(args) {
					continue
				}
				arg = args[index]

				if strings.HasPrefix(arg, "c") ||
					strings.HasPrefix(arg, "c++") ||
					strings.HasPrefix(arg, "objective-c") ||
					strings.HasPrefix(arg, "objective-c++") ||
					strings.HasPrefix(arg, "go") {
					continue
				}

				blog.Warnf("cc: scan args: gcc's -x handling is complex; running locally for %s", arg)
				return nil, ErrorNotSupportX
			}

			if strings.HasPrefix(arg, "-dr") {
				blog.Warnf("cc: scan args: gcc's debug option %s may write extra files; running locally", arg)
				return nil, ErrorNotSupportDr
			}

			if strings.HasPrefix(arg, "-o") {
				// -o should always appear once.
				if seenOptionO {
					blog.Warnf("cc: scan args: multi -o found in args")
					return nil, ErrorInvalidOption
				}
				seenOptionO = true

				// if -o just a prefix, the output file is also in this index, then skip the -o.
				if len(arg) > 2 {
					r.outputFile = arg[2:]
					continue
				}

				// if file name is in the next index, then take it.
				// Whatever follows must be the output file
				index++
				if index >= len(args) {
					blog.Warnf("cc: scan args: no output file found after -o")
					return nil, ErrorMissingOption
				}
				r.outputFile = args[index]
				continue
			}
			continue
		}

		// if this is not start with -, then it maybe a file.
		if isSourceFile(arg) {
			if seenInputFile {
				blog.Warnf("cc: scan args: multi input file found in args")
				return nil, ErrorInvalidOption
			}
			seenInputFile = true

			r.inputFile = arg
			continue
		}

		// if this file is end with .o, it must be the output file.
		if strings.HasSuffix(arg, ".o") {
			if seenOptionO {
				blog.Warnf("cc: scan args: multi -o found in args")
				return nil, ErrorInvalidOption
			}
			seenOptionO = true
			r.outputFile = args[index]
		}
	}

	if !seenOptionC && !seenOptionS {
		blog.Warnf("cc: scan args: no -c or -s found, compiler apparently called not for compile")
		return nil, ErrorMissingOption
	}

	if !seenInputFile {
		blog.Warnf("cc: scan args: no visible input file")
		return nil, ErrorMissingOption
	}

	// in some cases, input file can not run remotely.
	if base := filepath.Base(r.inputFile); strings.HasPrefix(base, "conftest.") ||
		strings.HasSuffix(base, "tmp.conftest.") {
		blog.Warnf("cc: scan args: autoconf tests are run locally: %s", r.inputFile)
		return nil, ErrorNotSupportConftest
	}

	// This is a commandline like "gcc -c hello.c".  They want
	// hello.o, but they don't say so.  For example, the Ethereal
	// makefile does this.
	//
	// Note: this doesn't handle a.out, the other implied
	// filename, but that doesn't matter because it would already
	// be excluded by not having -c or -S.
	if !seenOptionO {
		var outputFile string
		var err error

		// -S takes precedence over -c, because it means "stop after
		// preprocessing" rather than "stop after compilation."
		if seenOptionS {
			if outputFile, err = outputFromSource(r.inputFile, ".s"); err != nil {
				return nil, err
			}
		} else if seenOptionC {
			if outputFile, err = outputFromSource(r.inputFile, ".o"); err != nil {
				return nil, err
			}
		}

		blog.Infof("cc: no visible output file, going to add \"-o %s\" at end", outputFile)
		args = append(args, "-o", outputFile)
		r.outputFile = outputFile
	}

	if r.outputFile == "-" {
		blog.Warnf("cc: output to stdout, running locally")
		return nil, ErrorNotSupportOutputStdout
	}

	if strings.HasSuffix(r.outputFile, ".gch") {
		blog.Warnf("cc: output file is gch, running locally")
		return nil, ErrorNotSupportGch
	}

	if seenGcov {
		if gcovFile, _ := outputFromSource(r.outputFile, ".gcno"); gcovFile != "" {
			r.additionOutputFile = append(r.additionOutputFile, gcovFile)
			if seenFprofileDir {
				if !filepath.IsAbs(gcovFile) {
					gcovFile = filepath.Join(sandbox.Dir, gcovFile)
				}
				gcovFile = strings.ReplaceAll(gcovFile, "/", "#")
				r.additionOutputFile = append(r.additionOutputFile, filepath.Join(sandbox.Dir, gcovFile))
			}
		}
	}

	if seenGsplitDwarf {
		if dwoFile, _ := outputFromSource(r.outputFile, ".dwo"); dwoFile != "" {
			r.additionOutputFile = append(r.additionOutputFile, dwoFile)
		}
	}

	r.args = args
	blog.Debugf("cc: success to scan arguments: %s, input file %s, output file %s",
		r.args, r.inputFile, r.outputFile)
	return r, nil
}

// Work out the default object file name the compiler would use if -o
// was not specified.  We don't need to worry about "a.out" because
// we've already determined that -c or -S was specified.
//
// However, the compiler does put the output file in the current
// directory even if the source file is elsewhere, so we need to strip
// off all leading directories.
//
// @param sfile Source filename.  Assumed to match one of the
// recognized patterns, otherwise bad things might happen.
func outputFromSource(filename, ext string) (string, error) {
	if len(filepath.Base(filename)) < 3 {
		blog.Warnf("cc: outputFromSource failed: source file %s is bogus", filename)
		return "", ErrorInvalidOption
	}

	return strings.TrimSuffix(filename, filepath.Ext(filename)) + ext, nil
}

// rewrite "cc" to directly call gcc or clang
func rewriteGenericCompiler(args []string) ([]string, error) {
	if len(args) == 0 {
		blog.Warnf("cc: rewrite generic compiler got empty arg")
		return nil, ErrorMissingOption
	}

	cpp := false
	switch args[0] {
	case "cc":
	case "c++":
		cpp = true
	default:
		return args, nil
	}

	// TODO: finish the rewrite-generic-compiler
	_ = cpp
	return args, nil
}

// Clang is a native cross-compiler, but needs to be told to what target it is
// building.
func addClangTarget(args []string) ([]string, error) {
	if len(args) == 0 {
		blog.Warnf("cc: add clang target got empty arg")
		return nil, ErrorMissingOption
	}

	// if it is not about clang, just return.
	if args[0] == "clang" ||
		args[0] == "clang++" ||
		strings.HasPrefix(args[0], "clang-") ||
		strings.HasPrefix(args[0], "clang++-") {

	} else {
		return args, nil
	}

	// already has -target
	if hasOptions(args, "-target") {
		return args, nil
	}

	target := gnuHost
	blog.Infof("cc: adding '-target %s' to support clang cross-compilation", target)
	return append(args, "-target", target), nil
}

// Cross compilation for gcc
func gccRewriteFqn(args []string) ([]string, error) {
	if len(args) == 0 {
		blog.Warnf("cc: gcc rewrite fqn got empty arg")
		return nil, ErrorMissingOption
	}

	// if it is not about gcc, just return.
	if args[0] == "gcc" ||
		args[0] == "g++" ||
		strings.HasPrefix(args[0], "gcc-") ||
		strings.HasPrefix(args[0], "g++-") {

	} else {
		return args, nil
	}

	target := gnuHost
	newCmd := target + "-" + args[0]
	if strings.Contains(newCmd, "-pc-") {
		piece := strings.Split(newCmd, "-pc-")
		newCmd = piece[0] + "-" + piece[1]
	}

	path := getEnv("PATH")
	for _, p := range strings.Split(path, ":") {
		f, err := os.Stat(strings.TrimRight(p, "/") + "/" + newCmd)
		if err != nil {
			continue
		}

		// if the target exist and is executable
		if f.Mode()&0111 != 0 {
			blog.Infof("cc: re-writing call from '%s' to '%s' to support cross-compilation.", args[0], newCmd)
			return append([]string{newCmd}, args[1:]...), nil
		}
	}

	blog.Debugf("cc: gcc rewrite no found executable compiler from '%s' to '%s'", args[0], newCmd)
	return args, nil
}

func hasOptions(r []string, s string) bool {
	for _, i := range r {
		if i == s {
			return true
		}
	}

	return false
}

// Create a file inside the temporary directory and register it for
// later cleanup, and return its name.
//
// The file will be reopened later, possibly in a child.  But we know
// that it exists with appropriately tight permissions.
func makeTmpFile(tmpDir, prefix, filename string) (string, string, error) {
	stat, err := os.Stat(tmpDir)
	if err != nil {
		blog.Errorf("cc: can not access tmp dir \"%s\": %s", tmpDir, err)
		return "", "", err
	}
	if !stat.IsDir() || stat.Mode()&0555 == 0 {
		blog.Errorf("cc: can not access tmp dir \"%s\": is not a dir or could not be write or execute.", tmpDir)
		return "", "", ErrorFileInvalid
	}

	baseDir := filepath.Join(tmpDir,
		fmt.Sprintf("%s_%d_%s_%d", prefix, os.Getpid(), commonUtil.RandomString(8), time.Now().UnixNano()))
	target := filepath.Join(baseDir, filename)
	if err = os.MkdirAll(filepath.Dir(target), os.ModePerm); err != nil {
		blog.Errorf("cc: mkdir dir for %s failed: %v", filepath.Dir(target), err)
		return "", "", err
	}

	f, err := os.Create(target)
	if err != nil {
		blog.Errorf("cc: failed to create tmp file \"%s\": %s", target, err)
		return "", baseDir, err
	}

	if err = f.Close(); err != nil {
		blog.Errorf("cc: failed to close tmp file \"%s\": %s", target, err)
		return "", baseDir, err
	}

	blog.Infof("cc: success to make tmp file \"%s\"", target)
	return target, baseDir, nil
}

// Remove "-o" options from argument list.
//
// This is used when running the preprocessor, when we just want it to write
// to stdout, which is the default when no -o option is specified.
//
// Structurally similar to dcc_strip_local_args()
func stripDashO(args []string) []string {
	r := make([]string, 0, len(args))

	for index := 0; index < len(args); index++ {
		arg := args[index]

		if arg == "-o" {
			index++
			continue
		}

		if strings.HasPrefix(arg, "-o") {
			continue
		}

		r = append(r, arg)
	}

	return r
}

// Used to change "-c" or "-S" to "-E", so that we get preprocessed
// source.
func setActionOptionE(args []string, tryDirectives bool, inspectHeader bool) ([]string, []string, error) {
	var rDirectives []string
	if tryDirectives {
		rDirectives = make([]string, 0, len(args))
	}
	r := make([]string, 0, len(args))

	found := false
	for _, arg := range args {
		if arg == "-c" || arg == "-S" {
			found = true
			r = append(r, "-E")
			if rDirectives != nil {
				rDirectives = append(rDirectives, "-E")
				rDirectives = append(rDirectives, "-fdirectives-only")
			}
			continue
		}

		r = append(r, arg)
		if rDirectives != nil {
			rDirectives = append(rDirectives, arg)
		}
	}

	if !found {
		blog.Warnf("cc: not found -c or -S")
		return nil, nil, ErrorMissingOption
	}

	if inspectHeader {
		if rDirectives != nil {
			rDirectives = append(rDirectives, "-H")
		}
		r = append(r, "-H")
	}

	return rDirectives, r, nil
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

func getFirstIncludeFile(args []string) string {
	for index := 0; index < len(args); index++ {
		if args[index] == "-include" {
			if index+1 < len(args) {
				return args[index+1]
			}
			return ""
		}
	}
	return ""
}

func getOutputFile(args []string, sandbox *dcSyscall.Sandbox) []string {
	r := make([]string, 0, 10)
	seenOptionO := false
	seenOptionS := false
	seenOptionC := false
	seenGcov := false
	seenFprofileDir := false
	var inputFile string
	var outputFile string

	for index := 0; index < len(args); index++ {
		arg := args[index]

		if strings.HasPrefix(arg, "-") {
			switch arg {
			case "-S":
				seenOptionS = true
				continue

			case "-c":
				seenOptionC = true
				continue

			case "-MF":
				if index+1 < len(args) {
					r = append(r, args[index+1])
				}
				continue

			case "-ftest-coverage", "--coverage", "-coverage":
				seenGcov = true
			}
		}

		if strings.HasPrefix(arg, "-MF") {
			if tmp := strings.TrimPrefix(arg, "-MF"); tmp != "" {
				r = append(r, tmp)
			}
			continue
		}

		if strings.HasPrefix(arg, "-fprofile-dir") {
			seenFprofileDir = true
			continue
		}

		if isSourceFile(arg) {
			inputFile = arg
			continue
		}

		if strings.HasPrefix(arg, "-o") {
			seenOptionO = true

			// if -o just a prefix, the output file is also in this index, then skip the -o.
			if len(arg) > 2 {
				r = append(r, arg[2:])
				continue
			}

			// if file name is in the next index, then take it.
			// Whatever follows must be the output file
			index++
			if index >= len(args) {
				break
			}
			r = append(r, args[index])
			continue
		}
	}

	if !seenOptionO {
		// -S takes precedence over -c, because it means "stop after
		// preprocessing" rather than "stop after compilation."
		if seenOptionS {
			outputFile, _ = outputFromSource(inputFile, ".s")
		} else if seenOptionC {
			outputFile, _ = outputFromSource(inputFile, ".o")
		}

		if outputFile != "" {
			r = append(r, outputFile)
		}
	}

	if outputFile != "" && seenGcov {
		if gcovFile, _ := outputFromSource(outputFile, ".gcno"); gcovFile != "" {
			r = append(r, gcovFile)
			if seenFprofileDir {
				if !filepath.IsAbs(gcovFile) {
					gcovFile = filepath.Join(sandbox.Dir, gcovFile)
				}
				gcovFile = strings.ReplaceAll(gcovFile, "/", "#")
				r = append(r, filepath.Join(sandbox.Dir, gcovFile))
			}
		}
	}

	return r
}

func removeCCacheBinFromPATH(path string) string {
	r := make([]string, 0, len(path))
	for _, item := range strings.Split(path, ":") {
		if item == "/usr/lib64/ccache" {
			continue
		}
		r = append(r, item)
	}
	return strings.Join(r, ":")
}

func wrapActionOptions(action, k, v string) string {
	v = strings.ReplaceAll(v, "\"", "\\\"")
	v = strings.ReplaceAll(v, "$", "\\$")
	return fmt.Sprintf("--%s=%s=\"%s\"", action, k, v)
}

func replaceFileContent(path, oldContent, newContent string) error {
	oldContent = strings.ReplaceAll(oldContent, "/", "\\/")
	newContent = strings.ReplaceAll(newContent, "/", "\\/")

	sandbox := syscall.Sandbox{}
	var errOut bytes.Buffer
	sandbox.Stderr = &errOut

	if _, err := sandbox.ExecScripts(
		fmt.Sprintf("sed -i 's/%s/%s/g' %s", oldContent, newContent, path)); err != nil {
		return fmt.Errorf("sed error: %v, %s", err, errOut.String())
	}
	return nil
}

func parseInspectHeader(buffer string) []string {
	r := make([]string, 0, 20)
	for _, line := range strings.Split(buffer, "\n") {
		if strings.Contains(line, "Multiple include guards") {
			break
		}

		if !strings.HasPrefix(line, ".") {
			continue
		}

		r = append(r, strings.TrimSpace(strings.Trim(line, ".")))
	}

	return r
}
