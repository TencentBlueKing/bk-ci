import assert from 'node:assert/strict'
import test from 'node:test'
import { mergeLogPanelLines, nextAfterCursor } from '../logPanelTail.js'

test('late rows overwrite by storage line number and remain timestamp ordered', () => {
    const merged = mergeLogPanelLines(
        [{ lineNo: 100, timestamp: 2000, message: 'old' }],
        [
            { lineNo: 100, timestamp: 2000, message: 'new' },
            { lineNo: 50, timestamp: 1500, message: 'late' }
        ]
    )
    assert.deepEqual(merged.map(item => item.lineNo), [50, 100])
    assert.equal(merged[1].message, 'new')
})

test('after cursor uses maxima instead of the sorted last element', () => {
    assert.deepEqual(nextAfterCursor([
        { lineNo: 200, timestamp: 1000 },
        { lineNo: 100, timestamp: 3000 }
    ]), {
        startLineNo: 200,
        sinceTimestamp: 3000
    })
})

test('empty polling and duplicate lookbacks share the same 100k-row array and rows', () => {
    const lines = Array.from({ length: 100000 }, (_, index) => ({
        lineNo: index + 1, timestamp: index + 1, message: 'unchanged'
    }))
    assert.equal(mergeLogPanelLines(lines, []), lines)
    assert.equal(mergeLogPanelLines(lines, lines.slice(-1000).map(row => ({ ...row }))), lines)
    const [changed, unchanged] = lines.slice(-2)
    const merged = mergeLogPanelLines(lines, [{ ...changed, message: 'updated' }, { ...unchanged }])
    assert.notEqual(merged, lines)
    assert.equal(merged[0], lines[0])
    assert.equal(merged.at(-1), unchanged)
    assert.equal(merged.at(-2).message, 'updated')
})
