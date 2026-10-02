// Host integration example using only Node.js built-ins.
// node examples/aihub-host.mjs <managed-agent-executable> <initialization-json-file>
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';
import { createInterface } from 'node:readline';

const [executable, configurationFile] = process.argv.slice(2);
if (!executable || !configurationFile) {
  throw new Error('Usage: node aihub-host.mjs <executable> <initialization-json-file>');
}
const params = JSON.parse(readFileSync(configurationFile, 'utf8'));
const child = spawn(executable, [], {
  windowsHide: true,
  stdio: ['pipe', 'pipe', 'inherit'],
});
const send = (message) => child.stdin.write(JSON.stringify(message) + '\n');
let stopping = false;
function shutdown() {
  if (stopping) return;
  stopping = true;
  send({ jsonrpc: '2.0', id: 2, method: 'agent.shutdown', params: { mode: 'drain' } });
}
child.once('spawn', () => {
  send({ jsonrpc: '2.0', id: 1, method: 'agent.initialize', params });
});
child.stdin.on('error', (error) => {
  if (error.code !== 'EPIPE') console.error('agent input failed:', error.message);
});
createInterface({ input: child.stdout }).on('line', (line) => {
  const message = JSON.parse(line);
  if (message.error) console.error('agent request failed:', message.error);
  else if (message.method === 'agent.ready') console.error('agent is ready');
  else if (message.method === 'agent.stopped') console.error('agent stopped:', message.params.success, message.params.message);
  else if (message.method === 'agent.event') console.error('agent event:', message.params.type);
  // Production hosts should retain completionUnreported payloads for reconciliation.
  // Do not log the full initialization message or arbitrary build payloads.
});
process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);
child.on('error', (error) => {
  console.error('cannot start managed agent:', error.message);
  process.exitCode = 1;
});
child.on('exit', (code, signal) => {
  console.error('agent process exited:', code, signal ?? '');
  process.exitCode = code ?? 1;
});
// Keep stdin open while the agent is running. EOF requests a graceful drain.
// The real supervisor owns its shutdown deadline and whole-process-tree termination.
