"""Prevent yesterday's article reappearing as 'new' in a daily edition.

Source URLs, IDs and near-identical titles are compared against the last seven
published calendar days, plus the original 16 Nov Android starter edition.
A story must also have a publication date in today's Europe/London calendar day.
Never pad an edition by recycling yesterday's articles.
"""
from datetime import date, datetime, timedelta
from difflib import SequenceMatcher
from pathlib import Path
from urllib.parse import urlsplit
from zoneinfo import ZoneInfo
import re

from .models import Edition
from .safety import normal

LONDON = ZoneInfo('Europe/London')
_WORDS = re.compile(r'\b[a-z]{4,}\b')


def _canonical_url(url: str) -> str:
    parts = urlsplit(url)
    return f'{(parts.hostname or "").lower()}{parts.path.rstrip("/")}'


def _title_words(title: str) -> set[str]:
    return set(_WORDS.findall(normal(title)))


def same_event_title(a: str, b: str) -> bool:
    """Reject near-identical event headlines, not merely a shared generic word."""
    first, second = normal(a), normal(b)
    if first == second:
        return True
    wa, wb = _title_words(a), _title_words(b)
    overlap = len(wa & wb)
    if min(len(wa), len(wb)) < 3 or overlap < 3:
        return False
    jaccard = overlap / max(1, len(wa | wb))
    return jaccard >= .72 or (overlap >= 4 and SequenceMatcher(None, first, second).ratio() >= .82)


def recent_edition_stories(site_root: Path, today: date, starter: Path | None = None,
                            days: int = 7) -> list:
    """Read only genuine earlier editions; ignore today's replacement on force-refresh."""
    paths = list((site_root / 'editions').glob('*.json'))
    if starter is not None and starter.exists():
        paths.append(starter)
    seen = []
    ids = set()
    for path in paths:
        try:
            edition = Edition.model_validate_json(path.read_bytes())
        except (ValueError, OSError):
            # validate_site independently refuses corrupt published editions.
            continue
        if edition.sample or not (today - timedelta(days=days) <= edition.edition_date < today):
            continue
        for story in edition.stories:
            if story.id not in ids:
                ids.add(story.id)
                seen.append(story)
    return seen


def fresh_candidates(candidates: list, now: datetime, previous: list) -> list:
    """Use new UK-day reports only; compare against the full recent reading history."""
    today = now.astimezone(LONDON).date()
    ids = {s.id for s in previous}
    urls = {_canonical_url(ref.url) for s in previous for ref in s.source_refs}
    titles = [s.title for s in previous]
    kept = []
    for candidate in candidates:
        if candidate.published_at.astimezone(LONDON).date() != today:
            continue
        if candidate.id in ids or _canonical_url(candidate.url) in urls:
            continue
        if any(same_event_title(candidate.title, title) for title in titles):
            continue
        kept.append(candidate)
    return kept
