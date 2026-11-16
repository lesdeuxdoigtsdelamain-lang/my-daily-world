# Installation complète, sans service payant

L’APK fourni peut déjà lire une **véritable édition compacte du 16 novembre 2026**, avec sources et quiz, sans connexion. Cette édition reste datée et n’est jamais présentée comme celle d’un autre jour. La génération future nécessite votre dépôt GitHub : aucun compte ni jeton n’est incorporé à l’application.

## 1. Créer le dépôt public gratuit

1. Décompressez le projet `my-daily-world.zip`.
2. Installez **GitHub Desktop** depuis https://desktop.github.com/ et connectez-vous à votre compte GitHub gratuit.
3. Dans **File → New repository**, nommez le dépôt `my-daily-world`. Choisissez comme « Local path » le dossier **parent** du dossier `my-daily-world` décompressé. Le chemin final doit être celui contenant `README.md`, `android`, `generator` et `.github`, sans double dossier imbriqué. Créez le dépôt sur ces fichiers existants.
4. Vérifiez que `.github/workflows/publish-daily.yml` et `build-apk.yml` apparaissent dans les fichiers suivis. Validez les fichiers si nécessaire, puis **Publish repository**. Décochez **Keep this code private** pour les runners et Pages gratuits.
5. Le dépôt doit contenir uniquement le projet fourni. N’y ajoutez ni `.env`, jeton, identifiant personnel d’enfant, keystore ou mot de passe.

Alternative avec Git installé, depuis le dossier du projet :

```bash
git init
git add .
git commit -m "Initial My Daily World application"
git branch -M main
git remote add origin https://github.com/VOTRE_COMPTE/my-daily-world.git
git push -u origin main
```

Créez auparavant ce dépôt public vide sur github.com. L’authentification se fait par GitHub / votre navigateur, jamais dans l’APK.

## 2. Activer GitHub Pages

Dans le dépôt : **Settings → Pages → Build and deployment → Source → GitHub Actions**. Les workflows doivent être autorisés dans **Settings → Actions → General**. Les permissions minimales sont déjà déclarées dans leurs fichiers. Aucun abonnement GitHub Pro ni runner payant n’est nécessaire pour ce dépôt public sur runners standards.

Les horaires UK sont 15:20 (préparation), 16:15 (reprise) et 17:00 (dernière vérification / génération si nécessaire). Une édition déjà validée n’est jamais remplacée par ces relances.

Le workflow conserve les seules données publiques sur la branche `content-state`. Il faut pouvoir créer cette branche : ne lui appliquez pas une règle exigeant une PR pour les écritures du workflow. Ne choisissez pas une publication Pages basée sur un commit de branche : le déploiement utilise un artifact Pages officiel.

## 3. Configurer Cloudflare Workers AI Free

Vous pouvez laisser cette étape pour plus tard : une publication sans IA reste utilisable.

1. Dans Cloudflare, conservez **Workers Free**. N’ajoutez pas de moyen de paiement, n’activez pas Workers Paid et ne créez aucun Worker pour ce projet.
2. Dans Workers AI, utilisez **Create a Workers AI API Token**. Le jeton doit permettre l’inférence Workers AI pour votre compte, selon l’assistant officiel. Relevez également votre Account ID. Aucun compte ou jeton « Llama » distinct n’est nécessaire.
3. **Ne copiez aucune de ces valeurs dans le chat.** Dans GitHub, ouvrez **Settings → Secrets and variables → Actions → New repository secret**. Créez exactement :

   - `CLOUDFLARE_ACCOUNT_ID`
   - `CLOUDFLARE_API_TOKEN`

4. Les variables non secrètes facultatives sont `AI_PROVIDER=cloudflare` et `CLOUDFLARE_MODEL=@cf/meta/llama-3.3-70b-instruct-fp8-fast`. Ce sont déjà les valeurs par défaut. Pour désactiver volontairement l’IA, créez la variable `AI_PROVIDER=none`.
5. L’identifiant demandé `@cf/meta/llama-3.1-8b-instruct-fast` était indisponible lors de la vérification. Laissez `CLOUDFLARE_FALLBACK_MODEL` vide. N’utilisez ni la variante dépréciée `...-instruct-fp8`, ni le modèle Kimi du tutoriel. Une erreur du modèle principal ramène à une édition compacte sans IA.
6. **Actions → Publish Daily → Run workflow** : cochez `smoke_test` une fois pour vérifier un petit appel authentifié. Cette exécution ne publie rien. Le journal ne montre ni réponse brute privée, ni token, ni Account ID. Une erreur exige de corriger le compte / jeton dans les secrets, sans le communiquer ailleurs.

