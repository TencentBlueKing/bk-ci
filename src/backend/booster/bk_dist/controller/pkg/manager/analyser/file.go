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
	"bytes"
	"io/ioutil"
	"os"
	"regexp"
	"strconv"
	"strings"
	"sync"
	"time"

	"github.com/Tencent/bk-ci/src/booster/common/blog"
)

// FileResult provide a result of ParseFile
// Includes the files's include information
type FileResult struct {
	hitCache      bool
	QuoteIncludes []*File
	AngleIncludes []*File
	ExprIncludes  []string
	NextIncludes  []*File
}

var (
	ReBackSlash     = regexp.MustCompile(`\\\n`)
	ReMark          = regexp.MustCompile(`include|define|import`)
	RePairedComment = regexp.MustCompile(`(/[*](.|\n)*?[*]/)`)
	ReMacroExpr     = regexp.MustCompile(`(?P<symbol>\w+)(?P<outer>\s*[(]\s*(?P<args>([^(),])*([,]([^(),])*)*)[)])?`)
	ReContent       = regexp.MustCompile(`^[ \t]*([*][/])?[ \t]*([/][*][^\n]*[*][/])*[ \t]*(?P<directive>[#][ \t]*(define|include_next|include|import)\b(.|\n)*?(([^\\]\n)|$))`)
	ReDirective     = regexp.MustCompile(`^[ \t]*[#][ \t]*(((?P<include>include_next|include|import)\s*("(?P<quote>(\w|[_/.,+-])*)"|<(?P<angle>(\w|[_/.,+-])*)>|(?P<expr>.*?)))|(?P<define>define\s+(?P<lhs>(?P<symbol>\w+)(\s*[(]\s*(?P<args>([^(),])*([,]([^(),])*)*)[)])?)\s*(?P<rhs>.*?)))\s*((/[*]|//)(.|\n)*)?$`)
	ReSystemSearch  = regexp.MustCompile(`#include <...> search starts here:\n((.|\n)*?)\nEnd of search list`)
	ReMacroSymbol   = regexp.MustCompile(`\b\w+\b`)
	ReSinglePound   = regexp.MustCompile(`\B#\s*(\S*)`)
	ReDoublePound   = regexp.MustCompile(`\s*##\s*`)
	ReIncludes      = regexp.MustCompile(`^\s*("\s*(?P<quote>(\w|[\\_/.,+-])*)\s*"|<\s*(?P<angle>(\w|[\\_/.,+-])*)\s*>)\s*$`)
)

type directiveResult struct {
	// the type of the include symbol, include/import/include_next
	IncludeType string

	//
	Quote string

	//
	Angle string

	//

	Expr string

	Define string

	Lhs string

	Symbol string

	Args string

	Rhs string
}

