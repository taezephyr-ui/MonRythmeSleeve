# Mon Rythme Sleeve 🌷

Application **Android native (Java + Gradle)** pour planifier les repas et les pauses boissons après une sleeve, suivant les consignes personnelles indiquées.

## Fonctions de la version 1.0

- Écran d'accueil rose poudré / lavande, statut et compte à rebours jusqu'à la prochaine étape.
- Cinq prises alimentaires quotidiennes : petit-déjeuner, collation 1, déjeuner, collation 2, dîner.
- Durée de chaque prise : **30 minutes**.
- Intervalle entre **fin** d'une prise et **début** de la suivante : **2 h 30** par défaut, ou 2 h 45 / 3 h.
- Arrêt des boissons **30 minutes avant** chaque prise et reprise **30 minutes après** sa fin.
- Recalcul à partir de l'heure de **n'importe quel repas**, par exemple le dîner à 19 h 30.
- Heure de lever variable ; dernière prise terminée **au plus tard à minuit**.
- Boutons « Je commence » / « J'ai terminé » pour ajuster les prochains horaires si possible.
- Notifications Android avec **vibration**, une fois par événement : arrêt boissons, début repas, reprise boissons. Ces alertes sont activées par défaut et peuvent être désactivées dans l'application.
- Reprogrammation après redémarrage et changement d'heure.
- Données enregistrées **localement**, sans compte ni connexion Internet.

## Comment compiler sans ordinateur avec GitHub Actions

**Important :** importer les *contenus* de ce dossier à la **racine du dépôt GitHub**, sans les mettre dans un sous-dossier supplémentaire.

La racine du dépôt doit contenir directement :

```
.github/workflows/build.yml
app/build.gradle.kts
app/src/main/AndroidManifest.xml
app/src/main/java/fr/monrythmesleeve/app/MainActivity.java
build.gradle.kts
gradle.properties
settings.gradle.kts
README.md
```

1. Ouvrir le dépôt GitHub depuis le navigateur.
2. Mettre à jour le dépôt avec **le contenu décompressé** de ce ZIP, en conservant les dossiers `.github` et `app` (ne pas importer le ZIP tel quel comme unique fichier).
3. Ouvrir l'onglet **Actions** → **Construire Mon Rythme Sleeve APK** → **Run workflow**.
4. Attendre la **coche verte** de compilation.
5. Ouvrir l'exécution ; sous **Artifacts**, télécharger `MonRythmeSleeve-APK`.
6. Décompresser cette archive d'artifact ; installer le fichier `app-debug.apk` sur le téléphone Android, en autorisant l'installation depuis le navigateur/gestionnaire de fichiers si Android le demande.
7. Au premier démarrage, autoriser les notifications ; dans **Alertes → Vérifier les autorisations**, vérifier la permission des alarmes exactes si nécessaire.

### Si GitHub affiche une croix rouge

Ouvrir **Actions → exécution en échec → Compiler APK Android → étape rouge** et copier les dernières lignes du journal d'erreur.

## Configuration technique

- Java 17 (pas de Kotlin, pas de Flutter)
- Android Gradle Plugin 8.7.3 ; Gradle 8.9
- compileSdk 35 ; minSdk 26 (Android 8.0+) ; targetSdk 34
- Aucun service externe ni bibliothèque tierce
- Android Studio n'est pas nécessaire si GitHub Actions compile l'APK.

## Limites et vérifications sur téléphone

- Android peut retarder certaines alertes si la permission d'alarmes exactes est refusée ou si le mode économie d'énergie est actif.
- La montre connectée affiche les alertes seulement si son logiciel réplique bien les notifications de l'application et si les permissions nécessaires sont données.
- Les horaires et alertes dépendent du réglage de l'heure du téléphone.
- L'APK obtenu est une **version debug** installable pour tester, pas une publication Play Store.
- Il faut tester sur un vrai téléphone la vibration, les alertes, la reprise après redémarrage et le comportement sur la montre.

Cette application est un outil d'organisation et ne remplace pas les consignes de l'équipe soignante.