La limite gratuite documentée est **10 000 neurons/jour pour tout le compte**, remise à zéro à 00:00 UTC ; ce n’est pas un quota de tokens. Le programme limite les appels et ne peut pas surveiller les autres usages du compte. Il n’active jamais de facturation. Voir [sources / conditions](SOURCES_FR.md).

## 4. Publier le premier journal, même après 17 h

1. **Actions → Publish Daily → Run workflow**.
2. Laissez `smoke_test` et `force_refresh` décochés. Cliquez sur **Run workflow**.
3. Attendez le succès des jobs **generate** et **deploy**. Le pipeline utilise la date du jour **Europe/London**, et non une date fixe fournie par le parent.
4. Ouvrez `https://VOTRE_COMPTE.github.io/my-daily-world/`. Vérifiez la date, les sources et le mode. `latest.json` contient le manifeste ; `health.json` la dernière publication réussie et l’état des flux.
5. Si le journal existe déjà, le message **Today's edition already exists** est normal : il sera redéployé si nécessaire, sans être recréé. `force_refresh` est réservé à une correction administrative volontaire.

## 5. Installer l’APK sur la Galaxy Tab A9

1. Transférez `My-Daily-World-debug.apk` sur la tablette, par téléchargement ou câble USB.
2. Ouvrez-le dans **Mes fichiers**. Si Android le demande, autorisez temporairement cette application à installer des applications inconnues. Installez, puis retirez cette autorisation.
3. Si la fonction Samsung **Blocage automatique / Auto Blocker** empêche l’installation, le parent peut l’ajuster temporairement dans les réglages de sécurité puis la réactiver après installation.
4. Ouvrez **My Daily World**. Créez votre propre PIN de 4 à 8 chiffres et confirmez-le.
5. Appuyez sur le cadenas **Parent Settings**. Saisissez le PIN, puis renseignez :

   - Pages : `https://VOTRE_COMPTE.github.io/my-daily-world/`
   - Workflow : `https://github.com/VOTRE_COMPTE/my-daily-world/actions/workflows/publish-daily.yml`

6. **Save connection**, puis **Check for Today's Edition**.
7. **Allow notifications**, autorisez la demande Android, puis **Test notification**. Le rappel par défaut est 17:00 Europe/London, tous les jours. Il respecte les changements GMT/BST, même si la tablette utilise un autre fuseau.

À 17 h 30 le premier jour : **Generate Today's News → Open GitHub Actions**, connexion du parent dans le navigateur, puis **Run workflow**. Revenez à l’application, déverrouillez à nouveau les réglages si nécessaire et utilisez **Check for Today's Edition**. L’app vérifie aussi quatre fois sur deux minutes lorsqu’elle revient au premier plan. Ouvrir GitHub ne signifie jamais qu’une génération a été lancée.

## 6. APK de test et mises à jour durables

L’APK livré est **signé en debug**, installable pour l’essai. Il porte le paquet `uk.family.mydailyworld.debug`. Un APK reconstruit par un autre ordinateur ou runner a généralement une autre clé debug : Android peut exiger une désinstallation, qui efface les données locales.

Pour l’usage durable, créez votre propre signature : [SIGNATURE_FR.md](SIGNATURE_FR.md). Le paquet release est `uk.family.mydailyworld`, séparé de la version de test. Conservez ensuite toujours la même clé pour préserver les données lors des mises à jour. Ne démarrez pas un long historique de lecture en debug si vous souhaitez le garder après le passage en release.

Pour récupérer un nouvel APK gratuit sur GitHub : **Actions → Build APK → Run workflow**, sans cocher `signed_release` tant que votre signature n’est pas configurée. Téléchargez l’artifact `My-Daily-World-debug` après succès. Windows n’a pas besoin d’Android Studio pour cette méthode.

## 7. Contrôle occasionnel indispensable

GitHub peut retarder les jobs et désactiver les horaires après 60 jours sans activité du dépôt public. Vérifiez occasionnellement **Actions**, la date de la dernière édition et le tableau de bord de quota Cloudflare. En cas d’arrêt, réactivez le workflow dans Actions et lancez-le manuellement. Aucun mécanisme gratuit ne garantit une publication à la seconde ou une continuité éternelle.
