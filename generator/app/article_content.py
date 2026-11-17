"""Read *only* explicitly reusable article pages to supply enough evidence for a real in-app digest.

This intentionally does NOT crawl BBC/Sky/Al Jazeera or other headline-only feeds.
RSS titles and snippets alone are not sufficient evidence for a long story.
"""
from html.parser import HTMLParser
from .network import FetchError
from .safety import clean, excluded

class ArticleParagraphs(HTMLParser):
    def __init__(self):
        super().__init__(convert_charrefs=True)
        self.scope=0
        self.hidden=0
        self.paragraph=0
        self.parts=[]
        self.current=[]

    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style', 'nav', 'footer', 'aside'):
            self.hidden+=1
        if tag in ('main', 'article'):
            self.scope+=1
        if tag=='p' and self.scope>0 and not self.hidden:
            self.paragraph+=1
            self.current=[]

    def handle_data(self, data):
        if self.paragraph and not self.hidden:
            self.current.append(data)

    def handle_endtag(self, tag):
        if tag=='p' and self.paragraph:
            value=clean(' '.join(self.current),1400)
            if 45<=len(value)<=1100 and not excluded(value):
                self.parts.append(value)
            self.current=[]
            self.paragraph=0
        if tag in ('main','article'):
            self.scope=max(0,self.scope-1)
        if tag in ('script', 'style', 'nav', 'footer', 'aside'):
            self.hidden=max(0,self.hidden-1)

def expand_articles(candidates,http):
    """Enrich at most five allowlisted candidates; fail closed when access is unavailable."""
    for candidate in candidates[:5]:
        if not candidate.feed.ai_allowed or candidate.feed.reuse not in ('public_domain','licensed_excerpt'):
            continue
        try:
            body=http.get(candidate.url, limit=650_000).decode('utf-8','replace')
            parser=ArticleParagraphs()
            parser.feed(body[:650_000])
            paragraphs=parser.parts[:14]
            # Limit source material before passing it to the model. Keep complete paragraphs.
            selected=[]
            for p in paragraphs:
                if sum(len(s.split()) for s in selected)+len(p.split())>420: break
                selected.append(p)
            candidate.article_text=' '.join(selected)
        except (FetchError,ValueError,UnicodeError):
            candidate.article_text=''
