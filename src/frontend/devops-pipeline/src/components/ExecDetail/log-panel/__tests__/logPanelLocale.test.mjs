import assert from 'node:assert/strict'
import { readFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import test from 'node:test'

const __dirname = dirname(fileURLToPath(import.meta.url))
const localeDir = join(__dirname, '../../../../../../locale/pipeline')

function collectLeafPaths (obj, prefix = '') {
    const paths = []
    for (const [key, value] of Object.entries(obj || {})) {
        const path = prefix ? `${prefix}.${key}` : key
        if (value && typeof value === 'object' && !Array.isArray(value)) {
            paths.push(...collectLeafPaths(value, path))
        } else {
            paths.push(path)
        }
    }
    return paths.sort()
}

function loadLocale (name) {
    return JSON.parse(readFileSync(join(localeDir, name), 'utf8'))
}

test('logPanel locale structure is identical across zh-CN, en-US, and ja-JP', () => {
    const zh = loadLocale('zh-CN.json').logPanel
    const en = loadLocale('en-US.json').logPanel
    const ja = loadLocale('ja-JP.json').logPanel
    assert.deepEqual(collectLeafPaths(zh), collectLeafPaths(en))
    assert.deepEqual(collectLeafPaths(zh), collectLeafPaths(ja))
})

test('logPanel clean and closed fallbacks share logExpired copy in zh-CN', () => {
    const logPanel = loadLocale('zh-CN.json').logPanel
    const expected = '构建日志已超过保留期，已被清理，无法查看。'
    assert.equal(logPanel.logExpired, expected)
    assert.notEqual(logPanel.logExpired, '日志已过期')
})

test('logPanel setupJob uses natural Japanese in ja-JP', () => {
    const logPanel = loadLocale('ja-JP.json').logPanel
    assert.equal(logPanel.setupJob, 'Job のセットアップ')
    assert.notEqual(logPanel.setupJob, 'Set up job')
})

test('logPanel runningNode is localized in all supported locales', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.runningNode, '运行节点')
    assert.equal(loadLocale('en-US.json').logPanel.runningNode, 'Execution node')
    assert.equal(loadLocale('ja-JP.json').logPanel.runningNode, '実行ノード')
})

test('execution count label is localized with a count placeholder', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.executeCountLabel, '第 {0} 次执行')
    assert.equal(loadLocale('en-US.json').logPanel.executeCountLabel, 'Execution {0}')
    assert.equal(loadLocale('ja-JP.json').logPanel.executeCountLabel, '第{0}回実行')
})

test('Job failure step attribution is localized in all supported locales', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.status.failedByStep, '因步骤『{0}』失败')
    assert.equal(loadLocale('en-US.json').logPanel.status.failedByStep, 'Failed because step ‘{0}’ failed')
    assert.equal(loadLocale('ja-JP.json').logPanel.status.failedByStep, 'ステップ「{0}」の失敗により失敗しました')
})

test('logPanel subtaskSummary follows REQ completed n/m copy', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.subtaskSummary, '已完成 {0}/{1} 子任务')
    assert.equal(loadLocale('en-US.json').logPanel.subtaskSummary, 'Completed {0}/{1} subtasks')
    assert.equal(loadLocale('ja-JP.json').logPanel.subtaskSummary, '完了 {0}/{1} サブタスク')
})

test('logPanel structure has localized hidden issue summaries for collapsed groups', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.structure.hiddenIssues, '组内隐藏错误 {0} 条，告警 {1} 条')
    assert.equal(loadLocale('en-US.json').logPanel.structure.hiddenIssues, 'Hidden issues: {0} errors, {1} warnings')
    assert.equal(loadLocale('ja-JP.json').logPanel.structure.hiddenIssues, 'グループ内の非表示エラー {0} 件、警告 {1} 件')
})

test('logPanel params load-failed copy exists in all supported locales', () => {
    assert.equal(loadLocale('zh-CN.json').logPanel.params.loadFailed, '历史执行参数加载失败')
    assert.equal(loadLocale('en-US.json').logPanel.params.loadFailed, 'Failed to load historical execution parameters')
    assert.equal(loadLocale('ja-JP.json').logPanel.params.loadFailed, '過去の実行パラメータの読み込みに失敗しました')
    assert.equal(loadLocale('zh-CN.json').logPanel.params.retry, '重试')
    assert.equal(loadLocale('en-US.json').logPanel.params.retry, 'Retry')
    assert.equal(loadLocale('ja-JP.json').logPanel.params.retry, '再試行')
})

test('log group folding has no extra filter notice, matching the legacy viewer', () => {
    for (const locale of ['zh-CN.json', 'en-US.json', 'ja-JP.json']) {
        assert.equal(loadLocale(locale).logPanel.structure.filterNotice, undefined)
    }
})
