import assert from 'node:assert/strict'
import { test } from 'node:test'
import {
    getOverflowLength,
    getVariableValueIssue,
    getVariableValueText,
    isOverflowReference,
    VAR_MAX_LENGTH
} from './variableValue.js'

test('startup and default values accept 4000 characters and reject 4001', () => {
    for (const char of ['a', '中', '\n']) {
        assert.equal(getVariableValueIssue(char.repeat(VAR_MAX_LENGTH)), null)
        assert.deepEqual(getVariableValueIssue(char.repeat(VAR_MAX_LENGTH + 1)), {
            reference: false, length: 4001
        })
    }
})

test('character limit is independent of UTF-8 byte size and matches JVM UTF-16 length', () => {
    assert.equal(getVariableValueIssue('中'.repeat(4000)), null)
    assert.equal(getVariableValueIssue('😀'.repeat(2000)), null)
    assert.equal(getVariableValueIssue('😀'.repeat(2001)).length, 4002)
})

test('both legacy and current references are rejected even when shorter than 4000', () => {
    for (const reference of ['__BK_OVF__:9000', `__BK_OVF__:9000:2:${'a'.repeat(64)}`]) {
        assert.equal(isOverflowReference(reference), true)
        assert.equal(getOverflowLength(reference), 9000)
        assert.deepEqual(getVariableValueIssue(reference), { reference: true, length: 9000 })
    }
})

test('malformed references remain unavailable instead of becoming business values', () => {
    for (const value of ['__BK_OVF__:', '__BK_OVF__:bad:1:hash', '__BK_OVF__:9007199254740992']) {
        assert.equal(getOverflowLength(value), null)
        assert.equal(getVariableValueIssue(value).reference, true)
    }
    assert.equal(getOverflowLength('plain text'), null)
})

test('multi-select values are checked after comma serialization', () => {
    const values = ['a'.repeat(2000), 'b'.repeat(2000)]
    assert.equal(getVariableValueText(values).length, 4001)
    assert.equal(getVariableValueIssue(values).length, 4001)
    assert.equal(getVariableValueIssue(values.join(',')).length, 4001)
})

test('object values are checked as JSON, not as [object Object]', () => {
    const value = { text: 'a'.repeat(4000) }
    assert.equal(getVariableValueText(value), JSON.stringify(value))
    assert.equal(getVariableValueIssue(value).length, JSON.stringify(value).length)
})

test('empty, boolean and numeric values retain their existing meaning', () => {
    for (const value of [null, undefined, '', true, false, 0, 123]) {
        assert.equal(getVariableValueIssue(value), null)
    }
    assert.equal(getVariableValueText(false), 'false')
    assert.equal(getVariableValueText(0), '0')
})

test('validation does not truncate, resolve or clear historical values', () => {
    const values = ['a'.repeat(5000), '__BK_OVF__:9000']
    const before = [...values]
    values.forEach(getVariableValueIssue)
    assert.deepEqual(values, before)
})
