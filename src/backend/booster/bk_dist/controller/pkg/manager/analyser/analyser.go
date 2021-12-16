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
	"path/filepath"
	"time"

	"github.com/Tencent/bk-ci/src/booster/bk_dist/common/env"
	dcSDK "github.com/Tencent/bk-ci/src/booster/bk_dist/common/sdk"
	"github.com/Tencent/bk-ci/src/booster/common/blog"
	"github.com/Tencent/bk-ci/src/booster/common/util"
)

// New get an Analyser with new file cache and root cache
func New() *Analyser {
	return NewWithCache(NewFileCache(), NewRootCache())
}

// NewWithCache get an Analyser with specific file cache and root cache
func NewWithCache(fileCache *FileCache, rootCache *RootCache) *Analyser {
	if fileCache == nil {
		fileCache = NewFileCache()
	}

	if rootCache == nil {
		rootCache = NewRootCache()
	}

	symbolMgr := newSymbolManager()

	return &Analyser{
		id:                 util.RandomString(7),
		fileCache:          fileCache,
		treeCache:          rootCache,
		emptyDirectory:     fileCache.EmptyDirectory(),
		emptyRelativePath:  fileCache.EmptyRelativeFile(),
		emptyCanonicalPath: fileCache.EmptyCanonicalFile(),
		stats:              NewStats(),
		macros:             newMacroTable(symbolMgr),
		symbolMgr:          symbolMgr,
	}
}

// Analyser provide a manager of task dependence analysing
type Analyser struct {
	id string

	disableMacro bool

	fileCache *FileCache
	treeCache *RootCache
	nodeCache *NodeCache

	resolveSkipper resolveSkipFunc

	emptyDirectory     *File
	emptyRelativePath  *File
	emptyCanonicalPath *File

	ps *ParseStat

	// runDir在整个Analyse期间都是固定的, 这是单个指令发起的目录
	runDir *File

	stats *Stats

	macros    *macroTable
	symbolMgr *symbolManager
}

// NodeType 定义了当前分析的搜索节点的类型
// 分为
// - RESOLVED: 已经找到具体文件
// - QUOTE:    将要从quote include里面找文件
// - ANGLE:    将要从angle include里面找文件
// - NEXT:     将要从next include里面找文件
type NodeType int

// Get NodeType int
func (nt NodeType) Int() int {
	return int(nt)
}

// Get NodeType string
func (nt NodeType) String() string {
	s, _ := mapNodeType[nt.Int()]
	return s
}

const (
	NodeTypeResolved NodeType = iota
	NodeTypeQuote
	NodeTypeAngle
	NodeTypeNext
)

var mapNodeType = map[int]string{
	NodeTypeResolved.Int(): "RESOLVED",
	NodeTypeQuote.Int():    "QUOTE",
	NodeTypeAngle.Int():    "ANGLE",
	NodeTypeNext.Int():     "NEXT",
}

// Result 定义了Analyse Do的结果
// 对于一个命令的分析结果, 最终得到的是
// - DependentFile:    该命令所依赖的文件
// - DependentSymlink: 该命令及其所依赖文件所依赖的软链接, 这将被视为在远程复刻该命令的前提条件
type Result struct {
	DependentFile    []*dcSDK.FileDesc
	DependentSymlink []*dcSDK.FileDesc
}

