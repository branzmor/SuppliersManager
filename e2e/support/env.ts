// Single source of truth for where the E2E stack is reachable from the host. The same variables
// are read by docker-compose.e2e.yml (ports + the frontend's baked-in API URL), so overriding
// them once moves the whole stack and the tests together.
const frontendPort = process.env.E2E_FRONTEND_PORT ?? '15173';
const backendPort = process.env.E2E_BACKEND_PORT ?? '18080';

export const FRONTEND_URL = `http://localhost:${frontendPort}`;
export const API_URL = `http://localhost:${backendPort}`;

/** Leave the stack running after the suite (e.g. to inspect it); it is always recreated on the next run. */
export const KEEP_STACK = process.env.E2E_KEEP_STACK === '1';
