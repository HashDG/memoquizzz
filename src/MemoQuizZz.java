
import extensions.*;
import java.util.regex.Pattern;
import java.util.Properties;
import javax.mail.*;
import javax.mail.internet.*;

class MemoQuizZz extends Program {
    // Secondes vers millisecondes, utile pour initialiser les délais en secondes
    final int STOMS = 1000;
    // Bornes des entiers, utiles pour les menus ne définissant une (ou aucune) borne
    final int INT_MIN = -2147483648, INT_MAX = 2147483647;
    // Droits d'éditions et de manipulation du jeu
    final int DROIT_JOUER = 0x01, DROIT_AJOUTER_QUESTION = 0x02, DROIT_RETIRER_QUESTION = 0x04, DROIT_AJOUTER_CATEGORIE = 0x08, DROIT_RETIRER_CATEGORIE = 0x10, DROIT_ADMIN = 0x20;
    // Pour activer le mode mail, malheureusement en l'état il est inutilisable à l'IUT
    final boolean MODE_MAIL = false;

    void algorithm() {
		clearScreen();
		afficherLogo();
	
		ListeCategories categoriesChargees = newListeCategories(chargerCategories());
		ListeProfils profilsCharges = newListeProfils(chargerProfils());
	
		if (boolChoix('J', 'E', "Veuillez choisir un mode :\n\tJ: Joueur\n\tE: Éditeur")) { // mode Joueur
		    modeJoueur(profilsCharges, categoriesChargees);
		} else { // mode Éditeur
			modeEditeur(profilsCharges, categoriesChargees);
			enregistrerCategories(categoriesChargees.categories);
		}
		enregistrerProfils(profilsCharges.profils);
    }

    void afficherQuestions(Categorie[] a) {
		for (Categorie c : a) {
	    	println("C: "+ c.nom);
	    	for (Question q : c.questions) {	
				println("\tQ: " + q.question);
				for (Reponse r : q.reponses) {
				    println("\t\tR: " + r.texte);
				}
	    	}
		}	
    }

    void afficherMode(String mode) {
		clearScreen();
		cursor(0,0);
		afficherASCIIArt(mode);
    }

    void afficherLogo() {
		afficherASCIIArt("M-QUIZZZ");
		println();
		afficherASCIIArt("Devs: Damay et Keller");
		println();
		println("Appuyez sur Entrée");
		readString();
		clearScreen();
		cursor(0,0);
    }

