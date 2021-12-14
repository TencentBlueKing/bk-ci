/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package recorder

import (
	"crypto/md5"
	"fmt"
	"os"
	"path/filepath"
	"strings"
	"sync"
	"time"

	dcUtil "github.com/Tencent/bk-ci/src/booster/bk_dist/common/util"
	"github.com/Tencent/bk-ci/src/booster/common/blog"

	"github.com/go-yaml/yaml"
)

var (
	recordExpiryTime = 7 * 24 * time.Hour
)

// NewRecordersPool get a new RecordersPool
func NewRecordersPool() *RecordersPool {
	return &RecordersPool{
		recorders: make(map[string]*Recorder),
	}
}

// RecordersPool 提供一个池子来管理recorders, 可以用不同的key来区分不同的记录空间
// 例如以project为维度, 或以整台机器为维度
type RecordersPool struct {
	sync.RWMutex

	recorders map[string]*Recorder
}

// GetRecorders 获取一个recorder, 若不在缓存中, 则从文件中读取或创建
func (rp *RecordersPool) GetRecorder(key string) (*Recorder, error) {
	rp.Lock()
	defer rp.Unlock()

	recorder, ok := rp.recorders[key]
	if ok {
		return recorder, nil
	}

	var err error
	if recorder, err = NewRecorder(filepath.Join(dcUtil.GetRecordDir(), key+".yaml")); err != nil {
		return nil, err
	}
	rp.recorders[key] = recorder

	return recorder, nil
}

// SaveAll 保存所有recorders到文件
func (rp *RecordersPool) SaveAll() {
	rp.Lock()
	defer rp.Unlock()

	for key, recorder := range rp.recorders {
		if err := recorder.saveToFile(); err != nil {
			blog.Warnf("recorder: save recorder(%s) failed: %v", key, err)
			continue
		}

		blog.Infof("recorder: success to save recorder(%s)", key)
	}
}

// NewRecorder get a new Recorder
func NewRecorder(path string) (*Recorder, error) {
	rr := &Recorder{path: path}
	if err := rr.parseFromFile(); err != nil {
		return nil, err
	}

	return rr, nil
}

// Recorder 用于记录work中命令的历史表现, 以支持相应的分发或执行决策
// 数据可以保存在文件中, 并支持加载已存在的文件
type Recorder struct {
	Records []*Record `json:"records" yaml:"records"`

	path     string
	dictLock sync.RWMutex
	dict     map[string]*Record
}

func (rr *Recorder) parseFromFile() error {
	if !filepath.IsAbs(rr.path) {
		return fmt.Errorf("recorder: parse from file failed: %s is not an abs path", rr.path)
	}

	f, err := os.OpenFile(rr.path, os.O_RDWR, os.ModePerm)
	if os.IsNotExist(err) {
		f, err = os.OpenFile(rr.path, os.O_RDWR|os.O_CREATE|os.O_TRUNC, os.ModePerm)
	}
	if err != nil {
		blog.Errorf("recorder: open file(%s) failed: %v", rr.path, err)
		return err
	}

	defer func() {
		_ = f.Close()
	}()

	if err = yaml.NewDecoder(f).Decode(rr); err != nil {
		blog.Warnf("recorder: parse file(%s) to yaml failed: %v, will overwrite it", rr.path, err)
	}

	rr.initDict()
	blog.Infof("recorder: success to parse from file: %s", rr.path)

	return nil
}

func (rr *Recorder) saveToFile() error {
	if !filepath.IsAbs(rr.path) {
		return fmt.Errorf("recorder: save to file failed: %s is not an abs path", rr.path)
	}

	f, err := os.OpenFile(rr.path, os.O_RDWR|os.O_CREATE, os.ModePerm)
	if err != nil {
		return err
	}

	defer func() {
		_ = f.Close()
	}()

	_ = f.Truncate(0)
	_, _ = f.Seek(0, 0)

	rr.saveDict()
	if err = yaml.NewEncoder(f).Encode(*rr); err != nil {
		return err
	}

	blog.Infof("recorder: success to save to file %s", rr.path)
	return nil
}

func (rr *Recorder) initDict() {
	rr.dictLock.Lock()
	defer rr.dictLock.Unlock()

	if rr.dict != nil {
		return
	}

	rr.dict = make(map[string]*Record)
	for _, item := range rr.Records {
		rr.dict[item.Key] = item
	}
}

func (rr *Recorder) saveDict() {
	rr.dictLock.Lock()
	defer rr.dictLock.Unlock()

	rr.Records = nil
	for _, v := range rr.dict {
		if time.Unix(v.InspectTime, 0).Add(recordExpiryTime).Before(time.Now()) {
			continue
		}
		rr.Records = append(rr.Records, v)
	}
}

// Inspect 通过key来定位记录, 获取记录信息
// 若record不在记录里, 先不要写入, 只有等到Save的时候才写入
func (rr *Recorder) Inspect(key string) *Record {
	rr.dictLock.RLock()
	t, ok := rr.dict[key]
	rr.dictLock.RUnlock()

	if !ok {
		rr.dictLock.Lock()
		if t, ok = rr.dict[key]; !ok {
			t = &Record{
				Key: key,
			}
		}
		rr.dictLock.Unlock()
	}

	t.InspectTime = time.Now().Local().Unix()
	t.recorder = rr

	r := &Record{}
	*r = *t
	return r
}

// Save 保存recorder数据到文件中
func (rr *Recorder) Save() error {
	return rr.saveToFile()
}

// Record 记录单条job的历史记录
type Record struct {
	recorder *Recorder

	Ignore bool   `json:"ignore" yaml:"ignore"`
	Key    string `json:"key" yaml:"key"`
	Name   string `json:"name" yaml:"name"`

	RetryAndSuccess int   `json:"retry_and_success" yaml:"retry_and_success"`
	SuggestTimeout  int   `json:"suggest_timeout" yaml:"suggest_timeout"`
	UpdateTime      int64 `json:"update_time" yaml:"update_time"`
	InspectTime     int64 `json:"inspect_time" yaml:"inspect_time"`
}

// Save 保存当前的job记录到缓存中
func (r *Record) Save() error {
	if r.recorder == nil {
		return fmt.Errorf("recorder: save record failed, recoder not specific")
	}

	if r.SuggestTimeout == 0 && r.RetryAndSuccess == 0 {
		return r.Clean()
	}

	r.UpdateTime = time.Now().Local().Unix()
	t := &Record{}
	*t = *r
	r.recorder.dictLock.Lock()
	r.recorder.dict[r.Key] = t
	r.recorder.dictLock.Unlock()

	return nil
}

// Quit 将record数据从recorder中清除
func (r *Record) Clean() error {
	if r.recorder == nil {
		return fmt.Errorf("recorder: clean record failed, recoder not specific")
	}

	r.recorder.dictLock.Lock()
	if _, ok := r.recorder.dict[r.Key]; ok {
		delete(r.recorder.dict, r.Key)
	}
	r.recorder.dictLock.Unlock()

	return nil
}

// RecordKey 提供一种生成record key的办法, 计算commands的md5
func RecordKey(command []string) string {
	hash := md5.New()
	_, _ = hash.Write([]byte(strings.Join(command, " ")))
	return fmt.Sprintf("%x", hash.Sum(nil))
}
