# My Daily World — corrections qualité des articles et des images

Version du projet corrigé : 1.2.0 (17 novembre 2026)

## Corrections effectivement apportées

1. Le générateur classe jusqu'à 24 candidats récents et examine leur contenu **avant** de retenir cinq articles. Les articles avec moins de 80 mots de résumé issu de sources autorisées sont écartés. Si moins de cinq bons articles sont disponibles, il en publie moins (ou conserve l'édition datée précédente si aucun article n'est disponible), plutôt que de remplir le journal avec des titres ou des demi-lignes.
2. Le contenu des pages réutilisables est analysé avec des conteneurs d'article (`main`, `article`, `entry-content`, `article-body`...) ; la navigation et les sections publicitaires sont écartées. Aucun média marqué « headline only » n'est aspiré pour en reproduire le texte. Les descriptions RSS restent une source de secours si elles contiennent suffisamment de faits.
3. Des phrases identiques ne peuvent plus être répétées pour gonfler les résumés. Si la reformulation IA vérifiée s'avère trop courte, le générateur conserve la version longue fondée sur les phrases sources. Les sorties IA sont toujours contrôlées et attribuées.
4. La recherche d'images Wikimedia Commons accepte correctement les licences CC BY, CC BY-SA, CC0 et domaine public ; les licences restrictives sont refusées. Les photos sont présentées comme des **illustrations** et accompagnées de leur licence et crédit. Elles ne sont pas présentées comme des photos de l'événement lorsqu'elles ne le sont pas.
5. Huit **véritables illustrations graphiques intégrées à l'APK** remplacent le simple bloc violet/icône lorsqu'aucune photo Wikimedia Commons adaptée n'est trouvée ou si la connexion échoue. Une illustration ne doit jamais être confondue avec une photographie d'actualité.
6. Les actualités environnementales, par exemple un glacier situé en Indonésie, ne sont plus automatiquement classées dans « Muslim World » du seul fait du pays.

## Ce que cela ne garantit pas

- Il n'est pas possible de garantir cinq articles de 80–160 mots **tous les jours** si les sources autorisées et réutilisables n'en fournissent pas assez. La qualité prime sur le nombre. Certaines sources courtes (APOD, communiqués NPS) pourront être écartées.
- La présence d'une **photographie précise** n'est pas garantie. L'application montre à la place une illustration locale originale ; elle affichera une photo Wikimedia Commons lorsqu'une photo admissible est disponible.
- La sélection reste limitée aux sources légalement réutilisables activées. BBC, Sky, Al Jazeera restent désactivés pour le texte intégral, conformément aux contraintes de réutilisation du projet.
- L'anglais peut encore reprendre les formulations des sources : les contrôles actuels refusent les paraphrases IA qui n'ont pas de justification indépendante. Les longues phrases administratives doivent être améliorées ultérieurement à l'aide d'un contrôle de faits plus riche.

## Tests réalisés

- 63 tests Python : réussis, y compris nouvelles vérifications de contenu insuffisant, sélection du sujet suivant, extraction HTML, licences et images locales.
- Syntaxe Python : testée.
- Compilation Android locale : **non effectuée**. La distribution Gradle doit être téléchargée et ce conteneur n'a pas accès à `services.gradle.org`. Le workflow GitHub **Build APK** peut effectuer cette compilation après publication des fichiers.
- Aucune génération réelle du 17 novembre n'a été exécutée ici : le jeton API Cloudflare est conservé dans les secrets GitHub.

## Installer la nouvelle version

1. Publier les fichiers corrigés sur le dépôt GitHub (conserver les chemins des dossiers `generator/` et `android/`). Ne jamais remplacer les secrets GitHub.
2. Lancer `Actions > Build APK` et vérifier l'exécution verte. Télécharger l'artifact du build.
3. Installer l'APK recompilé sur la tablette, en tenant compte de la signature Android : certains APK `debug` émis par des runners différents ne peuvent pas mettre à jour l'ancienne application. Préserver les données avant une désinstallation.
4. Lancer `Actions > Publish Daily` avec `force_refresh=true` et `smoke_test=false` afin de régénérer la date courante, puis `Check for new edition` dans l'app.

Le projet conserve les clés dans GitHub Actions (pas dans le ZIP ou l'APK).
