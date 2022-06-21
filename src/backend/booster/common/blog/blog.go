/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package blog

import (
	"fmt"
	"log"
	"regexp"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog/glog"
	"github.com/Tencent/bk-ci/src/booster/common/conf"
)

// GlogWriter serves as a bridge between the standard log package and the glog package.
type GlogWriter struct{}

// Write implements the io.Writer interface.
func (writer GlogWriter) Write(data []byte) (n int, err error) {
	glog.Info(string(data))
	return len(data), nil
}

var once sync.Once

// InitLogs initializes logs the way we want for blog.
func InitLogs(logConfig conf.LogConfig) {
	glog.InitLogs(
		logConfig.ToStdErr,
		logConfig.AlsoToStdErr,
		logConfig.AsyncFlush,
		logConfig.Verbosity,
		logConfig.StdErrLevel,
		logConfig.StdErrThreshold,
		logConfig.VModule,
		logConfig.TraceLocation,
		logConfig.LogDir,
		logConfig.LogMaxSize,
		logConfig.LogMaxNum)

	Debugf = glog.V(3).Infof
	once.Do(func() {
		log.SetOutput(GlogWriter{})
		log.SetFlags(0)
		// The default glog flush interval is 30 seconds, which is frighteningly long.
		go func() {
			d := time.Duration(5 * time.Second)
			tick := time.Tick(d)
			for {
				select {
				case <-tick:
					glog.Flush()
				}
			}
		}()
	})
}

// CloseLogs will flush the logs in cache out.
func CloseLogs() {
	glog.Flush()
}

var (
	Info  = glog.Infof
	Infof = glog.Infof

	Warn  = glog.Warningf
	Warnf = glog.Warningf

	Error  = glog.Errorf
	Errorf = glog.Errorf

	Fatal  = glog.Fatal
	Fatalf = glog.Fatalf

	Debugf = glog.V(3).Infof

	V = glog.V
)

type StderrLevel int32

const (
	StderrLevelInfo StderrLevel = iota
	StderrLevelWarning
	StderrLevelError
	StderrLevelNothing
)

// SetV set the logs output level in runtime.
func SetV(level int32) {
	glog.SetV(glog.Level(level))
	Debugf = glog.V(3).Infof
}

// SetStderrLevel 设置高于等于level的内容会被打到stderr中
func SetStderrLevel(level StderrLevel) {
	glog.SetStderrLevel(int32(level))
}

// defaultRe and defaultHandler is for bcs-dns wrap its extra time tag in log.
// the extra time tag of bcs-dns: [04/Jan/2018:09:44:27 +0800]
var defaultRe = regexp.MustCompile(`\[\d{2}/\w+/\d{4}:\d{2}:\d{2}:\d{2} \+\d{4}\] `)
var defaultHandler WrapFunc = func(format string, args ...interface{}) string {
	src := fmt.Sprintf(format, args...)
	return defaultRe.ReplaceAllString(src, "")
}

// WrapFunc take the param the same as glog.Infof, and return string.
type WrapFunc func(string, ...interface{}) string

// Wrapper use WrapFunc to handle the log message before send it to glog.
// Can be use as:
//      var handler blog.WrapFunc = func(format string, args ...interface{}) string {
//          src := fmt.Sprintf(format, args...)
//          dst := regexp.MustCompile("boy").ReplaceAllString(src, "man")
//      }
//      blog.Wrapper(handler).V(2).Info("hello boy")
// And it will flush as:
//      I0104 09:44:27.796409   16233 blog.go:21] hello man
type Wrapper struct {
	Handler WrapFunc
	verbose glog.Verbose
}

// Info write the info log.
func (w *Wrapper) Info(format string, args ...interface{}) {
	if w.verbose {
		Info(w.Handler(format, args...))
	}
}

// Warn write the warn log.
func (w *Wrapper) Warn(format string, args ...interface{}) {
	if w.verbose {
		Warn(w.Handler(format, args...))
	}
}

// Error write the error log.
func (w *Wrapper) Error(format string, args ...interface{}) {
	if w.verbose {
		Error(w.Handler(format, args...))
	}
}

// Fatal write the fatal log.
func (w *Wrapper) Fatal(format string, args ...interface{}) {
	if w.verbose {
		Fatal(w.Handler(format, args...))
	}
}

// V specific the log level to print.
func (w *Wrapper) V(level glog.Level) *Wrapper {
	w.verbose = V(level)
	return w
}

// Wrap get a Wrapper with given WrapFunc.
func Wrap(handler WrapFunc) *Wrapper {
	if handler == nil {
		handler = defaultHandler
	}
	return &Wrapper{verbose: true, Handler: handler}
}
