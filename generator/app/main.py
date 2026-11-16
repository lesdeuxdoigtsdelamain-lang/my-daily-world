import argparse
from datetime import datetime, date, timezone
from pathlib import Path
from zoneinfo import ZoneInfo
import json
import math
import os
from .models import Edition
from .settings import load
from .network import SafeHttp
from .feeds import collect,rank
from .providers import configured,Cloudflare,AiError
from .editorial import story_from,enrich
from .history import LocalHistory
from .quiz import build_quiz
from .storage import DirectoryStore

ROOT=Path(__file__).resolve().parents[2]
LONDON=ZoneInfo('Europe/London')

def uk_date(now): return now.astimezone(LONDON).date()

def generate(settings,now,history,http,provider,provider_name,status):
    candidates,feed_status=collect(settings,now,http)
    chosen=rank(candidates,settings)
    if not chosen: return None,feed_status
    stories=[story_from(c) for c in chosen]
    accepted=0
    if provider_name=='cloudflare': stories,accepted,status=enrich(stories,chosen,provider)
    effective='cloudflare' if accepted else 'none'
    quiz=build_quiz(stories)
    day=uk_date(now);highlight=history.for_date(day)
    words=sum(len((s.title+' '+s.summary+' '+s.why_it_matters).split()) for s in stories)
    if highlight:words+=len(highlight.summary.split())
    full=(len(stories)>=7 and len(quiz)==5 and all(80<=len(s.summary.split())<=115 for s in stories))
    e=Edition(edition_id=f'{day}-en-GB',edition_date=day,generated_at=now,mode='full' if full else 'compact',
        estimated_reading_minutes=max(1,math.ceil(words/150)),stories=stories,quiz=quiz,on_this_day=highlight,
        ai_provider=effective,ai_status=status)
    return e,feed_status

def main():
    p=argparse.ArgumentParser(description='Generate verified, dated family news. No private data.')
    p.add_argument('--settings',type=Path,default=ROOT/'settings.example.json')
    p.add_argument('--output',type=Path,default=ROOT/'site')
    p.add_argument('--dry-run',action='store_true')
    p.add_argument('--date',type=date.fromisoformat,help='Historical testing only; requires --dry-run')
    p.add_argument('--force-refresh',action='store_true')
    p.add_argument('--smoke',action='store_true',help='One secure Cloudflare call; no publication')
    args=p.parse_args()
    if args.date and not args.dry_run:p.error('--date is permitted only with --dry-run')
    settings=load(args.settings)
    now=datetime.now(timezone.utc)
    if args.date:now=datetime.combine(args.date,datetime.min.time().replace(hour=17),LONDON).astimezone(timezone.utc)
    day=uk_date(now);store=DirectoryStore(args.output)
    provider,name,status=configured(settings)
    print('AI:',name,'—',status)
    if args.smoke:
        if not isinstance(provider,Cloudflare):print('Smoke test requires configured Cloudflare secrets.');return 2
        try:provider.smoke();print('Cloudflare smoke test passed (no private response logged).');return 0
        except AiError as e:print(str(e));return 2
    if store.existing(day) and not args.force_refresh:
        print("Today's edition already exists")
        return 0
    hosts={__import__('urllib.parse',fromlist=['urlsplit']).urlsplit(f.url).hostname for f in settings.feeds}
    http=SafeHttp(hosts)
    edition,feeds=generate(settings,now,LocalHistory(ROOT/'data/history/events.json'),http,provider,name,status)
    if not edition:
        print('No publishable current evidence; last edition retained.')
        if not args.dry_run:store.health('unavailable',now,detail='No publishable current evidence',feeds=feeds)
        return 0
    # Reparse through the public contract before any mutation.
    edition=Edition.model_validate_json(edition.model_dump_json())
    if args.dry_run:
        print(f'DRY RUN — {edition.edition_date}: {len(edition.stories)} stories, {len(edition.quiz)} questions, {edition.mode}; no files published.')
    else:
        published=datetime.now(timezone.utc)
        store.publish(edition,published,force=args.force_refresh)
        store.health('success',published,published,edition.ai_status,feeds,ai_usage={
            'requests':getattr(provider,'calls',0),'reported':getattr(provider,'usage',[]),
            'neuron_estimate':None,'maximum_requests':settings.max_ai_calls,
            'maximum_output_tokens_per_call':settings.max_output_tokens})
        print(f'Published {edition.edition_date}: {len(edition.stories)} sourced stories; {edition.mode}; AI={edition.ai_provider}.')
    summary=os.getenv('GITHUB_STEP_SUMMARY')
    if summary:
        with open(summary,'a') as f:f.write(f'Edition: {edition.edition_date}; mode: {edition.mode}; AI: {edition.ai_provider}; stories: {len(edition.stories)}.\n')
    return 0

if __name__=='__main__':raise SystemExit(main())