    void afficherScores(Profil[] profils, String titre, boolean general) {
		clearScreen();
		cursor(0,0);
		afficherASCIIArt(titre);
		int idx = 1;
		String s;

		Profil[] topJoueurs = trierScores(profils, 10, general);

		for (Profil p : topJoueurs) {
	    	if (general) {
				println("" + idx++ + ": " + p.nom + " a " + p.score + " points");
	    	} else {
				println("J" + idx++ + ": " + p.nom + " a " + p.scorePartie + " points");
	    	}	
		}
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	GESTIONS DES DEUX MODES
    
    void modeJoueur(ListeProfils profilsCharges, ListeCategories categoriesCharges) {
    	final int SOUS_MODE_JOUER = 1, SOUS_MODE_SCORES = 2, SOUS_MODE_PARAMETRES = 3, QUITTER = 4;
		int choix;	
		    
	    afficherMode("Joueur");
    	do {
			choix = menuChoix(1, 4, "Que désirez vous faire ?\n\t1: Jouer\n\t2: Consulter les scores\n\t3: Gérer votre compte\n\t4: Quitter");
			
			if (choix == SOUS_MODE_JOUER) { // sous-mode Jeu
			    initialisationJeu(profilsCharges, categoriesCharges.categories);
			} else if (choix == SOUS_MODE_SCORES) { // sous-mode Scores généraux
		    	afficherScores(profilsCharges.profils, "Scores généraux", true);
			} else if (choix == SOUS_MODE_PARAMETRES) { // sous-mode Paramétrage
				parametrageProfil(profilsCharges);
			}
	    } while(choix != QUITTER);
	    println("Vous nous quittez si tôt ? :'(");
    }
	
	void parametrageProfil(ListeProfils profilsCharges) {
		final int SOUS_SOUS_MODE_MDP = 1, SOUS_SOUS_MODE_NOM = 2, SOUS_SOUS_MODE_MAIL = 3, QUITTER = 4;
		Profil profil;
		int choix;
		String entree;
		
		if (profilsCharges.taille > 1) {
			boolean valide;
			println("Plusieurs profils sont actuellement chargés veuillez saisir votre pseudo");
			
			do {
				print("Votre pseudo : ");
				entree = readString();
				profil = rechercheParPseudo(entree, profilsCharges.profils);
				valide = !profil.equals(null);
				if (!valide) {
					println("Votre pseudo n'est pas actuellement chargé, veuillez le resaisir");
				}
			} while(!valide);
		} else {
			profil = profilsCharges.profils[0];
		}
		
		do {
			choix = menuChoix(1, 4, "Que désirez vous faire ?\n\t1: Changer de mot de passe\n\t2: Changer de pseudo\n\t3: Changer votre adresse mail\n\t4: Quitter");
			
			if (choix == SOUS_SOUS_MODE_MDP) {
				println("Avant de changer votre mot de passe veuillez le saisir");
			
				print("Votre mot de passe : ");
				demandeMdp(profil);
				
				print("Entrez votre nouveau mot de passe : ");
				entree = readString();
				profil.mdp = (equals(entree, "")) ? longsToBytes(0L, 0L) : md5(strToBytes(profil.nom + entree));
				
				print("Veuillez le saisir à nouveau : ");
				demandeMdp(profil);
			} else if (choix == SOUS_SOUS_MODE_NOM) {
				println("Avant de changer votre pseudo veuillez saisir votre mot de passe");
				String ancien = profil.nom, nouveau;
				
				print("Votre mot de passe : ");
				demandeMdp(profil);
				
				print("Entrez votre pseudo : ");
				nouveau = readString();
				
				print("Veuillez saisir à nouveau votre mot de passe");
				
				boolean valide;
    			do {
					entree = readString();
					valide = ((estVide(profil.mdp) && equals(entree, "")) || equals(md5(strToBytes(profil.nom + entree)), profil.mdp));
					if (!valide) {
						println("Mot de passe invalide");
					}
				} while (!valide);
				
				profil.nom = nouveau;
				profil.mdp = (equals(entree, "")) ? longsToBytes(0L, 0L) : md5(strToBytes(profil.nom + entree));		
			} else if (choix == SOUS_SOUS_MODE_MAIL) {
				println("Avant de changer votre mail veuillez saisir votre mot de passe");				
				
				print("Votre mot de passe : ");
				demandeMdp(profil);
				
				print("Entrez votre adresse mail : ");
				entree = readString();
				profil.mail = (equals(entree, "")) ? longsToBytes(0L, 0L) : md5(strToBytes(entree));
			}
		} while (choix != QUITTER);
	}
	
	void initialisationJeu(ListeProfils profilsCharges, Categorie[] categoriesCharges) {
		int quantite = menuChoix(1, 4, "Veuillez choisir le nombre de joueurs (entre 1 et 4)");
		Profil[] profils = entreeUtilisateurs(quantite, profilsCharges);

		int niveau = menuChoix(1, 3, "Veuillez entrer le niveau de difficulté désiré : facile (1), normal (2), difficile (3)");
		
		Categorie[] categoriesChoisies = choisirCategorie(categoriesCharges);
		Question[] questions = chargerQuestions(categoriesChoisies);
			
		int nombreTours = menuChoix(1, INT_MAX, "Veuillez choisir le nombre de tours désirés (au minimum 1 tour)");	    

		Profil actuel;
		int tours = 0, indiceJoueur = -1;
		
		do {
			actuel = profils[ (++indiceJoueur)%length(profils) ];
			if (poserQuestion(questions, actuel.nom, ++tours, niveau)) {
				afficherASCIIArt("Bravo");
			    actuel.scorePartie += 1;
			    actuel.score += 1;
			} else {
			    afficherASCIIArt("Échec");
			}
			delay(1*STOMS);
		} while (actuel.scorePartie < 20 && tours < nombreTours);
		gererStats(profils, actuel);	
		afficherScores(profils, "Score de fin de partie", false);
		majListe(profilsCharges, profils);
	}
	
	Categorie[] choisirCategorie(Categorie[] categoriesCharges) {
		ListeCategories lc = newListeCategories();
		final int QUITTER = 0;
		int choix;
		boolean valide;
		println("Ci-dessous les catégories disponibles. Veuillez entrer l'indice des catégories que vous désirez. Entrez 0 pour sortir du choix.");
		for (int i = 0; i < length(categoriesCharges); i++) {
			println(String.format("%d : %s", i+1, categoriesCharges[i].nom));
		}	
		
		do {
			print("Votre choix : ");
			choix = readInteger();
			valide = (choix >= 0 && choix <= length(categoriesCharges));
			if (valide && choix != QUITTER) {
				ajouter(lc, categoriesCharges[choix-1]);
				println("La catégorie " + categoriesCharges[choix-1].nom + " a été ajoutée");
			} else if (choix != QUITTER){
				println("Impossible de trouver la catégorie");
			}
		} while(choix != QUITTER);
		
		print("Vous avez donc choisi les catégories suivantes : ");
		for (int i = 0; i < lc.taille; i++) {
			print(lc.categories[i].nom);
			if (i < lc.taille - 1) {
				print(", ");
			}
		}
		println();
		return lc.categories;
	}
	
	void gererStats(Profil[] joueurs, Profil gagnant) {
		for (Profil p : joueurs) {
			p.nbPartiesJouees++;
		}
		gagnant.nbPartiesGagnees++;
	}
	
	void modeEditeur(ListeProfils profilsCharges, ListeCategories categoriesChargees) {
		final int SOUS_MODE_CATEGORIES = 1, SOUS_MODE_QUESTIONS = 2, SOUS_MODE_PROFILS = 3, QUITTER = 4;
		int choix;
		
  	    afficherMode("Éditeur");
	    Profil editeur = authentification(profilsCharges);
	    
	    do {
	    	choix = menuChoix(1, 4, "Que désirez-vous éditer ?\n\t1: Catégories\n\t2: Questions\n\t3: Profils\n\t4: Quitter");
	    	if (choix == SOUS_MODE_CATEGORIES) {
	    		editionCategories(editeur, categoriesChargees);
	    	} else if (choix == SOUS_MODE_QUESTIONS) {
	    		editionQuestions(editeur, categoriesChargees.categories);
	    	} else if (choix == SOUS_MODE_PROFILS) {
	    		editionProfils(editeur, profilsCharges);
	    	}
	    } while(choix != QUITTER);
	}
	
	void editionCategories(Profil editeur, ListeCategories categoriesChargees) {
		final int SOUS_SOUS_MODE_AJOUT = 1, SOUS_SOUS_MODE_RETRAIT = 2, SOUS_SOUS_MODE_MODIFICATION = 3, SOUS_SOUS_MODE_AFFICHAGE = 4, QUITTER = 5;
		int choix;
		Categorie[] categories;
		
		afficherMode("Éditeur de catégories");
		
		do {
			choix = menuChoix(1, 5, "Que désirez-vous faire ?\n\t1: Ajouter une catégorie\n\t2: Supprimer une catégorie\n\t3: Modifier une catégorie\n\t4: Afficher les catégories\n\t5: Quitter");
			if (choix == SOUS_SOUS_MODE_AJOUT) {
				if (aDroit(editeur, DROIT_AJOUTER_CATEGORIE)) {
					print("Veuillez entrer le nom de la catégorie: ");
					String nom = readString();
					if (trouverCategorie(nom, categoriesChargees.categories) == null) {
						Categorie c = newCategorie(nom);
						println("La catégorie " + nom + " a bien été créée. L'ajout des questions se fait dans la section 'Questions'");
						c.questions = new Question[0];
						ajouter(categoriesChargees, c);
					} else {
						println("Vous ne pouvez saisir le nom d'une catégorie déjà existante");
					}
				} else {
					println("Vous n'avez pas le droit d'ajouter une catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_RETRAIT) {
				if (aDroit(editeur, DROIT_RETIRER_CATEGORIE)) {
					print("Veuillez entrer le nom de la catégorie: ");
					String nom = readString();
					if (trouverCategorie(nom, categoriesChargees.categories) != null) {
						retirer(categoriesChargees, trouverCategorie(nom, categoriesChargees.categories));
					} else {
						println("Vous ne pouvez pas retirer une catégorie inexistante");
					}
				} else {
					println("Vous n'avez pas le droit de retirer une catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_MODIFICATION) {
				if (aDroit(editeur, DROIT_ADMIN)) {
					print("Veuillez entre le nom de la catégorie");
					Categorie c = trouverCategorie(readString(), categoriesChargees.categories);
					if (c != null) {
						if (boolChoix('n', 'c', "Que désirez-vous éditer ?\n\tn: Nom\n\tc: Chemin")) {
							print("Veuillez saisir le nouveau nom: ");
							c.nom = readString();
						} else {
							print("Veuillez saisir le nouveau chemin: ");
							c.chemin = readString();
						}
						majListe(categoriesChargees, c);
					} else {
						println("Vous ne pouvez pas modifier une catégorie inexistante");
					}
				} else {
					println("Vous n'avez pas le droit de retirer une catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_AFFICHAGE) {
				println("Affichage des catégories chargées");
				for (int i = 0; i < categoriesChargees.taille; i++) {
					println(String.format("%d : %s", i+1, categoriesChargees.categories[i].nom));
				}
			}
		} while(choix != QUITTER);
	}
	
	void editionQuestions(Profil editeur, Categorie[] categoriesChargees) {
		final int SOUS_SOUS_MODE_AJOUT = 1, SOUS_SOUS_MODE_RETRAIT = 2, SOUS_SOUS_MODE_AFFICHAGE = 3, SOUS_SOUS_MODE_CATEGORIE = 4, QUITTER = 5;
		int choix;
		Categorie categorie = null;
		ListeQuestions lq = null;
		
		afficherMode("Éditeur de questions");
		
		do {
			choix = menuChoix(1, 5, "Que désirez-vous faire ?\n\t1: Ajouter une question\n\t2: Supprimer une question\n\t3: Afficher questions\n\t4: Choisir une catégorie\n\t5: Quitter");
			if (choix == SOUS_SOUS_MODE_AJOUT) {
				if (categorie != null) {
					if (aDroit(editeur, DROIT_AJOUTER_QUESTION)) {
						Question q = new Question();
						q.matiere = categorie.nom;
						print("Entrez votre question : ");
						q.question = readString();
						print("Quelle illustration voulez-vous utiliser ? : ");
						
						String fichierCarte;
						do {
							fichierCarte = readString();
							if (!cheminCarteValide(fichierCarte)) {
								print("Fichier introuvable, veuillez le placer dans le dossier `ressources/cartes` et resaisir le nom du fichier : ");
							}
						} while(!cheminCarteValide(fichierCarte));
						
						q.carte = newFile(fichierCarte);
						q.cheminCarte = fichierCarte;
						int nbRep = menuChoix(2, INT_MAX, "Combien de réponses désirez-vous ? (2 minimum)");
						q.reponses = new Reponse[nbRep];
						Reponse r;
						int nbBonneRep = 0;
						for (int i = 0; i < nbRep; i++) {
							r = new Reponse();
							print("Entrez la réponse: ");
							r.texte = readString();
							r.valide = boolChoix('o', 'n', "La réponse est-elle valide ? (o|n)");
							q.reponses[i] = r;
							if (r.valide) {
							    nbBonneRep++;
							}
						}
						q.reponses = trierReponsesParValidite(q.reponses);
						q.nbBonneRep = nbBonneRep;
						
						ajouter(lq, q);
					}										
				} else {
					println("Vous n'avez pas choisi de catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_RETRAIT) {
				if (categorie != null) {
					if (aDroit(editeur, DROIT_RETIRER_QUESTION)) {
						int choix2 = menuChoix(1, lq.taille, "Entrez l'indice de la question que vous souhaitez modifier (pour connaître l'indice référez vous à la commande d'affichage)");
						retirer(lq, categorie.questions[choix2-1]);	
					}
				} else {
					println("Vous n'avez pas choisi de catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_AFFICHAGE) {
				if (categorie != null) {
					println("Affichage des questions chargées dans la catégorie " + categorie.nom);
					for (int i = 0; i < lq.taille; i++) {
						println(String.format("%d : %s", i+1, lq.questions[i].question));
					}
				} else {
					println("Vous n'avez pas choisi de catégorie");
				}
			} else if (choix == SOUS_SOUS_MODE_CATEGORIE) {
				boolean trouve;
				Categorie c;	
				print("Entrez le nom de la catégorie que vous désirez charger : ");	
				do {
					c = rechercheParNom(readString(), categoriesChargees);
					trouve = (!c.equals(null));
					if (!trouve) {
						print("Catégorie introuvable, tentez autre chose : ");
					}
				} while (!trouve);
				categorie = c;
				lq = newListeQuestions(c.questions);
				println("La catégorie "+c.nom+" a été chargée");
			}
		} while (choix != QUITTER);
		categorie.questions = lq.questions;
	}
	
	void editionProfils(Profil editeur, ListeProfils profilsCharges) {
		final int SOUS_SOUS_MODE_MODIFICATION_MDP = 1, SOUS_SOUS_MODE_MODIFICATION_DROITS = 2, SOUS_SOUS_MODE_AFFICHAGE = 3, SOUS_SOUS_MODE_CHOIX_PROFIL = 4, QUITTER = 5;
		
		int choix;
		Profil profil = null;
		
		afficherMode("Éditeur de profils");
		
		do {
			choix = menuChoix(1, 5, "Que désirez-vous faire ?\n\t1: Modifier le mot de passe du profil\n\t2: Modifier les droits du profil\n\t3: Afficher profil\n\t4: Choisir un profil\n\t5: Quitter");
			if (choix == SOUS_SOUS_MODE_MODIFICATION_MDP) {
				if (profil != null) {
					if (aDroit(editeur, DROIT_ADMIN)) {
						byte[] mdp;	
						boolean valide;
						print("Veuillez donner le nouveau mot de passe : ");					
						do {
							mdp = strToBytes(profil.nom+readString());
							print("Veuillez le rentrer à nouveau : ");
							valide = equals(mdp, strToBytes(profil.nom+readString()));
							if (!valide) {
								print("Vérification échouée, veuillez réitérer l'opération : ");
							}
						} while (!valide);
						profil.mdp = md5(mdp);
						println("Le mot de passe de " + profil.nom + " a bien été modifié");
					}
				} else {
					println("Vous n'avez pas choisi de profil");
				}
			} else if (choix == SOUS_SOUS_MODE_MODIFICATION_DROITS) {
				if (profil != null) {
					if (aDroit(editeur, DROIT_ADMIN)) {
						println("Les droits sur MemoQuizZz fonctionne selon les puissances de deux (1, 2, 4, 8, 16...) sont des droits valides. Et pour avoir de multiples droits, il suffit de faire la somme:\n\t1: Droit à jouer\n\t2: Droit à ajouter une question\n\t4: Droit à retirer une question\n\t8: Droit à ajouter une catégorie\n\t16: Droit à retirer une catgéorie\n\t32: Droit administrateur");
						print("Quels droits désirez vous ajouter à l'utilisateur ? : ");
						int droitsAajouter = 0;
						do {
							droitsAajouter = readInteger();
							if (droitsAajouter < 0 || droitsAajouter > 64) {
								print("Vous êtes hors des bornes veuillez ne pas dépasser 63 : ");
							}
						} while(droitsAajouter < 0 || droitsAajouter > 64);
						profil.droits |= droitsAajouter;
						println("Les droits de "+profil.nom+" ont bien été mis à jour");
					}
				} else {
					println("Vous n'avez pas choisi de profil");
				}
			} else if (choix == SOUS_SOUS_MODE_CHOIX_PROFIL) {
				print("Veuillez saisir le nom du profil : ");
				profil = trouverProfil(readString(), profilsCharges.profils);
				if (profil == null) {
					println("Impossible de trouver le profil !");
				} else {
					println("Le profil "  +profil.nom+" a été chargé");
				}
			} else if (choix == SOUS_SOUS_MODE_AFFICHAGE) {
				if (profil != null) {
					String rang;
					if (aDroit(profil, DROIT_ADMIN)) {
						rang = "Administrateur";
					} else if (aDroit(profil, DROIT_AJOUTER_QUESTION) || aDroit(profil, DROIT_RETIRER_QUESTION) || aDroit(profil, DROIT_AJOUTER_CATEGORIE) || aDroit(profil, DROIT_RETIRER_CATEGORIE)) {
						rang = "Éditeur";
					} else {
						rang = "Joueur";
					}
					
					println("Informations de profil :");
					println("\tPseudo: " + profil.nom);
					println("\tRang: " + rang);
					println("\tScore: " + profil.score);
					println("\tRatio: " + ratio(profil));
				} else {
					println("Vous n'avez pas choisi de profil");
				}
			}
		} while (choix != QUITTER);
	}
		
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	GESTIONS DES DONNEES PERSISTANTES

    Profil[] chargerProfils() {
		CSVFile csv = loadCSV("profils.csv", ';');
		final int LIMIT = rowCount(csv);
		final int COL_NOM = 0, COL_DROITS = 1, COL_SCORE = 2, COL_JOUEES = 3, COL_GAGNEES = 4, COL_MAIL1 = 5, COL_MAIL2 = 6, COL_MDP1 = 7, COL_MDP2 = 8;
		int idx = 0;
		
		Profil[] profils = new Profil[LIMIT -1];
		Profil p;
		for (int i = 1; i < LIMIT; i += 1) {
		    p = new Profil();
		    p.nom = getCell(csv, i, COL_NOM);
		    p.mdp = longsToBytes(Long.parseLong(getCell(csv, i, COL_MDP1)), Long.parseLong(getCell(csv, i, COL_MDP2)));
		    p.mail = longsToBytes(Long.parseLong(getCell(csv, i, COL_MAIL1)), Long.parseLong(getCell(csv, i, COL_MAIL2)));
		    p.droits = stringToInt(getCell(csv, i, COL_DROITS));
		    p.score = stringToInt(getCell(csv, i, COL_SCORE));
		    p.nbPartiesJouees = stringToInt(getCell(csv, i, COL_JOUEES));
		    p.nbPartiesGagnees = stringToInt(getCell(csv, i, COL_GAGNEES));
		    profils[idx++] = p;
		}
		return profils;
    }
	
    void afficher(String[][] tab) {
		println("lengths tab=" + length(tab) + " tab[i]=" + length(tab[0]));
		for (int i = 0; i < length(tab); i++) {
		    print(i + "|");
		    for (int j = 0; j < length(tab[i]); j++) {
				print(tab[i][j] + " ");
		    }
		    println();
		}
		println("fini");
    }

    Profil[] fusionner(Profil[] p1, Profil[] p2) {
    	if (p1==null) return p2;
    	if (p2==null) return p1;
		Profil[] temporaire = new Profil[length(p1)+length(p2)], resultat;
		
		int pos = 0;
		for (int i = 0; i < length(p1); i ++) {
		    if (trouverProfil(p1[i].nom, temporaire) == null) {
				temporaire[pos++] = p1[i];
		    }
		}
		for (int i = 0; i < length(p2); i ++) {
		    if (trouverProfil(p2[i].nom, temporaire) == null) {
				temporaire[pos++] = p2[i];
		    }
		}
		resultat = new Profil[pos];
		for (int i = 0; i < pos; i++) {
		    resultat[i] = temporaire[i];
		}
		return resultat;
    }
    	
	Categorie[] fusionner(Categorie[] p1, Categorie[] p2) {
    	if (p1==null) return p2;
    	if (p2==null) return p1;
		Categorie[] temporaire = new Categorie[length(p1)+length(p2)], resultat;
		
		int pos = 0;
		for (int i = 0; i < length(p1); i ++) {
		    if (trouverCategorie(p1[i].nom, temporaire) == null) {
				temporaire[pos++] = p1[i];
		    }
		}
		for (int i = 0; i < length(p2); i ++) {
		    if (trouverCategorie(p2[i].nom, temporaire) == null) {
				temporaire[pos++] = p2[i];
		    }
		}
		resultat = new Categorie[pos];
		for (int i = 0; i < pos; i++) {
		    resultat[i] = temporaire[i];
		}
		return resultat;
    }
     
    void testFusionner() {
		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C"), q4 = newQuestion("D"), q5 = newQuestion("E"), q6 = newQuestion("F");     	
    	Question[] qa1 = new Question[]{q1, q2, q3};
    	Question[] qa2 = new Question[]{q4, q5, q6};
    	Question[] qa3 = new Question[]{q1, q3, q5};
    	Question[] qa4 = new Question[]{q2, q4, q6};
    	
    	assertArrayEquals(new Question[]{q1, q2, q3, q4, q5, q6}, fusionner(qa1, qa2));
    	assertArrayEquals(new Question[]{q1, q3, q5, q2, q4, q6}, fusionner(qa3, qa4));
    	assertArrayEquals(new Question[]{q1, q2, q3, q4, q6}, fusionner(qa1, qa4));
    	assertArrayEquals(new Question[]{q4, q5, q6, q1, q3}, fusionner(qa2, qa3));
    }
    
    Question[] fusionner(Question[] p1, Question[] p2) {
    	if (p1==null) return p2;
    	if (p2==null) return p1;
		Question[] temporaire = new Question[length(p1)+length(p2)], resultat;
		
		int pos = 0;
		for (int i = 0; i < length(p1); i ++) {
		    if (trouverQuestion(p1[i].question, temporaire) == null) {
				temporaire[pos++] = p1[i];
		    }
		}
		for (int i = 0; i < length(p2); i ++) {
		    if (trouverQuestion(p2[i].question, temporaire) == null) {
				temporaire[pos++] = p2[i];
		    }
		}
		resultat = new Question[pos];
		for (int i = 0; i < pos; i++) {
		    resultat[i] = temporaire[i];
		}
		return resultat;
    }
    
    void enregistrerCategories(Categorie[] categories) {
    	String[][] contenu = new String[length(categories)+1][2];
    	contenu[0] = new String[]{"nom", "chemin"};
    	Categorie c;
    	
    	for (int i = 0; i < length(categories); i++) {
    		c = categories[i];
    		enregistrerQuestions(c);
    		contenu[i+1] = new String[]{c.nom, c.chemin};
    	}
    	saveCSV(contenu, "../ressources/categorie-base.csv", ';');
    }
    
    void testTailleEntete() {
    	Reponse[] ra3 = new Reponse[]{new Reponse(), new Reponse(), new Reponse()};
    	Reponse[] ra4 = new Reponse[]{new Reponse(), new Reponse(), new Reponse(), new Reponse()};
    	    
    	Question[] qa = new Question[] {
    		newQuestion("C", null, "Q", ra3),
    		newQuestion("C", null, "Q", ra4),
    		newQuestion("C", null, "Q", ra3),
    	};
    	
    	Question[] qa1 = new Question[] {
    		newQuestion("C", null, "Q", ra3),
    		newQuestion("C", null, "Q", ra3)
    	};
    
    	Categorie c = newCategorie("C", null, qa);
    	Categorie c1 = newCategorie("C", null, qa1);
    	
    	assertEquals(4, nombreMaximalReponse(c));   
    	assertEquals(3, nombreMaximalReponse(c1));	
    }

    int nombreMaximalReponse(Categorie c) {
		int nbMaxRep = 0;
		for (Question q : c.questions) {
		    nbMaxRep = max(length(q.reponses), nbMaxRep);
		}
		return nbMaxRep;
    }
    
    String[] faireEnTeteCSV(Categorie categorie) {
		int nbMaxRep = nombreMaximalReponse(categorie);
		String[] header = new String[nbMaxRep+4];
		header[0] = "Question"; header[1] = "Carte"; header[2] = "NombreDeRéponses"; header[3] = "NombreDeBonneRéponses";
		
		for (int i = 1; i <= nbMaxRep; i++) {
		    header[i+3] = "reponse"+i;
		}
		return header;
    }
    
    void enregistrerQuestions(Categorie categorie) {
    	String[][] contenu = new String[length(categorie.questions)+1][4+nombreMaximalReponse(categorie)];
    	contenu[0] = faireEnTeteCSV(categorie);
    	Question q;
    	
    	for (int i = 0; i < length(categorie.questions); i++) {
    		q = categorie.questions[i];
    		contenu[i+1][0] = q.question;
    		contenu[i+1][1] = q.cheminCarte;
    		contenu[i+1][2] = ""+length(q.reponses);
			contenu[i+1][3] = ""+q.nbBonneRep;
    		for (int j = 0; j < nombreMaximalReponse(categorie); j++) {
    			// si le nombre de réponses de la question et inférieur à celui de la catégorie en général
    			if (j < length(q.reponses)) {    				
					contenu[i+1][j+4] = q.reponses[j].texte;
    			} else {
    				contenu[i+1][j+4] = "vide";
    			}
    		}
    	}
    	categorie.chemin = String.format("questions/Question%s.csv", categorie.nom);
    	saveCSV(contenu, "../ressources/" + categorie.chemin, ';');
    }
    
    
    void enregistrerProfils(Profil[] profils) {
		// nombre de profils + 1 (en-tête), par le nombre de propriétés
		String[][] contenu = new String[length(profils)+1][6];
		contenu[0] = new String[]{"Nom", "Droits", "Score", "NbPartiesJouees","NbPartiesGagnees", "Mail1", "Mail2", "Mdp1", "Mdp2"};
		Profil p;
		
		for (int i = 0; i < length(profils); i++) {
		    p = profils[i];
		    long[] mail = bytesToLongs(p.mail);
		    long[] mdp = bytesToLongs(p.mdp);
		    contenu[i+1] = new String[]{p.nom, ""+p.droits, ""+p.score, ""+p.nbPartiesJouees, ""+p.nbPartiesGagnees,""+mail[0], ""+mail[1], ""+mdp[0], ""+mdp[1]};
		}

		saveCSV(contenu, "../ressources/profils.csv", ';');
    }

    Categorie[] chargerCategories() {
		CSVFile csv = chargerRessourceCSV("categorie-base.csv");
		final int LIMIT = rowCount(csv);
		final int COL_NOM = 0, COL_CHEMIN = 1; 
		int idx = 0;

		Categorie[] categories = new Categorie[LIMIT - 1];
		Categorie c;
		for (int i = 1; i < LIMIT; i -=- 1) {
		    c = new Categorie();
		    c.nom = getCell(csv, i, COL_NOM);
		    c.chemin = getCell(csv, i, COL_CHEMIN);
		    c.questions = chargerQuestions(c.chemin, c.nom);
		    categories[idx++] = c;
		}
		return categories;
    }

    Question[] chargerQuestions(String chemin, String matiere) {
	CSVFile csv = chargerRessourceCSV(chemin);
	final int LIMIT = rowCount(csv);
	final int COL_TEXTE = 0, COL_CARTE = 1, COL_NB_REP = 2, COL_NB_VREP = 3, COL_REPS = 4;
	int idx = 0, jdx;
	
	Question[] questions = new Question[LIMIT -1];
	Question q;
	Reponse r;
	for (int i = 1; i < LIMIT; i++) {
	    q = new Question();
	    q.matiere = matiere;
	    q.cheminCarte = getCell(csv, i, COL_CARTE);
	    q.carte = chargerRessource(q.cheminCarte);
	    q.question = getCell(csv,i, COL_TEXTE);
	    int nbReps = stringToInt(getCell(csv, i, COL_NB_REP)), nbVRep = stringToInt(getCell(csv,i, COL_NB_VREP));
	    q.reponses = new Reponse[nbReps];
	    int nbBonneRep = nbVRep;
	    q.nbBonneRep = nbVRep;
	    jdx = 0;
	    for (int j = COL_REPS; j < nbReps+COL_REPS; j++) {
		r = new Reponse();
		r.texte = getCell(csv, i,j);
		r.valide = nbVRep-- > 0;
		q.reponses[jdx++] = r;
	    }
	    questions[idx++] = q;
	}
	return questions;
    }

    Question[] chargerQuestions(Categorie[] categories) {
	int size = 0, idx = 0;
	for (int i = 0; i < length(categories); i ++) {
	    size += length(categories[i].questions);
	}
	Question[] questions = new Question[size];
	for (int i = 0; i < length(categories); i++) {
	    for (int j = 0; j < length(categories[i].questions); j++) {
		questions[idx++] = categories[i].questions[j];
	    }
	}
	return questions;
    }
    
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	GESTIONS DES CATÉGORIES

    
    
    Categorie[] retirerCategorie(String nom, Categorie[] ca) {
	if (trouverCategorie(nom, ca) != null) {
	    Categorie[] nca = new Categorie[length(ca) - 1];	
	    int idx = 0;
	    int jdx = 0;
	    while (idx < length(ca) && ca[idx] != null) {
		if (!equals(ca[idx].nom, nom)) {
		    nca[jdx++] = ca[idx++];
		}
	    }
	}
	return ca;
    }
	
	// Pour les fonctions trouver... se reporter à trouverProfil();
	
	Categorie trouverCategorie(String nom, Categorie[] ca) {
		int idx = 0;
		while (idx < length(ca) && ca[idx] != null) {
	    	if (equals(ca[idx].nom, nom)) {
				return ca[idx];
	    	}
	    	idx++;
		}
		return null;
    }
    
    Categorie trouverCategorie(Categorie c, Categorie[] ca) {
		int idx = 0;
		while (idx < length(ca) && ca[idx] != null && c !=null) {
	    	if (equals(ca[idx], c)) {
				return ca[idx];
	    	}
	    	idx++;
		}
		return null;
    }
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	GESTIONS DES QUESTIONS
	
	// Pour les fonctions trouver... se reporter à trouverProfil();
	
	Question trouverQuestion(String nom, Question[] ca) {
		int idx = 0;
		while (idx < length(ca) && ca[idx] != null) {
	    	if (equals(ca[idx].question, nom)) {
				return ca[idx];
	    	}
	    	idx++;
		}
		return null;
    }
    
    Question trouverQuestion(Question q, Question[] qa) {
    	int idx = 0;
    	
		while (idx < length(qa) && qa[idx] != null && q != null) {
	    	if (equals(qa[idx], q)) {
				return qa[idx];
	    	}
	    	idx++;
		}
		return null;
    }
		
    Reponse[] melanger(Reponse[] reponses) {
		Reponse[] reponsesMelangees = new Reponse[length(reponses)];
		int pos = 0, random;
		
		while (pos < length(reponses)) {
		    random = (int) (random() * length(reponses));
		    if (!reponses[random].melangee) {
				reponsesMelangees[pos++] = reponses[random];
				reponses[random].melangee = true;
		    }
		}
		return reponsesMelangees;
    }
	
    void afficherQuestion(Question q, Reponse[] reponses, int niveau) {
		clearScreen();
		cursor(0,0);
		afficherASCIIArt(q.matiere);
		imprimerFichier(q.carte);
		delay((1 + (1 * (3-niveau)))*STOMS);
		clearScreen();
		println("Question: \""+q.question+"\"");
		
		for (int i = 0; i < length(reponses); i++) {
		    println((i+1) + ": " + reponses[i].texte); 
		}
    }
    
    void afficherJoueur(String j, int tour) {
    	clearScreen();
    	cursor(0,0);
    	afficherASCIIArt("Tour: " + tour);
    	print("\n\n");
    	afficherASCIIArt("" + j + " joue");
    	delay(3*STOMS);
    }

    boolean poserQuestion(Question[] questions, String joueur, int tour, int niveau) {
		int aleaQuestion;
		Question q;
		
		do {
		    aleaQuestion = (int) (random() * length(questions));
		    q = questions[aleaQuestion];
		} while (q.posee);
	
		Reponse[] reponsesMelangees = melanger(q.reponses);
		afficherJoueur(joueur, tour);
		afficherQuestion(q, reponsesMelangees, niveau);
		
		print("Entrez votre réponse: ");
		int choix;
		do {
		    choix = readInteger();
		    if (choix <= 0 || choix > length(reponsesMelangees)) {
			print("Choix invalide, veuillez entrer à nouveau votre réponse : ");
		    }
		} while(choix <= 0 || choix > length(reponsesMelangees));
		
		return reponsesMelangees[choix-1].valide;
    }
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	GESTIONS DES PROFILS

    void afficherProfils(Profil[] profils, String prefix) {
		for (int i = 0; i < length(profils); i ++) {
	    	println(prefix+"J"+(i+1)+": " + profils[i].nom + ", score: " + profils[i].score );
		}
    }
	
	void testTrouverProfil() {
		Profil p1 = newProfil("A"), p2 = newProfil("B"), p3 = newProfil("C");
		Profil[] pa = new Profil[]{p1, p2};
		
		assertEquals(p1, trouverProfil("A", pa));
		assertEquals(p2, trouverProfil(p2, pa));
		
		assertNotEquals(p3, trouverProfil("C", pa));
		assertNotEquals(p3, trouverProfil(p3, pa));
	}
	
    Profil trouverProfil(String nom, Profil[] profils) {
		int idx = 0;
		while (idx < length(profils) && profils[idx] != null) {
	    	if (equals(profils[idx].nom, nom)) {
				return profils[idx];
	    	}
	    	idx++;
		}
		return null;
    }
    
     Profil trouverProfil(Profil p, Profil[] profils) {
		int idx = 0;
		while (idx < length(profils) && profils[idx] != null && p != null) {
	    	if (equals(profils[idx], p)) {
				return profils[idx];
	    	}
	    	idx++;
		}
		return null;
    }

    String generateurMdp() {
		String s = "";
		while (length(s) < 15) {
	    	s += (char) (random() * 94 + 33);
		}
		return s;
    }
    
    boolean reinitialiserMdp(Profil profil) {
		if (equals(profil.mail, longsToBytes(0L, 0L))) {
	    	println("Vous n'avez pas enregistré d'adresse mail !");
	    	return false;
		}
	
		print("Veuillez saisir votre adresse mail afin de lancer la réinitialisation : ");
		String mail;
		boolean valide;
		do {
		    mail = readString();
		    valide = equals(profil.mail, md5(strToBytes(mail)));
		    if (!valide) {
				print("Adresse mail incorrecte, veuillez la resaisir: ");
		    } else {
				String mdp = generateurMdp();
				profil.mdp = md5(strToBytes(profil.nom + mdp));
				String body = "<h1>Réinitialisation du mot de passe MemoQuizZz</h1>\n";
				body+="<p>Bonjour "+profil.nom+", merci de jouer à MemoQuizZz. Le mot de passe a été réinitialisé le voici : </p>\n";
				body+="<p style=\"font-size=15px\">"+mdp+"</p>\n";
				envoyerMail(mail, "Réinitialisation du mot de passe", body, "Email de réinitialisation envoyé !", "Une erreur est survenue lors de l'envoi de l'email...");
		    }
		} while (!valide);
	
    	return valide;
    }
    
    boolean demandeMdp(Profil p) {
    	boolean valide;
    	String entree;
    	do {
			entree = readString();
			valide = ((estVide(p.mdp) && equals(entree, "")) || equals(md5(strToBytes(p.nom + entree)), p.mdp));
			if (!valide) {
				println("Mot de passe invalide");
			}
		} while (!valide);
    	return true;
    }
	
    boolean demandeMdp(Profil profil, String message) {
		String entree;
		int tentatives = 0;
		boolean valide;
		print(message);
		do {
		    entree = readString();
		    valide =  ( (estVide(profil.mdp) && equals(entree, "")) || equals(md5(strToBytes(profil.nom + entree)), profil.mdp) );
		    
		    if (!valide) {
				if (tentatives < 2) {
				    print("Mauvais mot de passe ! Veuillez le resaisir : ");
				    tentatives++;
				} else {
				    if (boolChoix('y', 'n', "Voulez-vous réinitialiser votre mot de passe ? (y|n)")) {
						if (MODE_MAIL) {
							if (reinitialiserMdp(profil)) {
						    	valide = demandeMdp(profil, "Entrez votre nouveau mot de passe : ");
							} else {
						    	println("Une erreur est survenue lors de la réinitialisation de votre mot de passe");
							}
						} else {
							println("Pour des raisons techniques nous sommes dans l'incapacité de pouvoir vous fournir un service de réinitialisation de mot de passe");
						}
				    }
				}
	    	}
		} while(!valide);
		return valide;
    }
    
    Categorie rechercheParNom(String nom, Categorie[] categories) {
    	int idx = 0;
		Categorie c = trouverCategorie(nom, categories);
		
		while (c == null && idx < length(categories) && categories[idx] != null) {
		    double probabilite = jaroWinklerDistance(nom, categories[idx].nom, min(length(nom), 4) );
		    if (probabilite >= 0.9 && boolChoix('y', 'n', "Est-ce la catégorie que vous recherchez ? " + categories[idx].nom + " (y|n)")) {
				c = categories[idx];
		    }
		    idx++;
		}
		return c;
    }
	
    Profil rechercheParPseudo(String nom, Profil[] profils) {
		int idx = 0;
		Profil profil = trouverProfil(nom, profils);
		
		while (profil == null && idx < length(profils) && profils[idx] != null) {
		    double probabilite = jaroWinklerDistance(nom, profils[idx].nom, length(nom)%4);
		    if (probabilite >= 0.9 && boolChoix('y', 'n', "Est-ce votre pseudo ? " + profils[idx].nom + " (y|n)")) {
				profil = profils[idx];
		    }
		    idx++;
		}
		return profil;
    }
	
    Profil authentification(ListeProfils lp) {
		println("*** Authentification ***");
		String entree;
		Profil profil = null;
		
		// Recherche du pseudo
		do {
		    print("Entrez votre pseudo : ");
		    entree = readString();
		    profil = rechercheParPseudo(entree, lp.profils);
		    if (profil == null && boolChoix('y', 'n', "Il semble que votre pseudo n'est pas enregistré, souhaitez-vous créer un nouveau profil ? (y|n)")) {
				profil = enregistrerNouveauProfil(entree, lp.profils);
				ajouter(lp, profil);
		    }
		} while (profil == null);
		
		// Demande du mot de passe
		if (demandeMdp(profil, "Entrez votre mot de passe : ")) {
		    println("*** Authentification réussie ***");
		} else {
		    println("*** Authentification échouée ***");
		    profil = null;
		}
		return profil;
    }
	
    Profil enregistrerNouveauProfil(String nom, Profil[] profils) {
		Profil profil = newProfil(nom);
		if (MODE_MAIL && boolChoix('y', 'n', "Désirez-vous enregistrer une adresse mail afin de pouvoir réinitialiser votre mot de passe plus tard ?")) {
	    	byte[] mail;
	    	boolean valide;
	    	do {
				mail = md5(strToBytes(saisirMail()));
				valide = !mailDejaEntre(mail, profils);
				if (!valide) {
				    println("Cette adresse mail a déjà été saisie");
				}
	    	} while(!valide);
	    	profil.mail = mail;
		} else {
	    	profil.mail = longsToBytes(0L, 0L);
		}	
	
		print("Veuillez ajouter un mot de passe pour finaliser la création de votre profil : ");
		profil.mdp = md5(strToBytes(nom + readString()));
				
		if (demandeMdp(profil, "Veuillez le donner à nouveau : ")) {
		    println("Profil créé !");
		    profil.nouveau = true;
		} else {
		    println("Échec de la création du profil");
		    profil = null;
		}
		return profil;
    }
	
    Profil entreeUtilisateur(ListeProfils lp, int indice) {
		Profil profil = null;
		println("Authentification du joueur n°" + (indice+1));
		do {
	    	profil = authentification(lp);
	    	// échec de l'authentification
	    	if (profil == null || !aDroit(profil, DROIT_JOUER)) {
				println("L'authentification du joueur n°" + (indice+1) + " a échoué");
	    	} else {
				if (profil.nouveau) {
				    println("Bienvenue parmi nous " + profil.nom + " !");					
				} else {
				    println("Bon retour parmi nous " + profil.nom + " !");
				}
	    	}
		} while (profil == null || !aDroit(profil, DROIT_JOUER));
		return profil;
    }

    Profil[] entreeUtilisateurs(int quantite, ListeProfils lp) {
		Profil[] profils = new Profil[quantite];
		for (int i = 0; i < quantite; i -=- 1) {
		    profils[i] = entreeUtilisateur(lp, i);
		}
		return profils;
    }
	
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	PSEUDO-CONSTRUCTEURS

    Profil newProfil(String nom) {
		return newProfil(nom, DROIT_JOUER);
    }
	
    Profil newProfil(String nom, int droits) {
		return newProfil(nom, droits, 0);
    }

	Profil newProfil(String nom, int droits, int score) {
		Profil j = new Profil();
		j.nom = nom;
		j.score = score;
		j.droits = droits;
		return j;
	}

    Partie newPartie(Profil[] profils, Categorie[] categories, Question[] questions, int difficulte) {
		Partie p = new Partie();
		p.profils = profils;
		p.categories = categories;
		p.questions = questions;
		p.difficulte = difficulte;
		return p;
    }
    
    Categorie newCategorie(String nom) {
    	return newCategorie(nom, null, null);
    }

    Categorie newCategorie(String nom, String chemin, Question[] questions) {
		Categorie c = new Categorie();
		c.nom = nom;
		c.chemin = chemin;
		c.questions = questions;
		return c;
    }
    
    Question newQuestion(String texte) {
    	return newQuestion("", null, texte, null);
    }

    Question newQuestion(String categorie, File carte, String question, Reponse[] reponses) {
		Question q = new Question();
		q.matiere = categorie;
		q.carte = carte;
		q.question = question;
		q.reponses = reponses;
		return q;
    }

    Reponse newReponse(String texte, boolean valide) {
		Reponse r = new Reponse();
		r.texte = texte;
		r.valide = valide;
		return r;
    }
    
    QuicksortElement newElement(int indiceDansTableau, int valeur) {
    	QuicksortElement e = new QuicksortElement();
    	e.indiceDansTableau = indiceDansTableau;
    	e.valeur = valeur;
    	return e;
    }
       
    ListeProfils newListeProfils() {
    	ListeProfils lp = new ListeProfils();
    	return lp;
    }
    
    ListeProfils newListeProfils(Profil[] pa) {
    	ListeProfils lp = new ListeProfils();
    	lp.profils = pa;
    	lp.taille = length(pa);
    	lp.indice = lp.taille - 1;
    	return lp;
    }
       
    ListeQuestions newListeQuestions() {
    	ListeQuestions lq = new ListeQuestions();
    	return lq;
    }

	ListeQuestions newListeQuestions(Question[] qa) {
    	ListeQuestions lq = new ListeQuestions();
    	lq.questions = qa;
    	lq.taille = length(qa);
    	lq.indice = lq.taille - 1;
    	return lq;
    }
    
    ListeCategories newListeCategories() {
    	ListeCategories lc = new ListeCategories();
    	return lc;
    }
    
    ListeCategories newListeCategories(Categorie[] ca) {
    	ListeCategories lc = new ListeCategories();
    	lc.categories = ca;
    	lc.taille = length(ca);
    	lc.indice = lc.taille - 1;
    	return lc;
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //  FONCTIONS À PROPOS DES MAILS

    String saisirMail() {
		String pattern = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\.[A-Za-z0-9_-]+)*@[^-][A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*(\\.[A-Za-z]{2,})$", entree;
		Pattern p = Pattern.compile(pattern);
		boolean valide;
		print("Veuillez entrer votre adresse mail : ");
		do {
		    entree = readString();
		    valide = (p.matcher(entree).matches());
		    if (!valide) {
				print("Adresse mail incompréhensible, veuillez la resaisir : ");
		    }
		} while(!valide);
		return entree;
    }
    
    void envoyerMail(String destination, String objet, String mail, String trace, String erreur) {
    	Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable","true");
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
		props.put("mail.protocol.socks.host", "cache.univ-lille.fr");
		props.put("mail.protocol.socks.port", "3128");

	println("Instanciation de la session");
        Session session = Session.getInstance(props, new Authenticator() {
		@Override
		protected PasswordAuthentication getPasswordAuthentication() {
		    return new PasswordAuthentication("memoquizzz@gmail.com", "gchicwvhuiktzpfr");
		}
	    });
	println("Création du message");
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("memoquizzz@gmail.com"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destination));
            message.setSubject(objet);
            message.setContent(mail, "text/html");
	    println("Message:\n\tTo: "+destination+"\n\tObjet: "+objet+"\n\tCorps: "+mail);
	    println("Message créé, à envoyer");
	    Transport.send(message);
            System.out.println(trace);
        } catch (MessagingException e) {
	    System.out.println(erreur);
            throw new RuntimeException(e);
        }
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	FONCTIONS À PROPOS DES LISTES
    
    // Les tests effectués sur les listes ne se font que pour les questions (les implémentations pour les profils et les catégories sont sensiblement identiques)
    
   	void testAjouter() {
   		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C");
   	
   		ListeQuestions lq = newListeQuestions(new Question[]{q1, q2});
   		
   		assertEquals(1, lq.indice);
   		assertEquals(2, lq.taille);
   		
   		ajouter(lq, q3);	
   		
   		assertEquals(2, lq.indice);
   		assertEquals(3, lq.taille);
   		assertArrayEquals(new Question[]{q1, q2, q3}, lq.questions);
   	}
    
  	void ajouter(ListeQuestions lq, Question q) {
  		if (++lq.indice == lq.taille) {
  			Question[] tmp = lq.questions;
  			lq.questions = new Question[++lq.taille];
  			for (int i = 0; i < length(tmp); i++) {
  				lq.questions[i] = tmp[i];
  			}
  		}
  		lq.questions[lq.indice] = q;
  	}
  	
  	void testRetirer() {
   		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C");
   	
   		ListeQuestions lq = newListeQuestions(new Question[]{q1, q2, q3});
   		
   		assertEquals(2, lq.indice);
   		assertEquals(3, lq.taille);
   		
   		retirer(lq, q3);	
   		
   		assertEquals(1, lq.indice);
   		assertEquals(2, lq.taille);
   		assertArrayEquals(new Question[]{q1, q2}, lq.questions);
   		
   		retirer(lq, q1);	
   		
   		assertEquals(0, lq.indice);
   		assertEquals(1, lq.taille);
   		assertArrayEquals(new Question[]{q2}, lq.questions);
   		
   	}
  	
  	void retirer(ListeQuestions lq, Question q) {
  		Question[] tmp = lq.questions;
  		
  		if (contient(lq, q)) {
  			int idx = -1;
  			while (idx < lq.indice && !equals(lq.questions[++idx], q));
  			
  			if (equals(lq.questions[idx], q)) {
  				if (idx < lq.indice) {
  					lq.questions[idx] = lq.questions[lq.indice];
  				}
  				
  				lq.questions = new Question[--lq.taille];
  				
  				for (int i = 0; i < lq.taille; i++) {
  					lq.questions[i] = tmp[i];
  				}
  				
  				lq.indice--;
  			} 
  		}
  	}
  	
  	void testContient() {
  		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C");
   	
   		ListeQuestions lq = newListeQuestions(new Question[]{q1, q2, q3});
   		ListeQuestions lq1 = newListeQuestions(new Question[]{q1, q2});   		
   		ListeQuestions lq2 = newListeQuestions(new Question[0]);
   		
   		assertTrue(contient(lq, q1));
   		assertFalse(contient(lq2, q2));
   		assertFalse(contient(lq1, q3));
  	}
  	
  	boolean contient(ListeQuestions lq, Question q) {
  		return trouverQuestion(q, lq.questions) != null;
  	}
  	
  	void testMajListe() {
  		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C");
  		
  		ListeQuestions lq = newListeQuestions(new Question[]{q1, q2, q3});
  		
  		q1.question = "Z";
  		
  		majListe(lq, q1);
  		
  		assertArrayEquals(new Question[]{q3, q2, q1}, lq.questions);
  		assertEquals("Z", lq.questions[2].question);
  		assertTrue(contient(lq, q1));
  	}
   	
  	void majListe(ListeQuestions lq, Question[] qa) {
  		if (qa == null) return;
  		for (int i = 0; i < length(qa); i++) {
  			majListe(lq, qa[i]);
  		}
    }
  	
  	void majListe(ListeQuestions lq, Question q) {
  		if (contient(lq, q)) {
  			retirer(lq, q);
  			ajouter(lq, q); 
  		} else {
  			ajouter(lq, q);
  		}
  	}
  	
  	void testIndexOf() {
  		Question q1 = newQuestion("A"), q2 = newQuestion("B"), q3 = newQuestion("C"), q4 = newQuestion("D");
  		ListeQuestions lq = newListeQuestions(new Question[]{q1, q2, q3});
  		
  		assertEquals(0, indexOf(lq, q1));
  		assertEquals(1, indexOf(lq, q2));
  		assertEquals(-1, indexOf(lq, q4));
  		assertNotEquals(0, indexOf(lq, q3));  		
  	}
  	
  	int indexOf(ListeQuestions lq, Question q) {
  		if (!contient(lq, q)) return -1;
  		
  		int idx = -1;
  		while (idx < lq.indice && !equals(lq.questions[++idx], q));
  		return idx;
  	}

  	void ajouter(ListeProfils lp, Profil p) {
  		if (++lp.indice == lp.taille) {
  			Profil[] tmp = lp.profils;
  			lp.profils = new Profil[++lp.taille];
  			for (int i = 0; i < length(tmp); i++) {
  				lp.profils[i] = tmp[i];
  			}
  		}
  		lp.profils[lp.indice] = p;
  	}
  	
  	void retirer(ListeProfils lp, Profil p) {
  		Profil[] tmp = lp.profils;
  		
  		if (contient(lp, p)) {
  			int idx = -1;
  			while (idx < lp.indice && !equals(lp.profils[++idx], p));
  			
  			if (equals(lp.profils[idx], p)) {
  				if (idx < lp.indice) {
  					lp.profils[idx] = lp.profils[lp.indice];
  				}
  				
  				lp.profils = new Profil[--lp.taille];
  				
  				for (int i = 0; i < lp.taille; i++) {
  					lp.profils[i] = tmp[i];
  				}
  				
  				lp.indice--;
  			} 
  		}
  	}
  	
  	boolean contient(ListeProfils lp, Profil p) {
  		return trouverProfil(p, lp.profils) != null;
  	}
  	
  	void majListe(ListeProfils lp, Profil[] pa) {
  		if (pa == null) return;
  		for (int i = 0; i < length(pa); i++) {
  			majListe(lp, pa[i]);
  		}
    }
  	
  	void majListe(ListeProfils lp, Profil p) {
  		if (contient(lp, p)) {
			retirer(lp, p);
			ajouter(lp, p); 
  		} else {
  			ajouter(lp, p);
  		}
  	}
  	  	
  	void ajouter(ListeCategories lc, Categorie c) {
  		if (++lc.indice == lc.taille) {
  			Categorie[] tmp = lc.categories;
  			lc.categories = new Categorie[++lc.taille];
  			for (int i = 0; i < length(tmp); i++) {
  				lc.categories[i] = tmp[i];
  			}
  		}
  		lc.categories[lc.indice] = c;
  	}
  	
  	void retirer(ListeCategories lc, Categorie c) {
  		Categorie[] tmp = lc.categories;
  		
  		if (contient(lc, c)) {
  			int idx = -1;
  			while (idx < lc.indice && !equals(lc.categories[++idx], c));
  			
  			if (equals(lc.categories[idx], c)) {
  				if (idx < lc.indice) {
  					lc.categories[idx] = lc.categories[lc.indice];
  				}
  				
  				lc.categories = new Categorie[--lc.taille];
  				
  				for (int i = 0; i < lc.taille; i++) {
  					lc.categories[i] = tmp[i];
  				}
  				
  				lc.indice--;
  			} 
  		}
  	}
  	
  	boolean contient(ListeCategories lc, Categorie c) {
  		return trouverCategorie(c, lc.categories) != null;
  	}
  	
  	void majListe(ListeCategories lc, Categorie[] ca) {
  		if (ca == null) return;
  		for (int i = 0; i < length(ca); i++) {
  			majListe(lc, ca[i]);
  		}
    }
  	
  	void majListe(ListeCategories lc, Categorie c) {
  		if (contient(lc, c)) {
  				retirer(lc, c);
  				ajouter(lc, c); 
  		} else {
  			ajouter(lc, c);
  		}
  	} 

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //	FONCTIONS UTILES
    
    void testEstNumerique() {
    	String s = "2022", s1 = "2O22";
    	char c = '1', c1 = 'P';
    	
    	assertTrue(estNumerique(c));
    	assertFalse(estNumerique(c1));
    	
    	assertTrue(estNumerique(s));
    	assertFalse(estNumerique(s1));
    }
    
    boolean estNumerique(char c) {
    	return (c >= '0' && c <= '9');
    }
        
   	boolean estNumerique(String s) {
   		boolean valide;
   		int idx = 0;
   		while (idx < length(s) && estNumerique(charAt(s, idx++)));
   		return (estNumerique(charAt(s, idx-1)));
   	}
    
    void testStrToInt() {
   		String s = "2022", s1 = "2O22";
   		
   		assertEquals(2022, strToInt(s));
   		assertEquals(INT_MIN, strToInt(s1));
    }
    
    int strToInt(String s) {
    	int entier = 0;
    	if (estNumerique(s)) {
    		for (int i = length(s) - 1; i >= 0; i--) {
    			int val = charAt(s, i) - '0';
    			entier += val * pow(10, length(s) - i - 1);
    		}
    	} else {
    		entier = INT_MIN;
    	}
    	return entier;
    }
    
    // Le but est de protéger l'entrée utilisateur puisque l'entrée d'entiers est moins permissive que pour les chaînes de caractères
    int readInteger() {
    	String entree = readString();
    	
    	if (estNumerique(entree)) {
    		return strToInt(entree);
    	}
    	println("Veuillez renseigner un entier valide !");
    	return INT_MIN; 	
    }
    
    double ratio(Profil p) {
    	int jouees = p.nbPartiesJouees, gagnees = p.nbPartiesGagnees;
    	int perdues = jouees-gagnees;
    	if (jouees == 0) return 0.0;
    	return (gagnees / (double) perdues);
    }
    
    boolean cheminCarteValide(String c) {
    	String[] files = getAllFilesFromDirectory("cartes");
    	int idx = 0;
    	    	
    	while (idx < length(files) && !equals(files[idx++], c) );
    	
    	return equals(files[idx-1], c);
    }
    
    Reponse[] trierReponsesParValidite(Reponse[] reponses) {
    	Reponse[] reponsesTriees = new Reponse[length(reponses)];
    	QuicksortElement[] elements = new QuicksortElement[length(reponses)];
    	for (int i = 0; i < length(reponses); i++) {
    		elements[i] = newElement(i, ((reponses[i].valide) ? 1 : 0));
    	}
    	quicksort(elements, 0, length(reponses)-1);
    	for (int i = 0; i < length(reponses); i++) {
    		reponsesTriees[i] = reponses[elements[length(reponses) - i - 1].indiceDansTableau];
    	}    	
    	return reponsesTriees;    
    }

    void testTrierScores() {
    	Profil p1 = newProfil("A", 0, 1), p2 = newProfil("B", 0, 10), p3 = newProfil("C", 0, 5), p4 = newProfil("D", 0, 40), p5 = newProfil("E", 0, 0), p6 = newProfil("F", 0, -10);
    	Profil[] pa = new Profil[]{p1, p2, p3, p4};
    	Profil[] pa1 = new Profil[]{p1, p2, p3, p4, p5};
    	Profil[] pa2 = new Profil[]{p2, p4, p6, p1, p3, p5};
    	
    	assertEquals(4, length(trierScores(pa, 4, true)));
    	assertEquals(4, length(trierScores(pa1, 4, true)));
    	assertEquals(4, length(trierScores(pa2, 4, true)));

    	assertArrayEquals(new Profil[]{p4, p2, p3, p1}, trierScores(pa, 4, true));
    	assertArrayEquals(new Profil[]{p4, p2, p3, p1}, trierScores(pa1, 4, true));
    	assertArrayEquals(new Profil[]{p4, p2, p3, p1}, trierScores(pa2, 4, true));
    }
    
    Profil[] trierScores(Profil[] profils, int limit, boolean general) {
    	final int LIMIT = min(length(profils), limit);
		Profil[] profilsTries = new Profil[LIMIT];
    	QuicksortElement[] elements = new QuicksortElement[length(profils)];
    	for (int i = 0; i < length(profils); i++) {
	    	elements[i] = newElement(i, ((general) ? profils[i].score : profils[i].scorePartie));
    	}
		quicksort(elements, 0, length(elements)-1);
		for (int i = 0; i < LIMIT; i++) {
	    	profilsTries[i] = profils[elements[LIMIT - i + (length(profils) - (LIMIT+1))].indiceDansTableau]; //LIMIT - i + (length(profils) - (LIMIT+1) )
    	}
		return profilsTries;
    }

    int partitionner(QuicksortElement[] elements, int premier, int dernier) {
		int pivot = elements[dernier].valeur, i = premier - 1;
		QuicksortElement tmp;
		
		for (int j = premier; j < dernier; j++) {
		    if (elements[j].valeur < pivot) {
				i++;
				tmp = elements[i];
				elements[i] = elements[j];
				elements[j] = tmp;
		    }
		}
		tmp = elements[++i];
		elements[i] = elements[dernier];
		elements[dernier] = tmp;
		return i;
    }
    
    void quicksort(QuicksortElement[] elements, int premier, int dernier) {
		if (premier < dernier) {
	    	int pivot = partitionner(elements, premier, dernier);
	    	quicksort(elements, premier, pivot-1);
	    	quicksort(elements, pivot+1, dernier);
		}
    }
    
    void testLongToBytes() {
    	long l = 1;
    	byte[] ba = new byte[8]; ba[7] = 0x1;
    	
    	assertArrayEquals(ba, longToBytes(l));
    	
    	l=255;
    	ba = new byte[8]; ba[7]= (byte) 0xff;

	   	assertArrayEquals(ba, longToBytes(l));

		l=65535;
		ba = new byte[8]; ba[6]=(byte) 0xff; ba[7]=(byte) 0xff;

    }
    
    byte[] longToBytes(long value) {
		return new byte[] {(byte)(value>>>56), (byte)(value >>> 48), (byte)(value >>> 40), (byte)(value >>> 32), (byte)(value>>>24), (byte)(value>>>16), (byte)(value>>>8), (byte) value};
    }
    
    void testLongsToBytes() {
    	long l1 = 255, l2 = 255;
    	byte[] ba = new byte[16]; ba[7] = (byte) 0xff; ba[15] = (byte) 0xff;
    	
    	assertArrayEquals(ba, longsToBytes(l1, l2)); 
    }
    
    byte[] longsToBytes(long l1, long l2) {
		byte[] r = new byte[16], tmp;
		int idx = 0;
	
		tmp = longToBytes(l1);
		for (int i = 0; i < length(tmp); i++) {
		    r[idx++] = tmp[i];
		}

		tmp = longToBytes(l2);
		for (int i = 0; i < length(tmp); i++) {
		    r[idx++] = tmp[i];
		}
		return r;
    }
    
    void testBytesToLongs() {
    	byte[] ba = new byte[16]; ba[7] = (byte) 0xff; ba[15] = (byte) 0xff;
    	long[] la = new long[]{255, 255};
    	
    	assertArrayEquals(la, bytesToLongs(ba));    
    }
    
    long[] bytesToLongs(byte[] ba) {
		long[] r = new long[length(ba)/8];
		long l = 0L;
		int idx = 0;
		for (int i = 0; i < length(ba); i++) {
		    if (i != 0 && i % 8 == 0) {
				r[idx++] = l;
				l = (ba[i] & 0xff);
		    } else {
				l <<= 8;
				l += (ba[i] & 0xff);
		    }
		}
		r[idx] = l;
		return r;
    }
    
    void testEquals() {
    	byte[] b1 = longToBytes(100);
    	byte[] b2 = longToBytes(999);
    	byte[] b3 = new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, (byte) 0xE7};
    	
    	assertTrue(equals(b2, b3));
    	assertFalse(equals(b1, b2));
    	assertFalse(equals(b1, b3));
    }

    boolean equals(byte[] b1, byte[] b2) {
		boolean equals = true;
		int idx = 0;
		if (length(b1) != length(b2)) return false;
		
		while (idx < length(b1) && equals) {
		    equals = (b1[idx]==b2[idx++]);
		}
		return equals;
    }
       
    boolean equals(Profil p1, Profil p2) {
    	boolean equal = false, valide = false;
    	
		if (p1.nom != null && p2.nom != null) {
    		equal = equals(p1.nom, p2.nom);
    		valide = true;
    	}
    	
    	if (p1.mdp != null && p2.mdp != null) {
    		equal = equals(p1.mdp, p2.mdp);	
    		valide = true;    	
    	}
    	   	
    	return valide && equal && p1.score == p2.score;
   	}
    
    boolean equals(Categorie c1, Categorie c2) {
    	return equals(c1.nom, c2.nom);
    }
   
    boolean equals(Question q1, Question q2) {
    	return equals(q1.question, q2.question);
    }
    
    String toString(byte[] ba, String charset) {
		String s = "";
		int idx = 0;

		if (equals(charset, "UTF-8")) {
		    while (idx < length(ba)) {
				int c = ba[idx++] & 0xff;
				if (c <= 0x7f) {
				    s += (char) c;
				} else if ((c & 0xe0) == 0xc0) {
				    int c2 = ba[idx++] & 0xff;
				    s += (char) ((c & 0x1f) << 6 | (c2 & 0x3f));
				} else if ((c & 0xf0) == 0xe0){
				    int c2 = ba[idx++] & 0xff, c3 = ba[idx++] & 0xff;
				    s+= (char) ((c & 0x0f) << 12 | (c2 & 0x3f) << 6 | (c3 & 0x3f));
				} else if ((c & 0xf8) == 0xf0) {
				    int c2 = ba[idx++] & 0xff, c3 = ba[idx++] & 0xff, c4 = ba[idx++] & 0xff;		
		    		int codepoint = (c & 0x07) << 18 | (c2 & 0x3f) << 12 | (c3 & 0x3f) << 6 | (c4 & 0x3f);
		    		int high = (codepoint - 0x010000) / 0x400 + 0xd800;
		    		int low = (codepoint - 0x010000) % 0x400 + 0xdc00;
		    		s+= (char) high + "" + (char) low; 
				}
	    	}
		} else if (equals(charset, "ISO-8859-2")) {
	    	while (idx < length(ba)) {
				char c = (char) ba[idx++];
				s += c;
	    	}
		}
		return s;
    }
    
    void testStrToBytes() {
    	String sbonjour = "Bonjour";
    	byte[] bbonjour = new byte[]{0x42, 0x6f, 0x6e, 0x6a, 0x6f, 0x75, 0x72};
    	
    	String saccents = "Helléno-judaïque";
    	byte[] baccents = new byte[]{0x48, 0x65, 0x6c, 0x6c, (byte) 0xc3, (byte) 0xa9, 0x6e, 0x6f, 0x2d, 0x6a, 0x75, 0x64, 0x61, (byte) 0xc3, (byte) 0xaf, 0x71, 0x75, 0x65};
    	
    	assertArrayEquals(bbonjour, strToBytes(sbonjour));
    	assertArrayEquals(baccents, strToBytes(saccents));
    }
	
    byte[] strToBytes(String s) {
		byte[] tmp = new byte[length(s) * 4], r;
		int pos = 0;

		if (length(s) == 0) {
		    return new byte[]{0x0};
		}
		
		for (int i = 0; i < length(s); i ++) {
		    long c = (long) charAt(s, i);
			
		    final boolean est4b = ( (c & 0xdc00) == 0xd800 || (c & 0xdc00) == 0xdc00);
			
		    if (c <= 0x7f && !est4b ) { // ASCII CHARS (0x0 -> 0x7f)
				tmp[pos++] = (byte) c;
		    } else if (c <= 0x7ff && !est4b ) { // 2 bytes UTF-8 (0x80 -> 0x7ff)
				tmp[pos++] = (byte) ((c >> 6) | 0xc0);
				tmp[pos++] = (byte) ((c & 0x3f) | 0x80);
		    } else if (c <= 0xffff && !est4b) { // 3 bytes UTF-8 (0x800 -> 0xffff)
				tmp[pos++] = (byte) ((c >> 12) | 0xe0);
				tmp[pos++] = (byte) (((c >> 6) & 0x3f) | 0x80);
				tmp[pos++] = (byte) ((c & 0x3f) | 0x80);
		    } else { // 4 bytes UTF-8 (0x10000 -> 0x1fffff)
				long codepoint = 0x010000 | (c & 0x03ff) << 10 | (((long) charAt(s, ++i)) & 0x03ff);
				tmp[pos++] = (byte) ((codepoint >> 18) | 0xf0);
				tmp[pos++] = (byte) (((codepoint >> 12) & 0x3f) | 0x80);
				tmp[pos++] = (byte) (((codepoint >> 6) & 0x3f) | 0x80);
				tmp[pos++] = (byte) ((codepoint & 0x3f) | 0x80);				
	    	}
		}	
		r = new byte[pos];
		for (int i = 0; i < pos; i ++) {
	    	r[i] = tmp[i];
		}	
		return r;
    }
		
    int hashCode(String str) {
		int hcode = 0;
		for (int i = 0; i < length(str); i++) {
		    hcode += charAt(str, i) * pow(31, length(str) - i - 1);
		}
		return hcode;
    }

	void testRotationEntier() {
		int a = 0b1, b = 0b10000000000000000000000000000001;
		
		assertEquals(0b10, rotateLeft(a, 1));
		assertEquals(0b100000, rotateLeft(a, 5));
		assertEquals(0b11, rotateLeft(b, 1));
		assertEquals(0b110000, rotateLeft(b, 5));
	}

    int rotateLeft(int x, int n) {
		return ((x << n) | (x >>> (32-n))); 
    }
    
    String toHex(byte[] ba) {
		String s = "";
		for (byte b: ba) {
		    s+=String.format("%02x", b);
		}
		return s;
    }

    boolean estVide(byte[] ba) {
		int idx = 0;
		boolean vide = true;

		while (idx < length(ba) && vide) {
	    	vide = (ba[idx++] == (byte) 0x0);
		}
		return vide;
    }
    
    void testMd5() {
    	// "Bonjour"
    	byte[] bin = new byte[] {(byte) 0x42, (byte) 0x6f, (byte) 0x6e, (byte) 0x6a, (byte) 0x6f, (byte) 0x75, (byte) 0x72};
    	
    	// Les valeurs proviennent de md5.fr
    	byte[] bout = new byte[] {(byte) 0xeb, (byte) 0xc5, (byte) 0x8a, (byte) 0xb2, (byte) 0xcb, (byte) 0x48, (byte) 0x48, (byte) 0xd0, (byte) 0x4e, (byte) 0xc2, (byte) 0x3d, (byte) 0x83, (byte) 0xf7, (byte) 0xdd, (byte) 0xf9, (byte) 0x85};
    	
    	assertArrayEquals(bout, md5(bin));
    }
    
    byte[] md5(byte[] message) {
		final int INIT_A = 0x67452301, INIT_B = (int)0xEFCDAB89L, INIT_C = (int)0x98BADCFEL, INIT_D = 0x10325476;
		final int[] SHIFT_AMTS = {
		    7, 12, 17, 22,
		    5,  9, 14, 20,
		    4, 11, 16, 23,
		    6, 10, 15, 21
		};
		final int[] TABLE_T = { 0xd76aa478, 0xe8c7b756, 0x242070db, 0xc1bdceee, 0xf57c0faf, 0x4787c62a, 0xa8304613, 0xfd469501,
		    0x698098d8, 0x8b44f7af, 0xffff5bb1, 0x895cd7be, 0x6b901122, 0xfd987193, 0xa679438e, 0x49b40821,
		    0xf61e2562, 0xc040b340, 0x265e5a51, 0xe9b6c7aa, 0xd62f105d, 0x02441453, 0xd8a1e681, 0xe7d3fbc8,
		    0x21e1cde6, 0xc33707d6, 0xf4d50d87, 0x455a14ed, 0xa9e3e905, 0xfcefa3f8, 0x676f02d9, 0x8d2a4c8a,
		    0xfffa3942, 0x8771f681, 0x6d9d6122, 0xfde5380c, 0xa4beea44, 0x4bdecfa9, 0xf6bb4b60, 0xbebfbc70,
		    0x289b7ec6, 0xeaa127fa, 0xd4ef3085, 0x04881d05, 0xd9d4d039, 0xe6db99e5, 0x1fa27cf8, 0xc4ac5665,
		    0xf4292244, 0x432aff97, 0xab9423a7, 0xfc93a039, 0x655b59c3, 0x8f0ccc92, 0xffeff47d, 0x85845dd1,
		    0x6fa87e4f, 0xfe2ce6e0, 0xa3014314, 0x4e0811a1, 0xf7537e82, 0xbd3af235, 0x2ad7d2bb, 0xeb86d391 };
		
		int messageLenBytes = length(message);
		int numBlocks = ((messageLenBytes + 8) >>> 6) + 1;
		int totalLen = numBlocks << 6;
		byte[] paddingBytes = new byte[totalLen - messageLenBytes];
		paddingBytes[0] = (byte)0x80;
	
		long messageLenBits = (long)messageLenBytes << 3;
		for (int i = 0; i < 8; i++) {
		    paddingBytes[length(paddingBytes) - 8 + i] = (byte)messageLenBits;
		    messageLenBits >>>= 8;
		}

		int a = INIT_A;
		int b = INIT_B;
		int c = INIT_C;
		int d = INIT_D;
		int[] buffer = new int[16];
		for (int i = 0; i < numBlocks; i ++) {
		    int index = i << 6;
		    for (int j = 0; j < 64; j++, index++) {
				buffer[j >>> 2] = ((int)((index < messageLenBytes) ? message[index] : paddingBytes[index - messageLenBytes]) << 24) | (buffer[j >>> 2] >>> 8);
		    }
		    int originalA = a;
		    int originalB = b;
		    int originalC = c;
		    int originalD = d;
		    for (int j = 0; j < 64; j++) {
				int div16 = j >>> 4;
				int f = 0;
				int bufferIndex = j;
				switch (div16) {
					case 0:
					    f = (b & c) | (~b & d);
					    break;
			
					case 1:
					    f = (b & d) | (c & ~d);
					    bufferIndex = (bufferIndex * 5 + 1) & 0x0F;
					    break;
			
					case 2:
					    f = b ^ c ^ d;
					    bufferIndex = (bufferIndex * 3 + 5) & 0x0F;
					    break;
			
					case 3:
					    f = c ^ (b | ~d);
					    bufferIndex = (bufferIndex * 7) & 0x0F;
					    break;
				}
				int temp = b + rotateLeft(a + f + buffer[bufferIndex] + TABLE_T[j], SHIFT_AMTS[(div16 << 2) | (j & 3)]);
				a = d;
				d = c;
				c = b;
				b = temp;
		    }
		  
		    a += originalA;
		    b += originalB;
		    c += originalC;
		    d += originalD;
		}

		byte[] md5 = new byte[16];
		int count = 0;
		for (int i = 0; i < 4; i++) {
		    int n = (i == 0) ? a : ((i == 1) ? b : ((i == 2) ? c : d));
		    for (int j = 0; j < 4; j++) {
				md5[count++] = (byte)n;
				n >>>= 8;
		    }
		}
		return md5;
    }
	
    boolean boolChoix(char v, char f, String question) {
	String entree;
	int choix;
	println(question);
	print("Votre choix: ");
	do {
	    entree = readString();
	    if (equals(entree, "1") || equals(entree, "" + v) || equals(entree, "")) {
		choix = 1;
	    } else if (equals(entree, "2") || equals(entree, "" +f)) {
		choix = 0;
	    } else {
		print("Entrée invalide ! Veuillez la resaisir : ");				
		choix = -1;
	    }
	} while (choix == -1);
	return (choix == 1) ? true : false;
    }
	
    // En cas de besoin les bornes maximales et minimales de l'Integer Java sont disponibles sous forme de constantes	
    int menuChoix(int min, int max, String question) {
	int entree;
	boolean valide = false;
	println(question);
	print("Votre choix: ");
	do {
	    entree = readInteger();
	    valide = ( entree >= min && entree <= max );
	    if (!valide) {
		print("Entrée invalide ! Veuillez la resaisir : ");
	    }
	} while(!valide);
	return entree;
    }
    
    void testDroits() {
    	Profil p1 = newProfil("A", 16), p2 = newProfil("B", 32);
    	
    	assertTrue(aDroit(p1, DROIT_RETIRER_CATEGORIE));
    	assertFalse(aDroit(p1, DROIT_ADMIN));
    	
    	assertTrue(aDroit(p2, DROIT_RETIRER_CATEGORIE));
    	assertTrue(aDroit(p2, DROIT_ADMIN));
    }

    boolean aDroit(Profil profil, int droit) {
		return ((profil.droits & droit) == droit || (profil.droits & DROIT_ADMIN) == DROIT_ADMIN);
    }

    void afficherASCIIArt(String text) {
		text = toLowerCase(text);
		File[] lettres = new File[length(text)];
		for (int i = 0; i < length(text); i++) {
		    lettres[i] = chargerRessource("/lettres/letter-" + charAt(text,i) + ".txt");
		}
	
		final int SIZE = 6;
		for (int i = 0; i < SIZE; i++) {
		    for (int j = 0; j < length(lettres); j++) {
				print(readLine(lettres[j]));
		    }
		   	println();
		}
    }

    void testDistancesChaines() {
		String s1 = "test", s2 = "t3st", s3 = "johnny";

		assertTrue(jaroDistance(s1, s2) > jaroDistance(s1, s3));
		assertTrue(jaroDistance(s1, s2) > jaroDistance(s2, s3));
    	assertTrue(jaroWinklerDistance(s1, s2, 2) > jaroWinklerDistance(s1, s3, 2));
    	assertTrue(jaroWinklerDistance(s1, s2, 2) > jaroWinklerDistance(s2, s3, 2));
    }
	
    double jaroDistance(String s1, String s2) {
		final double TIERS = 3.0;
		int len1 = length(s1), len2 = length(s2), maxDist = floor(max(len1,len2)/2)-1, match = 0, point = 0;
		int[] hash1 = new int[len1], hash2 = new int[len2];
		double t = 0.0;

		if (s1 == s2 || equals(s1, s2)) return 1.0;
			
		if (len1 == 0 || len2 == 0) return 0.0;
		
		for (int i = 0; i < len1; i++) {
		    int j = max(0, i - maxDist);
		    while (j < min(len2, i + maxDist + 1) && !(charAt(s1, i) == charAt(s2, j) && hash2[j] == 0) ) j++;
			if (j < min(len2, i + maxDist + 1) ) {
				hash1[i] = 1;
				hash2[j] = 1;
				match++;
			}
		}
		if (match==0) return 0.0;
		
		for (int i = 0; i < len1; i++) {
	    	if (hash1[i] == 1) {
				while (hash2[point] == 0) {
		   			point++;
				}
				if (charAt(s1, i) != charAt(s2, point++)) {
				    t++;
				}
	    	}
		}
		t/=2;
		return (((double) match) / ((double)len1) + ((double) match) / ((double)len2) + ((double)match-t) / ((double)match)) / TIERS;
    }
	
    double jaroWinklerDistance(String s1, String s2, int prefixeCommun) {
		final double COEFFICIENT = 0.1, SEUIL = 0.7;
		
		double jaroDist = jaroDistance(s1, s2);
		if (jaroDist > SEUIL) {
		    int prefix = 0, i = 0;
			while(i < min(length(s1), length(s2)) && charAt(s1, i) == charAt(s2, i)) {
				i++; 
				prefix++;
		    }		
	    	prefix=min(prefixeCommun, prefix);		
	    	jaroDist+=COEFFICIENT*prefix*(1-jaroDist);
		}
		return jaroDist;
    }
		
    File chargerRessource(String nom) {
		return newFile("../ressources/"+nom);
    }

    CSVFile chargerRessourceCSV(String nom) {
		return loadCSV("../ressources/"+nom, ';');
    }
	
    boolean mailDejaEntre(byte[] mail, Profil profils[]) {
		boolean entered = false;
		int idx = 0;
		while (idx < length(profils) && !entered) {
		    entered = equals(mail, profils[idx++].mail);
		}
		return entered;
    }
	
    void imprimerFichier(File file) {
		while (file.ready()) {
		    println(file.readLine());
		}
    }
    
    void testFloor() {
    	double a = 2.4, b = -4.2, c = 2.0;
    	
    	assertEquals(2, floor(a));
    	assertEquals(-5, floor(b));
    	assertEquals(2, floor(c));
    }
	
    int floor(double a) {
		if (a >= 0.0) {
		    return (int) a;
		} else {
		    int b = (int)a;
		    return (int) (a==b ? b : b-1);
		}
    }
	
	void testMaxMin() {
		int a = 10, b = 42;
		
		assertEquals(a, min(a,b));
		assertEquals(a, min(b,a));
		assertEquals(a, min(a,a));
		
		assertEquals(b, max(a,b));
		assertEquals(b, max(b,a));
		assertEquals(b, max(b,b));
	}
	
    int max(int a, int b) {
		return (a > b) ? a : b;
    }
	
    int min(int a, int b) {
		return (a < b) ? a : b;
    }
}
