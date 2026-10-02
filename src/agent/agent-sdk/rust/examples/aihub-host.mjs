// node examples/aihub-host.mjs <managed-agent-executable> <configuration-json-file>
import { spawn } from 'node:child_process';
import { readFileSync } from 'node:fs';

const [executable, configurationFile] = process.argv.slice(2);
if (!executable || !configurationFile) {
  throw new Error('Usage: node aihub-host.mjs <executable> <configuration-json-file>');
}
const configuration = JSON.parse(readFileSync(configurationFile, 'utf8'));
const argument = Buffer.from(JSON.stringify(configuration), 'utf8').toString('base64');
const child = spawn(executable, [argument], {
  shell: false,
  windowsHide: true,
  stdio: ['ignore', 'ignore', 'inherit'],
});
const stop = () => child.kill('SIGKILL');
process.on('SIGINT', stop);
process.on('SIGTERM', stop);
child.on('error', (error) => {
  // Do not print spawnargs: the sole argument contains the agent credentials.
  console.error('cannot start managed agent:', error.code);
  process.exitCode = 1;
});
child.on('exit', (code, signal) => {
  console.error('agent process exited:', code, signal ?? '');
  process.exitCode = code ?? 1;
});
// This example terminates the agent itself. Production aihub must also own the worker process
// tree (for example via a Windows Job Object or an OS process group/container).
