# My Daily World — signature Android permanente

Le workflow **Build APK**, déclenché sur `main`, publie **uniquement** un APK release signé avec la clé stockée dans GitHub Actions Secrets. Il n'envoie plus les APK debug à signature temporaire. Les pull requests exécutent uniquement les tests, sans accès aux secrets.

## Configuration unique du propriétaire

1. Télécharger le ZIP **privé** `My-Daily-World-CLE-PRIVEE.zip` remis séparément, puis le sauvegarder en lieu sûr. Il contient la clé `.p12` et le fichier `SECRETS-A-COPIER.txt`. **Ne jamais publier ces fichiers dans le dépôt GitHub.**
2. Aller sur **Settings → Secrets and variables → Actions → New repository secret**.
3. Créer exactement les 4 secrets avec les valeurs du fichier privé : `MDW_KEYSTORE_BASE64`, `MDW_STORE_PASSWORD`, `MDW_KEY_ALIAS`, `MDW_KEY_PASSWORD`.
4. Ouvrir **Actions → Build APK → Run workflow → main**.
5. Après un résultat vert, télécharger l'artifact **My-Daily-World-SIGNED**, extraire `My-Daily-World.apk`, puis l'installer sur la tablette.

### Migration et prochaines mises à jour

L'application debug précédente utilise l'identifiant `uk.family.mydailyworld.debug` et la release utilise `uk.family.mydailyworld`. Elles peuvent cohabiter temporairement ; installer et tester la release avant de désinstaller l'ancienne debug. Désinstaller l'ancienne efface uniquement ses données locales.

Les mises à jour de la release utiliseront **la même clé**. Pour qu'Android accepte une mise à jour, le futur `versionCode` doit être supérieur et le nom de package inchangé.

**Conserver plusieurs sauvegardes privées de la clé et de son mot de passe.** Si la clé disparaît, on ne peut pas installer une nouvelle release en mise à jour par-dessus l'application signée existante.

Le workflow refuse volontairement de fournir un APK si l'un des quatre secrets manque. Le dépôt public ne contient jamais la clé.
