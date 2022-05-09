/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package link

import (
	"bufio"
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

func parseArgument(data string) ([]string, []string, error) {
	options := make([]string, 0, 0)
	sources := make([]string, 0, 0)
	curstr := make([]byte, 0, 0)
	i := 0
	for ; i < len(data); i++ {
		c := data[i]
		if c != ' ' && c != '\r' && c != '\n' {
			curstr = []byte{}
			inQuotes := 0
			for ; i < len(data); i++ {
				curChar := data[i]
				curIsQuote := 0
				if curChar == '"' {
					curIsQuote = 1
				}
				if curIsQuote == 1 {
					inQuotes = inQuotes ^ 1
				}

				if (curChar == ' ' || curChar == '\r' || curChar == '\n') && inQuotes == 0 {
					break
				}

				curstr = append(curstr, curChar)
			}

			// !!! here is maybe unnecesary !!!
			if !hasSpace(string(curstr)) {
				options = append(options, strings.Replace(string(curstr), "\"", "", -1))
			} else {
				options = append(options, string(curstr))
			}
		}
	}

	return options, sources, nil
}

func checkCharset(rawBytes []byte) (string, error) {
	detector := chardet.NewTextDetector()
	charset, err := detector.DetectBest(rawBytes)
	if err != nil {
		return "", err
	}

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
		os.Exit(1)
	}
	defer func() {
		_ = f.Close()
	}()

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
func readResponse(f string) (string, error) {
	if !dcFile.Stat(f).Exist() {
		return "", fmt.Errorf("%s dose not exist", f)
	}

	charset, err := checkResponseFileCharset(f)
	if err != nil {
		return "", err
	}

	data := ""
	if charset == "UTF-16LE" {
		data, err = readBom(f)
	} else {
		data, err = readUtf8(f)
	}
	if err != nil {
		return "", err
	}

	if data == "" {
		return "", fmt.Errorf("%s is empty", f)
	}

	return data, nil
}

// ensure compiler exist in args.
func ensureCompiler(args []string) (string, []string, error) {
	responseFile := ""
	if len(args) == 0 {
		blog.Errorf("link: ensure compiler got empty arg")
		return responseFile, nil, ErrorMissingOption
	}

	if args[0] == "/" || args[0] == "@" {
		return responseFile, append([]string{defaultCompiler}, args...), nil
	}

	if !strings.HasSuffix(args[0], defaultCompiler) {
		return responseFile, nil, fmt.Errorf("not supported cmd %s", args[0])
	}

	for _, v := range args {
		if strings.HasPrefix(v, "@") {
			responseFile = strings.Trim(v[1:], "\"")

			data := ""
			if responseFile != "" {
				var err error
				data, err = readResponse(responseFile)
				if err != nil {
					blog.Infof("link: failed to read response file:%s,err:%v", responseFile, err)
					return responseFile, nil, err
				}
			}
			options, _, err := parseArgument(data)
			if err != nil {
				blog.Infof("link: failed to parse response file:%s,err:%v", responseFile, err)
				return responseFile, nil, err
			}

			args = []string{args[0]}
			args = append(args, options...)
		}
	}

	return responseFile, args, nil
}

type libArgs struct {
	inputFile  []string
	outputFile []string
	args       []string
}

// https://docs.microsoft.com/en-us/cpp/build/reference/linker-options?view=msvc-160

func scanArgs(args []string) (*libArgs, error) {
	blog.Infof("lib: scanning arguments: %v", args)

	if len(args) == 0 || strings.HasPrefix(args[0], "/") {
		blog.Errorf("lib: scan args: unrecognized option: %s", args[0])
		return nil, ErrorUnrecognizedOption
	}

	r := new(libArgs)
	outputfilekeys := []string{"/OUT:", "/PDB:", "/PDBSTRIPPED:", "/MAP:",
		"/TSAWARE:", "/PROFILE:", "/WINMDFILE:", "/IMPLIB:"}
	inputfilekeys := []string{"/MANIFESTINPUT:", "/DEF:", "/PGD:", "/SOURCELINK:"}
	libpaths := make([]string, 0, 0)
	libfiles := make([]string, 0, 0)
	// .lib .pdb .dll .exp are necessary output files for link.exe
	libname := ""
	pdbname := ""
	dllname := ""
	expname := ""
	// these keys must exist now
	requiredkeys := map[string]bool{"/INCREMENTAL:NO": false, "/DLL": false, "/OUT:": false}
	for index := 1; index < len(args); index++ {
		arg := args[index]

		if strings.HasPrefix(arg, "/") {
			for k := range requiredkeys {
				if strings.HasPrefix(arg, k) {
					delete(requiredkeys, k)
					break
				}
			}

			switch arg {
			case "/EXPORT", "/INCREMENTAL", "/LINKREPRO", "/LINKREPROTARGET", "/INCLUDE":
				// should be run locally.
				blog.Warnf("link: scan args: %s call for cpp must be local", arg)
				return nil, ErrorNoAvailable4Remote

			// "LTCG" stands for link-time code generation. This feature requires cooperation
			// between the compiler (cl.exe), LIB, and the linker (LINK).
			// Together they can optimize code beyond what any component can do by itself.
			case "/LTCG":
				blog.Warnf("link: scan args: %s call must be local", arg)
				return nil, ErrorNoAvailable4Remote
			}

			for _, key := range outputfilekeys {
				if strings.HasPrefix(arg, key) {
					f := arg[len(key):]
					switch key {
					case "/OUT:":
						dllname = f
					case "/PDB:":
						pdbname = f
					case "/IMPLIB:":
						libname = f
					}
					r.outputFile = append(r.outputFile, f)
					continue
				}
			}

			for _, key := range inputfilekeys {
				if strings.HasPrefix(arg, key) {
					r.inputFile = append(r.inputFile, arg[len(key):])
					continue
				}
			}

			key := "/LIBPATH:"
			if strings.HasPrefix(arg, key) {
				libpaths = append(libpaths, arg[len(key):])
				continue
			}

			continue
		}

		// send file existed
		if filepath.IsAbs(arg) {
			r.inputFile = append(r.inputFile, arg)
		} else if dcFile.Stat(arg).Exist() {
			r.inputFile = append(r.inputFile, arg)
		} else {
			libfiles = append(libfiles, arg)
		}
	}

	for len(requiredkeys) > 0 {
		blog.Warnf("link: scan args failed: not found required key %v", requiredkeys)
		return nil, ErrorNoAvailable4Remote
	}

	if len(r.inputFile) == 0 {
		blog.Warnf("link: scan args failed: not found input file")
		return nil, ErrorNoAvailable4Remote
	}

	if len(r.outputFile) == 0 {
		blog.Warnf("link: scan args failed: not found output file")
		return nil, ErrorNoAvailable4Remote
	}

	if dllname == "" {
		blog.Warnf("link: scan args failed: not found output dll file")
		return nil, ErrorNoAvailable4Remote
	}

	// if lib file existed in /LIBPATH, set it as input file
	if len(libpaths) > 0 && len(libfiles) > 0 {
		for _, f := range libfiles {
			for _, p := range libpaths {
				fullfile, err := filepath.Abs(filepath.Join(p, f))
				if err == nil {
					if dcFile.Stat(fullfile).Exist() {
						blog.Debugf("link: found specifiled lib file:%s", fullfile)
						r.inputFile = append(r.inputFile, fullfile)
						break
					}
				}
			}
		}
	}

	// add necessary output files
	if pdbname == "" {
		r.outputFile = append(r.outputFile, strings.Replace(dllname, ".dll", ".pdb", -1))
	}
	if libname == "" {
		r.outputFile = append(r.outputFile, strings.Replace(dllname, ".dll", ".lib", -1))
	}
	if expname == "" {
		if libname != "" {
			r.outputFile = append(r.outputFile, strings.Replace(libname, ".lib", ".exp", -1))
		} else {
			r.outputFile = append(r.outputFile, strings.Replace(dllname, ".dll", ".exp", -1))
		}
	}

	// trip path for exe
	args[0] = filepath.Base(args[0])

	r.args = args
	blog.Infof("link: input file number [%d], output file number [%s] for arguments: [%s]",
		len(r.inputFile), len(r.outputFile), strings.Join(r.args, " "))
	blog.Infof("link: success to scan arguments: [%s], input file [%s], output file [%s]",
		strings.Join(r.args, " "), strings.Join(r.inputFile, " "), strings.Join(r.outputFile, " "))
	return r, nil
}

func saveResultFile(rf *dcSDK.FileDesc) error {
	fp := rf.FilePath
	data := rf.Buffer
	blog.Debugf("link: ready save file [%s]", fp)
	if fp == "" {
		blog.Warnf("link: file [%s] path is empty!", fp)
		return fmt.Errorf("file path is empty")
	}

	creatTime1 := time.Now().Local().UnixNano()
	f, err := os.Create(fp)
	if err != nil {
		blog.Errorf("link: create file %s error: [%s]", fp, err.Error())
		return err
	}
	creatTime2 := time.Now().Local().UnixNano()

	startTime := time.Now().Local().UnixNano()
	var allocTime int64
	var compressTime int64
	defer func() {

		endTime := time.Now().Local().UnixNano()
		blog.Warnf("link: [iotest] file [%s] srcsize [%d] compresssize [%d] createTime [%d] allocTime [%d] "+
			"uncpmpresstime [%d] savetime [%d] millionseconds",
			fp,
			rf.FileSize,
			rf.CompressedSize,
			(creatTime2-creatTime1)/1000/1000,
			(allocTime-startTime)/1000/1000,
			(compressTime-allocTime)/1000/1000,
			(endTime-compressTime)/1000/1000)

		_ = f.Close()
	}()

	if rf.CompressedSize > 0 {
		switch rf.Compresstype {
		case protocol.CompressNone:
			allocTime = time.Now().Local().UnixNano()
			compressTime = allocTime
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
		// 		blog.Errorf("link: decompress file %s error: [%s]", fp, err.Error())
		// 		return err
		// 	}
		// 	outlen := len(string(outdata))
		// 	blog.Debugf("link: decompressed file %s with lzo1x, from [%d] to [%d]",
		// 		fp, rf.CompressedSize, outlen)
		// 	if outlen != int(rf.FileSize) {
		// 		err := fmt.Errorf("link: decompressed size %d, expected size %d", outlen, rf.FileSize)
		// 		blog.Errorf("link: decompress error: [%v]", err)
		// 		return err
		// 	}

		// 	_, err = f.Write(outdata)
		// 	if err != nil {
		// 		blog.Errorf("link: save file [%s] error: [%v]", fp, err)
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

			allocTime = time.Now().Local().UnixNano()
			outdata, err := dcUtil.Lz4Uncompress(data, dst)
			if err != nil {
				blog.Errorf("link: decompress [%s] error: [%s], data len:[%d], buffer len:[%d], filesize:[%d]",
					fp, err.Error(), len(data), len(dst), rf.FileSize)
				return err
			}
			compressTime = time.Now().Local().UnixNano()
			// outlen := len(string(outdata))
			outlen := len(outdata)
			blog.Debugf("link: decompressed file %s with lz4, from [%d] to [%d]", fp, rf.CompressedSize, outlen)
			if outlen != int(rf.FileSize) {
				err := fmt.Errorf("decompressed size %d, expected size %d", outlen, rf.FileSize)
				blog.Errorf("link: decompress error: [%v]", err)
				return err
			}

			_, err = f.Write(outdata)
			if err != nil {
				blog.Errorf("link: save file [%s] error: [%v]", fp, err)
				return err
			}
			break
		default:
			return fmt.Errorf("link: unknown compress type [%s]", rf.Compresstype)
		}
	}

	blog.Debugf("link: succeed to save file [%s]", fp)
	return nil
}
