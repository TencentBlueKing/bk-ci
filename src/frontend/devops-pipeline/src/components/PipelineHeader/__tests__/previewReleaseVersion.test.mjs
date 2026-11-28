import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { createRequire } from 'node:module'
import test from 'node:test'

const require = createRequire(import.meta.url)
const Vue = require('vue/dist/vue.common.js')
const source = readFileSync(new URL('../PreviewHeader.vue', import.meta.url), 'utf8')
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
    .replace(/^\s*import[\s\S]*?\sfrom\s+['"][^'"]+['"];?\s*$/gm, '')
    .replace(/export default\s*\{/, 'return {')
const state = {
    executeStatus () { return false },
    pipelineInfo () { return { pipelineName: 'CI', releaseVersion: 7, permissions: {} } }
}
const getters = {
    isDebugPipeline: false,
    isBranchVersion: false,
    isReleaseVersion: false,
    pacEnabled: false
}
const options = new Function('mapState', 'mapGetters', 'mapActions', 'PipelineBreadCrumb', 'PacBranchSelector', script)(
    () => state,
    () => ({}),
    () => ({}),
    {},
    {}
)

function previewHeader (overrides = {}) {
    const navigations = []
    const vm = new Vue({
        ...options,
        computed: {
            ...options.computed,
            ...Object.fromEntries(Object.entries({ ...getters, ...overrides }).map(([key, value]) => [key, () => value]))
        },
        beforeCreate () {
            this.$route = {
                name: 'executePreview',
                params: { projectId: 'project', pipelineId: 'pipeline', version: '6' },
                query: { tab: 'params' }
            }
            this.$router = { replace: destination => navigations.push(destination) }
            this.$t = key => key
        }
    })
    return { vm, navigations }
}

test('switching in an older preview selects the latest release and keeps the route context', () => {
    const { vm, navigations } = previewHeader()
    assert.equal(vm.execDisabled, true)
    vm.switchToReleaseVersion()
    assert.deepEqual(navigations, [{
        name: 'executePreview',
        params: { projectId: 'project', pipelineId: 'pipeline', version: 7 },
        query: { tab: 'params' }
    }])
})

test('debug, branch, and current release previews need no version switch', () => {
    for (const overrides of [{ isDebugPipeline: true }, { isBranchVersion: true }, { isReleaseVersion: true }]) {
        const { vm, navigations } = previewHeader(overrides)
        assert.equal(vm.versionNotMatch, false)
        vm.switchToReleaseVersion()
        assert.deepEqual(navigations, [])
    }
})

test('switching a PAC release reuses the branch selector', () => {
    const { vm, navigations } = previewHeader({ pacEnabled: true })
    const versions = []
    vm.$refs.pacBranchSelectorRef = {
        switchToReleaseVersion (version) {
            versions.push(version)
            return true
        }
    }
    vm.switchToReleaseVersion()
    assert.deepEqual(versions, [7])
    assert.deepEqual(navigations, [])
})

test('PAC falls back to the released history when the current list omits that release', () => {
    const { vm, navigations } = previewHeader({ pacEnabled: true })
    vm.$refs.pacBranchSelectorRef = { switchToReleaseVersion: () => false }
    vm.switchToReleaseVersion()
    assert.deepEqual(navigations, [{
        name: 'pipelinesHistory',
        params: { projectId: 'project', pipelineId: 'pipeline', version: 7, type: 'history' },
        query: { tab: 'params' }
    }])
})
