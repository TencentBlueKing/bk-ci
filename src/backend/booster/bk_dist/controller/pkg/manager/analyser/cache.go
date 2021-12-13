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
	"context"
	"os"
	"path/filepath"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"

	dcFile "github.com/Tencent/bk-ci/src/booster/bk_dist/common/file"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
)

// NewFileCache get and init an empty file cache
func NewFileCache() *FileCache {
	return &FileCache{
		relativeMap:    NewMapRelativePath(),
		directoryMap:   NewMapDirectory(),
		canonicalMap:   NewMapCanonicalPath(NewCacheCanonicalPath()),
		parseCache:     NewParseCache(),
		systemDirCache: NewCacheSystemDir(),
		symlinkCache:   NewCacheSymlink(),
		stat:           newFileStatL3(),
		absStatCache:   newFileAbsStatCache(),
		blackList:      newBlackListFile(),
	}
}

// FileCache 提供了一个一体化的依赖文件搜索缓存方案
type FileCache struct {
	relativeMap  *MapRelativePath
	directoryMap *MapDirectory
	canonicalMap *MapCanonicalPath

	parseCache     *ParseCache
	systemDirCache *CacheSystemDir
	symlinkCache   *CacheSymlink

	stat         *fileStatL3
	absStatCache *fileAbsStatCache
	blackList    *blackListFile
}

func newFileStatL3() *fileStatL3 {
	return &fileStatL3{
		stat: make(map[int]*fileStatL2),
	}
}

type fileStatL3 struct {
	sync.RWMutex

	stat map[int]*fileStatL2
}

func (fsl3 *fileStatL3) getFileStatL1(runDir, filePath *File) *fileStatL1 {
	idx := runDir.Index()

	fsl3.RLock()
	s, ok := fsl3.stat[idx]
	fsl3.RUnlock()
	if ok {
		return s.getFileStatL1(filePath)
	}

	fsl3.Lock()
	if s, ok = fsl3.stat[idx]; ok {
		fsl3.Unlock()
		return s.getFileStatL1(filePath)
	}
	s = newFileStatL2()
	fsl3.stat[idx] = s
	fsl3.Unlock()

	return s.getFileStatL1(filePath)
}

func newFileStatL2() *fileStatL2 {
	return &fileStatL2{
		stat: make(map[int]*fileStatL1),
	}
}

type fileStatL2 struct {
	sync.RWMutex

	stat map[int]*fileStatL1
}

func (fsl2 *fileStatL2) getFileStatL1(filePath *File) *fileStatL1 {
	idx := filePath.Index()

	fsl2.RLock()
	s, ok := fsl2.stat[idx]
	fsl2.RUnlock()
	if ok {
		return s
	}

	fsl2.Lock()
	if s, ok = fsl2.stat[idx]; ok {
		fsl2.Unlock()
		return s
	}
	s = newFileStatL1()
	fsl2.stat[idx] = s
	fsl2.Unlock()

	return s
}

func newFileStatL1() *fileStatL1 {
	return &fileStatL1{
		stat: make(map[int]*fileStat),
	}
}

type fileStatL1 struct {
	sync.RWMutex

	stat map[int]*fileStat
}

func (fsl1 *fileStatL1) getFileStat(searchDir *File) *fileStat {
	idx := searchDir.Index()

	fsl1.RLock()
	s, ok := fsl1.stat[idx]
	fsl1.RUnlock()
	if ok {
		return s
	}

	fsl1.Lock()
	defer fsl1.Unlock()

	if s, ok = fsl1.stat[idx]; ok {
		return s
	}
	s = &fileStat{
		searchDir: searchDir,
	}
	fsl1.stat[idx] = s
	return s
}

type fileStat struct {
	sync.RWMutex

	resolved      bool
	exist         bool
	relativePath  *File
	canonicalPath *File
	searchDir     *File
	fileDesc      *dcSDK.FileDesc
	symlinksDesc  []*dcSDK.FileDesc
}

func newFileAbsStatCache() *fileAbsStatCache {
	return &fileAbsStatCache{
		cache: make(map[int]*[]*fileStat),
	}
}

type fileAbsStatCache struct {
	sync.RWMutex

	cache map[int]*[]*fileStat
}

func (fas *fileAbsStatCache) register(stat *fileStat) {
	f := stat.relativePath
	if f == nil || f.fileType != FileTypeRelative {
		return
	}

	fas.Lock()
	defer fas.Unlock()

	c, ok := fas.cache[f.Index()]
	if !ok {
		cp := make([]*fileStat, 0, 10)
		c = &cp
		fas.cache[f.Index()] = c
	}

	*c = append(*c, stat)
}

func (fas *fileAbsStatCache) wakeup(f *File) {
	if f == nil || f.fileType != FileTypeRelative {
		return
	}

	fas.RLock()
	_, ok := fas.cache[f.Index()]
	fas.RUnlock()
	if !ok {
		return
	}

	fas.Lock()
	defer fas.Unlock()
	c := fas.cache[f.Index()]
	for _, s := range *c {
		s.Lock()
		s.resolved = false
		s.Unlock()
	}
	delete(fas.cache, f.Index())
}

// FileResolveParam 作为提供给FileCache.Resolve的参数
type FileResolveParam struct {
	filePath      *File
	runDir        *File
	searchDir     *File
	searchDirList []*File
}

// FileSolveResult 作为FileCache.Resolve返回的数据
type FileSolveResult struct {
	hitCache      bool
	searchDir     *File
	filePath      *File
	canonicalPath *File
	relativePath  *File
	fileDesc      *dcSDK.FileDesc
	symlinksDesc  []*dcSDK.FileDesc
}

