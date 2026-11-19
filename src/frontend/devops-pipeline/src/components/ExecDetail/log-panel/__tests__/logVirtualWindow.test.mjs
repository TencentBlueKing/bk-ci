import assert from 'node:assert/strict'
import test from 'node:test'
import Vue, { markRaw, shallowRef } from 'vue'
import { readFileSync } from 'node:fs'
import * as adapter from '../logPanelAdapter.js'
import { withDisplayLineNumbers } from '../logPanelAdapter.js'
import * as vw from '../logVirtualWindow.js'

const mkLog = (lineNo, extra = {}) => ({ lineNo, message: `line-${lineNo}`, ...extra })

function loadLogLinesViewOptions (extraDependencies = {}) {
    const source = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
        .replace(/^\s*import[\s\S]*?\sfrom\s+['"][^'"]+['"];?\s*$/gm, '')
        .replace(/export default\s*\{/, 'return {')
    const dependencies = { ...adapter, ...vw, markRaw, shallowRef, LogErrorMinimap: {}, ...extraDependencies }
    const names = Object.keys(dependencies).filter(name => /^[A-Za-z_$][\w$]*$/.test(name))
    return new Function(...names, script)(...names.map(name => dependencies[name]))
}

function createLayoutRestoreHarness (viewportTop, actualHeight = 720) {
    const options = loadLogLinesViewOptions()
    const logs = Array.from({ length: 1300 }, (_, index) => mkLog(index + 1))
    const targetIndex = 1217
    const targetKey = '1218'
    const oldIndex = vw.createHeightIndex().rebuild(logs, { [targetKey]: 1140 })
    const heightIndex = vw.createHeightIndex().rebuild(logs, {})
    const queue = []
    const box = {
        scrollTop: 8 + oldIndex.prefix(targetIndex) - viewportTop,
        scrollLeft: 0,
        clientTop: 0,
        clientHeight: 600,
        scrollWidth: 400,
        clientWidth: 400,
        get scrollHeight () {
            return 8 + heightIndex.total()
        },
        getBoundingClientRect: () => ({ top: 100, left: 0, right: 400, height: 600 }),
        querySelectorAll (selector) {
            const rowTop = 8 + heightIndex.prefix(targetIndex) - box.scrollTop
            if (rowTop + actualHeight <= 0 || rowTop >= box.clientHeight) return []
            if (selector.includes('data-line-key')) return [row]
            return [row]
        }
    }
    const row = {
        dataset: { lineKey: targetKey, lineNo: targetKey },
        get offsetHeight () {
            return actualHeight
        },
        getBoundingClientRect () {
            const top = 100 + 8 + heightIndex.prefix(targetIndex) - box.scrollTop
            return { top, bottom: top + actualHeight, height: actualHeight }
        }
    }
    const anchor = { lineKey: targetKey, lineNo: targetKey, viewportTop }
    const restore = { anchor, interactionToken: 0, token: 1, callbacks: [], running: false, runId: 0 }
    const vm = {
        $refs: { box },
        $nextTick: callback => queue.push(callback),
        $emit () {},
        displayLines: logs,
        heightIndex,
        heightMap: { [targetKey]: 20 },
        indexVersion: 0,
        scrollTop: box.scrollTop,
        clientHeight: box.clientHeight,
        scrollDelta: 0,
        scrollGeneration: 0,
        layoutToken: 1,
        layoutInteractionToken: 0,
        followIntent: false,
        userScrollPending: false,
        pendingAnchor: anchor,
        _stableAnchor: anchor,
        _layoutRestore: restore,
        activeLocate: null,
        _pendingWheel: null,
        useVirtual: true,
        lineOrigin: () => 8,
        updateViewport () {},
        flush () {
            let steps = 0
            while (queue.length && steps < 100) {
                queue.shift()()
                steps += 1
            }
            assert.ok(steps < 100, 'layout restore remains bounded')
            assert.equal(queue.length, 0, 'all queued render and calibration work settles')
        }
    }
    Object.assign(vm, options.methods)
    vm.lineOrigin = () => 8
    vm.updateViewport = () => {}
    return { vm, box, row, anchor, restore, logs, targetIndex, heightIndex }
}

test('shouldUseVirtualScroll enables above threshold regardless of wrap', () => {
    assert.equal(vw.shouldUseVirtualScroll(800), false)
    assert.equal(vw.shouldUseVirtualScroll(801), true)
})

test('nowrap height estimates preserve explicit LF, CRLF, trailing, empty, and cached measured rows', () => {
    assert.equal(vw.estimateExplicitLineHeight('', 20), 20)
    assert.equal(vw.estimateExplicitLineHeight('one\n\ntwo', 20), 60)
    assert.equal(vw.estimateExplicitLineHeight('one\r\ntwo\r\n', 20), 60)
    const index = vw.createHeightIndex().rebuild([mkLog(1), mkLog(2)], { 1: 60, 2: 40 })
    assert.deepEqual(index.heights, [60, 40], 'HeightIndex accepts measured heights independently of wrap mode')
})

test('LogLinesView estimates explicit newline heights and measures nowrap mounted rows', () => {
    const options = loadLogLinesViewOptions()
    const logs = [{ lineNo: 1, text: 'one\ntwo\nthree' }, { lineNo: 2, text: 'tail' }]
    const index = vw.createHeightIndex()
    const vm = {
        displayLines: logs,
        wrap: false,
        heightMap: {},
        heightIndex: index,
        indexVersion: 0,
        $refs: { box: {
            scrollTop: 0,
            clientTop: 0,
            clientHeight: 100,
            getBoundingClientRect: () => ({ top: 0 }),
            querySelectorAll: () => [
                { dataset: { lineKey: '1', lineNo: '1' }, offsetHeight: 64, getBoundingClientRect: () => ({ top: 0, bottom: 64 }) },
                { dataset: { lineKey: '2', lineNo: '2' }, offsetHeight: 24, getBoundingClientRect: () => ({ top: 64, bottom: 88 }) }
            ]
        } },
        scrollGeneration: 0,
        pendingAnchor: null,
        activeLocate: null,
        _pendingWheel: null,
        $nextTick: callback => callback(),
        captureAnchor: () => null,
        updateViewport: () => {}
    }
    Object.assign(vm, options.methods)
    vm.rebuildHeightIndex()
    assert.deepEqual(index.heights, [60, 20])
    assert.equal(vm.measureVisibleRows(), true)
    assert.deepEqual(index.heights, [64, 24])
})

test('resolveLineKey prefers lineNo with stable fallback', () => {
    assert.equal(vw.resolveLineKey(mkLog(42)), '42')
    assert.equal(vw.resolveLineKey({ line: 7 }), '7')
    assert.equal(vw.resolveLineKey({ timestamp: 100, message: 'x' }, 3), 'ts:100:3')
    assert.equal(vw.resolveLineKey({}, 5), '__idx:5')
})

test('virtual render keys preserve absolute fallback indexes when the window starts mid-list', () => {
    const displayBase = 700
    const rows = [{ displayLineNo: 700 }, { displayLineNo: 701 }, { timestamp: 10, displayLineNo: 702 }]
    const keys = rows.slice(1).map(row => vw.resolveLineKey(row, row.displayLineNo - displayBase))
    assert.deepEqual(keys, ['__idx:1', 'ts:10:2'])
})

test('wheel deltas convert pixel, line, and page modes and gate native/modifier gestures', () => {
    assert.deepEqual(vw.resolveLogWheelDeltas({ deltaX: 2, deltaY: -3 }), { deltaLeft: 2, deltaTop: -3 })
    assert.deepEqual(vw.resolveLogWheelDeltas({ deltaX: 2, deltaY: -3, deltaMode: 1 }), { deltaLeft: 40, deltaTop: -60 })
    assert.deepEqual(vw.resolveLogWheelDeltas({ deltaX: 1, deltaY: 2, deltaMode: 2, pageHeight: 640 }), { deltaLeft: 640, deltaTop: 1280 })
    const allowed = { virtualized: true, cancelable: true, deltaY: 20 }
    assert.equal(vw.shouldInterceptLogWheel(allowed), true)
    for (const rejected of [
        { virtualized: false }, { cancelable: false }, { ctrlKey: true }, { metaKey: true },
        { shiftKey: true }, { deltaX: 30 }, { deltaY: 0 }
    ]) assert.equal(vw.shouldInterceptLogWheel({ ...allowed, ...rejected }), false)
})

test('wheel targets merge against pending offsets, clamp both axes, and preserve boundary chaining', () => {
    const dimensions = { scrollHeight: 2000, clientHeight: 500, scrollWidth: 1000, clientWidth: 400 }
    const first = vw.resolveLogWheelTarget({ ...dimensions, scrollTop: 100, scrollLeft: 20, deltaTop: 200, deltaLeft: 40 })
    const second = vw.resolveLogWheelTarget({ ...dimensions, scrollTop: first.top, scrollLeft: first.left, deltaTop: -50, deltaLeft: 10 })
    assert.deepEqual(first, { top: 300, left: 60, changed: true })
    assert.deepEqual(second, { top: 250, left: 70, changed: true })
    const topBoundary = vw.resolveLogWheelTarget({ ...dimensions, scrollTop: 0, deltaTop: -20 })
    assert.deepEqual(topBoundary, { top: 0, left: 0, changed: false })
    const horizontalCanMove = vw.resolveLogWheelTarget({ ...dimensions, scrollTop: 0, scrollLeft: 0, deltaTop: -20, deltaLeft: 15 })
    assert.equal(horizontalCanMove.changed, true)
    assert.equal(vw.shouldYieldLogWheelToNative({ hasPending: false, targetTop: horizontalCanMove.top, scrollTop: 0 }), true)
    assert.equal(vw.shouldYieldLogWheelToNative({ hasPending: true, targetTop: 0, scrollTop: 0 }), false)
    const shortList = vw.resolveLogWheelTarget({ scrollTop: 0, scrollHeight: 300, clientHeight: 500, deltaTop: 20 })
    assert.deepEqual(shortList, { top: 0, left: 0, changed: false })
})

test('wheel transaction identity rejects canceled nextTick commits and stale DOM scroll reads', () => {
    const pending = { top: 400, left: 0 }
    assert.equal(vw.isCurrentWheelTransaction(pending, pending), true)
    assert.equal(vw.isCurrentWheelTransaction(null, pending), false)
    const source = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(source, /if \(this\._pendingWheel\) \{[\s\S]*this\.updateViewport\(\)[\s\S]*return/)
    assert.match(source, /if \(!isCurrentWheelTransaction\(this\._pendingWheel, transaction\)\) return/)
    assert.match(source, /shouldYieldLogWheelToNative\(\{ hasPending: Boolean\(pending\), targetTop: target\.top, scrollTop: el\.scrollTop \}\)/)
    assert.match(source, /cancelPendingWheel \(\) \{[\s\S]*this\._pendingWheel = null[\s\S]*this\.scrollTop = el\.scrollTop/)
    assert.match(source, /@touchstart="onNativeInteraction"[\s\S]*@pointerdown="onNativeInteraction"[\s\S]*@keydown="onNativeInteraction"/)
    assert.match(source, /logs: \{\s*handler \(\) \{\s*this\.cancelPendingWheel\(\)/)
    assert.match(source, /wrap \(val\) \{\s*this\.cancelPendingWheel\(\)/)
    assert.match(source, /showTime \(\) \{\s*this\.cancelPendingWheel\(\)/)
    assert.match(source, /scrollToBottom \(\) \{\s*this\.cancelPendingWheel\(\)/)
    assert.match(source, /scrollToLine \(lineNo\) \{[\s\S]*?this\.cancelPendingWheel\(\)/)
    assert.match(source, /:logs="renderLines"[\s\S]*@locate="\$emit\('locate', \$event\)"/)
})

test('continuous user scroll keeps follow intent live across intermediate frames', () => {
    assert.equal(vw.isLogScrollKey('End'), true)
    assert.equal(vw.isLogScrollKey('PageUp'), true)
    assert.equal(vw.isLogScrollKey('a'), false)
    let follow = false
    follow = vw.resolveFollowIntent({ currentFollow: follow, atBottom: false, userScrollPending: true })
    assert.equal(follow, false)
    follow = vw.resolveFollowIntent({ currentFollow: follow, atBottom: true, userScrollPending: true })
    assert.equal(follow, true)
    follow = vw.resolveFollowIntent({ currentFollow: true, atBottom: true, userScrollPending: true })
    follow = vw.resolveFollowIntent({ currentFollow: follow, atBottom: false, userScrollPending: true })
    assert.equal(follow, false)
    assert.equal(vw.resolveFollowIntent({ currentFollow: false, atBottom: true, userScrollPending: false }), false)
    assert.equal(vw.isCurrentLayoutUpdate({ token: 3, currentToken: 3, interactionToken: 5, currentInteractionToken: 5 }), true)
    assert.equal(vw.isCurrentLayoutUpdate({ token: 3, currentToken: 3, interactionToken: 5, currentInteractionToken: 6 }), false)
})

test('LogLinesView consumes multi-frame native scroll intent and ignores programmatic scrolls', () => {
    const source = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(source, /import\s*\{[\s\S]*?resolveFollowIntent[\s\S]*?\}\s*from\s*'\.\/logVirtualWindow'/)
    const options = loadLogLinesViewOptions()
    const emitted = []
    const stableAnchor = { lineKey: '1631', lineNo: '1631', viewportTop: -378 }
    const box = { scrollTop: 900, scrollHeight: 2200, clientHeight: 500 }
    const vm = {
        $refs: { box },
        $emit: (name, value) => emitted.push([name, value]),
        scrollTop: 900,
        scrollGeneration: 0,
        clientHeight: 500,
        followIntent: false,
        userScrollPending: false,
        layoutInteractionToken: 0,
        layoutToken: 0,
        pendingAnchor: { lineNo: '40' },
        activeLocate: null,
        pendingLocateScrollTop: null,
        _pendingWheel: null,
        _pendingPanGeneration: 0
    }
    Object.assign(vm, options.methods)
    vm.captureAnchor = () => stableAnchor
    vm.updateViewport = () => {}

    vm.onNativeInteraction({ type: 'keydown', key: 'PageDown' })
    box.scrollTop = 950
    vm.onScroll()
    assert.equal(vm.followIntent, false)
    assert.equal(vm.userScrollPending, true)
    box.scrollTop = 1700
    vm.onScroll()
    assert.equal(vm.followIntent, true)
    assert.equal(vm.userScrollPending, true)
    clearTimeout(vm.userScrollTimer)
    vm.userScrollTimer = null

    vm.userScrollPending = false
    vm.followIntent = true
    box.scrollTop = 1698
    vm.onNativeInteraction({ type: 'keydown', key: 'PageUp' })
    vm.onScroll()
    assert.equal(vm.followIntent, true)
    box.scrollTop = 1300
    vm.onScroll()
    assert.equal(vm.followIntent, false)
    assert.equal(vm._stableAnchor, stableAnchor, 'history scroll stores a DOM-derived source anchor')
    const resizeLayout = vm.beginLayoutUpdate({ usePreviousIndex: true })
    assert.equal(resizeLayout.anchor, stableAnchor, 'a resize uses the stable pre-reflow anchor')
    clearTimeout(vm.userScrollTimer)
    vm.userScrollTimer = null

    vm.followIntent = false
    vm.userScrollPending = false
    box.scrollTop = 900
    vm.onScroll()
    assert.equal(vm.followIntent, false)
    assert.equal(emitted.filter(([name]) => name === 'stick-bottom-change').length, 4)
})

test('layout pending during one native scroll gesture resumes at its latest source position or tail', async () => {
    const options = loadLogLinesViewOptions()
    const runScenario = async ({ endAtBottom }) => {
        const box = { scrollTop: 1000, scrollHeight: 20000, clientHeight: 500 }
        const restored = []
        const vm = {
            $refs: { box },
            $emit () {},
            scrollTop: box.scrollTop,
            scrollGeneration: 0,
            clientHeight: box.clientHeight,
            followIntent: false,
            userScrollPending: false,
            layoutInteractionToken: 0,
            layoutToken: 0,
            pendingAnchor: null,
            activeLocate: null,
            _pendingWheel: null,
            updateViewport () {},
            captureAnchor () {
                const lineNo = Math.floor(box.scrollTop / 20) + 1
                return { lineNo, lineKey: String(lineNo), viewportTop: 0 }
            },
            restoreLayoutAnchor (layout) {
                const anchor = layout.restore.anchor
                restored.push(anchor.lineNo)
                box.scrollTop = (anchor.lineNo - 1) * 20
            },
            finishFollowLayoutUpdate () {
                box.scrollTop = box.scrollHeight - box.clientHeight
            }
        }
        Object.assign(vm, options.methods)
        vm.captureAnchor = () => {
            const lineNo = Math.floor(box.scrollTop / 20) + 1
            return { lineNo, lineKey: String(lineNo), viewportTop: 0 }
        }
        vm.updateViewport = () => {}
        vm.restoreLayoutAnchor = layoutToRestore => {
            const anchor = layoutToRestore.restore.anchor
            restored.push(anchor.lineNo)
            box.scrollTop = (anchor.lineNo - 1) * 20
        }
        vm.finishFollowLayoutUpdate = () => {
            box.scrollTop = box.scrollHeight - box.clientHeight
        }

        vm.onUserScrollInput('scrollbar')
        vm.onScroll()
        const layout = vm.beginLayoutUpdate()
        vm.finishLayoutUpdate(layout)
        assert.equal(vm.pendingLayoutUpdate.layout, layout)
        assert.equal(layout.restore.anchor.lineNo, 51)

        box.scrollTop = endAtBottom ? box.scrollHeight - box.clientHeight : 2000
        vm.onScroll()
        const latestLineNo = Math.floor(box.scrollTop / 20) + 1
        await new Promise(resolve => setTimeout(resolve, vw.USER_SCROLL_IDLE_MS + 25))
        return { box, vm, restored, latestLineNo }
    }

    const history = await runScenario({ endAtBottom: false })
    assert.deepEqual(history.restored, [101], 'idle resumes with the latest anchor, not the layout-start anchor')
    assert.equal(history.latestLineNo, 101)
    assert.equal(history.box.scrollTop, 2000)

    const tail = await runScenario({ endAtBottom: true })
    assert.deepEqual(tail.restored, [], 'reaching the tail clears the pending history restore')
    assert.equal(tail.vm.followIntent, true)
    assert.equal(tail.latestLineNo, 976)
    assert.equal(tail.box.scrollTop, tail.box.scrollHeight - tail.box.clientHeight)
})

test('shift-wheel horizontal navigation does not pause vertical tail following', () => {
    const options = loadLogLinesViewOptions()
    const vm = {
        $refs: { box: { scrollTop: 100, scrollHeight: 2100, clientHeight: 500 } },
        $emit () {},
        useVirtual: true,
        followIntent: true,
        userScrollPending: false,
        scrollTop: 100,
        scrollGeneration: 0
    }
    Object.assign(vm, options.methods)
    vm.onWheel({
        cancelable: true,
        ctrlKey: false,
        metaKey: false,
        shiftKey: true,
        deltaX: 0,
        deltaY: 60,
        deltaMode: 0,
        preventDefault () {
            throw new Error('shift-wheel must remain native horizontal navigation')
        }
    })
    assert.equal(vm.userScrollPending, false)
    assert.equal(vm.followIntent, true)
})

test('LogLinesView wheel transaction accumulates before DOM commit and ignores canceled or stale scroll work', () => {
    const options = loadLogLinesViewOptions()
    const callbacks = []
    const box = {
        scrollTop: 100,
        scrollLeft: 10,
        scrollHeight: 2100,
        clientHeight: 500,
        scrollWidth: 1000,
        clientWidth: 400
    }
    const vm = {
        $refs: { box },
        $nextTick: callback => callbacks.push(callback),
        $emit: () => {},
        captureAnchor: () => null,
        displayLines: Array.from({ length: 1000 }, (_, index) => mkLog(index + 1)),
        useVirtual: true,
        scrollTop: 100,
        scrollLeft: 10,
        scrollDelta: 0,
        clientHeight: box.clientHeight,
        scrollGeneration: 0,
        activeLocate: null,
        pendingLocateScrollTop: null,
        contentHeight: box.scrollHeight,
        viewport: { top: 0, height: 50 }
    }
    Object.assign(vm, options.methods)
    vm.captureAnchor = () => null
    const wheel = (deltaX, deltaY) => {
        let prevented = false
        vm.onWheel({
            cancelable: true,
            ctrlKey: false,
            metaKey: false,
            shiftKey: false,
            deltaX,
            deltaY,
            deltaMode: 0,
            preventDefault () {
                prevented = true
            }
        })
        return prevented
    }

    assert.equal(wheel(5, 30), true)
    const transaction = vm._pendingWheel
    assert.equal(vm.scrollTop, 130)
    assert.equal(transaction.left, 15)
    assert.equal(box.scrollTop, 100)
    assert.equal(box.scrollLeft, 10)
    assert.equal(callbacks.length, 1)
    assert.equal(wheel(3, 20), true)
    assert.equal(vm._pendingWheel, transaction)
    assert.equal(vm.scrollTop, 150)
    assert.equal(transaction.left, 18)
    assert.equal(callbacks.length, 1)
    assert.equal(box.scrollTop, 100)
    callbacks.shift()()
    assert.equal(box.scrollTop, 150)
    assert.equal(box.scrollLeft, 18)
    assert.equal(vm._pendingWheel, null)

    box.scrollTop = 200
    vm.scrollTop = 200
    assert.equal(wheel(0, 40), true)
    const canceledCallback = callbacks.shift()
    vm.cancelPendingWheel()
    canceledCallback()
    assert.equal(box.scrollTop, 200)
    assert.equal(vm.scrollTop, 200)
    assert.equal(vm._pendingWheel, null)

    assert.equal(wheel(0, 100), true)
    const pendingTarget = vm.scrollTop
    box.scrollTop = 50
    vm.onScroll()
    assert.equal(box.scrollTop, 50)
    assert.equal(vm.scrollTop, pendingTarget)
    callbacks.shift()()
    assert.equal(box.scrollTop, pendingTarget)
})

test('LogLinesView minimap pan renders the target window before committing scroll and ignores stale commits', () => {
    const options = loadLogLinesViewOptions()
    const callbacks = []
    const box = { scrollTop: 100, scrollLeft: 0, scrollHeight: 2100, clientHeight: 500, scrollWidth: 400, clientWidth: 400 }
    const vm = {
        $refs: { box },
        $nextTick: callback => callbacks.push(callback),
        $emit: () => {},
        captureAnchor: () => null,
        displayLines: Array.from({ length: 1000 }, (_, index) => mkLog(index + 1)),
        useVirtual: true,
        scrollTop: 100,
        scrollDelta: 0,
        clientHeight: box.clientHeight,
        contentHeight: box.scrollHeight,
        viewport: { top: 0, height: 50 },
        locateGeneration: 0,
        activeLocate: null,
        pendingLocateScrollTop: null,
        _pendingWheel: null
    }
    Object.assign(vm, options.methods)
    vm.captureAnchor = () => null
    assert.equal(vm.onPan(0.25), undefined)
    assert.equal(vm.scrollTop, 400)
    assert.equal(vm.scrollDelta, 300)
    assert.equal(box.scrollTop, 100)
    vm.onNativeInteraction()
    callbacks.shift()()
    assert.equal(box.scrollTop, 100, 'native interaction cancels an uncommitted pan')
    vm.scrollTop = box.scrollTop
    vm.onPan(0.75)
    assert.equal(vm.scrollTop, 1200)
    assert.equal(box.scrollTop, 100)
    vm.onPan(0.5)
    assert.equal(vm.scrollTop, 800)
    assert.equal(callbacks.length, 2)
    callbacks.shift()()
    assert.equal(box.scrollTop, 100, 'superseded pan cannot commit')
    callbacks.shift()()
    assert.equal(box.scrollTop, 800)
    assert.equal(vm.scrollTop, 800)
    assert.equal(vm.scrollDelta, 0)
})

function createTailPanHarness () {
    const options = loadLogLinesViewOptions()
    const callbacks = []
    let contentHeight = 185380
    const measuredHeight = 105720
    const row = {
        dataset: { lineKey: 'tail' },
        get offsetHeight () {
            return measuredHeight
        }
    }
    const box = {
        scrollTop: 0,
        clientHeight: 500,
        scrollWidth: 1000,
        clientWidth: 400,
        get scrollHeight () {
            return contentHeight
        },
        querySelectorAll () {
            return [row]
        }
    }
    const events = []
    const vm = {
        $refs: { box },
        $nextTick: callback => callbacks.push(callback),
        $emit: (name, value) => events.push([name, value]),
        displayLines: [],
        heightMap: { tail: 20 },
        heightIndex: {
            setHeight (_key, height) {
                const delta = height - (this.height || 20)
                if (Math.abs(delta) < 1) return false
                this.height = height
                contentHeight += delta
                return true
            }
        },
        indexVersion: 0,
        scrollTop: 0,
        scrollDelta: 0,
        clientHeight: box.clientHeight,
        followIntent: false,
        userScrollPending: false,
        userScrollTimer: null,
        layoutToken: 0,
        layoutInteractionToken: 0,
        layoutDestroyed: false,
        useVirtual: true,
        pendingAnchor: null,
        _layoutRestore: null,
        _stableAnchor: null,
        _pendingWheel: null,
        _pendingPanGeneration: 0,
        _panGeneration: 0,
        scrollGeneration: 0,
        activeLocate: null,
        pendingLocateScrollTop: null,
        updateViewport () {},
        cancelLocateCalibration () {},
        flush () {
            let count = 0
            while (callbacks.length && count < 80) {
                callbacks.shift()()
                count += 1
            }
            assert.ok(count < 80, 'tail correction is finite')
            assert.equal(callbacks.length, 0)
        }
    }
    Object.assign(vm, options.methods)
    return {
        vm,
        box,
        events,
        setContentHeight (value) {
            contentHeight = value
        },
        callbacks
    }
}

test('minimap End follows the measured new tail and subsequent After batches', () => {
    const { vm, box, events, setContentHeight } = createTailPanHarness()
    vm.onPan(1)
    assert.equal(vm.followIntent, true, 'the normalized End ratio expresses tail intent before geometry changes')
    assert.deepEqual(events[0], ['stick-bottom-change', true])
    vm.flush()
    assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight <= 1, 'measurement growth is followed to the current max')

    for (let batch = 0; batch < 4; batch++) {
        setContentHeight(box.scrollHeight + 240)
        const layout = vm.beginLayoutUpdate()
        vm.finishLayoutUpdate(layout)
        vm.flush()
        assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight <= 1, `After batch ${batch + 1} remains at tail`)
    }
    assert.equal(vm.followIntent, true)
    assert.equal(events.some(([name, value]) => name === 'stick-bottom-change' && value === false), false)
})

test('new minimap history pan or wheel cancels an in-flight End correction', () => {
    for (const cancelWith of ['pan', 'wheel']) {
        const { vm, box, callbacks } = createTailPanHarness()
        vm.onPan(1)
        callbacks.shift()()
        assert.ok(callbacks.length, 'End correction is awaiting layout measurement')
        if (cancelWith === 'pan') vm.onPan(0.5)
        else vm.onWheel({
            cancelable: true,
            ctrlKey: false,
            metaKey: false,
            shiftKey: false,
            deltaX: 0,
            deltaY: -40,
            deltaMode: 0,
            preventDefault () {}
        })
        vm.flush()
        if (cancelWith === 'pan') {
            assert.equal(vm.followIntent, false)
            assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight > 1)
        } else {
            assert.equal(vm.followIntent, false, 'a committed wheel movement cancels End and resumes historical reading')
            assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight > 1)
        }
        clearTimeout(vm.userScrollTimer)
        vm.userScrollTimer = null
    }
})

test('a stale user-scroll idle timer cannot cancel a newer minimap End', () => {
    const { vm, box } = createTailPanHarness()
    vm.onUserScrollInput('scrollbar')
    assert.ok(vm.userScrollTimer)
    vm.onPan(1)
    assert.equal(vm.userScrollTimer, null)
    vm.flush()
    assert.equal(vm.followIntent, true)
    assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight <= 1)
})

test('native body End enters measured tail following and preserves editable-control key behavior', async () => {
    const options = loadLogLinesViewOptions()
    const callbacks = []
    const logs = Array.from({ length: 1000 }, (_, index) => mkLog(index + 1))
    const heightMap = Object.fromEntries(logs.map(line => [String(line.lineNo), 20]))
    const heightIndex = vw.createHeightIndex().rebuild(logs, heightMap)
    const measuredHeight = 200
    const row = {
        dataset: { lineKey: '1000' },
        get offsetHeight () {
            return measuredHeight
        }
    }
    const box = {
        scrollTop: 18500,
        clientHeight: 500,
        get scrollHeight () {
            return heightIndex.total()
        },
        querySelectorAll () {
            return [row]
        }
    }
    const events = []
    const vm = {
        $refs: { box },
        $nextTick: callback => callbacks.push(callback),
        $emit: (name, value) => events.push([name, value]),
        displayLines: logs,
        heightMap,
        heightIndex,
        indexVersion: 0,
        scrollTop: box.scrollTop,
        scrollDelta: 0,
        clientHeight: box.clientHeight,
        followIntent: false,
        userScrollPending: false,
        userScrollTimer: null,
        layoutToken: 0,
        layoutInteractionToken: 0,
        layoutDestroyed: false,
        pendingAnchor: null,
        _layoutRestore: null,
        _stableAnchor: null,
        _pendingWheel: null,
        _pendingPanGeneration: 0,
        _panGeneration: 0,
        scrollGeneration: 0,
        activeLocate: null,
        pendingLocateScrollTop: null,
        useVirtual: true,
        updateViewport () {},
        cancelLocateCalibration () {},
        flush () {
            let count = 0
            while (callbacks.length && count < 80) {
                callbacks.shift()()
                count += 1
            }
            assert.ok(count < 80, 'keyboard tail correction is bounded')
            assert.equal(callbacks.length, 0)
        }
    }
    Object.assign(vm, options.methods)
    vm.updateViewport = () => {}
    vm.captureAnchor = () => ({ lineNo: 1000, lineKey: '1000', viewportTop: 0 })
    vm.applyAnchor = () => {}

    let prevented = false
    vm.onNativeInteraction({
        type: 'keydown',
        key: 'End',
        target: box,
        preventDefault () {
            prevented = true
        }
    })
    if (!prevented) {
        box.scrollTop = box.scrollHeight - box.clientHeight
        vm.onScroll()
        vm.measureVisibleRows()
        vm.flush()
        await new Promise(resolve => setTimeout(resolve, vw.USER_SCROLL_IDLE_MS + 25))
    } else {
        vm.flush()
    }
    assert.equal(prevented, true, 'body End bypasses a stale pre-measurement browser max')
    assert.equal(vm.followIntent, true)
    assert.ok(box.scrollHeight - box.scrollTop - box.clientHeight <= 1)
    assert.equal(events.some(([name, value]) => name === 'stick-bottom-change' && value === false), false)

    let editablePrevented = false
    let closestSelector = ''
    vm.onNativeInteraction({
        type: 'keydown',
        key: 'End',
        target: {
            closest (selector) {
                closestSelector = selector
                return {}
            }
        },
        preventDefault () {
            editablePrevented = true
        }
    })
    assert.equal(editablePrevented, false, 'editable descendants retain native End behavior')
    assert.match(closestSelector, /textarea/)
    assert.equal(vm.userScrollPending, false)
    clearTimeout(vm.userScrollTimer)
})

test('raw HeightIndex avoids Vue2 deep observation while indexVersion invalidates both layouts', () => {
    const index = markRaw(vw.createHeightIndex().rebuild([mkLog(1), mkLog(2)], {}, false))
    let mainRuns = 0
    let minimapRuns = 0
    const vm = new Vue({
        data: () => ({ heightIndex: index, indexVersion: 0 }),
        computed: {
            mainGeometry () {
                this.indexVersion
                mainRuns += 1
                return this.heightIndex.total()
            },
            minimapGeometry () {
                this.indexVersion
                minimapRuns += 1
                return this.heightIndex.prefix(2)
            }
        }
    })
    assert.equal(vm.heightIndex.__ob__, undefined)
    assert.equal(vm.mainGeometry, 40)
    assert.equal(vm.minimapGeometry, 40)
    index.setHeight('1', 60)
    assert.equal(vm.mainGeometry, 40)
    assert.equal(vm.minimapGeometry, 40)
    vm.indexVersion += 1
    assert.equal(vm.mainGeometry, 80)
    assert.equal(vm.minimapGeometry, 80)
    assert.equal(mainRuns, 2)
    assert.equal(minimapRuns, 2)
    vm.$destroy()
})

test('LogLinesView captures only intersecting rows and guards delayed anchor restore', () => {
    const source = readFileSync(new URL('../LogLinesView.vue', import.meta.url), 'utf8')
    assert.match(source, /rect\.bottom > viewportTop && rect\.top < viewportBottom/)
    assert.match(source, /paddingTop: lineOrigin/)
    assert.match(source, /scrollGeneration === this\.scrollGeneration/)
    assert.match(source, /Math\.abs\(el\.scrollTop - scrollTop\) < 1/)
    assert.match(source, /heightIndex: markRaw\(createHeightIndex\(\)\)/)
    assert.match(source, /v-for="row in visibleRows"/)
    assert.match(source, /'--lp-line-number-width': lineNumberColumnWidth \+ 'ch'/)
    assert.doesNotMatch(source, /:style="\{ width: lineNumberColumnWidth/)
    assert.match(source, /\.lp-lineno \{[^}]*user-select: none;/)
    assert.match(source, /renderLines: shallowRef\(\[\]\)/)
    assert.match(source, /this\.\$watch\('displayLines', next => \{\s*this\.renderLines = next/)
    assert.match(source, /lineNumberColumnWidth \(\) \{\s*return resolveLogLineNumberColumnWidth\(this\.renderLines\)/)
    assert.match(source, /useVirtual \(\) \{\s*return shouldUseVirtualScroll\(this\.renderLines\.length\)/)
})

test('layout restoration presents and measures long-row targets before validating the clamped viewport offset', () => {
    for (const [requestedTop, expectedTop] of [[-938, -719], [-100, -100]]) {
        const { vm, box, row, anchor, restore } = createLayoutRestoreHarness(requestedTop)
        const completedAt = []
        const layout = { token: 1, interactionToken: 0, follow: false, anchor, restore }
        vm.finishLayoutUpdate(layout, () => completedAt.push(row.getBoundingClientRect().top - box.getBoundingClientRect().top))
        vm.flush()
        const actualTop = row.getBoundingClientRect().top - box.getBoundingClientRect().top
        assert.ok(Math.abs(actualTop - expectedTop) <= 1, `expected target y=${expectedTop}, got ${actualTop}`)
        assert.equal(restore.callbacks.length, 0, 'completion drains callbacks only after DOM verification')
        assert.equal(vm._stableAnchor, anchor)
        assert.equal(completedAt.length, 1)
        assert.ok(Math.abs(completedAt[0] - expectedTop) <= 1, 'completion callback observes the verified position')
        assert.equal(vm.heightIndex.heights[1217], 720, 'target row was measured after it was rendered')
    }
})

test('After restore, repeated layouts, and show-time changes merge into one history transaction', () => {
    const { vm, box, row, anchor, restore } = createLayoutRestoreHarness(-100)
    const completedPositions = []
    const layout1 = vm.beginLayoutUpdate({ usePreviousIndex: true })
    vm.finishLayoutUpdate(layout1, () => completedPositions.push(row.getBoundingClientRect().top - box.getBoundingClientRect().top))
    vm.restoreAnchor(anchor)
    const layout2 = vm.beginLayoutUpdate({ usePreviousIndex: true })
    vm.finishLayoutUpdate(layout2, () => completedPositions.push(row.getBoundingClientRect().top - box.getBoundingClientRect().top))
    const layout3 = vm.beginLayoutUpdate()
    vm.finishLayoutUpdate(layout3, () => completedPositions.push(row.getBoundingClientRect().top - box.getBoundingClientRect().top))
    assert.equal(layout1.restore, layout2.restore)
    assert.equal(layout2.restore, layout3.restore)
    assert.equal(layout3.restore.anchor, anchor, 'all updates retain the original source line and viewport target')
    vm.flush()
    assert.ok(Math.abs(row.getBoundingClientRect().top - box.getBoundingClientRect().top + 100) <= 1)
    assert.deepEqual(completedPositions, [-100, -100, -100], 'callbacks complete only after final DOM calibration')
    assert.equal(vm._layoutRestore, null)
    assert.equal(box.scrollTop > 0, true)
})

test('user scrolling cancels an in-flight layout calibration without restoring its old anchor', () => {
    const { vm, box, anchor, restore } = createLayoutRestoreHarness(-100)
    const layout = { token: 1, interactionToken: 0, follow: false, anchor, restore }
    let completed = false
    vm.finishLayoutUpdate(layout, () => {
        completed = true
    })
    vm.beginUserInteraction()
    box.scrollTop = 1234
    vm.flush()
    assert.equal(box.scrollTop, 1234)
    assert.equal(completed, false)
    assert.equal(vm._layoutRestore, null)
})

test('ResizeObserver ignores repeated dimensions and starts one transaction for actual size changes', () => {
    let callback
    let frame
    let canceled = 0
    class FakeResizeObserver {
        constructor (handler) {
            callback = handler
        }
        observe () {}
        disconnect () {}
    }
    const options = loadLogLinesViewOptions({
        ResizeObserver: FakeResizeObserver,
        requestAnimationFrame: handler => {
            frame = handler
            return 1
        },
        cancelAnimationFrame: () => {
            canceled += 1
        }
    })
    const box = { clientWidth: 820, clientHeight: 600 }
    let begins = 0
    let finishes = 0
    let rebuilds = 0
    const vm = {
        $refs: { box },
        containerWidth: 820,
        clientHeight: 600,
        scrollTop: 0,
        displayLines: [mkLog(1218)],
        heightIndex: vw.createHeightIndex().rebuild([mkLog(1218)], {}),
        wrap: true,
        cancelPendingWheel () {},
        beginLayoutUpdate (options) {
            begins += 1
            assert.equal(options.usePreviousIndex, true)
            return { anchor: { lineKey: '1218' } }
        },
        finishLayoutUpdate () {
            finishes += 1
        },
        rebuildHeightIndex () {
            rebuilds += 1
        },
        unbindObserver () {}
    }
    Object.assign(vm, options.methods)
    vm.beginLayoutUpdate = options => {
        begins += 1
        assert.equal(options.usePreviousIndex, true)
        return { anchor: { lineKey: '1218' } }
    }
    vm.finishLayoutUpdate = () => {
        finishes += 1
    }
    vm.rebuildHeightIndex = () => {
        rebuilds += 1
    }
    vm.bindObserver()
    callback()
    frame()
    assert.equal(begins, 0, 'same width and height does not create a redundant layout restore')

    box.clientWidth = 1258
    callback()
    frame()
    assert.equal(begins, 1)
    assert.equal(finishes, 1)
    assert.equal(rebuilds, 1)
    assert.equal(vm.containerWidth, 1258)
    assert.equal(canceled, 0)
})

test('Vue2 render snapshot follows row mutations and pagination but ignores keyword changes', async () => {
    const logs = [{ text: 'alpha', level: 'INFO' }, { text: 'beta', level: 'INFO' }]
    let renderCount = 0
    const vm = new Vue({
        data: () => ({ logs, displayBase: 10, keyword: '', renderLines: shallowRef([]) }),
        computed: {
            displayLines () {
                return withDisplayLineNumbers(this.logs, this.displayBase)
            }
        },
        created () {
            this.$watch('displayLines', next => {
                this.renderLines = next
            }, { immediate: true })
        },
        render (h) {
            renderCount += 1
            return h('div', this.renderLines.map(line => h('i', { class: line.level }, `${line.displayLineNo}:${line.text}:${this.keyword}`)))
        }
    })
    const flush = () => new Promise(resolve => Vue.nextTick(() => Vue.nextTick(resolve)))
    const renderedText = () => vm._vnode.children.map(node => node.children[0].text)
    vm.$mount()
    await flush()
    assert.deepEqual(renderedText(), ['10:alpha:', '11:beta:'])
    let snapshot = vm.renderLines
    vm.keyword = 'alpha'
    await flush()
    assert.equal(vm.renderLines, snapshot)
    assert.deepEqual(renderedText(), ['10:alpha:alpha', '11:beta:alpha'])
    logs[0].text = 'changed'
    logs[0].level = 'WARN'
    await flush()
    assert.notEqual(vm.renderLines, snapshot)
    assert.deepEqual(renderedText(), ['10:changed:alpha', '11:beta:alpha'])
    snapshot = vm.renderLines
    logs.push({ text: 'gamma', level: 'ERROR' })
    await flush()
    assert.notEqual(vm.renderLines, snapshot)
    assert.deepEqual(renderedText(), ['10:changed:alpha', '11:beta:alpha', '12:gamma:alpha'])
    logs.unshift({ text: 'zero', level: 'INFO' })
    await flush()
    assert.deepEqual(renderedText(), ['10:zero:alpha', '11:changed:alpha', '12:beta:alpha', '13:gamma:alpha'])
    vm.displayBase = 100
    await flush()
    assert.deepEqual(renderedText(), ['100:zero:alpha', '101:changed:alpha', '102:beta:alpha', '103:gamma:alpha'])
    assert.ok(renderCount >= 5)
    vm.$destroy()
})

test('preserveHeightMapForLogs keeps heights by lineNo after prepend/reorder', () => {
    const map = { 10: 20, 11: 40, 12: 60 }
    const reordered = [mkLog(12), mkLog(10), mkLog(11)]
    const prepended = [mkLog(9), mkLog(10), mkLog(11), mkLog(12)]
    assert.deepEqual(vw.preserveHeightMapForLogs(map, reordered), { 10: 20, 11: 40, 12: 60 })
    assert.deepEqual(vw.preserveHeightMapForLogs(map, prepended), { 10: 20, 11: 40, 12: 60 })
    assert.equal(vw.preserveHeightMapForLogs(map, prepended)[9], undefined)
})

test('HeightIndex prefix/total/update after single height change', () => {
    const logs = [mkLog(1), mkLog(2), mkLog(3)]
    const map = { 1: 20, 2: 40, 3: 20 }
    const index = vw.createHeightIndex().rebuild(logs, map)
    assert.equal(index.prefix(0), 0)
    assert.equal(index.prefix(1), 20)
    assert.equal(index.prefix(2), 60)
    assert.equal(index.total(), 80)
    assert.equal(index.setHeight('2', 80), true)
    assert.equal(index.prefix(2), 100)
    assert.equal(index.total(), 120)
})

test('HeightIndex findIndexByOffset uses fenwick lifting without repeated prefix', () => {
    const logs = [mkLog(1), mkLog(2), mkLog(3)]
    const map = { 1: 20, 2: 80, 3: 20 }
    const index = vw.createHeightIndex().rebuild(logs, map)
    index.resetOpCount()
    assert.equal(index.findIndexByOffset(0), 0)
    assert.equal(index.findIndexByOffset(25), 1)
    assert.equal(index.findIndexByOffset(120), 2)
    assert.ok(index.getOpCount() <= Math.ceil(Math.log2(3)) * 3 + 4)
})

test('HeightIndex query counters stay non-enumerable while retaining operation diagnostics', () => {
    const index = vw.createHeightIndex().rebuild([mkLog(1), mkLog(2), mkLog(3)], {}, false)
    assert.equal(Object.keys(index).includes('_opCount'), false)
    assert.equal(Object.keys(index.tree).includes('_opCount'), false)
    index.resetOpCount()
    index.total()
    assert.ok(index.getOpCount() > 0)
})

test('Fenwick build is linear in n', () => {
    const index = vw.createHeightIndex()
    const n = 5000
    const logs = Array.from({ length: n }, (_, i) => mkLog(i + 1))
    index.resetOpCount()
    index.rebuild(logs, {}, false)
    assert.ok(index.tree.getOpCount() <= n * 2 + 8, `build ops ${index.tree.getOpCount()} not O(n)`)
})

test('resolveVirtualWindowFromIndex positions window in O(log n)', () => {
    const logs = [mkLog(1), mkLog(2), mkLog(3), mkLog(4), mkLog(5)]
    const map = { 1: 20, 2: 40, 3: 20, 4: 60, 5: 20 }
    const index = vw.createHeightIndex().rebuild(logs, map)
    index.resetOpCount()
    const win = vw.resolveVirtualWindowFromIndex(index, {
        scrollTop: 45,
        clientHeight: 40,
        overscan: 0
    })
    assert.equal(win.start, 1)
    assert.equal(win.padTop, 20)
    assert.equal(win.total, 160)
    const logN = Math.ceil(Math.log2(5))
    assert.ok(index.getOpCount() <= logN * 6 + 8)
})

test('dynamic virtual buffer grows with viewport and recent movement but stays bounded', () => {
    assert.equal(vw.resolveVirtualOverscanPx({ clientHeight: 800, dynamicOverscan: true }), 1200)
    assert.equal(vw.resolveVirtualOverscanPx({ clientHeight: 400, scrollDelta: 600, dynamicOverscan: true }), 750)
    assert.equal(vw.resolveVirtualOverscanPx({ clientHeight: 800, scrollDelta: 10000, dynamicOverscan: true }), vw.MAX_VIRTUAL_OVERSCAN_PX)
    assert.equal(vw.resolveVirtualOverscanPx({ clientHeight: 800, dynamicOverscan: false, overscan: 0 }), 0)
})

test('dynamic window covers a large viewport after fast bidirectional movement without rendering the jump span', () => {
    const logs = Array.from({ length: 10000 }, (_, i) => mkLog(i + 1))
    const index = vw.createHeightIndex().rebuild(logs, {}, false)
    const windowAt = (scrollTop, scrollDelta) => vw.resolveVirtualWindowFromIndex(index, {
        scrollTop,
        clientHeight: 900,
        scrollDelta,
        dynamicOverscan: true
    })
    const forward = windowAt(150000, 120000)
    assert.ok(forward.start < 7500 && forward.end > 7500)
    assert.ok(forward.end - forward.start < 500)
    const backward = windowAt(30000, -120000)
    assert.ok(backward.start < 1500 && backward.end > 1500)
    assert.ok(backward.end - backward.start < 500)
})

test('dynamic window uses measured long-line heights and jumps directly to both ends', () => {
    const logs = Array.from({ length: 10000 }, (_, i) => mkLog(i + 1))
    const heights = { 1: 20, 5000: 1640, 10000: 20 }
    const index = vw.createHeightIndex().rebuild(logs, heights)
    const middle = vw.resolveVirtualWindowFromIndex(index, {
        scrollTop: index.prefix(4999), clientHeight: 900, scrollDelta: 0, dynamicOverscan: true
    })
    assert.ok(middle.start <= 4999 && middle.end > 4999)
    assert.ok(middle.end - middle.start < 500)
    const head = vw.resolveVirtualWindowFromIndex(index, { scrollTop: 0, clientHeight: 600, dynamicOverscan: true })
    const tail = vw.resolveVirtualWindowFromIndex(index, { scrollTop: index.total(), clientHeight: 600, dynamicOverscan: true })
    assert.equal(head.start, 0)
    assert.equal(tail.end, logs.length)
    assert.ok(head.end < 500)
    assert.ok(tail.end - tail.start < 500)
})

test('captureAnchorFromDom uses viewportTop relative to scroll box', () => {
    const anchor = vw.captureAnchorFromDom({
        rowTop: 110,
        boxTop: 150,
        lineKey: '7',
        lineNo: '7'
    })
    assert.deepEqual(anchor, { lineKey: '7', lineNo: '7', viewportTop: -40 })
})

test('parseScrollBoxPaddingTop reads css padding-top px value', () => {
    assert.equal(vw.parseScrollBoxPaddingTop({ paddingTop: '8px' }), 8)
    assert.equal(vw.parseScrollBoxPaddingTop({ paddingTop: '0' }), 0)
    assert.equal(vw.parseScrollBoxPaddingTop(null), 0)
})

test('scrollTopForAnchor round-trips with css padding only not virtual pad', () => {
    const CSS_PAD = 8
    const logs = Array.from({ length: 900 }, (_, i) => mkLog(i + 1))
    const map = {}
    for (let i = 1; i <= 900; i++) map[String(i)] = 20
    const index = vw.createHeightIndex().rebuild(logs, map)
    const lineIndex = 450
    const scrollTop = index.prefix(lineIndex) + 12 - CSS_PAD
    const anchor = vw.captureAnchorFromIndex(index, {
        scrollTop,
        paddingTop: CSS_PAD,
        lineKey: String(lineIndex + 1),
        lineIndex
    })
    assert.equal(anchor.viewportTop, CSS_PAD + index.prefix(lineIndex) - scrollTop)
    assert.equal(vw.scrollTopForAnchor(index, anchor, { paddingTop: CSS_PAD, logs }), scrollTop)
    const virtualPadTop = index.prefix(100)
    assert.notEqual(
        vw.scrollTopForAnchor(index, anchor, { paddingTop: virtualPadTop, logs }),
        scrollTop
    )
})

test('scrollTopForAnchor round-trips viewportTop with css padding', () => {
    const logs = [mkLog(1), mkLog(2), mkLog(3)]
    const map = { 1: 20, 2: 40, 3: 20 }
    const index = vw.createHeightIndex().rebuild(logs, map)
    const paddingTop = 8
    const scrollTop = 38
    const anchor = vw.captureAnchorFromIndex(index, {
        scrollTop,
        paddingTop,
        lineKey: '2',
        lineIndex: 1
    })
    assert.equal(anchor.viewportTop, paddingTop + index.prefix(1) - scrollTop)
    assert.equal(vw.scrollTopForAnchor(index, anchor, { paddingTop, logs }), scrollTop)
})

test('scrollTopForAnchor keeps partial 40px row viewportTop after prepend', () => {
    const CSS_PAD = 8
    const beforeLogs = [mkLog(10), mkLog(11), mkLog(12)]
    const map = { 10: 20, 11: 40, 12: 20 }
    const beforeIndex = vw.createHeightIndex().rebuild(beforeLogs, map)
    const viewportTop = -10
    const anchor = { lineKey: '11', lineNo: 11, viewportTop }
    const scrollTopBefore = vw.scrollTopForAnchor(beforeIndex, anchor, { paddingTop: CSS_PAD, logs: beforeLogs })

    const afterLogs = [mkLog(8), mkLog(9), mkLog(10), mkLog(11), mkLog(12)]
    const afterIndex = vw.createHeightIndex().rebuild(afterLogs, map)
    const scrollTopAfter = vw.scrollTopForAnchor(afterIndex, anchor, { paddingTop: CSS_PAD, logs: afterLogs })

    assert.equal(scrollTopBefore, CSS_PAD + beforeIndex.prefix(1) - viewportTop)
    assert.equal(scrollTopAfter, CSS_PAD + afterIndex.prefix(3) - viewportTop)
})

test('variable heights at 800 preserve anchor prefix into line 801', () => {
    const CSS_PAD = 8
    const map = {}
    const logs800 = []
    for (let i = 1; i <= 800; i++) {
        map[String(i)] = i % 5 === 0 ? 40 : 20
        logs800.push(mkLog(i))
    }
    const index800 = vw.createHeightIndex().rebuild(logs800, map)
    const lineIndex = 350
    const scrollTop = index800.prefix(lineIndex) + 5
    const anchor = vw.captureAnchorFromIndex(index800, {
        scrollTop,
        paddingTop: CSS_PAD,
        lineKey: String(lineIndex + 1),
        lineIndex
    })
    const logs801 = [...logs800, mkLog(801)]
    map['801'] = 20
    const index801 = vw.createHeightIndex().rebuild(logs801, map)
    assert.equal(index801.prefix(800), index800.total())
    assert.equal(vw.scrollTopForAnchor(index801, anchor, { paddingTop: CSS_PAD, logs: logs801 }), scrollTop)
})

test('resolveScrollToBottomState clears pending anchor and sets scrollTop', () => {
    const next = vw.resolveScrollToBottomState({
        pendingAnchor: { lineKey: '1', viewportTop: 0 },
        scrollHeight: 5000,
        clientHeight: 400
    })
    assert.equal(next.pendingAnchor, null)
    assert.equal(next.scrollTop, 4600)
})

test('scrollTopForAnchor stable across nonvirtual to virtual threshold', () => {
    const CSS_PAD = 8
    const map = {}
    for (let i = 1; i <= 801; i++) map[String(i)] = 20
    const logs = Array.from({ length: 801 }, (_, i) => mkLog(i + 1))
    const index = vw.createHeightIndex().rebuild(logs, map)
    const lineIndex = 400
    const scrollTop = index.prefix(lineIndex) + 5
    const anchor = vw.captureAnchorFromIndex(index, {
        scrollTop,
        paddingTop: CSS_PAD,
        lineKey: String(lineIndex + 1),
        lineIndex
    })
    assert.equal(anchor.viewportTop, CSS_PAD + index.prefix(lineIndex) - scrollTop)
    assert.equal(vw.scrollTopForAnchor(index, anchor, { paddingTop: CSS_PAD, logs }), scrollTop)
})

test('10k resolveVirtualWindow avoids linear scan per call', () => {
    const n = 10000
    const logs = Array.from({ length: n }, (_, i) => mkLog(i + 1))
    const map = {}
    for (let i = 0; i < n; i++) map[String(i + 1)] = 20 + (i % 3) * 10
    const index = vw.createHeightIndex().rebuild(logs, map)
    const logN = Math.ceil(Math.log2(n))
    const maxOpsPerCall = logN * 8 + 12
    for (let pass = 0; pass < 20; pass++) {
        index.resetOpCount()
        const scrollTop = pass * 500
        vw.resolveVirtualWindowFromIndex(index, { scrollTop, clientHeight: 400, overscan: 30 })
        assert.ok(
            index.getOpCount() <= maxOpsPerCall,
            `pass ${pass}: ${index.getOpCount()} ops exceeds ${maxOpsPerCall}`
        )
    }
})

test('shouldAcceptMeasuredHeight ignores tiny jitter', () => {
    assert.equal(vw.shouldAcceptMeasuredHeight(20, 20.5), false)
    assert.equal(vw.shouldAcceptMeasuredHeight(20, 24), true)
    assert.equal(vw.shouldAcceptMeasuredHeight(undefined, 40), true)
})
