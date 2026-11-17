"""Only use images with verified reuse terms and identifiable attribution.

Wikimedia Commons captions are *illustrative*, not evidence that the picture
shows the exact current event. The Android app has its own illustrated fallback.
"""
import json
import re
from urllib.parse import urlencode, quote, urlsplit
from .network import FetchError
from .models import ImageAsset
from .safety import clean

COMMONS_API='https://commons.wikimedia.org/w/api.php'
GENERIC={
 'World':'physical world globe photograph',
 'UK':'United Kingdom countryside photograph',
 'Science & Tech':'observatory telescope photograph',
 'Environment':'forest landscape nature photograph',
 'People & Culture':'museum library architecture photograph',
 'Money & Business':'London city skyline photograph',
 'Sport':'stadium sports athletics photograph',
 'Muslim World':'mosque architecture photograph'
}
BLOCKED=re.compile(r'\b(?:war|death|dead|killed|injured|bomb|shoot|attack|flood|crash|victim|conflict|violence|blood)\b',re.I)

def acceptable_license(name):
    # Wikimedia metadata returns e.g. 'CC BY-SA 4.0', 'CC BY 3.0', 'CC0',
    # 'Public domain', 'PD-US'. Previous prefix matching rejected almost all.
    value=re.sub(r'\s+',' ',name.lower()).strip()
    return bool(re.match(r'^(?:cc\s*by(?:-sa)?\s+\d|cc0(?:\s|[-0-9]|$)|public domain|pd[- ])',value))

def find_image(story,http,used=None):
    used=used if used is not None else set()
    generic=GENERIC.get(story.category,'physical world globe photograph')
    searches=[]
    if story.sensitivity=='low' and not BLOCKED.search(story.title):
        # Avoid generic news words, dates, and dangerous sensational imagery.
        words=[x for x in re.findall(r'[A-Za-z]{4,}',story.title) if x.lower() not in
               ('announces','reports','update','today','latest','november','news','article')]
        if len(words)>=2: searches.append(' '.join(words[:4]))
    searches.append(generic)
    for term in dict.fromkeys(searches):
        params={
            'action':'query','format':'json','formatversion':'2',
            'generator':'search','gsrsearch':term,'gsrnamespace':'6','gsrlimit':'12',
            'prop':'imageinfo','iiprop':'url|mime|extmetadata','iiurlwidth':'960'
        }
        try:
            payload=http.get(COMMONS_API+'?'+urlencode(params),limit=350_000)
            pages=json.loads(payload).get('query',{}).get('pages',[])
        except (FetchError,ValueError,TypeError,KeyError):
            continue
        for page in pages:
            try:
                title=page['title'];info=page['imageinfo'][0]
                if info.get('mime') not in ('image/jpeg','image/png','image/webp'): continue
                url=info.get('thumburl') or info.get('url')
                if (not isinstance(url,str) or url in used or urlsplit(url).hostname!='upload.wikimedia.org'
                        or not url.startswith('https://')): continue
                meta=info['extmetadata']
                licence=clean(meta['LicenseShortName']['value'],80)
                if not acceptable_license(licence): continue
                license_url=meta.get('LicenseUrl',{}).get('value','')
                # Public-domain media sometimes have no explicit license URL.
                if not license_url.startswith('https://'):
                    if licence.lower().startswith(('public domain','pd-')):
                        license_url='https://commons.wikimedia.org/wiki/'+quote(title.replace(' ','_'),safe=':_()-')
                    else: continue
                credit=clean(meta.get('Artist',{}).get('value',''),120)
                if not credit:
                    if licence.lower().startswith(('public domain','pd-','cc0')):
                        credit='Wikimedia Commons contributor'
                    else: continue
                page_url='https://commons.wikimedia.org/wiki/'+quote(title.replace(' ','_'),safe=':_()-')
                return ImageAsset(url=url,alt='Illustration for '+story.category,
                    credit=credit,license=licence,license_url=license_url,source_page=page_url)
            except (KeyError,TypeError,ValueError):
                continue
    return None

def attach_licensed_images(stories,http):
    result=[];used=set()
    for story in stories:
        image=find_image(story,http,used)
        if image: used.add(image.url)
        result.append(story.model_copy(update={'image':image}) if image else story)
    return result
