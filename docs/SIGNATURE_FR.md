# Signature de production appartenant au parent

La version de test est déjà signée et installable. Pour une identité de mise à jour durable, créez une clé que **vous conservez**. Aucune clé privée ou mot de passe de production n’est livré ni créé au nom du parent.

## Méthode conseillée : Android Studio sous Windows

1. Installez Android Studio depuis https://developer.android.com/studio et ouvrez le dossier `android` du projet. Acceptez le téléchargement gratuit du SDK demandé. Utilisez le JDK compatible fourni par Studio ou Java 21.
2. **Build → Generate Signed App Bundle / APK → APK**.
3. **Create new…** : enregistrez le keystore dans un dossier privé **en dehors du dépôt** et sauvegardez-le hors ligne. Choisissez vos mots de passe et un alias. Le certificat ne nécessite aucun nom ou renseignement sur l’enfant.
4. Sélectionnez **release**, signez puis installez l’APK généré. Son paquet est `uk.family.mydailyworld`, différent du debug.
5. Pour les mises à jour, réutilisez exactement ce keystore, l’alias et les mots de passe. Incrémentez `versionCode` dans `android/app/build.gradle.kts`.

## Compilation locale automatisée

Les variables suivantes sont lues uniquement pour une release ; ne les mettez pas dans Git, un fichier committé ou la ligne de commande publique :

```text
MDW_KEYSTORE_PATH    chemin absolu privé du .jks
MDW_STORE_PASSWORD   mot de passe du magasin
MDW_KEY_ALIAS        alias choisi
MDW_KEY_PASSWORD     mot de passe de clé
```

Ensuite, dans `android` : `gradlew.bat assembleRelease` (Windows) ou `./gradlew assembleRelease`. L’APK signé est `app/build/outputs/apk/release/app-release.apk`. Sans clé configurée, Gradle ne fabrique pas de signature de production de remplacement.

## Option GitHub explicitement configurée

Pour le parent qui choisit de confier une copie chiffrée par GitHub de sa clé au workflow : créer les repository secrets `MDW_KEYSTORE_BASE64` (contenu du keystore encodé en base64), `MDW_STORE_PASSWORD`, `MDW_KEY_ALIAS`, `MDW_KEY_PASSWORD`. Conservez toujours aussi la clé originale **hors GitHub**. Base64 est un encodage ; seuls les **Secrets** GitHub assurent la protection au stockage.

Dans **Build APK → Run workflow**, cochez volontairement `signed_release`. Le keystore temporaire est écrit avec permissions restreintes sur le runner puis supprimé. L’artifact `my-daily-world-parent-signed-release` contient l’APK signé, jamais la clé. Aucun secret Cloudflare n’est injecté dans le workflow Android.