type resolveSkipFunc func(*File) bool

// Resolve 接收给定的搜索参数, 在缓存中直接获取, 或按优先级搜索目标文件是否存在
// 入参为:
// 1. filePath, 需要搜索的文件名, 如#include<foo/bar.h>中的foo/bar.h, 或-c /data/src/hello.c里的hello.c
//  必须为相对路径, 若为绝对路径则无需搜索, 并默认前把绝对路径拆分为searchDir和filePath, 同时清空searchDirList
// 2. runDir, 当前执行目录, 用于标定绝对路径, 是所有相对路径的默认前缀
// 3. searchDir, 优先搜索目录, 应该被置于searchDirList的最前面
// 4. searchDirList, 搜索目录, 按优先级排序, 应依次搜索这些目录下是否存在指定的目标文件
// 结果为：
// 1. searchDir, 找到目标文件的searchDir
// 2. filePath, 目标文件
// 3. canonicalPath, 目标文件真实路径
func (fc *FileCache) Resolve(param *FileResolveParam, skipper resolveSkipFunc) (*FileSolveResult, error) {
	if param == nil {
		return nil, ErrAnalyserInvalidParam
	}

	tag := util.RandomString(5)
	blog.Debugf("analyser: [%s] resolve get param filePath(%s), runDir(%s), searchDir(%s), searchDirList(%v)",
		tag, param.filePath, param.runDir, param.searchDir, param.searchDirList)

	// 若filePath为绝对路径目标, 则目标已标定, 默认将searchDir置为绝对路径的dir, fileName置为绝对路径的file, searchDirList置空
	if absPath := param.filePath.String(); filepath.IsAbs(absPath) {
		dir, name := filepath.Split(absPath)
		param.filePath = fc.GetRelativeFile(name)
		param.searchDir = fc.GetDirectoryFile(dir)
		param.searchDirList = nil
	}

	// 一级cache为runDir, 目的在于区分不同的执行/搜索目录
	// 二级cache为filePath, 相对路径, 目的在于区分不同的目标文件
	statL1 := fc.stat.getFileStatL1(param.runDir, param.filePath)

	// 收集在此轮进入resolved的stat, 若最后没有找到该文件, 说明这可能是一次无效的查找, 应该记录这些文件的stat, 等待唤醒
	resolvedList := make([]*fileStat, 0, 10)

	for _, sd := range append([]*File{param.searchDir}, param.searchDirList...) {
		if sd == nil || sd.Index() < 0 {
			continue
		}
		blog.Debugf("analyser: [%s] resolve search dir: %s", tag, sd)

		hit := true
		stat := statL1.getFileStat(sd)
		stat.Lock()
		if !stat.resolved {
			blog.Debugf("analyser: [%s] resolve search try add target, searchDir: %s, %d, filePath: %s, %d",
				tag, sd, sd.Index(), param.filePath, param.filePath.Index())

			hit = false
			path := filepath.Join(sd.String(), param.filePath.String())
			if !filepath.IsAbs(path) {
				path = filepath.Join(param.runDir.String(), path)
			}

			// 记录
			stat.resolved = true
			stat.relativePath = fc.GetRelativeFile(path)
			resolvedList = append(resolvedList, stat)

			if s := dcFile.Stat(path); s.Exist() && !s.Basic().IsDir() {
				blog.Debugf("analyser: [%s] resolve success %s, cache dir(%d), file(%d)",
					tag, path, sd.Index(), param.filePath.Index())

				stat.exist = true
				stat.canonicalPath = fc.GetCanonicalFile(path)
				go fc.absStatCache.wakeup(stat.relativePath)

				if skipper != nil && !skipper(stat.canonicalPath) {
					stat.fileDesc = getFileDesc(s)
					stat.symlinksDesc = fc.symlinkCache.GetLinks(fc.GetDirectoryFile(stat.relativePath.String()))

					// sending file path should be canonical
					stat.fileDesc.FilePath = stat.canonicalPath.String()
					stat.fileDesc.Targetrelativepath = filepath.Dir(stat.canonicalPath.String())
				}
			}
		}
		stat.Unlock()

		if stat.exist {
			return &FileSolveResult{
				hitCache:      hit,
				searchDir:     sd,
				filePath:      param.filePath,
				canonicalPath: stat.canonicalPath,
				relativePath:  stat.relativePath,
				fileDesc:      stat.fileDesc,
				symlinksDesc:  stat.symlinksDesc,
			}, nil
		}
	}

	// 重制所有resolved的stat, 此轮查找注册到待唤醒cache中
	for _, stat := range resolvedList {
		go fc.absStatCache.register(stat)
	}

	return nil, ErrAnalyserFileNotFound
}

// EmptyDirectory 提供一个空Directory File对象
func (fc *FileCache) EmptyDirectory() *File {
	return fc.GetDirectoryFile("")
}

// EmptyRelativeFile 提供一个空Relative File对象
func (fc *FileCache) EmptyRelativeFile() *File {
	return fc.GetRelativeFile("")
}

// EmptyCanonicalFile 提供一个空Canonical File对象
func (fc *FileCache) EmptyCanonicalFile() *File {
	return fc.GetCanonicalFile("/")
}

// GetDirectoryFile 提供一个由给定path生成的directory类型的File对象
func (fc *FileCache) GetDirectoryFile(path string) *File {
	return NewFileWithCache(fc, FileTypeDir, path)
}

// GetRelativeFile 提供一个由给定path生成的relative类型的File对象
func (fc *FileCache) GetRelativeFile(path string) *File {
	return NewFileWithCache(fc, FileTypeRelative, path)
}

