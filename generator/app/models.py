"""Public wire contract. Export this model to schema/edition.schema.json."""
from datetime import date, datetime
from typing import Literal
from urllib.parse import urlsplit
import re
from pydantic import BaseModel, ConfigDict, Field, model_validator, field_validator

CATEGORIES = ('World', 'UK', 'Science & Tech', 'Environment', 'People & Culture',
              'Money & Business', 'Sport', 'Muslim World')
Category = Literal['World', 'UK', 'Science & Tech', 'Environment', 'People & Culture',
                   'Money & Business', 'Sport', 'Muslim World']

def https_url(value: str) -> str:
    if len(value)>2048 or any(c.isspace() for c in value):
        raise ValueError('Invalid public URL length or whitespace')
    u = urlsplit(value)
    if u.scheme != 'https' or not u.hostname or u.username or u.password or u.port not in (None, 443):
        raise ValueError('A public HTTPS URL is required')
    if u.hostname in ('localhost', 'example.com', 'example.org') or re.fullmatch(r'[\d.:]+', u.hostname):
        raise ValueError('Public named sources only')
    return value

class Wire(BaseModel):
    model_config = ConfigDict(extra='forbid', str_strip_whitespace=True)

    @model_validator(mode='after')
    def plain_text(self):
        for v in self.__dict__.values():
            if isinstance(v, str) and ('<' in v or '>' in v or '\x00' in v):
                raise ValueError('Markup is not content')
        return self

class Source(Wire):
    publisher: str = Field(min_length=2, max_length=80)
    url: str
    published_at: datetime
    retrieved_at: datetime
    source_title: str = Field(min_length=5, max_length=250)
    event_date: date | None = None
    author: str | None = None
    license: str | None = None
    license_url: str | None = None
    _url = field_validator('url')(https_url)

    @field_validator('license_url')
    @classmethod
    def licence_https(cls, value):
        return https_url(value) if value else value

    @field_validator('published_at', 'retrieved_at')
    @classmethod
    def aware(cls, value):
        if value.tzinfo is None:
            raise ValueError('Timezone required')
        return value

class Fact(Wire):
    id: str = Field(min_length=3, max_length=80)
    text: str = Field(min_length=5, max_length=700)
    source_url: str
    _url = field_validator('source_url')(https_url)

class Word(Wire):
    word: str = Field(max_length=40)
    definition: str = Field(max_length=180)

class ImageAsset(Wire):
    url: str
    alt: str = Field(min_length=8, max_length=200)
    credit: str = Field(min_length=2, max_length=120)
    license: str = Field(min_length=3, max_length=80)
    license_url: str
    source_page: str
    _urls = field_validator('url','license_url','source_page')(https_url)

    @model_validator(mode='after')
    def verified_host(self):
        if urlsplit(self.url).hostname != 'upload.wikimedia.org':
            raise ValueError('Use only verified Commons thumbnail domains')
        if urlsplit(self.source_page).hostname != 'commons.wikimedia.org':
            raise ValueError('Link to the Commons attribution page')
        return self

class Story(Wire):
    id: str = Field(min_length=8, max_length=80)
    category: Category
    title: str = Field(min_length=5, max_length=250)
    summary: str = Field(max_length=1800)
    why_it_matters: str = Field(max_length=400)
    word_to_know: Word | None = None
    image: ImageAsset | None = None
    sensitivity: Literal['low', 'gentle'] = 'low'
    country: str | None = None
    source_refs: list[Source] = Field(min_length=1, max_length=4)
    supporting_fact_ids: list[str] = Field(min_length=1)
    facts: list[Fact] = Field(min_length=1, max_length=10)
    content_kind: Literal['headline', 'excerpt', 'grounded_ai'] = 'headline'
    age_suitability: Literal['10-15'] = '10-15'
    uncertainty_notes: list[str] = Field(default_factory=list)

    @model_validator(mode='after')
    def provenance(self):
        ids = {f.id for f in self.facts}
        urls = {s.url for s in self.source_refs}
        if len(ids) != len(self.facts) or not set(self.supporting_fact_ids) <= ids:
            raise ValueError('Unknown or duplicate evidence')
        if any(f.source_url not in urls for f in self.facts):
            raise ValueError('Uncited fact')
        return self

