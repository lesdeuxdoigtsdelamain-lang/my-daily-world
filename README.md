# My Daily World

Application Android native familiale en anglais britannique, conçue pour la Galaxy Tab A9. Kotlin / Compose / Room / DataStore / WorkManager. Génération Python, Cloudflare Workers AI facultatif et distribution statique GitHub Pages. Aucun compte enfant, publicité, paiement ou envoi de progression.

**Télécharger l’APK sur GitHub : [instructions précises](docs/TELECHARGER_APK_GITHUB_FR.md).**

**Commencer : [INSTALL_FR.md](docs/INSTALL_FR.md).** [Guide parental](docs/PARENT_GUIDE_FR.md) · [Dépannage](docs/TROUBLESHOOTING_FR.md) · [Limites](docs/LIMITATIONS_FR.md) · [Sources et licences](docs/SOURCES_FR.md).

## Contenu du dépôt

```text
android/                 Application Android Studio et wrapper Gradle
generator/app/           RSS sécurisé, classement, sécurité, IA, quiz, publication
generator/tests/         Tests isolés sans API payante ni RSS réel requis
data/history/            33 anniversaires grégoriens cités et vérification des liens
data/fixtures/           Exemples explicitement SAMPLE — NOT CURRENT NEWS
schema/                  Contrats JSON Schema exportés depuis Pydantic
site/                    Statut public et, après génération, JSON datés
.github/workflows/       Publish Daily et Build APK
docs/                    Installation, signature, maintenance et tests
```

## Exécuter les tests et compiler

Python 3.12+ et Java 21 ; SDK Android 36. Les versions AGP 8.13.2 / Gradle 8.13 / Kotlin 2.2.21 / Compose BOM 2025.11.01 ont été résolues et compilées ensemble. `minSdk=26`, `targetSdk=36`.

```bash
python -m venv .venv
.venv/bin/python -m pip install -r generator/requirements.lock
.venv/bin/python -m pytest -q generator/tests
# Ou, environnement activé : pytest -q generator/tests
AI_PROVIDER=none .venv/bin/python -m generator.app.main --dry-run --date 2026-11-16
# La date explicite sert exclusivement aux essais : jamais à la publication.
AI_PROVIDER=none .venv/bin/python -m generator.app.main
.venv/bin/python -m generator.app.validate_site site
cd android
./gradlew testDebugUnitTest
./gradlew lintDebug assembleDebug
# android/app/build/outputs/apk/debug/app-debug.apk
```

Sous Windows : utiliser `.venv\Scripts\python.exe`, `gradlew.bat`, ou les workflows gratuits décrits dans l’installation. Il n’est pas nécessaire de laisser un PC allumé.

Les secrets sont lus depuis l’environnement du seul processus de génération. `.env.example` ne charge rien automatiquement et ne contient que des noms / placeholders. En l’absence de secrets, le mode effectif est explicitement `none`.

## Publication

Préparation à 15:20 Europe/London ; deuxième tentative conditionnelle à 16:15 ; vérification / génération manquante à 17:00 ; rappel Android souhaité à 17:00. Le bouton parental ouvre `Publish Daily` dans le navigateur : le parent connecté clique ensuite **Run workflow**. Une publication manuelle fonctionne aussi après 17:00 et utilise la date UK courante au début de l’exécution.

Les éditions validées sont conservées sur une branche `content-state`, indépendamment des runners temporaires. Les données statiques sont publiées avec `upload-pages-artifact` / `deploy-pages`. Une exécution suivante ne remplace pas une édition existante, sauf `force_refresh` explicitement coché. L’ID du jour reste identique, donc aucune seconde notification.

Le contrat Pydantic est la référence ; `schema/*.schema.json` et les modèles Kotlin doivent évoluer ensemble. Les tests détectent la dérive du schéma. Room exporte sa version 1 sous `android/app/schemas/` ; ajouter une migration explicite avant toute future modification, sans migration destructive silencieuse.

## Prudence éditoriale concrète

Le programme ne fait aucune recherche d’actualité dans la mémoire d’un modèle. Les faits viennent des flux autorisés. Les informations sensibles restent des titres attribués ; les textes non vérifiables sont retirés. Le validateur IA exige des phrases de preuve complètes : une paraphrase libre plausible est rejetée. La conséquence peut être une édition courte. Ce choix est décrit précisément dans les limites ; il ne constitue pas une garantie générale de vérité des sources.

Les connecteurs BBC / Sky / Al Jazeera sont inclus, testables et désactivés tant que les droits de redistribution nécessaires ne sont pas établis. Global Voices (CC BY 3.0), GOV.UK (OGL) et les sources publiques américaines réutilisables sont activés. Aucun texte de presse complet ni image n’est publié. Les licences et auteurs sont indiqués sur les extraits concernés.

## Licence et attribution

Code original : MIT, voir `LICENSE`. Les contenus sources conservent leur licence propre. Les icônes Material sont sous Apache 2.0. L’icône du globe a été dessinée pour ce projet. IA facultative : **Built with Llama** ; [licence Llama 3.3](docs/LLAMA_3_3_LICENSE.txt), [usage acceptable](https://www.llama.com/llama3_3/use-policy/). Ni Meta ni les médias cités ne parrainent cette application.
