import { KEEP_STACK } from './env';
import { stopStack } from './stack';

export default function globalTeardown(): void {
  if (!KEEP_STACK) {
    stopStack();
  }
}
