from datetime import date
from pathlib import Path
import json
from .models import History

class LocalHistory:
    """Expandable, cited, Gregorian dataset. No invented date conversions."""
    def __init__(self,path: Path):
        self.records=[History.model_validate(r) for r in json.loads(path.read_text())]
    def for_date(self,day: date) -> History | None:
        return next((r for r in self.records if r.month_day==day.strftime('%m-%d') and r.year<day.year),None)