// ParseFile 分析单个文件的include信息, 并得到其依赖关系
func (a *Analyser) ParseFile(filePath *File) (*FileResult, error) {
	if r := a.fileCache.GetFileParse(filePath); r != nil {
		blog.Debugf("analyser: [%s] parse cache got %s", a.id, filePath)
		return &FileResult{
			hitCache:      true,
			QuoteIncludes: r.QuoteIncludes,
			AngleIncludes: r.AngleIncludes,
			ExprIncludes:  r.ExprIncludes,
			NextIncludes:  r.NextIncludes,
		}, nil
	}

	f, err := os.Open(filePath.String())
	if err != nil {
		return nil, err
	}

	data, err := ioutil.ReadAll(f)
	if err != nil {
		return nil, err
	}
	defer func() {
		_ = f.Close()
	}()

	fr := &FileResult{}
	lastPoint := -1
	i := 0

	for {
		idx := ReMark.FindIndex(data[i:])
		if idx == nil {
			break
		}

		i += idx[1]
		point := bytes.LastIndex(data[:i], []byte("\n")) + 1
		if point == lastPoint {
			continue
		}
		lastPoint = point

		match := ReContent.FindSubmatch(data[point:])

		if match != nil {
			content := match[3]

			content = ReBackSlash.ReplaceAll(content, nil)
			content = RePairedComment.ReplaceAll(content, nil)

			dr := directiveResult{}
			directive := ReDirective.FindSubmatch(content)
			for i, d := range directive {
				str := string(d)

				switch ReDirective.SubexpNames()[i] {
				case "include":
					dr.IncludeType = str
				case "quote":
					dr.Quote = str
				case "angle":
					dr.Angle = str
				case "expr":
					dr.Expr = str
				case "define":
					dr.Define = str
				case "lhs":
					dr.Lhs = str
				case "symbol":
					dr.Symbol = str
				case "args":
					dr.Args = str
				case "rhs":
					dr.Rhs = str
				}
			}

			if dr.IncludeType == "include" || dr.IncludeType == "import" {
				if dr.Quote != "" {
					fr.QuoteIncludes = append(fr.QuoteIncludes, a.fileCache.GetRelativeFile(dr.Quote))
				} else if dr.Angle != "" {
					fr.AngleIncludes = append(fr.AngleIncludes, a.fileCache.GetRelativeFile(dr.Angle))
				} else if dr.Expr != "" {
					expr := strings.TrimRight(dr.Expr, " ")
					if len(expr) > 0 && isExprBegin(expr[0]) {
						fr.ExprIncludes = append(fr.ExprIncludes, expr)
					}
				} else {
					// TODO: error handle, no found any target
				}

			} else if dr.IncludeType == "include_next" {
				if dr.Quote != "" {
					fr.NextIncludes = append(fr.NextIncludes, a.fileCache.GetRelativeFile(dr.Quote))
				} else if dr.Angle != "" {
					fr.NextIncludes = append(fr.NextIncludes, a.fileCache.GetRelativeFile(dr.Angle))
				} else if dr.Expr != "" {
					// TODO: can not deal with computed include here
				} else {
					// TODO: error handle, no found any target
				}

			} else if dr.Define != "" {
				//a.macros.insertDefine(dr.Lhs, dr.Rhs)
				//a.symbolMgr.invalidate(dr.Lhs)
			}
		}
	}

	a.fileCache.PutFileParse(filePath, fr)
	blog.Debugf("analyser: [%s] parse cache put file %s", a.id, filePath)
	return fr, nil
}

type macroExprCache struct {
	cache map[string]*macroExprCacheList
}

func (mec *macroExprCache) key(expr string, disabledSymbol map[string]bool) string {
	return expr + strconv.Itoa(len(disabledSymbol))
}

func (mec *macroExprCache) get(expr string, disabledSymbol map[string]bool) (bool, []string) {
	key := mec.key(expr, disabledSymbol)
	c, ok := mec.cache[key]
	if !ok {
		return false, nil
	}

	return c.match(expr, disabledSymbol)
}

func (mec *macroExprCache) set(expr string, disabledSymbol map[string]bool, value []string) {
	key := mec.key(expr, disabledSymbol)
	c, ok := mec.cache[key]
	if !ok {
		c = &macroExprCacheList{}
		mec.cache[key] = c
	}

	c.set(expr, disabledSymbol, value)
}

type macroExprCacheList struct {
	head *macroExprCacheItem
	tail *macroExprCacheItem
}

func (mel *macroExprCacheList) set(expr string, disabledSymbol map[string]bool, value []string) {
	if ok, _ := mel.match(expr, disabledSymbol); ok {
		return
	}

	item := &macroExprCacheItem{
		expr:           expr,
		value:          value,
		disabledSymbol: disabledSymbol,
	}

	if mel.head == nil {
		mel.head = item
		mel.tail = item
		return
	}

	mel.tail.next = item
	mel.tail = item
}

func (mel *macroExprCacheList) match(expr string, disabledSymbol map[string]bool) (bool, []string) {
	m := mel.head

	for m != nil {
		if m.expr == expr {
			match := true
			for k := range disabledSymbol {
				if _, ok := m.disabledSymbol[k]; !ok {
					match = false
					break
				}
			}
			if match {
				return true, m.value
			}
		}
		m = m.next
	}

	return false, nil
}

type macroExprCacheItem struct {
	expr           string
	value          []string
	disabledSymbol map[string]bool

	next *macroExprCacheItem
}

func newMacroTable(symbolMgr *symbolManager) *macroTable {
	return &macroTable{
		symbolMgr: symbolMgr,
		table:     make(map[string]*macroItemList),
		cache:     &macroExprCache{cache: make(map[string]*macroExprCacheList)},
	}
}

type macroTable struct {
	sync.RWMutex

	symbolMgr *symbolManager
	table     map[string]*macroItemList

	cache *macroExprCache
}