// Do 执行对一个命令的分析. includeSystemHeader 决定了是否要将system header包含在dependentFile中,
// 若为false, 则system header都会被过滤掉 disableMacro 决定了是否要彻底禁止macro,
// 若为true, 则该命令涉及的整颗依赖树, 一旦出现include macro, 则整颗树的所有节点都会被禁用
func (a *Analyser) Do(runDir string, cmd []string, environ *env.Sandbox,
	includeSystemHeader, disableMacro bool) (*Result, error) {
	enterTime := time.Now().Local()

	a.disableMacro = disableMacro
	// 首先解析命令行, 得到所有的入参信息
	ps, err := a.ParseCommand(runDir, cmd, environ)
	if err != nil {
		blog.Warnf("analyser: [%s] parse command failed with command(%v) runDir(%s): %v", a.id, cmd, runDir, err)
		return nil, err
	}
	a.ps = ps
	a.runDir = a.fileCache.GetDirectoryFile(runDir)
	a.nodeCache = a.treeCache.MatchOrInsert(a.runDir, a.ps.QuoteDirList, a.ps.AngleDirList)
	a.resolveSkipper = func(canonical *File) bool {
		if includeSystemHeader {
			return false
		}

		ok, _ := a.fileCache.StartWithSystemDir(ps.Compiler, ps.IncludeSysRoot(), ps.Language, canonical)
		return ok
	}

	dependentFileRaw := make([]*CalculateResultItem, 0, 50)
	dependentSymlink := make([]*dcSDK.FileDesc, 0, 50)
	for _, includeF := range ps.IncludeFileList {
		r, err := a.Run(includeF, ps.QuoteDirList)
		if err != nil {
			return nil, err
		}

		dependentFileRaw = append(dependentFileRaw, r.Files...)
		dependentSymlink = append(dependentSymlink, r.Symlinks...)
	}

	r, err := a.Run(ps.SourceFile, nil)
	if err != nil {
		return nil, err
	}
	dependentFileRaw = append(dependentFileRaw, r.Files...)
	dependentSymlink = uniqueSymlinkFileDesc(append(dependentSymlink, r.Symlinks...))

	dependentFile := make([]*dcSDK.FileDesc, 0, len(dependentFileRaw))
	for _, item := range dependentFileRaw {
		//ok, err := a.fileCache.StartWithSystemDir(ps.Compiler, ps.IncludeSysRoot(), ps.Language, item.Canonical)
		//if err != nil {
		//	return nil, err
		//}
		//if ok {
		//	continue
		//}
		dependentFile = append(dependentFile, item.Desc)
	}

	a.stats.TimeTotal = time.Now().Local().Sub(enterTime)
	a.stats.Calculate()

	// rlv for resolve, psf for parse-file, fnd for find node
	// C for count, PC for ProcessCount, HC for HitCount
	// T for time, PT for ProcessTime, HT for HitTime
	// PAT for ProcessAvgTime, PLT for ProcessLongestTime
	blog.Infof("analyser: Stats for(%s), totalT(%d). \n"+
		"analyser: Stats rlvC(%d) rlvPC(%d) rlvHC(%d) rlvT(%d) rlvPT(%d) rlvHT(%d) rlvPAT(%d) rlvPLT(%d) \n"+
		"analyser: Stats psfC(%d) psfPC(%d) psfHC(%d) psfT(%d) psfPT(%d) psfHT(%d) psfPAT(%d) psfPLT(%d) \n"+
		"analyser: Stats fndC(%d) fndPC(%d) fndHC(%d) fndT(%d) fndPT(%d) fndHT(%d) fndPAT(%d) fndPLT(%d)",
		ps.SourceFile, a.stats.TimeTotal.Milliseconds(),
		a.stats.ResolveCount, a.stats.ResolveProcessCount, a.stats.ResolveHitCount,
		a.stats.ResolveTimeTotal.Milliseconds(),
		a.stats.ResolveProcessTimeTotal.Milliseconds(),
		a.stats.ResolveHitTimeTotal.Milliseconds(),
		a.stats.ResolveProcessTimeAvg.Milliseconds(),
		a.stats.ResolveHitTimeAvg.Milliseconds(),
		a.stats.ParseFileCount, a.stats.ParseFileProcessCount, a.stats.ParseFileHitCount,
		a.stats.ParseFileTimeTotal.Milliseconds(),
		a.stats.ParseFileProcessTimeTotal.Milliseconds(),
		a.stats.ParseFileHitTimeTotal.Milliseconds(),
		a.stats.ParseFileProcessTimeAvg.Milliseconds(),
		a.stats.ParseFileHitTimeAvg.Milliseconds(),
		a.stats.FindNodeCount, a.stats.FindNodeProcessCount, a.stats.FindNodeHitCount,
		a.stats.FindNodeTimeTotal.Milliseconds(),
		a.stats.FindNodeProcessTimeTotal.Milliseconds(),
		a.stats.FindNodeHitTimeTotal.Milliseconds(),
		a.stats.FindNodeProcessTimeAvg.Milliseconds(),
		a.stats.FindNodeHitTimeAvg.Milliseconds())

	return &Result{
		DependentFile:    dependentFile,
		DependentSymlink: dependentSymlink,
	}, nil
}

// Run 通过分析起始文件, 生成搜索树, 并最终从搜索树中计算出所有依赖项
func (a *Analyser) Run(filePath *File, searchDirList []*File) (*CalculateResult, error) {
	startTime := time.Now().Local()
	r, err := a.fileCache.Resolve(&FileResolveParam{
		filePath:      filePath,
		runDir:        a.runDir,
		searchDir:     a.runDir,
		searchDirList: searchDirList,
	}, a.resolveSkipper)
	if err != nil {
		blog.Warnf("analyser: [%s] run and resolve failed, filePath(%s): %v", a.id, filePath.String(), err)
		return nil, err
	}
	if r != nil {
		a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))
	}

	// TODO: do -D insert, for macro extension

	nodes, err := a.FindNode(NodeTypeResolved, r.searchDir, r.filePath, a.runDir, a.emptyCanonicalPath, r)
	if err != nil {
		return nil, err
	}

	return CalculateNodes(nodes)
}

