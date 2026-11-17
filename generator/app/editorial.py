import re
import json
from pathlib import Path
from .feeds import Candidate
from .models import Story, Fact, Source, Word
from .providers import AiError
from .safety import clean, grounded_summary, normal, excluded, split_sentences

CONTEXT={
 'World':'Comparing reliable sources can help us understand events around the world.',
 'UK':'Following UK news can help us ask informed questions about life around us.',
 'Science & Tech':'Science news is a chance to ask what the evidence shows and what is still being studied.',
 'Environment':'Environmental news can help us learn about the world we share.',
 'People & Culture':'Learning about different ideas and communities can broaden our understanding.',
 'Money & Business':'Understanding money and business can help us make sense of everyday decisions.',
 'Sport':'Sport offers opportunities to learn about teamwork, practice and fair play.',
 'Muslim World':'Learning about daily life, culture and ideas in many communities can broaden our understanding.'}
GLOSSARY=json.loads(Path(__file__).with_name('glossary.json').read_text())

def story_from(c: Candidate) -> Story:
    fact=Fact(id='f-'+c.id,text=c.title,source_url=c.url)
    facts=[fact];summary=''
    # Only public-domain / explicitly reusable snippets may become stored evidence.
    boilerplate=bool(re.search(r'Archive Submissions|Today’s APOD|APOD Science APOD|Astronomy Picture of the Day',c.excerpt,re.I))
    material=c.article_text or c.excerpt
    if c.feed.reuse in ('public_domain','licensed_excerpt') and material and not c.attributed and not boilerplate:
        # Complete short sentences only; never store full RSS article bodies.
        sentences=split_sentences(material)
        kept=[]
        for s in sentences:
            if not re.search(r'[.!?]$',s) or len(s.split())>55 or len(s)<20: continue
            if re.search(r'(?:RSS|Archive Submissions|Originally published|Photo (?:by|taken)|Today’s APOD|copyright|Creative Commons)',s,re.I): continue
            if len((' '.join(kept)+' '+s).split())>160: break
            if excluded(s): continue
            if len(kept)>=9: break
            kept.append(s)
        summary=' '.join(kept)
        for i,s in enumerate(kept): facts.append(Fact(id=f'f-{c.id}-{i}',text=s,source_url=c.url))
    word=next((Word(word=w,definition=d) for w,d in GLOSSARY.items()
               if re.search(r'\b'+re.escape(w)+r's?\b',c.title+' '+summary,re.I)),None)
    uncertainty=['This headline reports the named publisher’s account. Read the original for context.'] if c.attributed else []
    if re.search(r'\b(?:will|plans? to|expected to|may|could)\b',c.title+' '+summary,re.I):
        uncertainty.append('This report includes a plan or prediction, which may change.')
    return Story(id=c.id,category=c.feed.category,title=c.title,summary=summary,word_to_know=word,
        why_it_matters=CONTEXT[c.feed.category],facts=facts,
        supporting_fact_ids=[f.id for f in facts],content_kind='excerpt' if summary else 'headline',
        sensitivity='gentle' if c.attributed else 'low',
        uncertainty_notes=uncertainty,
        source_refs=[Source(publisher=c.feed.publisher,url=c.url,published_at=c.published_at,
                           retrieved_at=c.retrieved_at,source_title=c.title,author=c.author,
                           license=c.feed.license,license_url=c.feed.license_url)])

def enrich(stories, candidates, provider):
    allowed={c.id for c in candidates if c.feed.ai_allowed and not c.attributed}
    eligible=[s for s in stories if s.id in allowed and len(s.facts)>1]
    if not eligible: return stories,0,'No reusable evidence suitable for AI'
    # Two small batches maximum. Pipeline never retries a failed publication loop.
    accepted={};attempted=False
    for start in range(0,min(len(eligible),6),3):
        batch=eligible[start:start+3]
        packets=[{'story_id':s.id,'title':s.title,'facts':[f.model_dump() for f in s.facts],
                  'safe_context':s.why_it_matters} for s in batch]
        try: outputs=provider.rewrite(packets);attempted=True
        except AiError as e: return [accepted.get(s.id,s) for s in stories],len(accepted),str(e)
        originals={s.id:s for s in batch}
        for o in outputs:
            s=originals.get(o.story_id)
            if not s: continue
            ids=set(o.fact_ids)
            facts=[f for f in s.facts if f.id in ids]
            if (not ids or ids!={f.id for f in facts} or o.title!=s.title or
                (o.why_it_matters and o.why_it_matters!=s.why_it_matters) or
                o.age_suitability!='10-15' or
                o.word_to_know or o.quiz_candidates or o.uncertainty_notes or
                not grounded_summary(o.summary,[f.text for f in facts])): continue
            accepted[s.id]=s.model_copy(update={'summary':o.summary,'content_kind':'grounded_ai',
                                               'supporting_fact_ids':list(dict.fromkeys([s.facts[0].id]+o.fact_ids))})
    return [accepted.get(s.id,s) for s in stories],len(accepted),'Validated evidence sentences' if accepted else 'Unverified AI wording rejected; compact fallback'