// GetCanonicalFile 提供一个由给定path生成的canonical类型的File对象
func (fc *FileCache) GetCanonicalFile(path string) *File {
	return NewFileWithCache(fc, FileTypeCanonical, path)
}

// GetDirectoryFileList 提供一个由给定path-list生成的directory类型的File对象组成的列表
func (fc *FileCache) GetDirectoryFileList(s []string) []*File {
	r := make([]*File, 0, len(s))
	for _, item := range s {
		r = append(r, fc.GetDirectoryFile(item))
	}
	return r
}

// GetRelativeFileList 提供一个由给定path-list生成的relative类型的File对象组成的列表
func (fc *FileCache) GetRelativeFileList(s []string) []*File {
	r := make([]*File, 0, len(s))
	for _, item := range s {
		r = append(r, fc.GetRelativeFile(item))
	}
	return r
}

// GetCanonicalFileList 提供一个由给定path-list生成的canonical类型的File对象组成的列表
func (fc *FileCache) GetCanonicalFileList(s []string) []*File {
	r := make([]*File, 0, len(s))
	for _, item := range s {
		r = append(r, fc.GetCanonicalFile(item))
	}
	return r
}

// GetFileParse 提供一个缓存, 用于查询文件正则解析的缓存
func (fc *FileCache) GetFileParse(f *File) *FileResult {
	return fc.parseCache.GetFileParse(f)
}

// PutFileParse 提供一个缓存, 用于插入文件正则解析的缓存
func (fc *FileCache) PutFileParse(f *File, r *FileResult) {
	fc.parseCache.PutFileParse(f, r)
}

// GetSystemDir 提供一个缓存, 用于查询给定参数下的系统头文件目录
func (fc *FileCache) GetSystemDir(compiler, sysRoot, language string) ([]string, error) {
	return fc.systemDirCache.GetSystemDir(compiler, sysRoot, language)
}

// StartWithSystemDir 提供一个缓存, 用于查询给定的参数下, 该文件是否在系统头文件目录里
func (fc *FileCache) StartWithSystemDir(compiler, sysRoot, language string, f *File) (bool, error) {
	return fc.systemDirCache.StartWithSystemDir(compiler, sysRoot, language, f)
}

// SetBlackList 将一个Canonical文件放进全局黑名单中
func (fc *FileCache) SetBlackList(f *File) {
	fc.blackList.set(f)
}

// CheckBlackList 测试一个Canonical文件是否在黑名单中, 若不是Canonical文件则返回false
func (fc *FileCache) CheckBlackList(f *File) bool {
	return fc.blackList.check(f)
}

// NewCacheCanonicalPath 提供一个全新的CacheCanonicalPath对象
func NewCacheCanonicalPath() *CacheCanonicalPath {
	return &CacheCanonicalPath{
		cache: make(map[string]string),
	}
}

// CacheCanonicalPath 提供了一个根据给定路径来获取其真实绝对路径的办法.
// 同时内部包含了一个简易的缓存, 用来减少对filepath库的调用
type CacheCanonicalPath struct {
	sync.RWMutex

	cache map[string]string
}

// GetCanonicalPath 根据给定路径, 若真实存在, 则返回目标的真实绝对路径.
// 所谓真实绝对路径, 首先是一个绝对路径, 并且该路径所包含的所有软链都会被解析到目标.
func (ccp *CacheCanonicalPath) GetCanonicalPath(path string) (string, error) {
	ccp.RLock()
	v, ok := ccp.cache[path]
	ccp.RUnlock()
	if ok {
		return v, nil
	}

	cPath, err := filepath.EvalSymlinks(path)
	if err != nil {
		return "", err
	}

	cPath, err = filepath.Abs(cPath)
	if err != nil {
		return "", err
	}

	ccp.Lock()
	ccp.cache[path] = cPath
	ccp.Unlock()
	return cPath, nil
}

// NewMap2Index 提供一个全新的Map2Index对象
func NewMap2Index() *Map2Index {
	return &Map2Index{
		index: make(map[string]int),
	}
}

// Map2Index 提供一个简易的str到int的一一对应的关系, 通过对str按插入顺序排序, 用其在队列中的位置作为索引, 同时保存在map中方便快速获取.
type Map2Index struct {
	sync.RWMutex

	index  map[string]int
	string []string
}

// Index 获取给定str在该关系中的索引, 若不存在则插入
func (mi *Map2Index) Index(path string) int {
	mi.RLock()
	v, ok := mi.index[path]
	mi.RUnlock()

	if ok {
		return v
	}

	mi.Lock()
	idx := len(mi.string)
	mi.index[path] = idx
	mi.string = append(mi.string, path)
	mi.Unlock()
	return idx
}

// String 获取给定索引对应的str, 若不存在则返回空字符串
func (mi *Map2Index) String(idx int) string {
	mi.RLock()
	defer mi.RUnlock()

	if idx < 0 || idx >= len(mi.string) {
		return ""
	}

	return mi.string[idx]
}

// Length 获取该关系中的队列长度
func (mi *Map2Index) Length() int {
	mi.RLock()
	defer mi.RUnlock()

	return len(mi.string)
}

// NewMapDirectory 获取一个MapDirectory关系
func NewMapDirectory() *MapDirectory {
	return &MapDirectory{
		Map2Index: *NewMap2Index(),
	}
}

// MapDirectory 通过组合Map2Index, 一般用于处理目录的关系索引.
// 其中, 保证目录str的末尾不会有后置的/
type MapDirectory struct {
	Map2Index
}

