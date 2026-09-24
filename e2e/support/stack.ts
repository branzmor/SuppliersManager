import { execFileSync } from 'node:child_process';
import path from 'node:path';

// Lifecycle of the dockerised E2E stack (docker-compose.e2e.yml), driven from globalSetup /
// globalTeardown rather than Playwright's `webServer`: `webServer` manages one foreground process
// polled on one URL, whereas this is four detached services with their own healthchecks
// (`--wait`) that must be reset to an empty database before seeding and removed afterwards.

const COMPOSE_FILE = path.resolve(import.meta.dirname, '..', 'docker-compose.e2e.yml');
const READY_TIMEOUT_SECONDS = 600;

function compose(...args: string[]): void {
  execFileSync('docker', ['compose', '-f', COMPOSE_FILE, ...args], { stdio: 'inherit' });
}

/**
 * Recreates the E2E stack from scratch and blocks until every service reports healthy. The
 * preceding `down` guarantees an empty database even if a previous run was interrupted or kept
 * its stack with E2E_KEEP_STACK.
 */
export function startStack(): void {
  stopStack();
  try {
    compose('up', '--detach', '--build', '--wait', '--wait-timeout', String(READY_TIMEOUT_SECONDS));
  } catch (error) {
    // Surface the containers' own output - "container unhealthy" alone says nothing useful in CI.
    compose('logs', '--tail', '200');
    throw error;
  }
}

export function stopStack(): void {
  compose('down', '--volumes', '--remove-orphans');
}
