/*
 * Copyright (c) 2021 THL A29 Limited, a Tencent company. All rights reserved
 *
 * This source code file is licensed under the MIT License, you may obtain a copy of the License at
 *
 * http://opensource.org/licenses/MIT
 *
 */

package cache

import (
	"crypto/md5"
	"fmt"
	"io/ioutil"
	"os"
	"path/filepath"
	"regexp"
	"strconv"
	"strings"
	"sync"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/codec"
)

// Description describe the file cache request
// it is as the file cache KEY
type Description struct {
	Name       string `json:"name"`
	Size       string `json:"size"`
	MD5        string `json:"md5"`
	ChangeTime int64  `json:"change_time"`
}

// Manager describe the manager of file cache
type Manager interface {
	Search(Description) (File, error)
	Store(File) error
}

// NewManager get a new file cache manager
func NewManager(c ManagerConfig) (Manager, error) {
	t, err := newTree(c.CacheDir, c.PoolSize, c.CacheMinSize)
	if err != nil {
		blog.Errorf("cache: NewManager with config(%v) failed: %v", c, err)
		return nil, err
	}

	return &manager{
		conf: c,
		tree: t,
	}, nil
}

// ManagerConfig describe the file cache manager config
type ManagerConfig struct {
	CacheDir     string `json:"cache_dir"`
	PoolSize     int    `json:"pool_size"`
	CacheMinSize int64  `json:"cache_min_size"`
}

type manager struct {
	conf ManagerConfig

	tree *tree
}

// Search check if the given description is point to a existing file cache
func (m *manager) Search(description Description) (File, error) {
	return m.tree.search(description)
}

// Store put a file to cache
func (m *manager) Store(f File) error {
	return m.tree.store(f)
}

func newTree(rootDir string, poolSize int, cacheMinSize int64) (*tree, error) {
	if poolSize <= 0 {
		poolSize = defaultPoolSize
	}

	if cacheMinSize <= 0 {
		cacheMinSize = defaultFileMinSize
	}

	t := &tree{rootDir: rootDir, poolSize: poolSize, cachedMinSize: cacheMinSize, tl: newTargetLock()}
	if err := t.init(); err != nil {
		blog.Error("cache: newTree with rootDir(%s) poolSize(%d) and init failed: %v", rootDir, poolSize, err)
		return nil, err
	}

	return t, nil
}

const (
	treeRecordFile = ".record.json"
	poolNamePrefix = "pool."

	defaultPoolSize          = 100
	defaultFileMinSize int64 = 5 * 1024 * 1024
)

var (
	ErrCacheNoFound    = fmt.Errorf("cache no found")
	ErrFileNoNeedStore = fmt.Errorf("file no need store")
)

type tree struct {
	rootDir  string
	poolSize int
	oldSize  int

	cachedMinSize int64

	tl *targetLock
}

type treeRecord struct {
	PoolSize int `json:"pool_size"`
}

func (t *tree) init() error {
	if err := os.MkdirAll(t.rootDir, os.ModePerm); err != nil {
		blog.Errorf("cache: init tree ensure cache dir(%s) failed: %v", t.rootDir, err)
		return err
	}

	fn := filepath.Join(t.rootDir, treeRecordFile)
	f, err := os.OpenFile(fn, os.O_RDWR, os.ModePerm)
	if os.IsNotExist(err) {
		f, err = os.OpenFile(fn, os.O_RDWR|os.O_CREATE|os.O_TRUNC, os.ModePerm)
	}

	if err != nil {
		blog.Errorf("cache: init tree and open record(%s) failed: %v", fn, err)
		return err
	}

	var r treeRecord
	if err = codec.DecJSONReader(f, &r); err != nil {
		blog.Warnf("cache: init tree and decode json from record(%s) failed: %v", fn, err)
		t.oldSize = defaultPoolSize
	} else {
		t.oldSize = r.PoolSize
	}
	blog.Infof("cache: init tree and get old-size: %d", t.oldSize)

	if err = t.migrate(); err != nil {
		blog.Errorf("cache: init tree and migrate in dir(%s) failed: %v", t.rootDir, err)
		return err
	}

	r.PoolSize = t.poolSize
	_ = f.Truncate(0)
	_, _ = f.Seek(0, 0)
	if err = codec.EncJSONWriter(r, f); err != nil {
		blog.Warnf("cache: init tree and encode json to record(%s) with %v failed: %v", fn, r, err)
	}

	_ = f.Close()

	return nil
}