// Index 通过Map2Index获取关系索引, 在查询/插入前会去除str中所有的后置/
func (md *MapDirectory) Index(directory string) int {
	if directory != "" {
		directory = strings.TrimRight(directory, "/") + "/"
	}

	for strings.HasPrefix(directory, "./") {
		directory = directory[2:]
	}

	return md.Map2Index.Index(directory)
}

// NewMapRelativePath 提供一个全新的MapRelativePath关系
func NewMapRelativePath() *MapRelativePath {
	return &MapRelativePath{
		Map2Index: *NewMap2Index(),
	}
}

// MapRelativePath 通过组合Map2Index, 一般用于处理相对路径(也可以为绝对路径)的关系索引.
// 其中, 保证路径str的开头不会有前置的./
type MapRelativePath struct {
	Map2Index
}

// Index 通过Map2Index获取关系索引, 在查询/插入之前会去除str中所有的前置./
func (mrp *MapRelativePath) Index(relativePath string) int {
	for strings.HasPrefix(relativePath, "./") {
		relativePath = relativePath[2:]
	}

	return mrp.Map2Index.Index(relativePath)
}

// NewMapCanonicalPath 提供一个全新的MapCanonicalPath关系
func NewMapCanonicalPath(canonical *CacheCanonicalPath) *MapCanonicalPath {
	return &MapCanonicalPath{
		Map2Index: *NewMap2Index(),
		canonical: canonical,
	}
}

// MapCanonicalPath 通过组合Map2Index, 一般用于处理绝对路径的关系索引.
// 其中, 保证路径str中不会含有任何软链接
type MapCanonicalPath struct {
	Map2Index

	canonical *CacheCanonicalPath
}

// Index 通过Map2Index获取关系索引, 在查询/插入之前会将str中的所有软链接展开, 并取绝对路径
func (mcp *MapCanonicalPath) Index(path string) int {
	p, err := mcp.canonical.GetCanonicalPath(path)
	if err != nil {
		return -1
	}

	return mcp.Map2Index.Index(p)
}

type cacheDirValue struct {
	dirIndex         int
	dirRealPathIndex int
}

// CacheDirName 提供查询目录索引, 和目录绝对路径索引的办法和缓存
// 包含了relativeMap, directoryMap和canonicalMap, 用于在查询时做必要的查询/插入操作
type CacheDirName struct {
	cache map[int]map[int]map[int]*cacheDirValue

	relativeMap  *MapRelativePath
	directoryMap *MapDirectory
	canonicalMap *MapCanonicalPath
}

// LookUp 提供了一个方法, 给定: 当前执行目录 + 搜索目录 + 目标路径, 获得该文件所在的目录(相对于当前执行目录)索引, 和目录绝对路径的索引
// * 该文件所在的目录为: 搜索目录 + 目标目录, 从directoryMap中拿到索引
// * 该文件所在的目录绝对路径为: 当前执行目录 + 该文件所在目录, 从canonicalMap中拿到索引
// 同时提供了一个简易的三级缓存, 用于缓存对相同参数的查询结果
func (cdn *CacheDirName) LookUp(currentDirIdx, searchDirIdx, includePathIdx int) (dirIdx, dirRealPathIdx int) {
	cacheD1, ok := cdn.cache[currentDirIdx]
	if !ok {
		cacheD1 = make(map[int]map[int]*cacheDirValue)
		cdn.cache[currentDirIdx] = cacheD1
	}

	cacheD2, ok := cacheD1[searchDirIdx]
	if !ok {
		cacheD2 = make(map[int]*cacheDirValue)
		cacheD1[searchDirIdx] = cacheD2
	}

	if v, ok := cacheD2[includePathIdx]; ok && v != nil {
		return v.dirIndex, v.dirRealPathIndex
	}

	directory := filepath.Dir(
		filepath.Join(cdn.directoryMap.String(searchDirIdx), cdn.relativeMap.String(includePathIdx)))
	dirIdx = cdn.directoryMap.Index(directory)

	realDirectory := directory
	if !filepath.IsAbs(realDirectory) {
		realDirectory = filepath.Join(cdn.directoryMap.String(currentDirIdx), directory)
	}
	dirRealPathIdx = cdn.canonicalMap.Index(realDirectory)

	cacheD2[includePathIdx] = &cacheDirValue{dirIndex: dirIdx, dirRealPathIndex: dirRealPathIdx}
	return dirIdx, dirRealPathIdx
}

// NewRootCache get an empty root cache
func NewRootCache() *RootCache {
	return &RootCache{
		cache: make(map[string]*RootCacheValueList),
	}
}

// RootCache 提供了一个搜索树节点缓存方案, 整体为一个二级缓存
// - 第一级缓存key为: runDir, quoteDirList, angleDirList
// - 第二级缓存key为: searchDir, filePath, lastFileDir, nodeType
// 其中第二级缓存由NodeCache提供
type RootCache struct {
	sync.RWMutex

	nodes int64

	cache map[string]*RootCacheValueList
}

func (r *RootCache) newNode() int64 {
	return atomic.AddInt64(&r.nodes, 1)
}

// MatchOrInsert 接收一级缓存, 根据给定的runDir, quoteDirList, angleDirList
// 匹配或插入一条一级缓存, 并把该缓存对应的NodeCache返回给调用者, 方便其发起对二级缓存的查询
func (r *RootCache) MatchOrInsert(runDir *File, quoteDirList, angleDirList []*File) *NodeCache {
	key := r.generateKey(runDir, quoteDirList, angleDirList)
	r.Lock()
	cache, ok := r.cache[key]
	if !ok {
		cache = &RootCacheValueList{root: r}
		r.cache[key] = cache
	}
	r.Unlock()

	return cache.MatchOrInsert(runDir, quoteDirList, angleDirList)
}