// 如果超时, 则立刻返回false
func (mt *macroTable) getExpressionTimeout(
	expr string,
	disabledSymbol map[string]bool,
	timeout time.Duration) ([]string, bool) {
	tick := time.NewTimer(timeout)
	done := make(chan []string)
	go func() {
		done <- mt.getExpression(expr, disabledSymbol)
	}()

	select {
	case r := <-done:
		return r, true
	case <-tick.C:
		return nil, false
	}
}

// getExpression 给定一个expr字符串, 和要过滤掉的symbol列表, 返回在当前table下, 宏展开后所有可能的值
func (mt *macroTable) getExpression(expr string, disabledSymbol map[string]bool) []string {
	if len(expr) == 0 {
		return []string{""}
	}

	//if ok, v := mt.cache.get(expr, disabledSymbol); ok {
	//	return v
	//}

	matchedIndex := getFirstWorld(expr)

	// 如果不带任何可以作为symbol的内容, 比如只有一些特殊字符, 直接返回原字符串
	if matchedIndex == nil || len(matchedIndex) != 2 {
		return []string{expr}
	}

	// 获得匹配到的symbol, 和它的首尾位置
	start, end := matchedIndex[0], matchedIndex[1]
	symbol := expr[start:end]

	// 若symbol在过滤列表中, 则直接返回原字符串
	if disabledSymbol != nil {
		if _, ok := disabledSymbol[symbol]; ok {
			r := []string{expr}
			//mt.cache.set(expr, disabledSymbol, r)
			return r
		}
	}

	// 获得在table中的symbol对应的可能值
	definitions := mt.getDefine(symbol)

	// 没有任何匹配的定义, 无需替换, 直接递归处理symbol后面的部分
	if definitions == nil || definitions.len() == 0 {
		r := prepend(expr[:end], mt.getExpression(expr[end:], disabledSymbol))
		//mt.cache.set(expr, disabledSymbol, r)
		return r
	}

	// 获得可能存在的symbol函数的入参列表
	argsList, argsEnd := parseMacroFunctionArgs(expr, end)
	nextDisabledSymbol := updateDisabledMap(disabledSymbol, symbol)

	result := []string{expr}
	for _, definition := range definitions.get() {
		// 至少有一个没替换的方案
		result = append(result, prepend(expr[:end], mt.getExpression(expr[end:], disabledSymbol))...)

		// 不是函数, 直接替换, 递归调用后续的情况, 并测试替换后的组合
		if !definition.isFunc {
			for _, after := range mt.getExpression(expr[end:], disabledSymbol) {
				result = append(result, prepend(expr[:start],
					mt.getExpression(definition.rhs+after, nextDisabledSymbol))...)
			}
			continue
		}

		// 有参数的情况
		// 如果参数个数不匹配, 则跳过
		if len(definition.args) != len(argsList) {
			continue
		}

		// 找到所有参数的所有可能值
		argsExpand := make([][]string, 0, 10)
		for _, arg := range argsList {
			argsExpand = append(argsExpand, mt.getExpression(arg, disabledSymbol))
		}

		// 广度优先扩展, 获取所有可能的参数组合
		expansions := []string{definition.rhs}
		for i, argSet := range argsExpand {
			addition := make([]string, 0, 10)

			// 第i个参数的所有可能, 都做一次替换, 并更新到expansions中
			for _, argI := range argSet {
				for _, ex := range expansions {
					nex := replaceSymbol(ex, definition.args[i], argI)
					if nex != ex {
						addition = append(addition, nex)
					}
				}
			}
			expansions = append(expansions, addition...)
		}

		// 对所有可能的替换值, 递归调用后续的情况, 并测试替换后的组合
		for _, ex := range expansions {
			realEx := realExpansion(ex)
			for _, after := range mt.getExpression(expr[argsEnd:], disabledSymbol) {
				result = append(result, prepend(expr[:start], mt.getExpression(realEx+after, nextDisabledSymbol))...)
			}
		}
	}

	r := uniqueStringSlice(result)
	//mt.cache.set(expr, disabledSymbol, r)
	return r
}

func (mt *macroTable) getDefine(symbol string) *macroItemList {
	mt.RLock()
	l, ok := mt.table[symbol]
	mt.RUnlock()

	if !ok {
		return nil
	}

	return l
}

