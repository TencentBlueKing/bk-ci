import { getLogIssueLevel, getRawLogText } from './logPanelAdapter.js'
import { parseLogLine } from './logLineFormat.js'

// lineNo 由后端按构建全局分配、列表按 timestamp 优先排序，并行任务会让同一插件的行号出现间隙或倒序；
// 已加载片段本身由游标查询保证连续，因此不以行号是否相邻判断分组完整性。
export function buildLogStructure (logs = [], { context = '', complete = false } = {}) {
    const groups = []
    const stack = []
    const rows = []
    const byId = new Map()
    const sourceIndexes = new Map()
    let continuous = true
    let segment = 0

    logs.forEach((line, index) => {
        const parsed = parseLogLine(line)
        const raw = getRawLogText(line)
        const hasMultipleGroupMarkers = (raw.match(/##\[(?:group|endgroup)\]/gi) || []).length > 1
        const marker = parsed.marker?.type
        const row = { line, index, marker: marker || '', groupId: '', literal: false }
        rows.push(row)
        sourceIndexes.set(String(line.lineNo ?? line.line), index)
        if (!['group', 'endgroup'].includes(marker)) return
        if (hasMultipleGroupMarkers) {
            row.literal = true
            while (stack.length) stack.pop().complete = false
            continuous = false
            segment += 1
            return
        }
        if (marker === 'group') {
            const sourceKey = parsed.sourceKey || `${line.lineNo ?? line.line}:${line.timestamp ?? ''}`
            const group = {
                id: `${context}|${sourceKey}`,
                sourceKey,
                title: parsed.plainText,
                startIndex: index,
                endIndex: -1,
                parentId: stack.length ? stack[stack.length - 1].id : '',
                segment,
                complete: false
            }
            groups.push(group)
            byId.set(group.id, group)
            row.groupId = group.id
            row.groupHeader = group
            stack.push(group)
        } else if (stack.length) {
            const group = stack.pop()
            group.endIndex = index
            group.complete = group.segment === segment
            row.groupId = group.id
            row.groupEnd = group
        } else {
            row.literal = true
        }
    })

    groups.forEach(group => {
        if (group.endIndex < 0 || group.startIndex >= group.endIndex) group.complete = false
    })
    return { logs, rows, groups, byId, sourceIndexes, complete: Boolean(complete && continuous) }
}

function groupDepth (group, groupsById) {
    let depth = 0
    let current = group
    while (current?.parentId && depth < 64) {
        depth += 1
        current = groupsById.get(current.parentId)
    }
    return depth
}

export function projectLogStructure (model, { enabled = false, collapsedIds = [] } = {}) {
    const collapsed = new Set(collapsedIds)
    const completeGroups = (model?.groups || []).filter(group => group.complete)
    const starts = new Map(completeGroups.map(group => [group.startIndex, group]))
    const ends = new Map(completeGroups.map(group => [group.endIndex, group]))
    const hiddenIssues = new Map()
    if (enabled && collapsed.size) {
        const active = []
        ;(model?.rows || []).forEach(row => {
            const start = starts.get(row.index)
            if (start) active.push(start)
            const issueLevel = getLogIssueLevel(row.line)
            if (issueLevel && row.index > 0 && active.length) {
                const owner = active.find(group => collapsed.has(group.id) && row.index <= group.endIndex)
                if (owner && row.index !== owner.startIndex) {
                    const counts = hiddenIssues.get(owner.id) || { error: 0, warn: 0 }
                    counts[issueLevel] += 1
                    hiddenIssues.set(owner.id, counts)
                }
            }
            const end = ends.get(row.index)
            if (end) {
                const activeIndex = active.findIndex(group => group.id === end.id)
                if (activeIndex >= 0) active.splice(activeIndex, 1)
            }
        })
    }
    const visible = []
    let skipThrough = -1
    ;(model?.rows || []).forEach(row => {
        if (row.index <= skipThrough) return
        const group = enabled && !row.literal ? starts.get(row.index) : null
        if (group) {
            const isCollapsed = collapsed.has(group.id)
            visible.push({
                ...row.line,
                _logGroup: {
                    id: group.id,
                    title: group.title,
                    collapsed: isCollapsed,
                    depth: groupDepth(group, model.byId),
                    issueCounts: hiddenIssues.get(group.id) || { error: 0, warn: 0 },
                    startLineNo: model.rows[group.startIndex]?.line.lineNo,
                    endLineNo: model.rows[group.endIndex]?.line.lineNo
                }
            })
            if (isCollapsed) skipThrough = group.endIndex
            return
        }
        visible.push(row.line)
    })
    return visible
}

export function resolveLogGroupAncestors (model, lineNo) {
    const index = model?.sourceIndexes?.get(String(lineNo))
    if (!Number.isInteger(index)) return []
    let innermost = null
    model.groups.forEach(group => {
        if (group.complete && group.startIndex < index && index <= group.endIndex
            && (!innermost || group.startIndex > innermost.startIndex)) innermost = group
    })
    const ancestors = []
    while (innermost) {
        ancestors.push(innermost.id)
        innermost = innermost.parentId ? model.byId.get(innermost.parentId) : null
    }
    return ancestors
}

// 后端 addFoldStartLine/addFoldEndLine 固定以 INFO 级别输出分组标记，勾选 INFO 即可保证标记完整加载。
export function isLogStructureFilterEligible (levels = []) {
    return (Array.isArray(levels) ? levels : []).some(level => String(level || '').toUpperCase() === 'INFO')
}
