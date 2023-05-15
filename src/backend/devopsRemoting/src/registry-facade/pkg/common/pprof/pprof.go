// Copyright (c) 2020 Gitpod GmbH. All rights reserved.
// Licensed under the GNU Affero General Public License (AGPL).
// See License.AGPL.txt in the project root for license information.

package pprof

import (
	"common/logs"
	"math/rand"
	"net/http"
	"net/http/pprof"
	"runtime"
	"strconv"
	"strings"
)

// TODO: 重构后移至common包

// http handler path which MUST be used as a prefix to route pprof endpoint
// since it is hardcoded inside pprof
const Path = "/debug/pprof/"

// Serve starts a new HTTP server serving pprof endpoints on the given addr
func Serve(addr string) {
	mux := Handler()

	logs.WithField("addr", addr).Info("serving pprof service")
	err := http.ListenAndServe(addr, mux)
	if err != nil {
		logs.WithField("addr", addr).WithError(err).Warn("cannot serve pprof service")
	}
}

// Handler produces the pprof endpoint handler
func Handler() *http.ServeMux {
	mux := http.NewServeMux()
	mux.HandleFunc(Path, index)
	mux.HandleFunc(Path+"cmdline", pprof.Cmdline)
	mux.HandleFunc(Path+"profile", pprof.Profile)
	mux.HandleFunc(Path+"symbol", pprof.Symbol)
	mux.HandleFunc(Path+"trace", pprof.Trace)

	mux.HandleFunc("/debug/logging", logs.LevelHandler)

	return mux
}

func index(w http.ResponseWriter, r *http.Request) {
	if strings.HasPrefix(r.URL.Path, Path) {
		// according to Ian Lance Taylor it's ok to turn on mutex and block profiling
		// when asking for the actual profile [1]. This handler implements this idea, as
		// discussed in [2]
		//
		// [1] https://groups.google.com/forum/#!topic/golang-nuts/qiHa97XzeCw
		// [2] https://github.com/golang/go/issues/23401

		var (
			name          = strings.TrimPrefix(r.URL.Path, Path)
			seconds, serr = strconv.ParseInt(r.URL.Query().Get("seconds"), 10, 64)
		)
		if name == "mutex" {
			frac, ferr := strconv.ParseInt(r.URL.Query().Get("frac"), 10, 64)
			if serr == nil && ferr == nil && seconds > 0 && frac > 0 {
				//nolint:gosec
				id := rand.Uint32()
				logs.WithField("id", id).WithField("frac", frac).WithField("seconds", seconds).Debug("enabled mutex profiling")

				runtime.SetMutexProfileFraction(int(frac))
				defer func() {
					runtime.SetMutexProfileFraction(0)
					logs.WithField("id", id).WithField("frac", frac).WithField("seconds", seconds).Debug("disabled mutex profiling")
				}()
			}
		} else if name == "block" {
			rate, rerr := strconv.ParseInt(r.URL.Query().Get("rate"), 10, 64)
			if rerr == nil && rate > 0 && serr == nil && seconds > 0 {
				//nolint:gosec
				id := rand.Uint32()
				logs.WithField("id", id).WithField("rate", rate).WithField("seconds", seconds).Debug("enabled mutex block sampling")
				runtime.SetBlockProfileRate(int(rate))

				defer func() {
					runtime.SetBlockProfileRate(0)
					logs.WithField("id", id).WithField("rate", rate).WithField("seconds", seconds).Debug("disabled mutex block sampling")
				}()
			}
		}
	}

	pprof.Index(w, r)
}