func (r *RootCache) generateKey(runDir *File, quoteDirList, angleDirList []*File) string {
	key := strconv.Itoa(runDir.Index())

	if l := len(quoteDirList); l > 0 {
		key += strconv.Itoa(quoteDirList[0].Index()) + strconv.Itoa(quoteDirList[l-1].Index())
	}

	if l := len(angleDirList); l > 0 {
		key += strconv.Itoa(angleDirList[0].Index()) + strconv.Itoa(angleDirList[l-1].Index())
	}

	return key
}

// RootCacheValueList 是RootCache中一级缓存的冲突队列, 当多个一级缓存的key相同时, 放在同一个RootCacheValueList里
type RootCacheValueList struct {
	sync.RWMutex

	root *RootCache

	head   *RootCacheValue
	tail   *RootCacheValue
	length int
}

// MatchOrInsert 在冲突队列RootCacheValueList中查找完全匹配的缓存, 并返回
func (rvl *RootCacheValueList) MatchOrInsert(runDir *File, quoteDirList, angleDirList []*File) *NodeCache {
	rvl.Lock()
	defer rvl.Unlock()

	head := rvl.head

	for head != nil {
		if head.Match(runDir, quoteDirList, angleDirList) {
			return head.node
		}

		head = head.next
	}

	node := NewNodeCache(rvl.root)
	value := &RootCacheValue{
		root:         rvl.root,
		runDir:       runDir,
		quoteDirList: quoteDirList,
		angleDirList: angleDirList,
		node:         node,
	}

	if rvl.tail != nil {
		rvl.tail.next = value
	}

	if rvl.head == nil {
		rvl.head = value
		rvl.tail = value
	}
	rvl.length++

	return node
}

// RootCacheValue 是RootCache的一级缓存对象, 存放了一些匹配用的字段, 以及缓存对象NodeCache
type RootCacheValue struct {
	root *RootCache

	runDir *File

	quoteDirList []*File
	angleDirList []*File

	node *NodeCache

	next *RootCacheValue
}

// Match 用于校验该一级缓存是否与查询完全匹配
func (rv *RootCacheValue) Match(runDir *File, quoteDirList, angleDirList []*File) bool {
	if !rv.runDir.Equal(runDir) {
		return false
	}

	if len(quoteDirList) != len(rv.quoteDirList) || len(angleDirList) != len(rv.angleDirList) {
		return false
	}

	for i, item := range quoteDirList {
		if !rv.quoteDirList[i].Equal(item) {
			return false
		}
	}

	for i, item := range angleDirList {
		if !rv.angleDirList[i].Equal(item) {
			return false
		}
	}

	return true
}

// NewNodeCache provide an empty NodeCache
func NewNodeCache(root *RootCache) *NodeCache {
	return &NodeCache{
		root:  root,
		cache: make(map[string]*NodeCacheValueList),
	}
}

// NodeCache 提供了RootCache下的二级缓存, 缓存了搜索节点，提供一级缓存前提下的继续匹配
type NodeCache struct {
	sync.RWMutex

	root  *RootCache
	cache map[string]*NodeCacheValueList
}

// MatchOrInsert 接收一级缓存, 根据给定的searchDir, filePath, lastFileDir, nodeType
// 匹配或插入一条二级缓存, 并把该缓存对应的最终结果返回给调用者
func (n *NodeCache) MatchOrInsert(searchDir, filePath, lastFileDir *File, nodeType NodeType) (*NodeCacheValue, bool) {
	key := n.generateKey(searchDir, filePath, lastFileDir, nodeType)
	n.Lock()
	cache, ok := n.cache[key]
	if !ok {
		cache = &NodeCacheValueList{root: n.root}
		n.cache[key] = cache
	}
	n.Unlock()

	return cache.MatchOrInsert(searchDir, filePath, lastFileDir, nodeType)
}

func (n *NodeCache) generateKey(searchDir, filePath, lastFileDir *File, nodeType NodeType) string {
	return strconv.Itoa(searchDir.Index()) + "_" +
		strconv.Itoa(filePath.Index()) + "_" +
		strconv.Itoa(lastFileDir.Index()) + "_" +
		strconv.Itoa(nodeType.Int())
}

// NodeCacheValueList 提供了NodeCacheValue的冲突队列
type NodeCacheValueList struct {
	sync.RWMutex

	root   *RootCache
	head   *NodeCacheValue
	tail   *NodeCacheValue
	length int
}

// MatchOrInsert 在冲突队列NodeCacheValueList中查找完全匹配的缓存, 并返回
func (nvl *NodeCacheValueList) MatchOrInsert(
	searchDir, filePath, lastFileDir *File,
	nodeType NodeType) (*NodeCacheValue, bool) {
	nvl.Lock()
	defer nvl.Unlock()

	head := nvl.head

	for head != nil {
		if head.Match(searchDir, filePath, lastFileDir, nodeType) {
			return head, true
		}

		head = head.next
	}

	ctx, cancel := context.WithCancel(context.Background())

	node := &NodeCacheValue{
		root:          nvl.root,
		visitor:       nvl.root.newNode(),
		lastFileDir:   lastFileDir,
		searchDir:     searchDir,
		filePath:      filePath,
		canonicalPath: nil,
		nodeType:      nodeType,
		children:      make([]*NodeCacheValue, 0, 20),
		valid:         false,
		validCtx:      ctx,
		validCancel:   cancel,
	}

	if nvl.tail != nil {
		nvl.tail.next = node
	}

	if nvl.head == nil {
		nvl.head = node
		nvl.tail = node
	}
	nvl.length++

	return node, false
}