func (mt *macroTable) insertDefine(lhs, rhs string) {
	expr := ReMacroExpr.FindStringSubmatch(lhs)

	var symbol string
	var args []string
	isFunc := false
	for i, d := range expr {
		switch ReMacroExpr.SubexpNames()[i] {
		case "symbol":
			symbol = d
		case "args":
			d = strings.TrimSpace(d)
			if d != "" {
				for _, di := range strings.Split(d, ",") {
					args = append(args, strings.TrimSpace(di))
				}
			}
		case "outer":
			if d = strings.TrimSpace(d); len(d) > 0 {
				isFunc = true
			}
		}
	}

	lhs = symbol
	item := &macroItem{
		rhs:    rhs,
		isFunc: isFunc,
		args:   args,
	}

	mt.RLock()
	l, ok := mt.table[lhs]
	mt.RUnlock()

	if ok {
		if l.add(item) {
			mt.symbolMgr.invalidate(lhs)
		}
		return
	}

	mt.Lock()
	l, ok = mt.table[lhs]
	if ok {
		mt.Unlock()
		if l.add(item) {
			mt.symbolMgr.invalidate(lhs)
		}
		return
	}

	l = &macroItemList{}
	if l.add(item) {
		mt.symbolMgr.invalidate(lhs)
	}
	mt.table[lhs] = l
	mt.Unlock()
}

type macroItemList struct {
	sync.RWMutex

	list []*macroItem
}

func (mil *macroItemList) add(item *macroItem) bool {
	mil.Lock()
	defer mil.Unlock()

	for _, e := range mil.list {
		if e.equal(item) {
			return false
		}
	}
	mil.list = append(mil.list, item)
	return true
}

func (mil *macroItemList) get() []*macroItem {
	mil.RLock()
	defer mil.RUnlock()

	return mil.list
}

func (mil *macroItemList) len() int {
	mil.RLock()
	defer mil.RUnlock()

	return len(mil.list)
}

type macroItem struct {
	rhs string

	isFunc bool
	args   []string
}

func (mi *macroItem) equal(omi *macroItem) bool {
	if mi.rhs != omi.rhs {
		return false
	}

	if len(mi.args) != len(omi.args) {
		return false
	}

	for i := range mi.args {
		if mi.args[i] != omi.args[i] {
			return false
		}
	}

	return true
}

// String return the string message of macroItem
func (mi *macroItem) String() string {
	return "rhs(" + mi.rhs + ") args(" + strings.Join(mi.args, ",") + ")"
}

func newSymbolManager() *symbolManager {
	return &symbolManager{
		symbolMap:  make(map[string]*symbolList),
		symbolPool: make(map[int64]*symbolRecord),
	}
}

type symbolManager struct {
	symbolMap  map[string]*symbolList
	symbolPool map[int64]*symbolRecord
}

func (sm *symbolManager) get(id int64) *symbolRecord {
	sr, ok := sm.symbolPool[id]
	if ok {
		return sr
	}

	sr = &symbolRecord{
		mgr:     sm,
		id:      id,
		symbols: make(map[string]bool),
	}
	sm.symbolPool[id] = sr
	return sr
}

func (sm *symbolManager) add(symbols []string, sr *symbolRecord) {
	for _, symbol := range symbols {
		sl, ok := sm.symbolMap[symbol]
		if !ok {
			sl = &symbolList{}
			sm.symbolMap[symbol] = sl
		}

		sl.add(sr)
	}
}

func (sm *symbolManager) invalidate(symbol string) {
	sl, ok := sm.symbolMap[symbol]
	if !ok {
		return
	}

	sl.invalidate(symbol)
}

type symbolList struct {
	list []*symbolRecord
}

func (sl *symbolList) invalidate(symbol string) {
	for _, sr := range sl.list {
		sr.valid = false
	}
}

func (sl *symbolList) add(sr *symbolRecord) {
	sl.list = append(sl.list, sr)
}

type symbolRecord struct {
	mgr *symbolManager

	id      int64
	valid   bool
	symbols map[string]bool
}

func (sr *symbolRecord) isValid() bool {
	return sr.valid
}

func (sr *symbolRecord) setValid() {
	sr.valid = true
}

func (sr *symbolRecord) add(symbols []string) {
	newSb := make([]string, 0, len(symbols))
	for _, s := range symbols {
		if _, ok := sr.symbols[s]; !ok {
			sr.symbols[s] = true
			newSb = append(newSb, s)
		}
	}

	sr.mgr.add(newSb, sr)
}

func (sr *symbolRecord) combine(osr *symbolRecord) {
	sb := make([]string, 0, len(osr.symbols))
	for k := range osr.symbols {
		sb = append(sb, k)
	}
	sr.add(sb)
}
