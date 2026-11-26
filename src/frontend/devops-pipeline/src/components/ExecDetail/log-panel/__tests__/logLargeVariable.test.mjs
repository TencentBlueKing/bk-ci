import assert from 'node:assert/strict'
import test from 'node:test'
import {
    isLargeVariableRef,
    parseLargeVariableLength,
    parseLargeVariableLogLine,
    resolveLargeVariableResult
} from '../logLargeVariable.js'

test('parses output overflow and except placeholder lines as queryable', () => {
    assert.deepEqual(parseLargeVariableLogLine('output(overflow): MY_JSON=__BK_OVF__:204800'), {
        kind: 'overflow', key: 'MY_JSON', length: 204800, label: '', queryable: true
    })
    assert.deepEqual(parseLargeVariableLogLine('output(except):  BIG_VAR =__BK_OVF__:5000  '), {
        kind: 'except', key: 'BIG_VAR', length: 5000, label: '', queryable: true
    })
})

test('parses input placeholder lines as not queryable', () => {
    const result = parseLargeVariableLogLine('input(normal): (Script)SCRIPT=__BK_OVF__:8000')
    assert.equal(result.queryable, false)
    assert.equal(result.key, 'SCRIPT')
    assert.equal(result.length, 8000)
    assert.equal(result.label, '(Script)')
})

test('ignores ordinary log lines and malformed placeholders', () => {
    assert.equal(parseLargeVariableLogLine('output(normal): A=1'), null)
    assert.equal(parseLargeVariableLogLine('output(overflow): A=__BK_OVF__:abc'), null)
    assert.equal(parseLargeVariableLogLine('echo output(overflow): A=__BK_OVF__:1'), null)
    assert.equal(parseLargeVariableLogLine(undefined), null)
})

test('keeps html-sensitive keys raw so the renderer escapes them', () => {
    const result = parseLargeVariableLogLine('output(overflow): <b>"x"=__BK_OVF__:10')
    assert.equal(result.key, '<b>"x"')
})

test('reads length from log and api reference forms', () => {
    assert.equal(isLargeVariableRef('__BK_OVF__:12'), true)
    assert.equal(isLargeVariableRef('plain'), false)
    assert.equal(parseLargeVariableLength('__BK_OVF__:204800'), 204800)
    assert.equal(parseLargeVariableLength('__BK_OVF__:204800:abc:def'), 204800)
    assert.equal(parseLargeVariableLength('nope'), null)
})

test('classifies variable value responses', () => {
    assert.deepEqual(resolveLargeVariableResult(null), { state: 'notFound', value: '' })
    assert.deepEqual(resolveLargeVariableResult(undefined), { state: 'notFound', value: '' })
    assert.deepEqual(resolveLargeVariableResult('******'), { state: 'sensitive', value: '******' })
    assert.deepEqual(resolveLargeVariableResult('__BK_OVF__:10:a:b'), { state: 'expired', value: '' })
    assert.deepEqual(resolveLargeVariableResult('{"a":1}'), { state: 'ready', value: '{"a":1}' })
    assert.deepEqual(resolveLargeVariableResult(''), { state: 'ready', value: '' })
})