// NodeCacheValue 主要处理对搜索节点的缓存, 在保证正确性的情况下尽量减少搜索分支
// cache的key是 searchDir+filePath, lastFileDir, nodeType
// * 当nodeType为NodeTypeResolved时, searchDir+filePath为找到的文件的位置, lastFileDir为空
// * 当nodeType为NodeTypeQuote时, searchDir为空, 已知的只有filePath, lastFileDir为上一个文件的所在目录
// * 当nodeType为NodeTypeAngle时, searchDir为空, 已知的只有filePath, lastFileDir为空
type NodeCacheValue struct {
	sync.RWMutex

	root    *RootCache
	visitor int64

	// 上一个节点文件的目录, 用于在搜索quote引用时, 可以在本目录搜索
	lastFileDir *File

	// 文件被找到的搜索目录, foo/bar/hello.h有多种情况:
	// * searchDir为foo/, includePath为"bar/hello.h"
	// * searchDir为foo/bar/, includePath为"hello.h"
	searchDir *File

	// 文件被include引用的路径, 或在命令行中引用的路径
	filePath *File

	// 文件的真实绝对路径
	canonicalPath *File

	// 文件的非真实绝对路径
	relativePath *File

	// 节点的文件描述, 以及所处路径上的所有symlink描述
	fileDesc     *dcSDK.FileDesc
	symlinksDesc []*dcSDK.FileDesc

	// node的类型, 即进入node的状态
	nodeType NodeType

	// children是该节点的所有儿子
	children []*NodeCacheValue

	// node是否可用, 取决于以该node为根的整颗搜索树的所有节点是否都可用, 若不可用, 说明某些节点可能自上次cache后发生了变化, 或还未就绪
	valid       bool
	validCtx    context.Context
	validCancel context.CancelFunc
	disabled    bool
	inBlacklist bool

	// 在缓存结构链表中的关联信息, 与节点树本身结构无关
	next *NodeCacheValue
}

// Match 用于校验该二级缓存是否与查询完全匹配
func (nv *NodeCacheValue) Match(searchDir, filePath, lastFileDir *File, nodeType NodeType) bool {
	if !nv.searchDir.Equal(searchDir) ||
		!nv.filePath.Equal(filePath) ||
		!nv.lastFileDir.Equal(lastFileDir) ||
		nv.nodeType != nodeType {
		return false
	}

	return true
}

// Enable 标记该搜索节点为"已处理"且有效的
// 没有被Enable的搜索节点, 只能作为拓扑节点, 而不能作为计算节点
// 并行的拓扑搜索, 能够在多任务之间, 快速构建多颗相互交叉的搜索树; 但一个任务只有等到它的所有搜索节点都被Enable之后, 才能完成最终的依赖计算
func (nv *NodeCacheValue) Enable(r *FileSolveResult, children []*NodeCacheValue) {
	nv.Lock()
	defer nv.Unlock()

	if nv.valid {
		return
	}

	if r != nil {
		nv.canonicalPath = r.canonicalPath
		nv.relativePath = r.relativePath
		nv.fileDesc = r.fileDesc
		nv.symlinksDesc = r.symlinksDesc
	}
	nv.children = children
	nv.valid = true
	nv.validCancel()
}

// Disable means set the node disabled, every one meet this node should ignore it.
func (nv *NodeCacheValue) Disable() {
	nv.Lock()
	defer nv.Unlock()

	nv.disabled = true
	nv.valid = true
	nv.validCancel()
}

// SetBlackList 将该节点标记为黑名单, 所有涉及黑名单的节点所在的搜索树, 都会被放弃
func (nv *NodeCacheValue) SetBlackList() {
	nv.Lock()
	defer nv.Unlock()

	nv.inBlacklist = true
	nv.valid = true
	nv.validCancel()
}

// IsDisabled check if node is disabled
func (nv *NodeCacheValue) IsDisabled() bool {
	return nv.disabled
}

// IsValid check if node is valid
func (nv *NodeCacheValue) IsValid() bool {
	return nv.valid
}

// InBlackList check if node is in blacklist
func (nv *NodeCacheValue) InBlackList() bool {
	return nv.inBlacklist
}

// WaitUntilValid 将会阻塞, 直到该节点valid为止
func (nv *NodeCacheValue) WaitUntilValid() {
	nv.RLock()
	if nv.valid {
		nv.RUnlock()
		return
	}

	ctx, _ := context.WithCancel(nv.validCtx)
	nv.RUnlock()

	select {
	case <-ctx.Done():
	}
}

// CanonicalPath get node's canonical path
func (nv *NodeCacheValue) CanonicalPath() *File {
	return nv.canonicalPath
}

// Children 获取该节点的所有儿子节点
func (nv *NodeCacheValue) Children() []*NodeCacheValue {
	return nv.children
}

// NewParseCache 提供了一个ParseCache实例
func NewParseCache() *ParseCache {
	return &ParseCache{
		cache: make(map[int]*FileResult),
	}
}

// ParseCache 提供文件依赖解析的缓存, 一个文件的静态依赖只与其内容有关系, 因此只需要绝对真实路径为索引即可
type ParseCache struct {
	sync.RWMutex

	cache map[int]*FileResult
}

// GetFileParse 根据提供的File的绝对真实路径, 直接给到解析结果
func (pc *ParseCache) GetFileParse(f *File) *FileResult {
	if f == nil || f.fileType != FileTypeCanonical {
		return nil
	}

	pc.RLock()
	r, _ := pc.cache[f.Index()]
	pc.RUnlock()
	return r
}

