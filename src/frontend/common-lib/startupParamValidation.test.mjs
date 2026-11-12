import assert from 'node:assert/strict'
import { readFile } from 'node:fs/promises'
import { stripTypeScriptTypes } from 'node:module'
import { test } from 'node:test'

// Execute both production validators with the same cases, without a browser or bundler.
const sharedUrl = new URL('./variableValue.js', import.meta.url).href
async function loadValidator(path, typescript = false) {
    let source = await readFile(new URL(path, import.meta.url), 'utf8')
    if (typescript) source = stripTypeScriptTypes(source)
    source = source.replace('../../../common-lib/variableValue', sharedUrl)
    return import(`data:text/javascript;base64,${Buffer.from(source).toString('base64')}`)
}

const pipeline = await loadValidator('../devops-pipeline/src/utils/startupParamValidation.js')
const flow = await loadValidator('../devops-flow/src/utils/startupParamValidation.ts', true)
const translate = (key) => key
globalThis.window = { pipelineVue: { $i18n: { t: translate } } }

for (const [name, validate] of [
    ['pipeline', values => pipeline.validateStartupValues(values)],
    ['flow', values => flow.validateStartupValues(values, translate)]
]) {
    test(`${name}: final payload guard checks constants and restored combinations without mutation`, () => {
        const payload = { INPUT: 'ok', READ_ONLY: 'a'.repeat(4001), HIDDEN: false }
        const before = structuredClone(payload)
        assert.throws(() => validate(payload), /LengthLimit|valueTooLong/)
        assert.deepEqual(payload, before)
        payload.READ_ONLY = 'a'.repeat(4000)
        assert.doesNotThrow(() => validate(payload))
    })

    test(`${name}: legacy references cannot be reused as startup values`, () => {
        assert.throws(() => validate({ PREVIOUS: '__BK_OVF__:8000:2:hash' }), /OverflowReference|overflowReference/)
        assert.doesNotThrow(() => validate({ EMPTY: '', BOOLEAN: false, ZERO: 0 }))
    })
}

test('pipeline: model/template drafts validate defaults regardless of failure policy', () => {
    const draft = {
        storageType: 'MODEL',
        modelAndSetting: {
            model: { stages: [{ containers: [{ params: [{ id: 'TEXT', defaultValue: 'a'.repeat(4001) }] }] }] },
            setting: { failIfVariableInvalid: false }
        }
    }
    assert.throws(() => pipeline.validateDraftDefaults(draft), /paramLengthLimitTips/)
    draft.modelAndSetting.setting.failIfVariableInvalid = true
    assert.throws(() => pipeline.validateDraftDefaults(draft), /paramLengthLimitTips/)
    // A code draft can contain a stale model; the server validates its actual YAML.
    draft.storageType = 'YAML'
    assert.doesNotThrow(() => pipeline.validateDraftDefaults(draft))
})
