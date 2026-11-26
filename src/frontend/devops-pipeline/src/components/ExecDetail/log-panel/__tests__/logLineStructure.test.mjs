import assert from 'node:assert/strict'
import test from 'node:test'
import { buildLogStructure, isLogStructureFilterEligible, projectLogStructure, resolveLogGroupAncestors } from '../logLineStructure.js'
import { findLogMatchIds, getLogIssueLevel, getLogText, renderLogHtml, sampleLogIssueMarks } from '../logPanelAdapter.js'

function line (lineNo, text, level = 'INFO') {
    return { lineNo, timestamp: 1000 + lineNo, level, text }
}

test('nested groups use context and source identity, project collapsed intervals, and reveal all ancestors', () => {
    const logs = [
        line(1, '##[group]outer'), line(2, 'outer body'),
        line(3, '##[group]inner'), line(4, 'inner body'), line(5, '##[endgroup]'),
        line(6, 'outer tail'), line(7, '##[endgroup]')
    ]
    const model = buildLogStructure(logs, { context: 'stepA|3', complete: true })
    assert.equal(model.groups.length, 2)
    assert.equal(model.groups[0].title, 'outer')
    assert.notEqual(model.groups[0].id, buildLogStructure(logs, { context: 'stepB|3', complete: true }).groups[0].id)
    assert.deepEqual(resolveLogGroupAncestors(model, 4), [model.groups[1].id, model.groups[0].id])
    const expanded = projectLogStructure(model, { enabled: true })
    assert.deepEqual(expanded.map(row => row.lineNo), [1, 2, 3, 4, 5, 6, 7], 'expanded groups keep end marker rows like the legacy viewer')
    assert.deepEqual(resolveLogGroupAncestors(model, 5), [model.groups[1].id, model.groups[0].id])
    const collapsed = projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] })
    assert.deepEqual(collapsed.map(row => row.lineNo), [1])
    assert.equal(collapsed[0]._logGroup.title, 'outer')
    assert.equal(projectLogStructure(model, { enabled: false }).length, logs.length)
})

test('group projections preserve source text and keep formatted title parsing consistent', () => {
    const source = '##[group]\u001b[31mRED\u001b[0m <a href="https://example.com">build link</a>'
    const logs = [line(1, source), line(2, 'body'), line(3, '##[endgroup]')]
    const projected = projectLogStructure(buildLogStructure(logs, { complete: true }), { enabled: true })
    const header = projected[0]
    const html = renderLogHtml(header, 'RED')
    assert.equal(header.text, source)
    assert.equal(header._logGroup.title, 'RED build link')
    assert.equal(getLogText(header), 'RED build link')
    assert.match(html, /<span style="color:#ff6666"><mark class="lp-hl">RED<\/mark><\/span>/)
    assert.match(html, /<a href="https:\/\/example\.com\/" target="_blank" rel="noopener noreferrer">build link<\/a>/)
    assert.equal(logs[0].text, source, 'format projection does not alter the source record')
    const collapsed = projectLogStructure(buildLogStructure(logs, { complete: true }), {
        enabled: true,
        collapsedIds: [buildLogStructure(logs, { complete: true }).groups[0].id]
    })
    assert.equal(getLogText(collapsed[0]), getLogText(header))
    assert.equal(renderLogHtml(collapsed[0], 'RED'), html)
})

test('group title marker text stays literal and issue classification matches minimap projection', () => {
    const logs = [line(1, '##[group]##[error]literal title'), line(2, 'body'), line(3, '##[endgroup]')]
    const model = buildLogStructure(logs, { complete: true })
    const expanded = projectLogStructure(model, { enabled: true })
    const collapsed = projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] })
    for (const header of [expanded[0], collapsed[0]]) {
        assert.equal(header.text, logs[0].text)
        assert.equal(getLogText(header), '##[error]literal title')
        assert.equal(getLogIssueLevel(header), '')
    }
    assert.deepEqual(sampleLogIssueMarks(expanded), [])
    assert.deepEqual(sampleLogIssueMarks(collapsed), [])
})

test('end markers with message content remain searchable, locatable, and count as hidden group issues', () => {
    const logs = [
        line(1, '##[group]build'),
        line(2, 'working'),
        line(3, '##[endgroup]fatal closing summary', 'ERROR'),
        line(4, 'after group')
    ]
    const model = buildLogStructure(logs, { context: 'job|step', complete: true })
    const groupId = model.groups[0].id
    const expanded = projectLogStructure(model, { enabled: true })
    assert.deepEqual(expanded.map(row => row.lineNo), [1, 2, 3, 4])
    assert.deepEqual(findLogMatchIds(logs, 'fatal closing summary'), [3])
    assert.deepEqual(resolveLogGroupAncestors(model, 3), [groupId])
    const collapsed = projectLogStructure(model, { enabled: true, collapsedIds: [groupId] })
    assert.deepEqual(collapsed.map(row => row.lineNo), [1, 4])
    assert.deepEqual(collapsed[0]._logGroup.issueCounts, { error: 1, warn: 0 })
})