// PutFileParse 根据给定的File和解析结果, 更新缓存
func (pc *ParseCache) PutFileParse(f *File, r *FileResult) {
	if f == nil || r == nil || f.fileType != FileTypeCanonical {
		return
	}

	pc.Lock()
	defer pc.Unlock()
	pc.cache[f.Index()] = r
}

// NewCacheSystemDir get an empty CacheSystemDir
func NewCacheSystemDir() *CacheSystemDir {
	return &CacheSystemDir{
		systemDir: newSystemDirL3(),
	}
}

// CacheSystemDir 提供对系统头文件所在目录的缓存
// 用户提供compiler(编译器), sysRoot(编译指令中指定的sysRoot), language(编译指令中指定的语言)
// 则可以获取对饮过的系统头文件所在目录
type CacheSystemDir struct {
	systemDir *systemDirL3
}

// GetSystemDir 获取系统头文件所在目录列表, 按搜索优先级高到低排序
func (csd *CacheSystemDir) GetSystemDir(compiler, sysRoot, language string) ([]string, error) {
	r, err := csd.systemDir.getOrInsert(compiler, sysRoot, language)
	if err != nil {
		return nil, err
	}

	return r.systemDirs, nil
}

// StartWithSystemDir 判断一个文件f是否在系统头文件所在目录中
func (csd *CacheSystemDir) StartWithSystemDir(compiler, sysRoot, language string, f *File) (bool, error) {
	r, err := csd.systemDir.getOrInsert(compiler, sysRoot, language)
	if err != nil {
		return false, err
	}

	return r.startsWithSystemDir(f), nil
}

func newSystemDirL3() *systemDirL3 {
	return &systemDirL3{
		cache: make(map[string]*systemDirL2),
	}
}

type systemDirL3 struct {
	sync.RWMutex

	cache map[string]*systemDirL2
}

func (sdl3 *systemDirL3) getOrInsert(compiler, sysRoot, language string) (*cacheSystemDirPrefix, error) {
	sdl3.RLock()
	l2, ok := sdl3.cache[compiler]
	sdl3.RUnlock()

	if ok {
		return l2.getOrInsert(compiler, sysRoot, language)
	}

	sdl3.Lock()
	if l2, ok = sdl3.cache[compiler]; ok {
		sdl3.Unlock()
		return l2.getOrInsert(compiler, sysRoot, language)
	}

	l2 = newSystemDirL2()
	sdl3.cache[compiler] = l2
	sdl3.Unlock()

	return l2.getOrInsert(compiler, sysRoot, language)
}

func newSystemDirL2() *systemDirL2 {
	return &systemDirL2{
		cache: make(map[string]*systemDirL1),
	}
}

type systemDirL2 struct {
	sync.RWMutex

	cache map[string]*systemDirL1
}

func (sdl2 *systemDirL2) getOrInsert(compiler, sysRoot, language string) (*cacheSystemDirPrefix, error) {
	sdl2.RLock()
	l1, ok := sdl2.cache[sysRoot]
	sdl2.RUnlock()

	if ok {
		return l1.getOrInsert(compiler, sysRoot, language)
	}

	sdl2.Lock()
	if l1, ok = sdl2.cache[sysRoot]; ok {
		sdl2.Unlock()
		return l1.getOrInsert(compiler, sysRoot, language)
	}

	l1 = newSystemDirL1()
	sdl2.cache[sysRoot] = l1
	sdl2.Unlock()

	return l1.getOrInsert(compiler, sysRoot, language)
}

func newSystemDirL1() *systemDirL1 {
	return &systemDirL1{
		cache: make(map[string]*cacheSystemDirPrefix),
	}
}

type systemDirL1 struct {
	sync.RWMutex

	cache map[string]*cacheSystemDirPrefix
}

func (sdl1 *systemDirL1) getOrInsert(compiler, sysRoot, language string) (*cacheSystemDirPrefix, error) {
	sdl1.RLock()
	r, ok := sdl1.cache[language]
	sdl1.RUnlock()

	if ok {
		return r, nil
	}

	sdl1.Lock()
	defer sdl1.Unlock()

	if r, ok = sdl1.cache[language]; ok {
		return r, nil
	}

	dirs, err := getSystemSearch(compiler, sysRoot, language)
	if err != nil {
		return nil, err
	}
	r = newCacheSystemDirPrefix(dirs)
	sdl1.cache[language] = r
	return r, nil
}

// NewCacheSystemDirPrefix 提供一个全新的CacheSystemDirPrefix对象
func newCacheSystemDirPrefix(systemDirs []string) *cacheSystemDirPrefix {
	return &cacheSystemDirPrefix{
		systemDirs: systemDirs,
		cache:      make(map[int]bool),
	}
}

// cacheSystemDirPrefix 提供插入和查询某个路径是否在系统保留目录(如系统头文件目录)下
type cacheSystemDirPrefix struct {
	sync.RWMutex

	systemDirs []string
	cache      map[int]bool
}

// StartsWithSystemDir 检查提供的canonicalPath索引所对应的绝对路径, 是否位于系统保留目录内
func (csd *cacheSystemDirPrefix) startsWithSystemDir(f *File) bool {
	if f.fileType != FileTypeCanonical {
		return false
	}

	canonicalIndex := f.Index()

	csd.RLock()
	target, ok := csd.cache[canonicalIndex]
	csd.RUnlock()
	if ok {
		return target
	}

	path := f.String()

	found := false
	for _, dir := range csd.systemDirs {
		if strings.HasPrefix(path, dir) {
			found = true
			break
		}
	}

	csd.Lock()
	csd.cache[canonicalIndex] = found
	csd.Unlock()

	return found
}

