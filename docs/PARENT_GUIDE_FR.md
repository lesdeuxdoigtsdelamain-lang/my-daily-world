# Guide parental

L’interface et les contenus sont en anglais britannique. L’édition incluse dans l’APK est une véritable publication datée ; elle n’est pas une simulation de nouvelles quotidiennes. Le bouton de test charge séparément une édition **SAMPLE — NOT CURRENT NEWS**, uniquement dans la version debug. Ce sample ajouté par le bouton de test se sélectionne dans Archive à la date du 1 janvier 2000.

- **Today** : édition datée, durée approximative à 150 mots/minute, cartes, provenance, « Why it matters », lecture cochée et reprise de position. Une édition plus ancienne est signalée clairement.
- **Categories** : huit rubriques ; une rubrique vide le dit. Les préférences locales du parent influencent uniquement les sections affichées sur Today. Aucun profil religieux ou historique de lecture n’est envoyé pour classer les sujets.
- **On This Day** : anniversaire de la date de l’édition sélectionnée, référence vérifiable. La base contient 33 jours, dont le 29 février et plusieurs anniversaires des mondes musulman et arabe. Les autres jours affichent l’absence d’entrée vérifiée.
- **Quiz** : jusqu’à cinq questions fondées sur les titres effectivement affichés, trois réponses, correction et explication. Le quiz déterministe est un exercice de lecture de titres à compléter ; il n’invente pas de faits pour créer cinq questions. Les réponses et scores restent dans Room sur la tablette. « Try again » les remet à zéro pour cette édition.
- **Archive** : éditions téléchargées, état de lecture et score. Conservation par nombre d’éditions, 30 par défaut, réglable jusqu’à 90 ; ce n’est pas un téléchargement automatique rétroactif des jours manqués.
- **Parent Settings** : PIN local, connexion Pages / workflow, rappel, taille de texte, clair/sombre, niveau d’affichage 10–12 / 13–15, rubriques, cache et diagnostic. Les deux niveaux utilisent les mêmes faits ; le niveau 10–12 agrandit la présentation du texte. Il ne prétend pas réécrire le journal spécialement pour l’enfant.

**Generate Today's News** ouvre le workflow configuré dans le navigateur externe après une explication. Seul le parent connecté à GitHub peut le lancer. Aucun jeton GitHub n’est demandé par l’app. Le retour au navigateur verrouille les réglages ; un nouveau PIN peut être demandé au retour. La vérification de publication continue brièvement au premier plan et s’arrête en arrière-plan.

**Changer le PIN** se fait après déverrouillage des réglages. Le PIN est protégé par PBKDF2-HMAC-SHA256, 150 000 itérations et sel aléatoire de 16 octets. Après cinq erreurs, pause d’une minute. Ce mécanisme est une protection familiale locale ; un utilisateur contrôlant les réglages système peut réinitialiser les données de l’app.

**PIN oublié** : le parent ouvre Réglages Android → Applications → My Daily World → Stockage → Effacer les données. Cela supprime aussi réglages, cache, progression et quiz. Aucun code universel ni récupération à distance n’existe. Les sauvegardes Android de l’application sont désactivées.

**Rappel quotidien** : une alarme inexacte vise l’heure UK choisie ; une préparation de synchronisation est programmée 15 minutes avant ; WorkManager vérifie aussi environ toutes les heures avec réseau. Une alerte n’est autorisée qu’une fois le journal du jour présent localement, à partir de l’heure choisie. Une publication tardive peut notifier plus tard. Le refus de permission laisse la lecture utilisable. Aucune permission d’alarme exacte privilégiée n’est sollicitée pour une app de nouvelles.

Les liens sortants demandent une confirmation et ouvrent un navigateur séparé. Les conditions, cookies et contenus de ces sites sont ceux de leurs éditeurs. L’app ne charge aucune image de presse, publicité, outil de suivi ou page web intégrée.