class HistorySource(Wire):
    publisher: str
    url: str
    _url = field_validator('url')(https_url)

class History(Wire):
    month_day: str = Field(pattern=r'^\d{2}-\d{2}$')
    year: int = Field(ge=1583, le=2100)
    title: str = Field(min_length=5)
    summary: str = Field(min_length=30, max_length=1500)
    sources: list[HistorySource] = Field(min_length=1)
    verified_on: date

    @field_validator('month_day')
    @classmethod
    def calendar_date(cls, value):
        date.fromisoformat('2000-' + value)
        return value

class Question(Wire):
    id: str
    question: str = Field(min_length=8, max_length=400)
    choices: list[str] = Field(min_length=3, max_length=3)
    correct_index: int = Field(ge=0, le=2)
    explanation: str = Field(min_length=5, max_length=700)
    story_id: str
    fact_id: str

    @model_validator(mode='after')
    def distinct(self):
        if len({c.casefold() for c in self.choices}) != 3:
            raise ValueError('Answers must be distinct')
        return self

class Edition(Wire):
    schema_version: Literal[1] = 1
    edition_id: str
    edition_date: date
    timezone: Literal['Europe/London'] = 'Europe/London'
    language: Literal['en-GB'] = 'en-GB'
    generated_at: datetime
    mode: Literal['full', 'compact']
    ai_provider: Literal['cloudflare', 'none'] = 'none'
    ai_status: str = Field(max_length=160)
    estimated_reading_minutes: int = Field(ge=1, le=20)
    stories: list[Story] = Field(min_length=1, max_length=10)
    on_this_day: History | None = None
    quiz: list[Question] = Field(max_length=5)
    sample: bool = False
    sample_label: str | None = None

    @model_validator(mode='after')
    def coherent(self):
        if self.edition_id != f'{self.edition_date}-en-GB':
            raise ValueError('Stable date-based identity required')
        if self.generated_at.tzinfo is None:
            raise ValueError('UTC timestamp required')
        if self.sample and self.sample_label != 'SAMPLE — NOT CURRENT NEWS':
            raise ValueError('Samples must be explicit')
        stories = {s.id:s for s in self.stories}
        if len(stories) != len(self.stories):
            raise ValueError('Duplicate stories')
        if self.on_this_day and self.on_this_day.month_day != self.edition_date.strftime('%m-%d'):
            raise ValueError('Wrong history anniversary')
        for q in self.quiz:
            story = stories.get(q.story_id)
            if not story or q.fact_id not in story.supporting_fact_ids:
                raise ValueError('Quiz must refer to a displayed fact')
            fact = next(f for f in story.facts if f.id == q.fact_id)
            if q.choices[q.correct_index] not in fact.text:
                raise ValueError('Answer not present in evidence')
        if self.mode == 'full' and (len(self.stories) < 5 or len(self.quiz) != 5 or
                any(not 80 <= len(s.summary.split()) <= 170 for s in self.stories)):
            raise ValueError('Full edition requires sufficient evidence and reading length')
        return self

class Manifest(Wire):
    schema_version: Literal[1] = 1
    edition_id: str
    edition_date: date
    published_at: datetime
    last_successful_generation: datetime
    url: str = Field(pattern=r'^editions/\d{4}-\d{2}-\d{2}\.json$')
    sha256: str = Field(pattern=r'^[0-9a-f]{64}$')
    state: Literal['success'] = 'success'
    ai_provider: Literal['cloudflare', 'none']
    mode: Literal['full', 'compact']
