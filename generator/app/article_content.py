"""Conservative extraction of source evidence from explicitly reusable pages.

Never visit press sites marked headline-only. Never inflate the summary with
unverified model knowledge or with navigation/footer copy.
"""
from html.parser import HTMLParser
from .network import FetchError
from .safety import clean, excluded

BLOCKED_TAGS={'script','style','nav','footer','aside','noscript','form','header','svg'}
BODY_HINTS=('article-body','article-content','entry-content','post-content','story-content',
            'field--name-body','main-content','content-body','wysiwyg','rich-text')

class ArticleParagraphs(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.stack=[]
        self.current=None
        self.parts=[]
        self.loose=[]
        self.descriptions=[]
        self.scope=0
        self.hidden=0

    def handle_starttag(self,tag,attrs):
        a=dict(attrs)
        hint=' '.join((a.get('class') or '',a.get('id') or '')).lower()
        in_article=tag in ('main','article') or a.get('role')=='main' or any(x in hint for x in BODY_HINTS)
        self.stack.append((tag,in_article,tag in BLOCKED_TAGS))
        if in_article: self.scope+=1
        if tag in BLOCKED_TAGS: self.hidden+=1
        if tag=='meta' and a.get('content'):
            kind=(a.get('property') or a.get('name') or '').lower()
            if kind in ('description','og:description','twitter:description'):
                self.descriptions.append(clean(a['content'],800))
        if tag=='p':
            self.current=[] if not self.hidden else None

    def handle_data(self,data):
        if self.current is not None and not self.hidden:
            self.current.append(data)

    def handle_endtag(self,tag):
        if tag=='p' and self.current is not None:
            value=clean(' '.join(self.current),1100)
            if 45<=len(value)<=1100 and not excluded(value) and not any(x in value.lower() for x in
              ('subscribe to our newsletter','cookie policy','privacy notice','all rights reserved')):
                if self.scope and not self.hidden: self.parts.append(value)
                elif not self.hidden: self.loose.append(value)
            self.current=None
        # Repair malformed HTML without allowing hidden/semantic scopes to leak.
        for i in range(len(self.stack)-1,-1,-1):
            if self.stack[i][0]==tag:
                for _,semantic,hidden in self.stack[i:]:
                    self.scope-=int(semantic)
                    self.hidden-=int(hidden)
                del self.stack[i:]
                break

    def paragraphs(self):
        paragraphs=self.parts if sum(len(p.split()) for p in self.parts)>=60 else self.loose
        if sum(len(p.split()) for p in paragraphs)<60:
            paragraphs=self.parts or self.loose
        result=[];seen=set()
        # Descriptions are snippets of the same source, not additional reporting.
        for p in paragraphs:
            key=p.casefold()
            if key in seen: continue
            seen.add(key)
            if sum(len(x.split()) for x in result)+len(p.split())>480: break
            result.append(p)
            if len(result)>=14: break
        return result


def expand_article(candidate,http):
    """Fetch once, only when publisher explicitly permits reuse. Fail closed."""
    candidate.article_text=''
    if not candidate.feed.ai_allowed or candidate.feed.reuse not in ('public_domain','licensed_excerpt'):
        return False
    try:
        data=http.get(candidate.url,limit=900_000)
        parser=ArticleParagraphs()
        parser.feed(data.decode('utf-8','replace'))
        paragraphs=parser.paragraphs()
        candidate.article_text=' '.join(paragraphs)
        return bool(candidate.article_text)
    except (FetchError,ValueError,UnicodeError,RecursionError):
        return False


def expand_articles(candidates,http):
    """Backwards-compatible helper: original candidate objects are enriched in place."""
    for candidate in candidates:
        expand_article(candidate,http)
