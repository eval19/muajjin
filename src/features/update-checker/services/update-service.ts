const GITHUB_RELEASES_URL =
  'https://api.github.com/repos/eval19/muajjin/releases/latest';

export type ReleaseInfo = {
  latestVersion: string;
  releaseName?: string;
  downloadUrl?: string | null;
  releaseUrl?: string | null;
};

const normalizeVersion = (version: string) =>
  version.trim().replace(/^v/i, '').split('+')[0];

const parseVersion = (version: string) =>
  normalizeVersion(version)
    .split('.')
    .map((part) => Number.parseInt(part, 10))
    .filter((part) => Number.isFinite(part));

export const isNewerVersion = (
  currentVersion: string,
  latestVersion: string,
) => {
  const currentParts = parseVersion(currentVersion);
  const latestParts = parseVersion(latestVersion);
  const maxLength = Math.max(currentParts.length, latestParts.length);

  for (let i = 0; i < maxLength; i += 1) {
    const current = currentParts[i] ?? 0;
    const latest = latestParts[i] ?? 0;
    if (latest > current) return true;
    if (latest < current) return false;
  }

  return false;
};

export const fetchLatestRelease = async (
  signal?: AbortSignal,
): Promise<ReleaseInfo | null> => {
  if (signal?.aborted) return null;

  const controller = new AbortController();
  const abort = () => controller.abort();
  signal?.addEventListener('abort', abort, { once: true });
  const deadline = setTimeout(abort, 10_000);

  try {
    const response = await fetch(GITHUB_RELEASES_URL, {
      signal: controller.signal,
      headers: {
        Accept: 'application/vnd.github+json',
      },
    });

    if (!response.ok) {
      return null;
    }

    const data = await response.json();
    if (controller.signal.aborted) return null;

    const tag = typeof data?.tag_name === 'string' ? data.tag_name : '';
    const name = typeof data?.name === 'string' ? data.name : '';
    const latestVersion = normalizeVersion(tag || name);

    if (!latestVersion) {
      return null;
    }

    const assets = Array.isArray(data?.assets) ? data.assets : [];
    const apkAsset = assets.find(
      (asset: any) =>
        typeof asset?.name === 'string' &&
        asset.name.toLowerCase().endsWith('.apk'),
    );

    return {
      latestVersion,
      releaseName: name || tag || latestVersion,
      downloadUrl: apkAsset?.browser_download_url ?? null,
      releaseUrl: typeof data?.html_url === 'string' ? data.html_url : null,
    };
  } catch {
    return null;
  } finally {
    clearTimeout(deadline);
    signal?.removeEventListener('abort', abort);
  }
};
