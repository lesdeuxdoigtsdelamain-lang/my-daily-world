from pathlib import Path
from typing import Literal
from pydantic import Field, model_validator
from .models import Wire, Category, https_url

class Feed(Wire):
    enabled: bool = True
    id: str
    publisher: str
    url: str
    article_hosts: list[str]
    category: Category
    reuse: Literal['headline', 'public_domain', 'licensed_excerpt'] = 'headline'
    ai_allowed: bool = False
    terms_url: str
    license: str | None = None
    license_url: str | None = None

    @model_validator(mode='after')
    def valid(self):
        https_url(self.url); https_url(self.terms_url)
        if self.ai_allowed and self.reuse not in ('public_domain','licensed_excerpt'):
            raise ValueError('AI needs explicit reusable evidence')
        if self.reuse=='licensed_excerpt' and not (self.license and self.license_url):
            raise ValueError('Licence and attribution required')
        return self

class Settings(Wire):
    feeds: list[Feed]
    max_stories: int = Field(default=5, ge=1, le=5)
    max_age_hours: int = Field(default=36, ge=1, le=48)
    category_weights: dict[Category,int] = Field(default_factory=dict)
    max_ai_calls: int = Field(default=2, ge=0, le=2)
    max_output_tokens: int = Field(default=1400, ge=64, le=1600)

def load(path: Path) -> Settings:
    return Settings.model_validate_json(path.read_text())
