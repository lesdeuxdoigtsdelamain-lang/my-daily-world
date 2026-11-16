# Dépannage

| Situation | Action du parent |
|---|---|
| « Today's edition isn't ready yet » | Vérifier la date affichée. Ouvrir les réglages parentaux et « Check for Today's Edition ». Si nécessaire lancer Publish Daily manuellement. L’ancienne édition reste lisible, datée. |
| App installée après 17 h | « Generate Today's News », puis **Run workflow** dans GitHub et retour dans l’app. Aucun verrou d’heure ne bloque la publication du jour. |
| Bouton Generate désactivé | Sauvegarder l’URL exacte `https://github.com/COMPTE/DEPOT/actions/workflows/publish-daily.yml`. Aucun token dans cette adresse. |
| GitHub demande une connexion | Se connecter comme propriétaire / parent dans le navigateur externe. Le compte enfant n’existe pas. Ouvrir la page ne suffit pas : presser Run workflow. |
| « Today's edition already exists » | C’est le contrôle anti-doublon. Ne cocher `force_refresh` que pour une correction souhaitée. Le même ID de jour empêche une seconde notification. |
| `latest.json` répond 404 | Première publication pas encore déployée ; vérifier les jobs generate et deploy et la configuration Pages « GitHub Actions ». L’app affiche « Not published yet ». |
| Erreur Pages / environnement | Activer Pages depuis Settings → Pages. Vérifier que le dépôt est public, Actions autorisé et l’environnement `github-pages` autorise le déploiement de la branche principale. |
| Erreur d’écriture `content-state` | Le workflow a besoin de `contents: write`, déjà déclaré. Une règle du dépôt ne doit pas bloquer sa branche de données. Ne pas y stocker de secrets. Relancer rétablit un déploiement Pages interrompu à partir des fichiers validés. |
| Workflow quotidien arrêté | Actions → Publish Daily → Enable workflow si désactivé. Les horaires des dépôts publics peuvent cesser après 60 jours sans activité. Exécution manuelle et contrôle occasionnel restent nécessaires. |
| Cloudflare 401 / 403 | Recréer ou corriger le jeton et son périmètre Workers AI dans les Secrets GitHub ; vérifier l’Account ID. Ne jamais poster les valeurs dans un ticket ou le chat. |
| Cloudflare 404 / 410 | Modèle supprimé / indisponible. Le repli sûr est `none`. Contrôler le catalogue officiel et mettre à jour l’allowlist non secrète après vérification du plan gratuit et des conditions. |
| Cloudflare 429 / quota / timeout | Le journal reste compact sans IA si les faits le permettent. Attendre la remise à zéro de quota à 00:00 UTC ; ne pas activer Paid. |
| IA configurée mais édition marquée `none` | Aucun texte IA n’a passé la validation stricte, preuves insuffisantes ou erreur API. La configuration n’est pas une preuve de production de texte IA. Voir `ai_status` et `health.json`. |
| Peu de sujets / rubrique vide | Pas de remplissage inventé. Consulter `health.json` et [SOURCES_FR.md](SOURCES_FR.md). Les sources dont les droits sont incertains restent désactivées. |
| « Couldn't connect » | Vérifier le Wi-Fi et l’URL Pages sauvegardée. Les téléchargements existants restent disponibles hors ligne. Les redirections et contenus non vérifiables sont refusés. |
| Schéma ou hash invalide | Ne pas modifier manuellement le JSON public. Relancer le pipeline validé ; garder Android et le schéma à la même version. La dernière édition valide reste en cache. |
| Aucun rappel | Autoriser les notifications Android et le canal Daily edition ; vérifier « Test notification », le rappel parental et la présence locale d’un journal d’aujourd’hui. Une alerte de journal ancien est délibérément interdite. |
| Rappel tardif sur Samsung | Examiner les restrictions de batterie de l’app si cela gêne. WorkManager / Doze et Actions n’ont pas de ponctualité garantie ; une app arrêtée de force peut attendre sa prochaine ouverture. |
| PIN oublié / réglages illisibles | Redémarrer d’abord. À défaut : Android → Applications → Stockage → Effacer les données. Réinitialisation locale documentée, avec perte de progression et scores ; aucun PIN secret de secours. |
| APK « non installé » / signature différente | Utiliser la même clé que la version déjà installée. Pour un debug issu d’un autre runner, désinstaller l’ancien debug si nécessaire (perte locale). Pour la durée, utiliser votre release signée. |

Les journaux techniques ne contiennent pas les lectures, scores ou PIN. Pour demander de l’aide, partager le nom du job, le code HTTP et l’étape, jamais les secrets ou une copie de votre keystore.
