"""Optional live diagnostics. Never enables a source or republishes feed content."""
from .settings import load
from .feeds import parse_feed
from .network import SafeHttp,FetchError
from .main import ROOT
from datetime import datetime,timezone
from urllib.parse import urlsplit

def main():
    settings=load(ROOT/'settings.example.json')
    http=SafeHttp({urlsplit(f.url).hostname for f in settings.feeds})
    for f in settings.feeds:
        try:
            b=http.get(f.url)
            n=len(parse_feed(b,f,datetime.now(timezone.utc)))
            print(f'{f.id}: reachable; {n} current eligible items; publication enabled={f.enabled}')
        except FetchError as e:print(f'{f.id}: {e}; skipped')
    print('Availability is not a reuse licence. No files published; no AI calls.')

if __name__=='__main__':main()