test('collapsed groups retain visible source numbering and aggregate hidden issue markers without changing source logs', () => {
    const logs = [
        line(10, '##[group]build'),
        line(11, 'compiler failed', 'ERROR'),
        line(12, 'slow retry', 'WARN'),
        line(13, '##[endgroup]'),
        line(14, 'outside failure', 'ERROR')
    ].map((row, index) => ({ ...row, displayLineNo: 41 + index }))
    const model = buildLogStructure(logs, { context: 'job|step', complete: true })
    const collapsed = projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] })

    assert.deepEqual(collapsed.map(row => row.displayLineNo), [41, 45])
    assert.deepEqual(collapsed[0]._logGroup.issueCounts, { error: 1, warn: 1 })
    assert.deepEqual(logs.map(row => row.lineNo), [10, 11, 12, 13, 14], 'source cursor numbers remain untouched')
    assert.equal(collapsed[1].text, 'outside failure')
})

test('orphan, unclosed, or same-record malformed groups remain visible and unfurled', () => {
    const malformed = [
        line(10, '##[endgroup]orphan'),
        line(11, '##[group]unterminated'),
        line(12, 'both ##[group] and ##[endgroup]')
    ]
    const model = buildLogStructure(malformed, { context: 'x', complete: true })
    assert.equal(projectLogStructure(model, { enabled: true }).length, malformed.length)
    assert.equal(model.groups[0].complete, false)
})

test('build-wide line number gaps between loaded rows do not break group pairing', () => {
    const logs = [
        line(1, '##[group]crosses other task lines'),
        line(3, 'inside'),
        line(7, '##[endgroup]'),
        line(9, 'after group')
    ]
    const model = buildLogStructure(logs, { context: 'x', complete: false })
    assert.equal(model.groups[0].complete, true)
    assert.deepEqual(projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] }).map(getLogText), [
        'crosses other task lines', 'after group'
    ])
})

test('ALL sub-tags keep interleaved groups foldable when timestamp order differs from line numbers', () => {
    const logs = [
        { lineNo: 20, timestamp: 2000, level: 'INFO', subTag: 'compile', text: '##[group]compile' },
        { lineNo: 22, timestamp: 2001, level: 'INFO', subTag: 'test', text: '##[group]test' },
        { lineNo: 21, timestamp: 2002, level: 'INFO', subTag: 'compile', text: 'compile body' },
        { lineNo: 25, timestamp: 2003, level: 'INFO', subTag: 'test', text: 'test body' },
        { lineNo: 24, timestamp: 2004, level: 'INFO', subTag: 'test', text: '##[endgroup]' },
        { lineNo: 23, timestamp: 2005, level: 'INFO', subTag: 'compile', text: '##[endgroup]' },
        { lineNo: 30, timestamp: 2006, level: 'INFO', subTag: 'deploy', text: 'tail' }
    ]
    assert.equal(isLogStructureFilterEligible(['INFO', 'WARN', 'ERROR', 'DEBUG']), true)
    const model = buildLogStructure(logs, { context: 'job|step' })
    assert.deepEqual(model.groups.map(group => [group.title, group.complete]), [['compile', true], ['test', true]])
    const outer = model.groups[0].id
    assert.deepEqual(projectLogStructure(model, { enabled: true, collapsedIds: [outer] }).map(getLogText), ['compile', 'tail'])
})

test('ambiguous multi-marker rows invalidate open groups but later complete groups still fold', () => {
    const logs = [
        line(1, '##[group]outer'),
        line(2, '##[endgroup] ##[group]mixed'),
        line(3, 'outside-but-new-group-body'),
        line(4, '##[group]later reliable group'),
        line(5, 'later body'),
        line(6, '##[endgroup]')
    ]
    const model = buildLogStructure(logs, { context: 'x', complete: true })
    assert.equal(model.groups[0].complete, false)
    assert.equal(model.groups[1].complete, true)
    const projected = projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id, model.groups[1].id] })
    assert.deepEqual(projected.map(row => row.lineNo), [1, 2, 3, 4])
})

test('a complete group can pair across adjacent loaded page boundaries', () => {
    const beforePage = [line(999, '##[group]before page boundary'), line(1000, 'first page body')]
    const afterPage = [line(1001, 'second page body'), line(1002, '##[endgroup]')]
    const model = buildLogStructure([...beforePage, ...afterPage], { context: 'page-boundary' })
    assert.equal(model.groups[0].complete, true)
    assert.deepEqual(projectLogStructure(model, { enabled: true, collapsedIds: [model.groups[0].id] }).map(row => row.lineNo), [999])
})

test('folding is available whenever INFO level group markers are loaded, including default levels and sub-tags', () => {
    assert.equal(isLogStructureFilterEligible(['INFO', 'WARN', 'ERROR']), true)
    assert.equal(isLogStructureFilterEligible(['INFO', 'WARN', 'ERROR', 'DEBUG']), true)
    assert.equal(isLogStructureFilterEligible(['info']), true)
    assert.equal(isLogStructureFilterEligible(['WARN', 'ERROR']), false)
    assert.equal(isLogStructureFilterEligible([]), false)
    assert.equal(isLogStructureFilterEligible(null), false)
})