func (t *tree) migrate() error {
	blog.Infof("cache: migrate cache dir %s", t.rootDir)
	fl, err := ioutil.ReadDir(t.rootDir)
	if err != nil {
		blog.Errorf("cache: migrate tree and read dir(%s) failed: %v", t.rootDir, err)
		return err
	}

	blog.Infof("cache: going to make pool dir")
	for i := 0; i < t.poolSize; i++ {
		fd := filepath.Join(t.rootDir, poolNamePrefix+strconv.Itoa(i))
		if err = os.MkdirAll(fd, os.ModePerm); err != nil {
			blog.Errorf("cache: migrate tree make pool dir(%s) failed: %v", fd, err)
			return err
		}
	}

	for _, fi := range fl {
		if !strings.HasPrefix(fi.Name(), poolNamePrefix) {
			continue
		}

		index, err := strconv.Atoi(strings.TrimPrefix(fi.Name(), poolNamePrefix))
		if err != nil {
			blog.Infof("cache: migrate tree and found %s is not a normal set, ignore it", fi.Name())
			continue
		}

		if t.oldSize != t.poolSize {
			if err = t.migrateDir(index, filepath.Join(t.rootDir, fi.Name())); err != nil {
				blog.Errorf("cache: migrate tree and migrate dir(%s) failed: %v", fi.Name(), err)
				return err
			}
		}

		if index >= t.poolSize {
			if err = os.RemoveAll(filepath.Join(t.rootDir, fi.Name())); err != nil {
				blog.Warnf("cache: migrate tree and delete dir(%s) failed: %v", fi.Name(), err)
			}
		}
	}

	blog.Infof("cache: migrate tree with pool-size(%d) in dir(%s) success and done", t.poolSize, t.rootDir)
	return nil
}

func (t *tree) migrateDir(poolNum int, dir string) error {
	blog.Infof("cache: going to migrate dir %s", dir)
	fl, err := ioutil.ReadDir(dir)
	if err != nil {
		blog.Errorf("cache: migrate dir(%s) and read dir failed: %v", dir, err)
		return err
	}

	for _, fi := range fl {
		d := checkAndGetFileDescription(fi.Name())
		if d == nil {
			blog.Infof("cache: migrate dir(%s) and found unexpected file: %s", dir, fi.Name())
			continue
		}

		h, err := newHash(t.rootDir, t.poolSize, *d)
		if err != nil {
			blog.Warnf("cache: migrate dir(%s) and calculate cache hash for file(%s) md5(%s) failed: %v",
				dir, d.Name, d.MD5, err)
			continue
		}

		if h.poolNum != poolNum {
			f, err := newFile(filepath.Join(dir, fi.Name()))
			if err != nil {
				blog.Warnf("cache: migrate dir(%s) and open file(%s) failed: %v", dir, fi.Name(), err)
				continue
			}

			target := filepath.Join(t.rootDir, poolNamePrefix+strconv.Itoa(h.poolNum), fi.Name())
			if err = f.SaveTo(target); err != nil {
				blog.Warnf("cache: migrate dir(%s) and save file(%s) to new place(%s) failed: %v", dir, fi.Name(), target, err)
				continue
			}

			blog.Infof("cache: success to migrate dir(%s) and save file(%s) to new place(%s)", dir, fi.Name(), target)
			if err = os.Remove(filepath.Join(dir, fi.Name())); err != nil {
				blog.Warnf("cache: migrate dir(%s) and delete file(%s) failed: %v", dir, fi.Name(), err)
				continue
			}
		}
	}

	blog.Infof("cache: success to migrate dir %s", dir)
	return nil
}

func (t *tree) search(description Description) (File, error) {
	h, err := newHash(t.rootDir, t.poolSize, description)
	if err != nil {
		blog.Errorf("cache: try search cache and calculate hash with description(%v) failed: %v", description, err)
		return nil, err
	}

	target := h.target()
	t.tl.rLock(target)
	defer t.tl.rUnlock(target)

	f, err := h.inspect()
	if os.IsNotExist(err) {
		blog.Infof("cache: try search cache and inspect no found with description(%v)", description)
		return nil, ErrCacheNoFound
	}
	if err != nil {
		blog.Errorf("cache: try search cache and inspect file with description(%v) failed: %v", description, err)
		return nil, err
	}

	if f.MD5() != description.MD5 {
		blog.Errorf("cache: try search cache and found target has diff md5(%s) with description(%v)", f.MD5(), description)
		return nil, fmt.Errorf("target md5 %s not same as request %s", f.MD5(), description.MD5)
	}

	blog.Infof("cache: found cache file %s", f.AbsPath())
	return f, nil
}

