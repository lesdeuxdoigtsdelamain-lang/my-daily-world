# Télécharger l’APK depuis votre dépôt GitHub

Ces instructions s’appliquent **après l’envoi des fichiers du projet dans le dépôt**. Le fait de créer un dépôt public ne donne pas automatiquement à Codex l’autorisation d’y écrire.

## Compilation automatique

Le workflow `.github/workflows/build-apk.yml`, affiché sous le nom **Build APK**, s’exécute à chaque envoi de modifications Android / générateur / schéma / données sur `main`, ainsi que pour les pull requests. Il peut aussi être lancé manuellement. Il exécute les tests, compile un APK debug signé, puis publie l’artifact **My-Daily-World-debug** pendant 14 jours. Cet artifact contient :

- `My-Daily-World-debug.apk`
- `SHA256SUMS.txt`

Ce workflow n’a besoin d’aucun secret Cloudflare, abonnement, Play Store ou clé de production.

## Où cliquer précisément

1. Connectez-vous à votre compte GitHub dans le navigateur. GitHub demande une connexion pour télécharger les artifacts, même pour un dépôt public.
2. Ouvrez https://github.com/lesdeuxdoigtsdelamain-lang/my-daily-world/actions .
3. Dans la colonne gauche, cliquez sur **Build APK**.
4. Ouvrez l’exécution la plus récente. Attendez la coche verte du job **test-and-build**.
5. Revenez à la page de résumé de cette exécution si vous êtes dans les journaux d’une étape. Descendez jusqu’à **Artifacts**.
6. Cliquez sur **My-Daily-World-debug**. GitHub télécharge une archive ZIP contenant l’APK ; il ne s’agit pas d’un aperçu du fichier source.
7. Décompressez l’archive, puis ouvrez **My-Daily-World-debug.apk** sur la tablette pour lancer l’installation. Le gestionnaire Samsung **Mes fichiers** sait extraire un ZIP.

Si aucune exécution ne démarre : dans **Build APK**, cliquez sur **Run workflow**, choisissez la branche **main**, laissez **signed_release** décoché, puis confirmez **Run workflow**. Si GitHub affiche un bouton d’activation d’Actions, activez les workflows du dépôt.

Si le job est rouge, aucun APK réussi n’est annoncé : ouvrez le job **test-and-build**, puis la première étape rouge pour consulter l’erreur. Les artifacts expirent après 14 jours ; relancer le workflow en produit un nouveau.

## Sources préconfigurées dans l’app

Les deux adresses publiques suivantes sont préconfigurées, sans secret :

- Pages : `https://lesdeuxdoigtsdelamain-lang.github.io/my-daily-world/`
- Publication : `https://github.com/lesdeuxdoigtsdelamain-lang/my-daily-world/actions/workflows/publish-daily.yml`

Activer Pages et renseigner les secrets Cloudflare est nécessaire pour la publication de nouvelles éditions avec IA, **pas pour compiler et télécharger l’APK**. L’édition réelle datée incluse dans l’app reste lisible hors ligne après création du PIN parental.

L’APK téléchargé est signé en **debug**. Pour des mises à jour durables sans réinstallation, utilisez la signature appartenant au parent décrite dans `SIGNATURE_FR.md`.