// FindNode 针对每一个搜索节点做分析, 根据不同的类型, 分析得到实际文件, 和该文件的父依赖, 并组织下一个深度的搜索
func (a *Analyser) FindNode(
	kind NodeType,
	searchDir, filePath, lastFileDir, lastFile *File,
	r *FileSolveResult) (*NodeCacheValue, error) {
	var f, s *File
	var err error
	result := make([]*NodeCacheValue, 0, 20)

	blog.Debugf("analyser: [%s] find node got param kind(%s), searchDir(%s), "+
		"filePath(%s), lastFileDir(%s), lastFile(%s)", a.id, kind, searchDir, filePath, lastFileDir, lastFile)
	enterTime := time.Now().Local()
	nodeValue, match := a.nodeCache.MatchOrInsert(searchDir, filePath, lastFileDir, kind)
	//sm := a.symbolMgr.get(nodeValue.visitor)

	//if match && (sm.isValid() || !nodeValue.IsValid()) {
	//	a.stats.findNodeEx(true, time.Now().Local().Sub(enterTime))
	//	return nodeValue, nil
	//}

	if match {
		a.stats.findNodeEx(true, time.Now().Local().Sub(enterTime))
		return nodeValue, nil
	}

	switch kind {
	case NodeTypeResolved:
	case NodeTypeQuote:
		startTime := time.Now().Local()
		r, err = a.fileCache.Resolve(&FileResolveParam{
			filePath:      filePath,
			runDir:        a.runDir,
			searchDir:     lastFileDir,
			searchDirList: a.ps.QuoteDirList,
		}, a.resolveSkipper)

		if r != nil {
			a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))
		}

	case NodeTypeAngle:
		startTime := time.Now().Local()
		r, err = a.fileCache.Resolve(&FileResolveParam{
			filePath:      filePath,
			runDir:        a.runDir,
			searchDir:     a.emptyDirectory,
			searchDirList: a.ps.AngleDirList,
		}, a.resolveSkipper)

		if r != nil {
			a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))
		}

	case NodeTypeNext:
		found := 0
		// 对于include_next, 枚举拿到next
		for _, d := range a.ps.QuoteDirList {
			startTime := time.Now().Local()
			r, err = a.fileCache.Resolve(&FileResolveParam{
				filePath:      filePath,
				runDir:        a.runDir,
				searchDir:     a.emptyDirectory,
				searchDirList: []*File{d},
			}, a.resolveSkipper)

			if r != nil {
				a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))

				res, err := a.FindNode(NodeTypeResolved, r.searchDir, r.filePath, lastFileDir, lastFile, r)
				if err != nil {
					nodeValue.SetBlackList()
					return nil, err
				}

				if !res.IsDisabled() {
					result = append(result, res)
				}

				found++
				// 为了避免一些异常情况, 至少获取三个
				if found >= 3 {
					break
				}
			}
		}

		// 找到了next
		if found >= 2 {
			nodeValue.Enable(nil, result)
			return nodeValue, nil
		}

		// 没有找到next
		nodeValue.Disable()
		blog.Debugf("analyser: [%s] file resolve failed, file(%s): %v", a.id, filePath, err)
		a.stats.findNodeEx(false, time.Now().Local().Sub(enterTime))
		return nodeValue, nil

	default:
		err = ErrAnalyserNodeTypeNoSupport
	}

	if err != nil {
		nodeValue.Disable()
		blog.Debugf("analyser: [%s] file resolve failed, file(%s): %v", a.id, filePath, err)
		a.stats.findNodeEx(false, time.Now().Local().Sub(enterTime))
		return nodeValue, nil
	}

	if r == nil {
		nodeValue.SetBlackList()
		return nil, ErrAnalyserInvalidParam
	}

	f = r.canonicalPath
	s = r.searchDir

	if a.fileCache.CheckBlackList(f) {
		blog.Warnf("analyser: give up analysing for file %s", f)
		nodeValue.SetBlackList()
		return nil, ErrAnalyserGiveUpAnalysing
	}

	blog.Debugf("analyser: [%s] success find %s", a.id, f)
	dir, _ := filepath.Split(f.String())

	startTime := time.Now().Local()
	fr, err := a.ParseFile(f)
	if err != nil {
		blog.Warnf("analyser: [%s] parse file failed with file %s: %v", a.id, f, err)
		nodeValue.SetBlackList()
		return nil, err
	}
	a.stats.parseFileEx(fr.hitCache, time.Now().Local().Sub(startTime))

	blog.Debugf("analyser: [%s] parse file code %s, find quotes(%v), angles(%v), expr(%v)",
		a.id, f, fr.QuoteIncludes, fr.AngleIncludes, fr.ExprIncludes)
	a.stats.findNodeEx(false, time.Now().Local().Sub(enterTime))

	// set valid
	//sm.setValid()

	for _, item := range fr.QuoteIncludes {
		res, err := a.FindNode(NodeTypeQuote, s, item, a.fileCache.GetDirectoryFile(dir), f, nil)
		if err != nil {
			nodeValue.SetBlackList()
			return nil, err
		}

		if !res.IsDisabled() {
			//sm.combine(a.symbolMgr.get(r.visitor))
			result = append(result, res)
		}
	}

	for _, item := range fr.AngleIncludes {
		res, err := a.FindNode(NodeTypeAngle, s, item, a.emptyDirectory, f, nil)
		if err != nil {
			nodeValue.SetBlackList()
			return nil, err
		}

		if !res.IsDisabled() {
			//sm.combine(a.symbolMgr.get(r.visitor))
			result = append(result, res)
		}
	}

	for _, item := range fr.NextIncludes {
		res, err := a.FindNode(NodeTypeNext, s, item, a.fileCache.GetDirectoryFile(dir), f, nil)
		if err != nil {
			nodeValue.SetBlackList()
			return nil, err
		}

		if !res.IsDisabled() {
			//sm.combine(a.symbolMgr.get(r.visitor))
			result = append(result, res)
		}
	}

	if a.disableMacro && len(fr.ExprIncludes) > 0 {
		blog.Warnf("analyser: give up analysing for file %s, because it has expr includes: %v",
			f, fr.ExprIncludes)
		a.fileCache.SetBlackList(f)
		nodeValue.SetBlackList()
		return nil, ErrAnalyserGiveUpAnalysing
	}

	// 对所有expr的include, 做宏展开, 并直接resolve其文件
	//for _, expr := range fr.ExprIncludes {
	//	exprFList := make([]*FileSolveResult, 0, 10)
	//	unSolvedSymbols := make([]string, 0, 10)
	//
	//	// 对单个expr, 一次性resolve所有的可能值, 再依次find-node
	//	possibleV, ok := a.macros.getExpressionTimeout(expr, nil, 1*time.Second)
	//	if !ok {
	//		blog.Warnf("analyser: give up analysing for file %s", f)
	//		a.fileCache.SetBlackList(f)
	//		return nil, ErrAnalyserGiveUpAnalysing
	//	}
	//	for _, value := range possibleV {
	//		t, str := getRealIncludeFromExpr(value)
	//
	//		switch t {
	//		case includeTypeQuote:
	//			startTime := time.Now().Local()
	//			r, _ := a.fileCache.Resolve(&FileResolveParam{
	//				filePath:      a.fileCache.GetRelativeFile(str),
	//				runDir:        a.runDir,
	//				searchDir:     a.fileCache.GetDirectoryFile(dir),
	//				searchDirList: a.ps.QuoteDirList,
	//			}, a.resolveSkipper)
	//
	//			if r != nil {
	//				a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))
	//				exprFList = append(exprFList, r)
	//			}
	//		case includeTypeAngle:
	//			startTime := time.Now().Local()
	//			r, _ := a.fileCache.Resolve(&FileResolveParam{
	//				filePath:      a.fileCache.GetRelativeFile(str),
	//				runDir:        a.runDir,
	//				searchDir:     a.emptyDirectory,
	//				searchDirList: a.ps.AngleDirList,
	//			}, a.resolveSkipper)
	//
	//			if r != nil {
	//				a.stats.resolveEx(r.hitCache, time.Now().Local().Sub(startTime))
	//				exprFList = append(exprFList, r)
	//			}
	//		default:
	//			unSolvedSymbols = append(unSolvedSymbols, findAllSymbol(str)...)
	//			// TODO: not definition symbols, should be recorded and expressed in the future.
	//		}
	//	}
	//
	//	for _, exprFr := range exprFList {
	//		r, err := a.FindNode(NodeTypeResolved, exprFr.searchDir, exprFr.filePath,
	//		a.runDir, a.emptyCanonicalPath, exprFr)
	//		if err != nil {
	//			return nil, err
	//		}
	//
	//		if !r.IsDisabled() {
	//			sm.combine(a.symbolMgr.get(r.visitor))
	//			result = append(result, r)
	//		}
	//	}
	//
	//	// 把该include目标所有未解决的macro, 都加到symbol manager里, 等待后续通知
	//	sm.add(unSolvedSymbols)
	//}

	nodeValue.Enable(r, result)
	// TODO: handle for expr-includes and include-next

	blog.Debugf("analyser: [%s] end find node and got files, filePath(%s)", a.id, f)
	return nodeValue, nil
}
