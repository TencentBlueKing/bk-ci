import { strict as assert } from 'node:assert'
import { createAutoSendGate } from './analysisAutoSend.ts'

const prompt = '这条流水线是做什么的，各阶段分别在干什么？'
const payload = { prompt }

{
  const gate = createAutoSendGate()
  const gen = gate.beginInit()
  gate.queue(payload)
  assert.equal(gate.take(), null)
  assert.equal(gate.endInit(gen), true)
  assert.deepEqual(gate.take(), payload)
  assert.equal(gate.take(), null)
}

{
  const gate = createAutoSendGate()
  const first = gate.beginInit()
  gate.queue(payload)
  const second = gate.beginInit()
  assert.equal(gate.endInit(first), false)
  assert.equal(gate.take(), null)
  assert.equal(gate.endInit(second), true)
  assert.deepEqual(gate.take(), payload)
}

{
  const gate = createAutoSendGate()
  gate.queue(payload)
  assert.deepEqual(gate.take(), payload)
}

{
  const gate = createAutoSendGate()
  gate.queue({ prompt: 'old' })
  gate.queue(payload)
  assert.deepEqual(gate.take(), payload)
}

{
  const gate = createAutoSendGate()
  const gen = gate.beginInit()
  assert.equal(gate.isCurrent(gen), true)
  gate.beginInit()
  assert.equal(gate.isCurrent(gen), false)
}

console.log('analysisAutoSend tests passed')
