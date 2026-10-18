import { expect, test } from 'bun:test';
import { fetchLatestRelease } from '../src/features/update-checker/services/update-service';

const latestReleaseUrl = 'https://api.github.com/repos/eval19/muajjin/releases/latest';
const release = {
  tag_name: 'v2.8.4',
  name: 'Muajjin 2.8.4',
  html_url: 'https://github.com/eval19/muajjin/releases/tag/v2.8.4',
  assets: [{ name: 'muajjin.apk', browser_download_url: 'https://example.com/muajjin.apk' }],
};

// Fake clocks cannot drive TCP/body I/O. Bound failures so finally still restores
// shared globals; successful checks wait on network events, never a fixed delay.
async function withNetworkWatchdog<T>(pending: Promise<T>): Promise<T> {
  let cancelWatchdog: (() => void) | undefined;
  try {
    return await Promise.race([
      pending,
      new Promise<never>((_resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('Release request did not settle')), 2000);
        cancelWatchdog = () => clearTimeout(timer);
      }),
    ]);
  } finally {
    cancelWatchdog?.();
  }
}

test('cancelled release checks settle null during headers and body reads without blocking a later check', async () => {
  let mode: 'valid' | 'headers' | 'body' = 'valid';
  let receivedHeadersRequest: (() => void) | undefined;
  let receivedBodyResponse: (() => void) | undefined;
  let finishHeadersRequest: (() => void) | undefined;
  const server = Bun.serve({
    hostname: '127.0.0.1',
    port: 0,
    fetch() {
      if (mode === 'headers') {
        return new Promise<Response>((resolve) => {
          finishHeadersRequest = () => resolve(Response.json(release));
          receivedHeadersRequest?.();
        });
      }
      if (mode === 'body') {
        return new Response(new ReadableStream({
          start(controller) {
            controller.enqueue(new TextEncoder().encode('{"tag_name":'));
          },
        }), { headers: { 'Content-Type': 'application/json' } });
      }
      return Response.json(release);
    },
  });
  const originalFetch = globalThis.fetch;
  const controllers: AbortController[] = [];

  try {
    globalThis.fetch = ((input: Parameters<typeof fetch>[0], init?: Parameters<typeof fetch>[1]) => {
      const url = input instanceof Request ? input.url : String(input);
      if (url !== latestReleaseUrl) return originalFetch(input, init);
      return originalFetch(server.url, init).then((response) => {
        if (mode === 'body') receivedBodyResponse?.();
        return response;
      });
    }) as typeof fetch;

    const alreadyCancelled = new AbortController();
    controllers.push(alreadyCancelled);
    alreadyCancelled.abort();
    expect(await withNetworkWatchdog(fetchLatestRelease(alreadyCancelled.signal))).toBeNull();

    mode = 'headers';
    const headersStarted = new Promise<void>((resolve) => { receivedHeadersRequest = resolve; });
    const headersController = new AbortController();
    controllers.push(headersController);
    const headersPending = fetchLatestRelease(headersController.signal);
    await withNetworkWatchdog(headersStarted);
    headersController.abort();
    expect(await withNetworkWatchdog(headersPending)).toBeNull();
    finishHeadersRequest?.();

    mode = 'body';
    const bodyStarted = new Promise<void>((resolve) => { receivedBodyResponse = resolve; });
    const bodyController = new AbortController();
    controllers.push(bodyController);
    const bodyPending = fetchLatestRelease(bodyController.signal);
    await withNetworkWatchdog(bodyStarted);
    bodyController.abort();
    expect(await withNetworkWatchdog(bodyPending)).toBeNull();

    mode = 'valid';
    expect(await withNetworkWatchdog(fetchLatestRelease())).toEqual({
      latestVersion: '2.8.4',
      releaseName: release.name,
      releaseUrl: release.html_url,
      downloadUrl: release.assets[0].browser_download_url,
    });
  } finally {
    for (const controller of controllers) controller.abort();
    globalThis.fetch = originalFetch;
    finishHeadersRequest?.();
    server.stop(true);
  }
});
