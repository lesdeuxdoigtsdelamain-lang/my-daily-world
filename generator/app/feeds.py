from dataclasses import dataclass
from datetime import datetime, timezone, timedelta
from email.utils import parsedate_to_datetime
from hashlib import sha256
from urllib.parse import urlsplit, urlunsplit
from difflib import SequenceMatcher
import re
from defusedxml import ElementTree
from .settings import Feed, Settings
from .network import SafeHttp, FetchError
from .safety import clean, excluded, contested, normal

@dataclass
class Candidate:
    id: str
    title: str
    excerpt: str
    url: str
    published_at: datetime
    retrieved_at: datetime
    feed: Feed
    attributed: bool
    author: str | None = None
    article_text: str = ''

def canonical(url: str, hosts: list[str]) -> str | None:
    if len(url)>2048 or any(c.isspace() for c in url): return None
    try:
        u=urlsplit(url.strip())
        if u.port not in (None,443): return None
    except ValueError: return None
    if u.scheme == 'http': u=u._replace(scheme='https')
    if u.scheme != 'https' or u.hostname not in hosts or u.username or u.password:
        return None
    # Query strings are tracking in these RSS sources; preserve article path only.
    return urlunsplit((u.scheme,u.netloc,u.path.rstrip('/'),'', ''))

def parse_date(text: str) -> datetime | None:
    try:
        try: d=parsedate_to_datetime(text)
        except (TypeError,ValueError): d=datetime.fromisoformat(text.replace('Z','+00:00'))
        return d.astimezone(timezone.utc) if d.tzinfo else None
    except (ValueError,TypeError,OverflowError): return None

def parse_feed(raw: bytes, feed: Feed, now: datetime, max_age=36) -> list[Candidate]:
    try: root=ElementTree.fromstring(raw)
    except Exception: raise FetchError('Invalid or unsafe RSS XML') from None
    items=list(root.findall('.//item'))+list(root.findall('.//{http://www.w3.org/2005/Atom}entry'))
    def val(item, names):
        for name in names:
            e=item.find(name)
            if e is not None:
                if name.endswith('link') and e.get('href'): return e.get('href')
                return ''.join(e.itertext())
        return ''
    out=[]
    for item in items[:100]:
        title=clean(val(item,['title','{http://www.w3.org/2005/Atom}title']),1000)
        if len(title)>250: continue
        excerpt=clean(val(item,['description','{http://www.w3.org/2005/Atom}summary']),1000)
        if any(c in title+excerpt for c in '<>'): continue
        url=canonical(val(item,['link','{http://www.w3.org/2005/Atom}link']),feed.article_hosts)
        dt=parse_date(val(item,['pubDate','{http://www.w3.org/2005/Atom}published','{http://www.w3.org/2005/Atom}updated']))
        if not dt or not url or len(title)<10 or excluded(title+' '+excerpt): continue
        if not now-timedelta(hours=max_age)<=dt<=now+timedelta(minutes=5): continue
        author=clean(val(item,['{http://purl.org/dc/elements/1.1/}creator','{http://www.w3.org/2005/Atom}author']),100) or None
        if feed.reuse=='licensed_excerpt' and not author: continue
        text=title+' '+excerpt
        # Explicit editorial topic rules, never inferred from a child's identity.
        category=feed.category
        if re.search(r'\b(?:climate|glacier|flood|wildlife|forest|emissions|saltmarsh|restoration|biodiversity|marine)\b',text,re.I): category='Environment'
        elif re.search(r'\b(?:sport|football|cricket|Olympic|athlet)\w*',text,re.I): category='Sport'
        elif re.search(r'\b(?:museum|music|artist|theatre|heritage|library|cultural)\w*',text,re.I): category='People & Culture'
        elif re.search(r'\b(?:economy|inflation|business|trade|treasury|loan|bank)\w*',text,re.I): category='Money & Business'
        elif feed.category=='UK' and re.search(r'\b(?:UN|Ukraine|DPRK|China|Africa|international|Nordic|Baltic)\b',text): category='World'
        elif re.search(r'\b(?:Pakistan|Indonesia|Indonesian|Malaysia|Malaysian|Bangladesh|Morocco|Tunisia|Egypt|Jordan|Saudi Arabia|United Arab Emirates|UAE|Türkiye|Turkey|Turkish|Muslim)\b',text,re.I): category='Muslim World'
        assigned=feed.model_copy(update={'category':category})
        out.append(Candidate(sha256(url.encode()).hexdigest()[:20],title,excerpt,url,dt,now,assigned,contested(text),author))
    return out

def collect(settings: Settings, now: datetime, http: SafeHttp) -> tuple[list[Candidate],dict]:
    candidates=[]; status={}
    for feed in settings.feeds:
        if not feed.enabled:
            status[feed.id]={'state':'disabled','reason':'Reuse permission not established'}
            continue
        try:
            items=parse_feed(http.get(feed.url),feed,now,settings.max_age_hours)
            status[feed.id]={'state':'ok','eligible':len(items)}; candidates+=items
        except FetchError as e: status[feed.id]={'state':'unavailable','reason':str(e)}
    return candidates,status

def rank(candidates: list[Candidate], settings: Settings, limit: int | None = None) -> list[Candidate]:
    """Greedy diversity plus event clustering. Conservative: at most one similar title."""
    selected=[]; seen=set(); cats={}; publishers={}
    candidates=sorted(candidates,key=lambda x:x.published_at,reverse=True)
    while candidates and len(selected)<(limit if limit is not None else settings.max_stories):
        def score(c):
            return (settings.category_weights.get(c.feed.category,1)*3 - cats.get(c.feed.category,0)*4
                    -publishers.get(c.feed.publisher,0)*2 + (0 if c.attributed else 1))
        c=max(candidates,key=score); candidates.remove(c)
        if c.url in seen or any(SequenceMatcher(None,normal(c.title),normal(s.title)).ratio()>.64 for s in selected): continue
        # Shared majority of meaningful terms catches reordered event headlines.
        words=set(re.findall(r'\b[a-z]{4,}\b',normal(c.title)))
        if any(len(words & set(re.findall(r'\b[a-z]{4,}\b',normal(s.title)))) / max(1,len(words | set(re.findall(r'\b[a-z]{4,}\b',normal(s.title)))))>.56 for s in selected): continue
        selected.append(c); seen.add(c.url)
        cats[c.feed.category]=cats.get(c.feed.category,0)+1
        publishers[c.feed.publisher]=publishers.get(c.feed.publisher,0)+1
    return selected
