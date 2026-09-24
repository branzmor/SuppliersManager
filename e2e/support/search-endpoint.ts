import type { Page, Request, Route } from '@playwright/test';

const isSearchRequest = (url: URL) => url.pathname === '/suppliers/potential';

/**
 * The browser's view of GET /suppliers/potential. It always records the requests the page sends;
 * the stubbing methods are only used by the @network scenarios, which need failure and timing
 * conditions the real backend can't produce on demand.
 */
export class SearchEndpoint {
  private readonly sentRequests: Request[] = [];
  private releaseHeldResponses?: () => void;

  constructor(private readonly page: Page) {
    page.on('request', (request) => {
      if (isSearchRequest(new URL(request.url()))) {
        this.sentRequests.push(request);
      }
    });
  }

  get requestCount(): number {
    return this.sentRequests.length;
  }

  /** Lets requests reach the real backend but holds their responses until {@link releaseResponses}. */
  async holdResponses(): Promise<void> {
    const released = new Promise<void>((resolve) => {
      this.releaseHeldResponses = resolve;
    });
    await this.page.route(isSearchRequest, async (route) => {
      await released;
      await route.continue();
    });
  }

  releaseResponses(): void {
    if (!this.releaseHeldResponses) {
      throw new Error('No responses are being held - call holdResponses() first');
    }
    this.releaseHeldResponses();
  }

  async failWith(status: number, info?: string): Promise<void> {
    await this.page.route(isSearchRequest, (route: Route) =>
      route.fulfill({
        status,
        // The page calls the API cross-origin, so a stubbed response still has to pass CORS.
        headers: { 'access-control-allow-origin': '*' },
        ...(info === undefined ? { body: '' } : { json: { info } }),
      }),
    );
  }

  async failConnection(): Promise<void> {
    await this.page.route(isSearchRequest, (route) => route.abort('internetdisconnected'));
  }

  async restore(): Promise<void> {
    await this.page.unroute(isSearchRequest);
  }
}
