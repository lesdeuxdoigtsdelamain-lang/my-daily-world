# Sources, licences et maintenance éditoriale

Vérification technique effectuée le 16 novembre 2026. La disponibilité d’un flux et les autorisations de réutilisation sont deux questions distinctes. Le fichier `settings.example.json` déclare explicitement les URL, domaines d’articles autorisés, catégories et droits de réutilisation.

## Sources activées

- **Global Voices** : https://globalvoices.org/feed/ découvert dans la page d’accueil ; RSS 200. Licence CC BY 3.0 et [politique d’attribution](https://globalvoices.org/about/global-voices-attribution-policy/) vérifiées. L’auteur issu du flux, le média et le lien original apparaissent au début des cartes ; licence et indication d’extrait raccourci sont affichées. Aucune photo, vidéo ou production de tiers n’est copiée.
- **GOV.UK** : https://www.gov.uk/search/news-and-communications.atom ; Atom 200. [Open Government Licence v3.0](https://www.nationalarchives.gov.uk/doc/open-government-licence/version/3/). Publications officielles attribuées ; les déclarations politiques ne deviennent pas des affirmations indépendamment confirmées.
- **NASA** : https://www.nasa.gov/news-release/feed/ ; RSS 200. [Politique de réutilisation](https://www.nasa.gov/nasa-brand-center/images-and-media/) vérifiée. Textes institutionnels réutilisables, attribution NASA ; pas d’images, logos ou pièces de tiers. Les phrases sans contexte suffisant sont ignorées.
- **National Science Foundation** : https://www.nsf.gov/rss/rss_www_news.xml ; RSS 200, parfois aucun article dans les 36 heures. [Politique officielle](https://www.nsf.gov/policies). Absence d’actualité récente ≠ panne du journal.
- **National Park Service** : https://www.nps.gov/feeds/getNewsRSS.htm?id=nps ; RSS 200. [Politique officielle](https://www.nps.gov/aboutus/disclaimer.htm). Publications fédérales avec attribution ; pas de médias de tiers.

Dans les flux, les descriptions sont limitées et nettoyées ; aucun article complet n’est enregistré ni récupéré derrière un paywall. Les extraits autorisés sont conservés en phrases complètes, 110 mots maximum. Un titre seul reste un titre avec son lien ; il ne devient pas artificiellement une narration de 100 mots.

## Connecteurs présents, non activés sans droits établis

BBC News RSS et Al Jazeera RSS répondaient pendant le contrôle. Les flux Sky ont été découverts sur la [page officielle RSS](https://news.sky.com/info/rss), en remplaçant son protocole HTTP par HTTPS. Tous peuvent être contrôlés avec `python -m generator.app.check_sources` sans publication.

Les [conditions BBC sur les métadonnées / RSS / IA](https://www.bbc.co.uk/usingthebbc/terms/can-i-use-bbc-content/) limitent l’extraction et la transformation ; la permission d’afficher un flux intact n’autorise pas automatiquement un JSON éditorial transformé. Les [conditions Al Jazeera](https://www.aljazeera.com/terms-and-conditions) restreignent copie / redistribution. Les droits Sky restent à établir pour cet usage. L’accès gratuit au flux ne suffit donc pas pour activer ces traitements publics. Ne pas activer des résumés IA de ces sources sans accord approprié ; aucun achat n’est nécessaire au fonctionnement du projet avec ses sources ouvertes.

BBC Newsround sert uniquement de référence de style, sans collecte automatique de flux deviné. Reuters, AP et Gemini ne sont pas utilisés. Aucun scraping ou contournement de droits / robots / paywall.

## Limites du contrôle automatique

Le filtre écarte les sujets explicites et les instructions malveillantes, retire HTML/scripts, impose les domaines, date les sources et regroupe les doublons. La catégorie Muslim World repose sur le sujet géographique / civique, jamais sur une donnée religieuse de l’enfant. La science, la culture et les initiatives quotidiennes peuvent y figurer. Pas de sujet ajouté pour remplir une case.

Les conflits et allégations sont attribués, sans ajout de détails, ou exclus. Aucun modèle ne produit ses propres faits. Le contrôle des phrases IA est strictement extractif : il rejette une phrase qui n’est pas dans les preuves. Cela limite la longueur et le caractère paraphrasé, mais évite de faire passer une similarité lexicale pour une preuve de vérité. Les questions sont des phrases à compléter à partir des titres affichés, avec justification exacte.

## Cloudflare / Llama

Les pages officielles [tarifs Free](https://developers.cloudflare.com/workers-ai/platform/pricing/), [modèle principal](https://developers.cloudflare.com/workers-ai/models/llama-3.3-70b-instruct-fp8-fast/), [API REST](https://developers.cloudflare.com/workers-ai/get-started/rest-api/), [licence Llama 3.3](LLAMA_3_3_LICENSE.txt) et [usage acceptable Meta](https://www.llama.com/llama3_3/use-policy/) ont été consultées. Aucun interdit général équivalent à celui de Gemini concernant une app de lecture pour mineurs n’a été identifié dans ces textes ; le compte d’inférence est celui du parent, jamais de l’enfant. La conformité dépend aussi des conditions du compte lors de son activation ; seuls des textes publics autorisés partent vers l’API.

La page du modèle de repli exact `llama-3.1-8b-instruct-fast` redirigeait vers le catalogue avec `unavailable`. Il est désactivé, sans substitution silencieuse par un nom ressemblant. L’allowlist des modèles vérifiés se trouve dans `data/ai-models.json`. Une mise à jour de modèle doit être accompagnée de la vérification de ses conditions et de sa disponibilité Free ; ne pas élargir la liste à un modèle payant. Les variables GitHub sélectionnent uniquement les modèles approuvés dans ce fichier.

## Étendre On This Day

`data/history/events.json` contient 33 anniversaires grégoriens, textes originaux de 60–100 mots et citations. `source-check.json` conserve les statuts HTTP et courts passages contenant la date, contrôlés pendant le développement. Cette vérification de date ne remplace pas la lecture du contexte de la référence. Les résumés évitent les conversions de dates antérieures à l’adoption grégorienne.

Pour ajouter un jour : vérifier une référence institutionnelle ou un article encyclopédique documenté, distinguer ouverture / signature / entrée en vigueur, écrire un texte original, renseigner `month_day`, année, sources et `verified_on`, puis lancer les tests. Pour une couverture annuelle, maintenir ce même jeu local validé jusqu’à 366 jours ; aucune API historique opaque ne complète les trous automatiquement. Ne jamais convertir une date hégirienne par intuition. Les jours non couverts restent explicitement vides.

Les courts passages de contrôle issus de Wikipédia dans `source-check.json` sont attribués aux contributeurs de Wikipédia et réutilisés sous CC BY-SA 4.0 : https://creativecommons.org/licenses/by-sa/4.0/. Chaque passage conserve le lien de son article. Les résumés historiques sont des textes originaux fondés sur les faits cités.