func newBlackListFile() *blackListFile {
	return &blackListFile{
		blackList: make(map[int]bool),
	}
}

type blackListFile struct {
	sync.RWMutex

	blackList map[int]bool
}

func (b *blackListFile) set(f *File) {
	if f.fileType != FileTypeCanonical {
		return
	}

	b.Lock()
	b.blackList[f.Index()] = true
	b.Unlock()
}

func (b *blackListFile) check(f *File) bool {
	if f.fileType != FileTypeCanonical {
		return false
	}

	b.RLock()
	_, ok := b.blackList[f.Index()]
	b.RUnlock()
	return ok
}

// NewCacheSymlink provide an empty CacheSymlink
func NewCacheSymlink() *CacheSymlink {
	return &CacheSymlink{
		dir: newSymlinkCache(),
	}
}

// CacheSymlink 提供软链接的信息缓存, 防止重复的调用syscall来查询
type CacheSymlink struct {
	dir *symlinkCache
}

// GetLinks 获取一个文件f的所在路径上, 所有的软链接信息
// 例如/foo/bar/hello/world.cpp
// 依次查询/foo, /foo/bar, /foo/bar/hello, /foo/bar/hello/world.cpp是否为软链接, 若是则依次加入返回队列中
func (cs *CacheSymlink) GetLinks(f *File) []*dcSDK.FileDesc {
	if f.fileType != FileTypeDir {
		return nil
	}
	return cs.dir.getLinks(f)
}

func newSymlinkCache() *symlinkCache {
	return &symlinkCache{
		cache: make(map[int]*symlinkCacheItem),
	}
}

type symlinkCache struct {
	sync.RWMutex

	cache map[int]*symlinkCacheItem
}

func (sc *symlinkCache) getLinks(f *File) []*dcSDK.FileDesc {
	dir := strings.TrimRight(f.String(), "/")

	if dir == "/" || dir == "" {
		return nil
	}

	item := sc.getItem(f)
	item.Lock()
	defer item.Unlock()

	if item.resolved {
		return item.cache
	}

	r := sc.getLinks(f.Dir())
	if s := dcFile.Lstat(dir); s.Exist() && s.Basic().Mode()&os.ModeSymlink != 0 {
		lt, err := os.Readlink(dir)
		if err != nil {
			blog.Warnf("analyser: read link for %s failed: %v", dir, err)
			return nil
		}
		if !filepath.IsAbs(lt) {
			base, _ := filepath.EvalSymlinks(filepath.Dir(dir))
			lt = filepath.Join(base, lt)
		}
		lt, err = filepath.EvalSymlinks(lt)
		if err != nil {
			blog.Warnf("analyser: eval symlinks for %s failed: %v", dir, err)
			return nil
		}

		st := dcFile.Stat(dir)
		r = append(r, &dcSDK.FileDesc{
			FilePath:           dir,
			FileSize:           st.Size(),
			Lastmodifytime:     st.ModifyTime64(),
			Filemode:           st.Mode32(),
			Targetrelativepath: filepath.Dir(dir),
			LinkTarget:         lt,
		})
	}
	item.cache = r
	item.resolved = true

	return r
}

func (sc *symlinkCache) getItem(f *File) *symlinkCacheItem {
	idx := f.Index()

	sc.RLock()
	r, ok := sc.cache[idx]
	sc.RUnlock()
	if ok {
		return r
	}

	sc.Lock()
	if r, ok = sc.cache[idx]; ok {
		sc.Unlock()
		return r
	}

	r = newSymlinkCacheItem()
	sc.cache[idx] = r
	sc.Unlock()

	return r
}

func newSymlinkCacheItem() *symlinkCacheItem {
	return &symlinkCacheItem{}
}

type symlinkCacheItem struct {
	sync.RWMutex

	resolved bool
	cache    []*dcSDK.FileDesc
}

// CalculateResult provide the result of CalculateNodes
// Includes the dependent files and the dependent symlinks
type CalculateResult struct {
	Files    []*CalculateResultItem
	Symlinks []*dcSDK.FileDesc
}

// CalculateResultItem provide the item in CalculateResult
// Includes every single dependent file
type CalculateResultItem struct {
	Desc      *dcSDK.FileDesc
	Canonical *File
}

// CalculateNode 使用广度优先搜索, 根据提供的node作为跟节点, 搜索整个关系树并返回所依赖的全部路径
func CalculateNodes(node *NodeCacheValue) (*CalculateResult, error) {
	if node == nil {
		return &CalculateResult{}, nil
	}

	r := make([]*CalculateResultItem, 0, 20)
	l := make([]*dcSDK.FileDesc, 0, 20)
	visited := make(map[int64]bool)
	searchList := []*NodeCacheValue{node}
	head := 0
	tail := 1

	for ; head < tail; head++ {
		current := searchList[head]
		current.WaitUntilValid()

		if current.IsDisabled() {
			continue
		}

		if current.InBlackList() {
			return nil, ErrAnalyserGiveUpAnalysing
		}

		if _, ok := visited[current.visitor]; ok {
			continue
		}
		visited[current.visitor] = true

		if current.fileDesc != nil {
			r = append(r, &CalculateResultItem{
				Desc:      current.fileDesc,
				Canonical: current.canonicalPath,
			})
			l = append(l, current.symlinksDesc...)
		}
		for _, child := range current.children {
			searchList = append(searchList, child)
			tail++
		}
	}

	return &CalculateResult{
		Files:    r,
		Symlinks: l,
	}, nil
}
