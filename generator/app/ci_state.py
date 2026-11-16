"""Keep public editions across runners, without giving API secrets to Git.

Uses a separate worktree of a dedicated branch. Only static, validated site files
are committed. The main branch and source code are never modified by the job.
"""
from pathlib import Path
import os
import shutil
import subprocess
import sys
from .main import ROOT

def git(*args,cwd=None,check=True):
    return subprocess.run(['git',*args],cwd=cwd or ROOT,check=check,stdout=subprocess.PIPE,stderr=subprocess.PIPE)

def main():
    if not os.getenv('GITHUB_ACTIONS'):raise SystemExit('This helper is only for GitHub Actions.')
    worktree=Path(os.environ['RUNNER_TEMP'])/'mdw-public-state'
    if sys.argv[1]=='restore':
        # Distinguish a missing branch from a network/authentication failure.
        result=git('ls-remote','--exit-code','--heads','origin','content-state',check=False)
        if result.returncode==0:
            git('fetch','--depth=1','origin','refs/heads/content-state')
            git('worktree','add','--detach',str(worktree),'FETCH_HEAD')
            previous=worktree/'site'
            if not previous.is_dir():raise SystemExit('Publication state is incomplete; refusing to replace it.')
            for name in ('editions','latest.json','health.json'):
                src=previous/name;dest=ROOT/'site'/name
                if src.is_dir():shutil.copytree(src,dest,dirs_exist_ok=True)
                elif src.is_file():shutil.copy2(src,dest)
        elif result.returncode==2:
            git('worktree','add','--detach',str(worktree),'HEAD')
            git('checkout','--orphan','content-state',cwd=worktree)
            git('rm','-rf','--ignore-unmatch','.',cwd=worktree)
        else:raise SystemExit('Could not read existing publication state; retry later.')
    elif sys.argv[1]=='save':
        from .validate_site import validate_site
        validate_site(ROOT/'site')
        dest=worktree/'site'
        if dest.exists():shutil.rmtree(dest)
        shutil.copytree(ROOT/'site',dest)
        git('config','user.name','My Daily World publication',cwd=worktree)
        git('config','user.email','publication@users.noreply.github.com',cwd=worktree)
        git('add','site',cwd=worktree)
        changed=git('diff','--cached','--quiet',cwd=worktree,check=False).returncode
        if changed:
            git('commit','-m','Publish validated public edition and health',cwd=worktree)
            git('push','origin','HEAD:refs/heads/content-state',cwd=worktree)
        print('Validated public publication state retained.')
    else:raise SystemExit('Expected restore or save')

if __name__=='__main__':main()
