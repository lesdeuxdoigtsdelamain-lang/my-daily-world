from pathlib import Path
from datetime import datetime, timezone
import pytest
from generator.app.settings import Feed,Settings
from generator.app.models import Edition

ROOT=Path(__file__).resolve().parents[2]
@pytest.fixture(autouse=True)
def offline_dns(monkeypatch):
    # Even the SSRF resolver is isolated from external DNS during CI.
    monkeypatch.setattr('socket.getaddrinfo',lambda *a,**k:[(2,1,6,'',('93.184.216.34',443))])
@pytest.fixture
def now():return datetime(2026,11,16,17,30,tzinfo=timezone.utc)
@pytest.fixture
def feed():return Feed(id='test',publisher='NASA',url='https://www.nasa.gov/feed/',
    article_hosts=['www.nasa.gov'],category='Science & Tech',reuse='public_domain',ai_allowed=True,
    terms_url='https://www.nasa.gov/nasa-brand-center/images-and-media/')
@pytest.fixture
def settings(feed):return Settings(feeds=[feed])
@pytest.fixture
def sample():return Edition.model_validate_json((ROOT/'data/fixtures/sample.json').read_text())
@pytest.fixture
def edition(sample,now):
    # Offline test object only: never installed into site or labelled live evidence.
    return sample.model_copy(update={'sample':False,'sample_label':None,'edition_date':now.date(),
        'edition_id':f'{now.date()}-en-GB','generated_at':now})

def rss(title='Researchers measure sunlight in a new experiment',link='https://www.nasa.gov/news/test/',
        description='The team measured sunlight. The experiment used careful observations.',when='Mon, 16 Nov 2026 12:00:00 GMT'):
    from xml.sax.saxutils import escape
    return f'<rss version="2.0"><channel><item><title>{escape(title)}</title><link>{escape(link)}</link><description>{escape(description)}</description><pubDate>{when}</pubDate></item></channel></rss>'.encode()
