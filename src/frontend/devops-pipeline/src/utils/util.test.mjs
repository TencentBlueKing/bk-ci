import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { test } from 'node:test'

// Load the production utility module without bundler-only dependencies, which
// are not used by the pure parameter comparison helpers exercised here.
const source = (await readFile(new URL('./util.js', import.meta.url), 'utf8'))
    .replace(/^import[\s\S]*?from ['"][^'"]+['"]\r?\n/gm, '')
const { isParamValueEqual } = await import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)

test('template upgrades compare changed inherited and overridden defaults without throwing', () => {
    assert.equal(isParamValueEqual('parent-v2', 'parent-v1'), false)
    assert.equal(isParamValueEqual('instance-local-keep', 'parent-v2-local'), false)
    assert.equal(isParamValueEqual('parent-v2', 'parent-v2'), true)
    assert.equal(isParamValueEqual('a'.repeat(4000), 'b'.repeat(4000)), false)
})

test('boolean and empty-value semantics are preserved', () => {
    for (const [a, b] of [[false, 'false'], [true, 'true'], ['', null], [undefined, null]]) {
        assert.equal(isParamValueEqual(a, b), true)
        assert.equal(isParamValueEqual(b, a), true)
    }
    for (const value of ['hello', 0, null, {}, []]) {
        assert.equal(isParamValueEqual(false, value), false)
        assert.equal(isParamValueEqual(value, false), false)
    }
    assert.equal(isParamValueEqual(false, true), false)
    assert.equal(isParamValueEqual('', 'parent-v2'), false)
    assert.equal(isParamValueEqual(0, '0'), true)
})

test('form-list defaults compare arrays and JSON arrays independent of object key order', () => {
    const value = [{ name: 'row', config: { enabled: false, count: 0 }, items: [1, 2] }]
    const reordered = [{ items: [1, 2], config: { count: 0, enabled: false }, name: 'row' }]
    const before = structuredClone(value)
    assert.equal(isParamValueEqual(value, reordered), true)
    assert.equal(isParamValueEqual(value, JSON.stringify(reordered)), true)
    assert.equal(isParamValueEqual(JSON.stringify(value), reordered), true)
    assert.equal(isParamValueEqual(JSON.stringify(value), JSON.stringify(reordered)), true)
    assert.deepEqual(value, before)
    assert.equal(isParamValueEqual([1, 2], [2, 1]), false)
    assert.equal(isParamValueEqual([false], [0]), false)
    assert.equal(isParamValueEqual([], '[]'), true)
    assert.equal(isParamValueEqual(value, '[invalid JSON'), false)
    assert.equal(isParamValueEqual('ordinary text', '[invalid JSON'), false)
})

test('repository objects retain shallow comparison semantics', () => {
    assert.equal(isParamValueEqual({ repo: 'a', branch: 'main' }, { branch: 'main', repo: 'a' }), true)
    assert.equal(isParamValueEqual({ repo: 'a' }, { repo: 'b' }), false)
    assert.equal(isParamValueEqual({ repo: 'a' }, { repo: 'a', branch: 'main' }), false)
})
