"""Illustrative imagery from Wikimedia Commons with verified reuse metadata.

No newspaper photograph is copied or embedded. Images are labelled illustrative
unless the editorial team independently verifies their connection to the event.
"""
import json
import re
from urllib.parse import urlencode,quote,urlsplit
from .network import FetchError
from .models import ImageAsset
from .safety import clean

COMMONS_API='https://commons.wikimedia.org/w/api.php'
SAFE_LICENSE_PREFIXES=('cc by ', 'cc by-sa ', 'cc0 ', 'public domain', 'pd-')
GENERIC={
 'World':'planet Earth globe world',
 'UK':'United Kingdom landscape',
 'Science & Tech':'science laboratory research',
 'Environment':'nature landscape wildlife',
 'People & Culture':'museum library architecture',
 'Money & Business':'city financial district buildings',
 'Sport':'sports stadium athletics',
 'Muslim World':'Islamic architecture world heritage'
}
BLOCKED=re.compile(r'\b(?:war|death|dead|killed|injured|bomb|shoot|attack|flood|crash|victim|conflict|violence|blood)\b',re.I)

def find_image(story,http):
    # Avoid sensational or inaccurate photographs for sensitive news.
    terms=GENERIC.get(story.category,'planet Earth')
    if story.sensitivity=='low' and not BLOCKED.search(story.title):
        words=re.findall(r'[A-Za-z]{4,}', story.title)
        if len(words)>=2:
            terms=' '.join(words[:4])
    params={
        'action':'query','format':'json','formatversion':'2',
        'generator':'search','gsrsearch':terms,'gsrnamespace':'6','gsrlimit':'7',
        'prop':'imageinfo','iiprop':'url|mime|extmetadata','iiurlwidth':'720'
    }
    try:
        raw=http.get(COMMONS_API+'?'+urlencode(params),limit=220_000)
        pages=json.loads(raw).get('query',{}).get('pages',[])
    except (FetchError,ValueError,KeyError,TypeError):
        return None
    for page in pages:
        try:
            title=page['title']; info=page['imageinfo'][0]
            if info.get('mime') not in ('image/jpeg','image/png','image/webp'): continue
            url=info.get('thumburl') or info.get('url')
            if urlsplit(url).hostname != 'upload.wikimedia.org' or not url.startswith('https://'):
                continue
            meta=info['extmetadata']
            licence=clean(meta['LicenseShortName']['value'],80)
            license_url=meta['LicenseUrl']['value']
            if not licence.lower().startswith(SAFE_LICENSE_PREFIXES): continue
            if not license_url.startswith('https://'): continue
            credit=clean(meta.get('Artist',{}).get('value',''),100)
            if not credit: continue
            page_url='https://commons.wikimedia.org/wiki/'+quote(title.replace(' ','_'),safe=':_()-')
            return ImageAsset(url=url,alt='Illustrative photo for '+story.category,
                credit=credit,license=licence,license_url=license_url,
                source_page=page_url)
        except (KeyError,TypeError,ValueError):
            continue
    return None

def attach_licensed_images(stories,http):
    result=[]
    for story in stories:
        image=find_image(story,http)
        result.append(story.model_copy(update={'image':image}) if image else story)
    return result
