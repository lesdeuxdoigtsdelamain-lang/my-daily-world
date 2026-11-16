"""Directory publication: edition first, manifest last, persistent gh-pages state."""
from datetime import datetime, timezone
from hashlib import sha256
from pathlib import Path
from typing import Protocol
import json
import os
from .models import Edition, Manifest

class Storage(Protocol):
    def existing(self,day): ...
    def publish(self,edition,now): ...

def atomic(path: Path,data: bytes):
    path.parent.mkdir(parents=True,exist_ok=True)
    temp=path.with_suffix(path.suffix+'.tmp')
    temp.write_bytes(data);os.replace(temp,path)

class DirectoryStore:
    def __init__(self,root: Path): self.root=root
    def existing(self,day):
        path=self.root/'editions'/f'{day}.json'
        if not path.exists(): return None
        try:
            e=Edition.model_validate_json(path.read_bytes())
            return e if e.edition_date==day and not e.sample else None
        except ValueError: return None
    def publish(self,edition: Edition,now: datetime,force=False):
        if edition.sample: raise ValueError('Sample data cannot be published')
        if self.existing(edition.edition_date) and not force: return False
        payload=(edition.model_dump_json(indent=2)+'\n').encode()
        relative=f'editions/{edition.edition_date}.json'
        manifest=Manifest(edition_id=edition.edition_id,edition_date=edition.edition_date,
            published_at=now,last_successful_generation=edition.generated_at,url=relative,
            sha256=sha256(payload).hexdigest(),ai_provider=edition.ai_provider,mode=edition.mode)
        atomic(self.root/relative,payload)
        atomic(self.root/'latest.json',(manifest.model_dump_json(indent=2)+'\n').encode())
        self.health('success',now,now,edition.ai_status)
        return True
    def health(self,state,now,last_success=None,detail='',feeds=None,ai_usage=None):
        old=self.root/'latest.json'
        if last_success is None and old.exists():
            try:last_success=Manifest.model_validate_json(old.read_bytes()).published_at
            except ValueError:pass
        body={'schema_version':1,'state':state,'checked_at':now.isoformat(),
              'last_successful_publication':last_success.isoformat() if last_success else None,
              'detail':detail[:160],'feeds':feeds or {},'ai_usage':ai_usage or {
                  'requests':0,'reported':[],'neuron_estimate':None}}
        atomic(self.root/'health.json',(json.dumps(body,indent=2)+'\n').encode())
