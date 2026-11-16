from pathlib import Path
from hashlib import sha256
import sys
from .models import Edition,Manifest

def validate_site(root:Path):
    for path in (root/'editions').glob('*.json'):
        e=Edition.model_validate_json(path.read_bytes())
        if e.sample or path.name!=f'{e.edition_date}.json':raise ValueError('Sample or misdated public edition')
    path=root/'latest.json'
    if path.exists():
        m=Manifest.model_validate_json(path.read_bytes())
        payload=(root/m.url).read_bytes()
        if sha256(payload).hexdigest()!=m.sha256:raise ValueError('Manifest hash mismatch')
        e=Edition.model_validate_json(payload)
        if m.edition_id!=e.edition_id or m.edition_date!=e.edition_date:raise ValueError('Manifest date mismatch')
    print('Static publication validated; no sample news.')

if __name__=='__main__':validate_site(Path(sys.argv[1]))
