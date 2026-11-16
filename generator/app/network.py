"""HTTPS requests only; exact hosts, no redirect-based SSRF, size/time limits."""
from urllib.parse import urlsplit
import ipaddress
import socket
import httpx

class FetchError(Exception):
    pass

class SafeHttp:
    def __init__(self, hosts: set[str], client: httpx.Client | None = None):
        self.hosts = hosts
        self.client = client or httpx.Client(timeout=httpx.Timeout(18, connect=8), follow_redirects=False,
            headers={'User-Agent':'MyDailyWorld/1.0 (family educational RSS reader; no tracking)'})

    def get(self, url: str, limit=1_500_000) -> bytes:
        for _ in range(3):
            u = urlsplit(url)
            if (u.scheme != 'https' or u.hostname not in self.hosts or u.username or u.password
                    or u.port not in (None, 443)):
                raise FetchError('URL is outside the source allowlist')
            # Never fetch private/local destinations, even for a misconfigured allowed host.
            try:
                addresses = socket.getaddrinfo(u.hostname, 443, type=socket.SOCK_STREAM)
                if any(not ipaddress.ip_address(a[4][0]).is_global for a in addresses):
                    raise FetchError('Private network destination')
            except socket.gaierror:
                # The managed HTTPS proxy may resolve names remotely. The exact host
                # allowlist and TLS hostname verification remain mandatory.
                pass
            try:
                with self.client.stream('GET', url) as r:
                    if r.status_code in (301,302,303,307,308):
                        from urllib.parse import urljoin
                        url = urljoin(url, r.headers.get('location','')); continue
                    if r.status_code != 200: raise FetchError(f'HTTP {r.status_code}')
                    chunks=[]; size=0
                    for chunk in r.iter_bytes():
                        size += len(chunk)
                        if size > limit: raise FetchError('Response too large')
                        chunks.append(chunk)
                    return b''.join(chunks)
            except httpx.HTTPError:
                raise FetchError('Network unavailable') from None
        raise FetchError('Too many redirects')
