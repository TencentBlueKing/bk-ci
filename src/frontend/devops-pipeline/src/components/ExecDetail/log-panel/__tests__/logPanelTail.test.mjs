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
