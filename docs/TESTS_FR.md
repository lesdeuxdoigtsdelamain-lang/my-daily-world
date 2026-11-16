# Vérifications exécutées — 16 novembre 2026

## Résultats confirmés

| Vérification | Résultat |
|---|---|
| Python / pytest | **40 tests réussis** ; RSS / Atom, HTML / scripts, injection malveillante, XML dangereux, SSRF, dates / GMT-BST, doublons, citations, quiz, erreurs Cloudflare, absence de secrets, limites de requêtes, publication idempotente et restauration entre runners GitHub simulés. |
| Android / JVM / Robolectric | **16 tests réussis**, dont Room / état de lecture et quiz, cache hors ligne, PIN salé, notification après l’heure UK / refus d’édition périmée ou sample, contrôle de hash et schéma, et test des cartes / histoire / quiz / archive avec état réel. |
| Gradle | Wrapper 8.13 exécuté avec SHA-256 de distribution vérifié ; compilation `assembleDebug` réussie et APK de tests instrumentés compilé. |
| Analyse Android | `lintDebug` réussi : aucune erreur bloquante ; avertissements principalement sur versions plus récentes de dépendances, KAPT et conseils Android. |
| Signature APK | `apksigner verify` réussi, signature APK v2. Signature **debug**, pas une signature de production appartenant au parent. |
| Compatibilité déclarée | `minSdk=26`, `targetSdk=36`, bibliothèques natives `arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`. La Galaxy Tab A9 est couverte par l’ABI ARM64 et Android 13+. |
| Collecte réelle | Édition réelle compacte du **16 novembre 2026** produite depuis les RSS / Atom activés, **10 sujets**, **5 questions**, histoire UNESCO citée. Aucune IA n’a été utilisée. |
| Validation réelle | Manifeste, hash et JSON daté validés. Une seconde exécution renvoie « Today's edition already exists ». Le dry-run daté produit un diagnostic sans publier de fichiers. |
| Flux réels | Global Voices, NASA, GOV.UK, NSF, NPS, BBC, Sky et Al Jazeera répondent. NSF n’avait aucun article éligible dans la fenêtre récente. Les connecteurs sans droits établis restent désactivés. |
| Histoire | 33 entrées originales citées, 60–100 mots ; références et dates vérifiées, dont le 29 février. Couverture partielle explicitement déclarée. |
| Émulateur Android 13 | APK final installé et lancé ; processus de l’app actif et interface native affichée. L’émulateur fonctionne sans KVM et ses propres services système ont rencontré des plantages / lenteurs. Ne pas assimiler son comportement à celui de la tablette physique. |
| Tests instrumentés sur l’émulateur | Deux tests compilés et lancement tenté. Le transport ADB s’est interrompu après une minute sans résultat final ; **ces deux tests ne sont pas comptés comme réussis**. Les 16 tests Android réussis ci-dessus sont les tests JVM / Robolectric, dont un test d’interface Compose. |

Le dossier de livraison `reports` contient les preuves textuelles et résultats machine disponibles.

## Vérifications externes non exécutées

- **Cloudflare réel** : aucun jeton ni Account ID disponible ici. Les erreurs 401 / 403 / 404 / 410 / 429 / timeout et réponses malformées sont testées par transport HTTP simulé. Le smoke test authentifié est fourni dans Publish Daily ; il doit être lancé après configuration des deux secrets par le parent.
- **Publication sur votre GitHub / Pages** : aucun dépôt ou compte autorisé disponible. Les workflows, horaires UK, permissions et persistance sont testés localement ; aucun déploiement public n’est annoncé comme effectué.
- **Scénario réel à 17 h 30 avec compte parent** : date UK, idempotence, synchronisation et chemin navigateur sont implémentés et testés séparément. Le lancement authentifié Run workflow et le téléchargement depuis votre Pages restent à vérifier après installation externe.
- **Galaxy Tab A9 physique / batterie Samsung / TalkBack réel** : appareil non connecté. Les états de lecture, interfaces Compose et règles de programmation sont testés ; la ponctualité réelle d’une alerte dépend aussi du système et des permissions de la tablette.
- **APK release parental** : aucune clé privée du parent n’a été créée ou reçue. La procédure Android Studio et le workflow de signature volontaire sont fournis.

## Reproduire

```bash
python -m pip install -r generator/requirements.lock
python -m pytest -q generator/tests
AI_PROVIDER=none python -m generator.app.main --dry-run --date 2026-11-16
python -m generator.app.check_sources
cd android
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

La dernière commande exige un émulateur ou appareil Android opérationnel. Les tests unitaires et instrumentés sont distincts. Pour un contrôle manuel : créer un PIN, lire / cocher un sujet, répondre au quiz, fermer / rouvrir, activer le mode avion, vérifier l’archive, retourner les orientations, agrandir la police, puis essayer une notification après avoir donné la permission.
