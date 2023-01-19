# MemoQuizZz

Développé par Hippolyte Damay--Glorieux Arthur Keller

Contacts : hippolyte.damayglorieux.etu@univ-lille.fr, arthur.keller.etu@univ-lille.fr

## Présentation de MemoQuizZz

Lors d’un lancement de partie, une image apparait. Le joueur l’observe pendant un temps défini, une question sera posée à laquelle il devra répondre. En plus de faire booster la mémoire, les enfants intégreront des connaissances et enrichiront leur culture générale. Le but du jeu est d’avoir le plus de points possible au fur et à mesure du jeu.

Une fois le jeu lancé, l’utilisateur arrive sur la première page du MemoQuizZz. Il peut choisir entre le mode joueur ou éditeur. Une fois choisi, il suffit de rentrer votre nom d'utilsateur ainsi que votre mot de passe. Si vous n'avez pas encore de compte pas d'inquiétude, il suffit de le rentrer et ensuite MemoQuizZz vous demandera de renseigner votre mot de passe. 

En tant que joueur, vous pourrez choisir le niveau de difficulté du jeu, une fois celui-ci décider le jeu se lancera. Une matière sera choisie automatiquement. La question sera ensuite posée après la disparition de l’image. Les niveaux de difficulté sont différenciés par le temps d’affichage de la carte. 

En tant qu’éditeur, vous pourrez modifier, retirer, ajouter des questions avec ses réponses, des catégories ou gérer les profils des utilisateurs pour améliorer le jeu au fur et à mesure de l’avancée du programme scolaire. Pour ajouter des questions avec leurs réponses ou des catégories, il suffit de suivre les indications quand le jeu est lancé. En ce qui concerne les images le jeu s’occupe de tout ; ).

### Captures d'écran

Des captures d'écran illustrant le fonctionnement du logiciel sont proposées dans le répertoire 'shots'.

## Utilisation de MemoQuizZz

### Compilation

`./compile.sh`

Exécute la compilation des fichiers présents dans 'src'. Ainsi que la création des fichiers *\*.class* et la copie des fichiers situés dans 'ressources' dans 'classes'.

Si vous souhaitez utiliser la réinitialisation de mot de passe par mail. Il faut impérativement passer à vrai la constante **MODE_MAIL** dans le fichier *MemoQuizZz.java*.

### Exécution

Une fois la compilation effectuée, vous pouvez exécuter le programme par la commande suivante.

`./run.sh MemoQuizZz`

Le jeu se décompose en deux modes différents. Le premier est le mode de base, le mode Joueur.

#### Mode Joueur

Accessible à tous, il permet après avoir entré les joueurs, leurs mots de passes respectifs et le paramétrage de la partie de jouer à MemoQuizZz. Il permet aussi de regarder les meilleurs scores faits par les autres utilisateurs sur la machine. Et enfin le joueur a la possibilité de paramétrer son profil.

#### Mode Éditeur

Accessible à tous, mais les actions effectuées dans son mode sont soumises au droits donnés à l'utilisateur. Ce mode a pour but de donner à l'utilisateur une interface de création et/ou de modification de catégories, de questions et de profils. Les ajouts, retraits et modifications sont stockées de manière persistente sur le disque.

### Tests (facultatif)

Modifiez le fichier *MemoQuizZz.java* dans 'src' et ajoutez un trait de soulignement au nom de la fonction principale comme suit :

`void _algorithm()`

Compilez, exécutez.

Bien sûr, il faut enlever le caractère pour revenir à l'exécution normale.


