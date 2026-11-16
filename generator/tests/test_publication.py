from pathlib import Path
from datetime import datetime,timezone
import json
import re
import subprocess
import sys
import pytest
import yaml
import httpx
from generator.app import ci_state
from generator.app.models import Manifest
from generator.app.storage import DirectoryStore
from generator.app.editorial import story_from,enrich
from generator.app.providers import Cloudflare
from generator.app.feeds import parse_feed
from generator.app.network import SafeHttp,FetchError
from .conftest import ROOT,rss

def test_ci_restores_history_without_changing_source(tmp_path,monkeypatch,edition,now):
    remote=tmp_path/'remote.git';project=tmp_path/'project';project.mkdir()
    def git(*args,cwd=project):
        return subprocess.run(['git',*args],cwd=cwd,check=True,capture_output=True)
    git('init','--bare',str(remote),cwd=tmp_path)
    git('init','-b','main');git('config','user.name','Offline test');git('config','user.email','test@example.invalid')
    (project/'README.md').write_text('Source must remain unchanged')
    (project/'site').mkdir();(project/'site/index.html').write_text('Public status')
    git('add','.');git('commit','-m','Initial');git('remote','add','origin',str(remote));git('push','origin','main')
    monkeypatch.setattr(ci_state,'ROOT',project)
    monkeypatch.setenv('GITHUB_ACTIONS','true');monkeypatch.setenv('RUNNER_TEMP',str(tmp_path/'runner-one'))
    (tmp_path/'runner-one').mkdir()
    monkeypatch.setattr(sys,'argv',['ci_state','restore']);ci_state.main()
    DirectoryStore(project/'site').publish(edition,now)
    expected=(project/'site/latest.json').read_bytes()
    monkeypatch.setattr(sys,'argv',['ci_state','save']);ci_state.main()
    assert (project/'README.md').read_text()=='Source must remain unchanged'
    assert git('branch','--show-current').stdout.strip()==b'main'
    # A second runner has no generated files: restore the durable branch.
    import shutil
    git('worktree','remove','--force',str(tmp_path/'runner-one/mdw-public-state'))
    shutil.rmtree(project/'site/editions');(project/'site/latest.json').unlink();(project/'site/health.json').unlink()
    (tmp_path/'runner-two').mkdir();monkeypatch.setenv('RUNNER_TEMP',str(tmp_path/'runner-two'))
    monkeypatch.setattr(sys,'argv',['ci_state','restore']);ci_state.main()
    assert (project/'site/latest.json').read_bytes()==expected
    assert DirectoryStore(project/'site').existing(edition.edition_date)
    assert not DirectoryStore(project/'site').publish(edition,now)
    assert git('ls-tree','--name-only','origin/content-state').stdout.strip()==b'site'

def test_workflow_permissions_schedule_and_secret_isolation():
    pub=yaml.load((ROOT/'.github/workflows/publish-daily.yml').read_text(),Loader=yaml.BaseLoader)
    assert [(s['cron'],s['timezone']) for s in pub['on']['schedule']]==[
        ('20 15 * * *','Europe/London'),('15 16 * * *','Europe/London'),('0 17 * * *','Europe/London')]
    assert 'workflow_dispatch' in pub['on'] and pub['concurrency']['cancel-in-progress']=='false'
    android=(ROOT/'.github/workflows/build-apk.yml').read_text()
    assert 'CLOUDFLARE' not in android and 'pull_request_target' not in android
    for name in ('build-apk.yml','publish-daily.yml'):
        text=(ROOT/'.github/workflows'/name).read_text()
        assert all(re.fullmatch('[a-f0-9]{40}',ref) for ref in re.findall(r'uses: [^@\s]+@([^\s]+)',text))

def test_licensed_source_needs_author(feed,now):
    licensed=feed.model_copy(update={'reuse':'licensed_excerpt','license':'CC BY 3.0','license_url':'https://creativecommons.org/licenses/by/3.0/'})
    assert not parse_feed(rss(),licensed,now)
    raw=rss().replace(b'<item>',b'<item xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:creator>Example reporter</dc:creator>')
    story=story_from(parse_feed(raw,licensed,now)[0])
    assert story.source_refs[0].author=='Example reporter' and story.source_refs[0].license=='CC BY 3.0'

@pytest.mark.parametrize('hallucinate',[True,False])
def test_ai_supported_sentences_only(hallucinate,feed,now):
    chosen=parse_feed(rss(),feed,now);s=story_from(chosen[0])
    summary='A spacecraft found gold on Mars.' if hallucinate else 'The team measured sunlight.'
    obj={'story_id':s.id,'title':s.title,'summary':summary,'why_it_matters':s.why_it_matters,
        'fact_ids':[f.id for f in s.facts],'age_suitability':'10-15','uncertainty_notes':[],
        'word_to_know':None,'quiz_candidates':[]}
    client=httpx.Client(transport=httpx.MockTransport(lambda r:httpx.Response(200,json={
        'success':True,'result':{'response':json.dumps({'stories':[obj]})}})))
    result,count,_=enrich([s],chosen,Cloudflare('a'*32,'TEST_ONLY_SECRET',client=client))
    assert count==(0 if hallucinate else 1)
    assert result[0].content_kind==('excerpt' if hallucinate else 'grounded_ai')

def test_private_dns_rejected(monkeypatch):
    monkeypatch.setattr('socket.getaddrinfo',lambda *a,**k:[(2,1,6,'',('127.0.0.1',443))])
    with pytest.raises(FetchError):SafeHttp({'www.nasa.gov'}).get('https://www.nasa.gov/feed/')
