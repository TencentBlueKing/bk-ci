import assert from 'node:assert/strict'
import test from 'node:test'
import { runInNewContext } from 'node:vm'
import { existsSync, readFileSync } from 'node:fs'
import { findLogMatchIds, getLogIssueLevel, getLogText, renderLogHtml } from '../logPanelAdapter.js'
import { createLogPanelFormatStressLogs, createLogPanelSearchStressLogs, filterLogPanelDevMockLogs, resolveLogPanelDevMock } from './logPanelDevMock.fixture.mjs'
import { parseLogLine, renderFormattedLogHtml } from '../logLineFormat.js'

test('100k format fixture is development-only, deterministic, and keeps the 10k search fixture', () => {
    const first = createLogPanelFormatStressLogs('development')
    const second = createLogPanelFormatStressLogs('development')
    assert.equal(createLogPanelFormatStressLogs('production'), null)
    assert.equal(first.length, 100000)
    assert.deepEqual(first[49999], second[49999])
    assert.deepEqual(new Set(first.map(row => row.level)), new Set(['INFO', 'WARN', 'ERROR', 'DEBUG']))
    assert.deepEqual(new Set(first.map(row => row.subTag)), new Set(['compile', 'test', 'deploy']))
    assert.equal(first[49999].text.length, 10000)
    assert.match(first[49999].text, /格式压力中文定位词/)
    assert.equal(first[109].text, 'nested group unique search needle')
    assert.match(first[99].text, /^##\[group\].*outer group.*https:\/\/example\.com\/outer-build/)
    assert.equal(first[114].text, '##[endgroup] closing summary needle')
    assert.equal(first[99989].text, '末尾中文样例：收尾完成')
    assert.equal(first[99990].text, '##[error]末尾错误标记')
    assert.equal(first[99991].text, '##[group]malformed open group')
    assert.equal(first[99998].text, '孤立结构后仍可见')
    assert.equal(first[99999].text, '##[warning]最后一行标记 MOCK_DEVOPS_LINK <a href="https://devops.example.com">https://devops.example.com</a>')
    assert.equal(getLogIssueLevel(first[99999]), 'warn')
    assert.equal(first[998].text, '##[group]before page boundary')
    assert.equal(first[1001].text, '##[endgroup]')
    assert.equal(resolveLogPanelDevMock('format-stress-100k', 'development').scope, 'step')
    assert.equal(resolveLogPanelDevMock('format-stress-100k', 'production'), null)
    assert.equal(createLogPanelSearchStressLogs('development').length, 10000)
})

test('search and format stress fixtures expose the requested safe development link at source line 9', () => {
    const fixtures = [
        {
            logs: createLogPanelSearchStressLogs('development'),
            expectedCount: 10000,
            defaultTargets: [9, 5000, 10000],
            metadata: { 9: ['INFO', 'deploy'], 5000: ['INFO', 'test'], 10000: ['INFO', 'compile'] }
        },
        {
            logs: createLogPanelFormatStressLogs('development'),
            expectedCount: 100000,
            defaultTargets: [9],
            metadata: { 9: ['INFO', 'deploy'], 5000: ['DEBUG', 'test'], 10000: ['DEBUG', 'compile'], 100000: ['DEBUG', 'compile'] }
        }
    ]
    const allTargets = [9, 5000, 10000, 100000]
    const sample = 'MOCK_DEVOPS_LINK <a href="https://devops.example.com">https://devops.example.com</a>'
    for (const { logs, expectedCount, defaultTargets, metadata } of fixtures) {
        assert.equal(logs.length, expectedCount)
        const row = logs[8]
        assert.equal(row.lineNo, 9)
        assert.equal(row.level, 'INFO')
        assert.equal(row.subTag, 'deploy')
        assert.equal(row.text, 'MOCK_DEVOPS_LINK <a href="https://devops.example.com">https://devops.example.com</a>')
        const defaultLogs = filterLogPanelDevMockLogs(logs, ['INFO', 'WARN', 'ERROR'], '')
        assert.equal(defaultLogs.find(log => log.lineNo === 9), row)
        const allLogs = filterLogPanelDevMockLogs(logs, ['INFO', 'WARN', 'ERROR', 'DEBUG'], '')
        const expectedAllTargets = expectedCount === 10000 ? [9, 5000, 10000] : allTargets
        assert.deepEqual(findLogMatchIds(defaultLogs, 'MOCK_DEVOPS_LINK'), defaultTargets)
        assert.deepEqual(findLogMatchIds(defaultLogs, 'https://devops.example.com'), defaultTargets)
        assert.deepEqual(findLogMatchIds(allLogs, 'MOCK_DEVOPS_LINK'), expectedAllTargets)
        assert.deepEqual(findLogMatchIds(allLogs, 'https://devops.example.com'), expectedAllTargets)

        for (const lineNo of expectedAllTargets) {
            const targetRow = logs[lineNo - 1]
            assert.equal(targetRow.level, metadata[lineNo][0])
            assert.equal(targetRow.subTag, metadata[lineNo][1])
            if (lineNo !== 9) assert.ok(targetRow.text.endsWith(` ${sample}`))
            const html = renderLogHtml(targetRow, '')
            assert.match(html, /<a href="https:\/\/devops\.example\.com\/" target="_blank" rel="noopener noreferrer">/)
            assert.match(html, /<a\b[^>]*>[\s\S]*https:\/\/devops\.example\.com[\s\S]*<\/a>/)
            const minimapText = renderLogHtml(targetRow, '', false)
            assert.match(minimapText, /MOCK_DEVOPS_LINK[\s\S]*https:\/\/devops\.example\.com/)
            assert.doesNotMatch(minimapText, /<a\b/)
        }
    }
    assert.equal(createLogPanelSearchStressLogs('production'), null)
    assert.equal(createLogPanelFormatStressLogs('production'), null)
})

test('ANSI foreground segments, resets, and search across color boundaries share plain text', () => {
    const log = { lineNo: 10, text: '\u001b[31mSEC\u001b[32mRET\u001b[39m done' }
    const original = log.text
    assert.equal(getLogText(log), 'SECRET done')
    assert.equal(log.text, original)
    const html = renderLogHtml(log, 'secret')
    assert.equal((html.match(/<mark class="lp-hl">/g) || []).length, 2)
    assert.match(html, /color:#ff6666/)
    assert.match(html, /color:#7fca54/)
    assert.equal(parseLogLine('\u001b[38;5;31mplain').segments[0].color, '')
    assert.equal(parseLogLine('\u001b[38;2;1;2;31mplain').segments[0].color, '')
    assert.equal(parseLogLine('plain [31mred[m default').segments[0].color, '')
    assert.deepEqual(parseLogLine('plain [31mred[m default').segments.map(segment => segment.color), ['', '#ff6666', ''])
    assert.equal(parseLogLine('##[command]echo hello').segments[0].color, '#5fb4e1')
    assert.equal(parseLogLine('##[info]informational').segments[0].color, '#f0f1f5')
    assert.equal(parseLogLine('##[warning]careful').segments[0].color, '#e18732')
    assert.equal(parseLogLine('##[error]broken').segments[0].color, '#d25050')
    assert.equal(parseLogLine('##[debug]diagnostic').segments[0].color, '#83828c')
    const ansiColors = ['#000000', '#ff6666', '#7fca54', '#ff9c01', '#5fb4e1', '#ff00ff', '#00ffff', '#ffffff', '#83828c']
    const ansiCodes = [30, 31, 32, 33, 34, 35, 36, 37, 90]
    ansiCodes.forEach((code, index) => assert.equal(parseLogLine(`\u001b[${code}mcolor`).segments[0].color, ansiColors[index]))
    for (const code of [91, 92, 93, 94, 95, 96, 97]) assert.equal(parseLogLine(`\u001b[${code}mcolor`).segments[0].color, '')
})

test('safe anchors discard supplied attributes, reject unsafe and malformed hrefs, and do not expose URLs to search', () => {
    const log = { text: '<a href="https://example.com/a?x=1&amp;y=2" target="_self" onclick="bad()">needle &amp; text</a>' }
    const parsed = parseLogLine(log)
    assert.equal(parsed.plainText, 'needle & text')
    assert.match(renderLogHtml(log, 'needle'), /href="https:\/\/example\.com\/a\?x=1&amp;y=2" target="_blank" rel="noopener noreferrer"/)
    assert.match(renderLogHtml(log, 'needle'), /<mark class="lp-hl">needle<\/mark>/)
    assert.doesNotMatch(renderFormattedLogHtml(parsed, '', false), /<a\b/)
    for (const href of ['javascript:alert(1)', 'data:text/html,x', '/relative', '//example.com', 'https://x/&#10;bad', 'https://x/&#12xyz;']) {
        const html = renderLogHtml({ text: `<a href="${href}">visible</a>` }, '')
        assert.equal(html, 'visible')
    }
    const internal = renderLogHtml({ text: '<a href="/console/environment/proj/pipeline/node/allNode?a=1&amp;b=2#detail" onclick="bad()">internal &amp; safe</a>' }, 'safe')
    assert.match(internal, /<a href="\/console\/environment\/proj\/pipeline\/node\/allNode\?a=1&amp;b=2#detail" target="_blank" rel="noopener noreferrer">internal &amp; <mark class="lp-hl">safe<\/mark><\/a>/)
    for (const href of ['/console', '/console/', '/console?x=1', '/console#detail', '/console?', '/console/a#', '/console?#x', '/console/x?next=https%3A%2F%2Fevil.test%2Fa%3Fb%3D1']) {
        assert.match(renderLogHtml({ text: `<a href="${href}">console link</a>` }, ''), /<a href="\/console/)
    }
    for (const href of ['/console?', '/console/a#', '/console?#x']) {
        assert.match(renderLogHtml({ text: `<a href="${href}">delimiter</a>` }, ''), new RegExp(`href="${href.replace(/[?#[\]\\]/g, '\\$&')}"`))
    }
    for (const href of [
        '/consolex', '/console-evil', 'console/x', '/admin/console/x',
        '/console/../admin', '/console/%2e%2e/admin', '/console/.%2e/admin',
        '/console/%2fadmin', '/console/%5cadmin', '/console/%252e%252e/admin', '/console/%252fadmin',
        '/console\\admin', '/console/%0aadmin', '/console/%7fadmin', '/console/a&#x85;b', '/console/a%C2%85b', `/console/a\u0085b`, '//console.example/path'
    ]) {
        const html = renderLogHtml({ text: `<a href="${href}">blocked</a>` }, '')
        assert.equal(html, 'blocked')
    }
    const queryEncodedUrl = renderLogHtml({ text: '<a href="/console/x?redirect=https%3A%2F%2Fevil.test%2F%252fadmin">query</a>' }, '')
    assert.match(queryEncodedUrl, /href="\/console\/x\?redirect=https%3A%2F%2Fevil\.test%2F%252fadmin"/)
    assert.match(renderLogHtml({ text: '<a href="/console/中文">valid unicode</a>' }, ''), /href="\/console\/%E4%B8%AD%E6%96%87"/)
    assert.doesNotMatch(renderLogHtml('<a href="/console/x">text</a>', ''), /<a\b/)
    const fakeAttribute = renderLogHtml({ text: `<a title='href="https://example.com/"'>text</a>` }, '')
    assert.equal(fakeAttribute, 'text')
    const duplicate = renderLogHtml({ text: '<a href="https://a" href="https://b">x</a>' }, '')
    assert.equal(duplicate, 'x')
    const nested = renderLogHtml({ text: '<a href="https://example.com/"><b>nested</b></a>' }, '')
    assert.equal(nested, '&lt;a href=&quot;https://example.com/&quot;&gt;&lt;b&gt;nested&lt;/b&gt;&lt;/a&gt;')
    assert.match(renderLogHtml({ text: '<script>alert(1)</script><img src=x onerror=bad()>' }, ''), /&lt;script&gt;/)
})

test('real-world anchors render as links like the legacy log viewer and never leak tag markup', () => {
    const linked = [
        ['<a href=https://ci.example.com/console>unquoted</a>', 'https://ci.example.com/console', 'unquoted'],
        ['<a href="https://a.com/x?a=1&b">bare amp</a>', 'https://a.com/x?a=1&amp;b', 'bare amp'],
        ['<a href="https://a.com/x?list[]=1&list[]=2">array</a>', 'https://a.com/x?list[]=1&amp;list[]=2', 'array'],
        ['<a href="https://a.com/x?名称=1&名=2">cjk</a>', 'https://a.com/x?%E5%90%8D%E7%A7%B0=1&amp;%E5%90%8D=2', 'cjk'],
        ['<a href="https://a.com/x?a=1&%E5%90%8D=2">encoded</a>', 'https://a.com/x?a=1&amp;%E5%90%8D=2', 'encoded'],
        ['<a href="https://example.com/&bogus">bogus</a>', 'https://example.com/&amp;bogus', 'bogus'],
        ['<a href=" https://a.com/x ">padded</a>', 'https://a.com/x', 'padded'],
        ['<a href="https://a.com/path with space">space</a>', 'https://a.com/path%20with%20space', 'space'],
        ['<a href="https://a.com/x" download>bool attr</a>', 'https://a.com/x', 'bool attr'],
        ['<a target=_blank href=\'/console/pipeline/p/x\'>console</a>', '/console/pipeline/p/x', 'console'],
        ['<a href="https://a.com/x">a &foo; b</a>', 'https://a.com/x', 'a &amp;foo; b'],
        ['<a href="https://a.com/x"></a>', 'https://a.com/x', 'https://a.com/x']
    ]
    for (const [text, href, label] of linked) {
        assert.equal(renderLogHtml({ text }, ''), `<a href="${href}" target="_blank" rel="noopener noreferrer">${label}</a>`)
    }
    const agentMonitor = "<a href='http://ci.example.com/console/pipeline/p/x/detail/b'>运行中 #12</a> (pipe task)"
    assert.equal(parseLogLine({ text: agentMonitor }).plainText, '运行中 #12 (pipe task)')
    assert.match(renderLogHtml({ text: agentMonitor }, ''), /^<a href="http:\/\/ci\.example\.com\/console\/pipeline\/p\/x\/detail\/b" target="_blank" rel="noopener noreferrer">运行中 #12<\/a> \(pipe task\)$/)
    for (const href of ['ftp://a.com/x', 'www.qq.com', '/ms/artifactory/x', 'https://a.com/\tx']) {
        const html = renderLogHtml({ text: `prefix <a href="${href}" onclick="bad()">label</a> suffix` }, '')
        assert.equal(html, 'prefix label suffix')
    }
})

test('plain http(s) URLs without an <a> tag stay as text like the legacy log viewer', () => {
    const checkoutReason = '【问题原因】：[user_a]没有仓库[https://git.example.com/group/webhook_test.git]拉取权限。'
    assert.equal(getLogText({ text: checkoutReason }), checkoutReason)
    assert.equal(renderLogHtml({ text: checkoutReason }, ''), checkoutReason)
    assert.equal(
        renderLogHtml({ text: checkoutReason }, 'webhook'),
        '【问题原因】：[user_a]没有仓库[https://git.example.com/group/<mark class="lp-hl">webhook</mark>_test.git]拉取权限。'
    )
    const submodule = "  子模组 'mr_test' (https://github.com/example/mr_test.git) 已对路径 'mr_test' 注册"
    assert.equal(renderLogHtml({ text: submodule }, ''), submodule.replace(/'/g, '&#39;'))
    for (const text of [
        'plain https://example.com/a?x=1&y=2#top',
        'see http://ci.example.com/console/x.',
        '(https://a.com/x)',
        '访问https://a.com/x查看',
        '\u001b[33mhttps://a.com/x\u001b[0m tail'
    ]) {
        assert.doesNotMatch(renderLogHtml({ text }, ''), /<a\b/, text)
    }
    const anchored = renderLogHtml({ text: '<a href="https://a.com/x">https://a.com/x</a> https://b.com/y' }, '')
    assert.equal(anchored, '<a href="https://a.com/x" target="_blank" rel="noopener noreferrer">https://a.com/x</a> https://b.com/y')
})

test('anchors render for any complete http(s) URL or /console absolute path and only add the opener/referrer guard', () => {
    const linkOf = href => renderLogHtml({ text: `<a href="${href}">label</a>` }, '').match(/^<a href="([^"]+)" target="_blank" rel="noopener noreferrer">label<\/a>$/)?.[1] || ''
    const accepted = [
        ['https://example.com', 'https://example.com/'],
        ['http://intranet.example.org:8080/p/123?a=1&b=2#top', 'http://intranet.example.org:8080/p/123?a=1&amp;b=2#top'],
        ['HTTPS://Repo.Example.NET/x', 'https://repo.example.net/x'],
        ['/console/pipeline/p/x/detail/b', '/console/pipeline/p/x/detail/b'],
        ['/console/environment/p/node?a=1&b=2#detail', '/console/environment/p/node?a=1&amp;b=2#detail']
    ]
    for (const [href, expected] of accepted) assert.equal(linkOf(href), expected, href)
    const rejected = [
        '//example.com/x', 'example.com', 'example.com/p/123', '/ms/artifactory/x',
        'javascript://example.com/%0aalert(1)', 'mailto:a@example.com', 'ftp://example.com/x'
    ]
    for (const href of rejected) assert.equal(renderLogHtml({ text: `<a href="${href}">label</a>` }, ''), 'label', href)
})

test('root-relative console links remain idempotent with the real URL prefix click interceptor', (t) => {
    const interceptorUrl = new URL('../../../../../../devops-nav/src/assets/static/url-prefix-interceptor.js', import.meta.url)
    if (!existsSync(interceptorUrl)) {
        t.skip('url-prefix-interceptor.js is not present on github/master')
        return
    }
    const interceptorSource = readFileSync(interceptorUrl, 'utf8')
    const originalWindow = Object.getOwnPropertyDescriptor(globalThis, 'window')
    const createInterceptedWindow = prefix => {
        let clickHandler
        const opened = []
        const windowObject = {
            PUBLIC_URL_PREFIX: prefix,
            location: { href: 'https://console.test/app', origin: 'https://console.test' },
            open: (...args) => opened.push(args)
        }
        const documentObject = {
            addEventListener: (name, handler) => {
                if (name === 'click') clickHandler = handler
            }
        }
        runInNewContext(interceptorSource, { window: windowObject, document: documentObject, URL })
        return { windowObject, opened, clickHandler: event => clickHandler(event) }
    }
    const withWindow = (value, run) => {
        Object.defineProperty(globalThis, 'window', { configurable: true, writable: true, value })
        try {
            return run()
        } finally {
            if (originalWindow) Object.defineProperty(globalThis, 'window', originalWindow)
            else delete globalThis.window
        }
    }
    const click = (handler, href) => {
        let prevented = false
        const anchor = { getAttribute: name => ({ href, target: '_blank' })[name] ?? null, hasAttribute: () => false, closest: () => anchor }
        handler({
            target: anchor,
            button: 0,
            defaultPrevented: false,
            preventDefault: () => {
                prevented = true
            },
            stopPropagation: () => {}
        })
        return prevented
    }

    withWindow(undefined, () => {
        assert.match(renderLogHtml({ text: '<a href="/console/x">x</a>' }, ''), /href="\/console\/x"/)
    })
    for (const [prefix, expected] of [['', '/console/x'], ['/tenant', '/tenant/console/x']]) {
        const fixture = createInterceptedWindow(prefix)
        withWindow(fixture.windowObject, () => {
            const html = renderLogHtml({ text: '<a href="/console/x">x</a>' }, '')
            const href = html.match(/href="([^"]+)"/)[1]
            assert.equal(href, expected)
            assert.equal(fixture.windowObject.__prefixSameOriginUrl(href), href)
            assert.equal(click(fixture.clickHandler, href), false)
            assert.equal(fixture.opened.length, 0)
        })
    }
    const unicodePrefix = createInterceptedWindow('/租户')
    withWindow(unicodePrefix.windowObject, () => {
        const html = renderLogHtml({ text: '<a href="/console/中文?x=1#detail">x</a>' }, '')
        const href = html.match(/href="([^"]+)"/)[1]
        assert.equal(href, '/租户/console/%E4%B8%AD%E6%96%87?x=1#detail')
        assert.match(html, /target="_blank" rel="noopener noreferrer"/)
        assert.equal(unicodePrefix.windowObject.__prefixSameOriginUrl(href), href)
        assert.equal(click(unicodePrefix.clickHandler, href), false)
        assert.equal(unicodePrefix.opened.length, 0)
    })
    for (const badResult of ['//evil.test/console', 'https://evil.test/console']) {
        const fixture = createInterceptedWindow('/tenant')
        fixture.windowObject.__prefixSameOriginUrl = value => value === '/console' ? '/tenant/console' : badResult
        withWindow(fixture.windowObject, () => assert.doesNotMatch(renderLogHtml({ text: '<a href="/console/x">x</a>' }, ''), /<a\b/))
    }
})

test('line-start markers classify issues; embedded markers remain visible', () => {
    assert.equal(getLogIssueLevel({ lineNo: 1, level: 'INFO', text: '##[error]failed' }), 'error')
    assert.equal(getLogIssueLevel({ lineNo: 2, level: 'INFO', text: 'prefix ##[error] remains' }), '')
    assert.equal(getLogText({ text: '##[warning]careful' }), 'careful')
    assert.equal(getLogText({ text: 'prefix ##[warning] remains' }), 'prefix ##[warning] remains')
    assert.equal(getLogText({ text: '##[group]Readable title' }), 'Readable title')
    assert.equal(getLogText({ text: '##[endgroup] orphan marker' }), 'orphan marker')
})

test('CRLF and solitary CR display as line breaks, tabs use four-column stops, and backspace removes one Unicode code point', () => {
    const formatted = parseLogLine({ text: 'ab\r\n中\tZ\r🙂x\bY' })
    assert.equal(formatted.plainText, 'ab\n中  Z\n🙂Y')
    assert.equal(getLogText({ text: '🧪x\b!' }), '🧪!')
    assert.equal(getLogText({ text: 'ab\u001b[31m中\tX\bY\u001b[0m' }), 'ab中    Y')
    assert.equal(getLogText({ text: 'one\nx\bZ' }), 'one\nZ', 'backspace never crosses a logical line')
    assert.equal(getLogText({ text: 'ab\b\tX' }), 'a   X')
    assert.equal(getLogText({ text: '中\b\tX' }), '    X')
    assert.equal(getLogText({ text: 'one\nx\b\tZ' }), 'one\n    Z')
    assert.equal(getLogText({ text: `${'a'.repeat(10000)}${'\b'.repeat(10000)}done` }), 'done')
    assert.equal(getLogText({ text: `${'a'.repeat(20000)}${'\b'.repeat(20000)}done` }), 'done')
})

test('OSC controls are removed without swallowing malformed trailing text; cache follows content changes', () => {
    assert.equal(getLogText({ text: 'before\u001b]0;title\u0007after' }), 'beforeafter')
    assert.equal(getLogText({ text: 'before\u001b]0;unterminated visible tail' }), 'before]0;unterminated visible tail')
    const log = { text: 'first' }
    assert.equal(getLogText(log), 'first')
    log.text = 'changed'
    assert.equal(getLogText(log), 'changed')
    const identity = { lineNo: 1, timestamp: 100, text: 'stable content' }
    parseLogLine(identity)
    identity.lineNo = 2
    assert.equal(parseLogLine(identity).sourceKey, '2:100')
    const repeatedUnclosed = `${'\u001b]'.repeat(10000)}tail-preserved`
    assert.ok(getLogText({ text: repeatedUnclosed }).endsWith('tail-preserved'))
})

test('safe anchor ranges remain aligned across ANSI controls', () => {
    const log = { text: '\u001b[31m<a href="https://example.com">colored link</a> tail\u001b[39m' }
    const html = renderLogHtml(log, 'link')
    assert.match(html, /<a href="https:\/\/example\.com\/" target="_blank" rel="noopener noreferrer"><span style="color:#ff6666">colored <mark class="lp-hl">link<\/mark><\/span><\/a>/)
    assert.doesNotMatch(html, /href="[^"]*tail/)
})
