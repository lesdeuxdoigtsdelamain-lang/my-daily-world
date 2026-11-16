"""Fail-closed editorial rules, shared by feed and AI validation."""
from html import unescape
from html.parser import HTMLParser
from pathlib import Path
import json
import re

RULES = json.loads((Path(__file__).with_name('safety_rules.json')).read_text())

class Cleaner(HTMLParser):
    def __init__(self):
        super().__init__(); self.parts = []; self.hidden = 0
    def handle_starttag(self, tag, attrs):
        if tag in ('script', 'style', 'iframe'): self.hidden += 1
    def handle_endtag(self, tag):
        if tag in ('script', 'style', 'iframe') and self.hidden: self.hidden -= 1
    def handle_data(self, data):
        if not self.hidden: self.parts.append(data)

def clean(value: str, limit=1200) -> str:
    p = Cleaner(); p.feed(unescape(value[:20000])); p.close()
    return re.sub(r'\s+', ' ', unescape(' '.join(p.parts))).strip()[:limit]

def excluded(text: str) -> bool:
    text = text.casefold()
    return any(re.search(p, text, re.I) for p in RULES['exclude_patterns'])

def contested(text: str) -> bool:
    return any(re.search(p, text, re.I) for p in RULES['contested_patterns'])

def normal(value: str) -> str:
    return re.sub(r'[^\w\s]', '', value.casefold()).strip()

def split_sentences(value: str) -> list[str]:
    # Keep common abbreviations and initials inside complete evidence sentences.
    return re.split(r'(?<!\b(?:Mr|Ms|Dr|St|Jr|Sr|vs|No)\.)(?<!\bMrs\.)(?<!\bProf\.)(?<!\b[A-Z]\.)(?<=[.!?])\s+',value)

def grounded_summary(summary: str, fact_texts: list[str]) -> bool:
    """Only complete source-supported sentences pass; lexical similarity is NOT proof.

    Deliberately rejects plausible free-form prose. This makes automatic publication
    conservative until a stronger independently audited entailment gate is available.
    """
    # Preserve case, decimal points, signs and punctuation. Removing punctuation
    # could incorrectly equate “1.2” and “12”, or a question and an assertion.
    def evidence_key(s): return re.sub(r'\s+',' ',s).strip()
    allowed = {evidence_key(s) for f in fact_texts for s in split_sentences(f) if s.strip()}
    sentences = [evidence_key(s) for s in split_sentences(summary) if s.strip()]
    return bool(sentences) and all(s in allowed for s in sentences) and not excluded(summary)