func (t *tree) store(f File) error {
	if f == nil {
		return fmt.Errorf("file not specific")
	}

	if f.Size() < t.cachedMinSize {
		return ErrFileNoNeedStore
	}

	h, err := newHash(
		t.rootDir,
		t.poolSize,
		Description{
			MD5:  f.MD5(),
			Name: f.Name(),
		},
	)
	if err != nil {
		blog.Errorf("cache: try store file %s with md5 %s and get hash failed: %v", f.Name(), f.MD5(), err)
		return err
	}

	target := h.target()
	t.tl.lock(target)
	defer t.tl.unlock(target)

	// try check if target is already exist
	c, err := h.inspect()
	if err == nil && f.Equal(c) {
		blog.Infof("cache: success to store cache file %s with md5 %s to target %s, target already exist",
			f.Name(), f.MD5(), target)
		return nil
	}

	// target not exist, do save
	if err = f.SaveTo(target); err != nil {
		blog.Errorf("cache: try store file %s with md5 %s to target %s failed: %v", f.Name(), f.MD5(), target, err)
		return err
	}

	blog.Infof("cache: success to store cache file %s with md5 %s to target %s", f.Name(), f.MD5(), target)
	return nil
}

func newHash(rootDir string, poolSize int, description Description) (*hash, error) {
	if !isEncrypt(description.MD5) || description.Name == "" {
		blog.Errorf("cache: get cache hash failed, one/both of md5(%s) and file-name(%s) are invalid",
			description.MD5, description.Name)
		return nil, fmt.Errorf("invalid description with md5(%s) and file-name(%s)", description.MD5, description.Name)
	}

	num := 0
	for _, b := range []rune(description.MD5) {
		num = (num + int(b)) % poolSize
	}

	return &hash{
		rootDir: rootDir,
		poolNum: num,
		key:     description.MD5 + keyConnector + description.Name,
	}, nil
}

type hash struct {
	rootDir  string
	poolNum  int
	poolSize int
	key      string
}

func (h *hash) target() string {
	return filepath.Join(h.rootDir, h.poolDirName(), h.targetName())
}

func (h *hash) targetName() string {
	return h.key
}

func (h *hash) poolDir() string {
	return filepath.Join(h.rootDir, h.poolDirName())
}

func (h *hash) poolDirName() string {
	return poolNamePrefix + strconv.Itoa(h.poolNum)
}

func (h *hash) inspect() (File, error) {
	return newFile(h.target())
}

const (
	// byte are 8bit, but md5 sum result is hex which are 16bit, so the encrypt length of byte should * 2
	encryptionSize = md5.Size * 2
	keyConnector   = "."
)

var (
	isEncrypt = regexp.MustCompile(`^[a-z0-9]{` + strconv.Itoa(encryptionSize) + `}$`).MatchString
)

func checkAndGetFileDescription(key string) *Description {
	i := strings.Index(key, keyConnector)
	if i != encryptionSize {
		return nil
	}

	encrypt, fileName := key[:i], key[i:]
	if !isEncrypt(encrypt) {
		return nil
	}

	return &Description{
		MD5:  encrypt,
		Name: fileName,
	}
}

func newTargetLock() *targetLock {
	return &targetLock{
		pool: make(map[string]*sync.RWMutex),
	}
}

type targetLock struct {
	l sync.RWMutex

	pool map[string]*sync.RWMutex
}

func (tl *targetLock) lock(target string) {
	tl.l.Lock()
	l, ok := tl.pool[target]
	if !ok {
		l = &sync.RWMutex{}
		tl.pool[target] = l
	}
	tl.l.Unlock()

	l.Lock()
}

func (tl *targetLock) unlock(target string) {
	tl.l.Lock()
	l, ok := tl.pool[target]
	tl.l.Unlock()
	if !ok {
		return
	}

	l.Unlock()
}

func (tl *targetLock) rLock(target string) {
	tl.l.Lock()
	l, ok := tl.pool[target]
	if !ok {
		l = &sync.RWMutex{}
		tl.pool[target] = l
	}
	tl.l.Unlock()

	l.RLock()
}

func (tl *targetLock) rUnlock(target string) {
	tl.l.Lock()
	l, ok := tl.pool[target]
	tl.l.Unlock()
	if !ok {
		return
	}

	l.RUnlock()
}
