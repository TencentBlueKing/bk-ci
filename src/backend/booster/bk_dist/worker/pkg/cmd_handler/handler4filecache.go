/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package pbcmd

import (
	"fmt"
	"os"
	"strconv"
	"sync"
	"time"

	dcConfig "github.com/Tencent/bk-ci/src/booster/bk_dist/common/config"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcProtocol "github.com/Tencent/bk-ci/src/booster/bk_dist/common/protocol"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/cache"
	"github.com/Tencent/bk-ci/src/booster/bk_dist/worker/pkg/protocol"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

var defaultCM cache.Manager

// Handle4FileCache describe the file cache handler in remote worker
type Handle4FileCache struct {
	cm cache.Manager
}

// NewHandle4FileCache get a new file cache handler
func NewHandle4FileCache() *Handle4FileCache {
	handler := &Handle4FileCache{}

	if env.GetEnv(env.KeyWorkerCacheEnable) != "" {
		cacheDir := env.GetEnv(env.KeyWorkerCacheDir)
		poolSize, _ := strconv.Atoi(env.GetEnv(env.KeyWorkerCachePoolSize))
		cacheMinSize, _ := strconv.ParseUint(env.GetEnv(env.KeyWorkerCacheMinSize), 10, 64)
		var err error

		blog.Infof("file cache: cache enabled, try new a cache manager with dir(%s) pool-size(%d)",
			cacheDir, poolSize)

		if handler.cm, err = cache.NewManager(cache.ManagerConfig{
			CacheDir:     cacheDir,
			PoolSize:     poolSize,
			CacheMinSize: int64(cacheMinSize),
		}); err != nil {
			blog.Warnf("file cache: try new a cache manager with dir(%s) pool-size(%d) failed: %v",
				cacheDir, poolSize, err)
		} else {
			blog.Infof("file cache: success to enable cache in dir(%s) with pool-size(%d)",
				cacheDir, poolSize)
		}

		defaultCM = handler.cm
	}

	return handler
}

// ReceiveBody receive body for this cmd
func (h *Handle4FileCache) ReceiveBody(client *protocol.TCPClient,
	head *dcProtocol.PBHead,
	basedir string,
	c chan<- string) (interface{}, error) {
	req, err := protocol.ReceiveBKCheckCache(client, head, basedir, FilepathMapping, c)
	if err != nil {
		blog.Errorf("file cache: failed to receive dispatch req body error: %v", err)
		return nil, err
	}

	blog.Infof("file cache: succeed to receive dispatch req body")
	return req, nil
}

// Handle handle the request
func (h *Handle4FileCache) Handle(client *protocol.TCPClient,
	head *dcProtocol.PBHead,
	body interface{},
	receivedtime time.Time,
	_ string,
	_ []dcConfig.CmdReplaceRule) error {
	blog.Infof("file cache: handle file check cache")
	// convert to req
	req, ok := body.(*dcProtocol.PBBodyCheckCacheReq)
	if !ok {
		err := fmt.Errorf("failed to get body from interface")
		blog.Errorf("file cache: handle and get body failed: %v", err)
		return err
	}

	var wg sync.WaitGroup
	result := make([]*dcProtocol.PBCacheResult, len(req.GetParams()))
	for index, param := range req.GetParams() {
		name := string(param.GetName())
		md5 := string(param.GetMd5())
		target := string(param.GetTarget())
		filemode := param.GetFilemode()
		modifytime := param.GetModifytime()

		if name == "" || md5 == "" || target == "" {
			blog.Warnf("file cache: try check file cache with name(%s) md5(%s) target(%s) failed: "+
				"invalid params and will skip this param", name, md5, target)
			result[index] = encodeRsp(dcProtocol.PBCacheStatus_ERRORWHILEFINDING, "invalid param")
			continue
		}

		wg.Add(1)
		go func(i int, n, m, t string, fm uint32, mt int64) {
			defer wg.Done()
			result[i] = h.searchCacheAndGetFileSaved(n, m, t, fm, mt)
		}(index, name, md5, target, filemode, modifytime)
	}
	wg.Wait()

	// encode response to messages
	messages, err := protocol.EncodeBKCheckCacheRsp(result)
	if err != nil {
		blog.Errorf("file cache: failed to encode rsp to messages for error:%v", err)
		return nil
	}
	blog.Infof("file cache: success to encode check cache response to messages")

	// send response
	err = protocol.SendMessages(client, &messages)
	if err != nil {
		blog.Errorf("file cache: failed to send messages for error:%v", err)
	}
	blog.Infof("file cache: succeed to send messages")

	return nil
}

func (h *Handle4FileCache) searchCacheAndGetFileSaved(name, md5, target string,
	filemode uint32, modifytime int64) *dcProtocol.PBCacheResult {
	// TODO (tomtian) : if target existed and md5 same, do nothing
	// maybe should check whether file mode is changed
	existed := false
	defer func() {
		if existed {
			if filemode > 0 {
				blog.Infof("ready set cached file[%s] with filemode[%d]", target, filemode)
				if err := os.Chmod(target, os.FileMode(filemode)); err != nil {
					blog.Warnf("chmod file %s to file-mode %s failed: %v", target, os.FileMode(filemode), err)
				}
			}

			if modifytime > 0 {
				blog.Infof("ready set cached file[%s] modify time [%d]", target, modifytime)
				if err := os.Chtimes(target, time.Now(), time.Unix(0, modifytime)); err != nil {
					blog.Warnf("Chtimes file %s to time %s failed: %v", target, time.Unix(0, modifytime), err)
				}
			}
		}
	}()

	finfo := dcFile.Stat(target)
	if finfo.Exist() {
		oldmd5, _ := finfo.Md5()
		if oldmd5 == md5 {
			blog.Infof("file cache: target file[%s] with md5(%s) existed, set filemode and modify time now", target, md5)
			existed = true
			return encodeRsp(dcProtocol.PBCacheStatus_SUCCESS, "")
		}
	}

	if h.cm == nil {
		return encodeRsp(dcProtocol.PBCacheStatus_NOFOUND, "no found")
	}

	f, err := h.cm.Search(cache.Description{Name: name, MD5: md5})
	if err == cache.ErrCacheNoFound {
		blog.Infof("file cache: try check file cache with name(%s) md5(%s) but no found", name, md5)
		return encodeRsp(dcProtocol.PBCacheStatus_NOFOUND, "no found")
	}
	if err != nil {
		blog.Errorf("file cache: try check file cache with name(%s) md5(%s) failed: %v", name, md5, err)
		return encodeRsp(dcProtocol.PBCacheStatus_ERRORWHILEFINDING, err.Error())
	}

	blog.Infof("file cache: try check file cache with name(%s) md5(%s) and hit", name, md5)
	if err = f.SaveTo(target); err != nil {
		blog.Errorf("file cache: file hit cache with name(%s) md5(%s) but failed to save to %s: %v",
			name, md5, target, err)
		return encodeRsp(dcProtocol.PBCacheStatus_ERRORWHILESAVING, err.Error())
	}

	existed = true
	blog.Infof("file cache: success to hit file cache with name(%s) md5(%s) and save to %s", name, md5, target)
	return encodeRsp(dcProtocol.PBCacheStatus_SUCCESS, "")
}

func encodeRsp(status dcProtocol.PBCacheStatus, reason string) *dcProtocol.PBCacheResult {

	return &dcProtocol.PBCacheResult{
		Status: &status,
		Reason: []byte(reason),
	}
}
