package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;
    private static final String REGEX_CA = "^[0-9]+(\\.[0-9]{1,2})?$";
    private static final String REGEX_PERFORMANCE = "[0-9]+";
    /*private static final String REGEX_SALAIRE = "^[0-9]+\\.[0-9]{0,2}$";
    ^ => début de chaîne
    [0-9] => on un chiffre entre 0 et 9 (compris)
    + => on veut au moins 1 chiffre
    \\. => on veut un point (pour la virgule du salaire)
    {0,2} => on veut de 0 à 2 chiffres après la virgule
    $ => fin de chaîne
     */


    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    /*
    logger sert à afficher un certain nombre d'infos et de pouvoir par configuration dire qu'on veut que les éléments
    concernés doivent être archivés. C'est une manière de consigner des éléments dans un fichier dans un format. Il a un
    certain nombre de niveaux:
        - info
        - warn: attention, on est presque à une erreur
        - error
    */

    @Override
    public void run(String... strings){
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName) {
        Stream<String> stream;
        //Stream contient l'ensemble des lignes sous forme de liste et il va falloir parcourir cette liste

        logger.info("Lecture du fichier : " + fileName);

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
            //Files.lines : renvoie une seule ligne à chaque fois
        } catch (IOException e) {
            logger.error("Problème dans l'ouverture du fichier " + fileName);
            return new ArrayList<>();
            //on fait ça parce-qu'on ne peut pas sortir une liste de notre chapeau. On renvoie donc une liste vide.
        }

        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size() + " lignes lues");
        for (int i = 0; i < lignes.size(); i++){
            try {
                processLine(lignes.get(i));
            } catch (BatchException e) {
                logger.error("Ligne " + (i+1) + " : " + e.getMessage() + " => " + lignes.get(i));
                /*i+1 => c'est pour commencer à la ligne 1.
                Avec ce logger.error l'exception ne va pas se préoccuper pas de la ligne ou du problème. On dit juste
                qu'il y a un problème. C'est ici qu'on se préoccupe de la ligne.
                */
            }
        }

        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        //Si la première lettre de la ligne n'est pas C, T ou M, renvoyer une erreur
        //Sinon appeler méthode de création de l'employé correspondant
        switch (ligne.substring(0,1)){
            case "T":
                processTechnicien(ligne);
                break;
            case "M":
                processManager(ligne);
                break;
            case "C":
                processCommercial(ligne);
                break;
            default:
                throw new BatchException("Type d'employé inconnu");
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {
        Commercial c = new Commercial();
        String[] commercialFields = ligneCommercial.split(",");
        //on split la ligne pour séparer chaque colonne. La séparation se fait sur la virgule

        //on vérifie si le matricule (1ère colonne, donc '0') respecte bien l'expression régulière d'un matricule
        if (commercialFields.length != NB_CHAMPS_COMMERCIAL){ //Si la longueur du champs Commercial est différente
            throw new BatchException("La ligne commercial ne contient pas 7 éléments mais " + commercialFields.length);
        }

        //appel de la méthode par défaut qui vérifie les formats de certain champs
        processEmploye(ligneCommercial, c);

        //vérification format chiffre d'affaire
        if (!commercialFields[5].matches(REGEX_CA)){
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect: " + commercialFields[5]);
        }

        //vérification format performance
        if (!commercialFields[6].matches(REGEX_PERFORMANCE)){
            throw new BatchException( ("La performance du commercial est incorrecte : " + commercialFields[6]));
        }

        employes.add(c);
    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {
        Manager m = new Manager();
        String[] managerFields = ligneManager.split(",");

        if (managerFields.length != NB_CHAMPS_MANAGER){
            throw new BatchException("La ligne manager ne contient pas 5 éléments mais " + managerFields.length);
        }

        //appel de la méthode par défaut qui vérifie les formats de certain champs
        processEmploye(ligneManager, m);


        employes.add(m);

    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {
        Technicien t = new Technicien();
        String[] technicienFields = ligneTechnicien.split(",");

        if (technicienFields.length != NB_CHAMPS_TECHNICIEN){
            throw new BatchException("La ligne technicien ne contient pas 7 éléments mais " + technicienFields.length);
        }

        //appel de la méthode par défaut qui vérifie les formats de certain champs
        processEmploye(ligneTechnicien, t);

        //vérification du grade du technicien
        Integer grade;
        try {
            grade = Integer.parseInt(technicienFields[5]);
        }
        catch (Exception e) {
            throw new BatchException(technicienFields[5] + " n'est pas un format valide de grade");
        }

        //vérification que le grade soit bien entre 1 et 5
        try {
            t.setGrade(grade);
        }
        catch (Exception e){
            throw new BatchException("Le grade doit être compris entre 1 et 5");
        }

        //vérification du matricule du manager
        if (!technicienFields[6].matches(REGEX_MATRICULE_MANAGER)){
            throw new BatchException("La chaîne " + technicienFields[6] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE_MANAGER);
        }

        //vérification de l'existence du manager dans la BDD puis dans le fichier
        Manager manager = managerRepository.findByMatricule(technicienFields[6]);


        if (manager == null){
            throw new BatchException("Le manager de matricule " + technicienFields[6] + " n'a pas été trouvé en base de données");
        }

        /* parcourir liste employe
         * vérifier si chaque élément est un manager
         * si c'est un manager vérifier son matricule
         * parcourir toute la liste
         * si matricule pas trouvé, on lance l'exception
        */

        /*for (int i = 0; i < employes.size(); i++){
            on parcourt chaque ligne avec processLine
            si processLine appelle processManager c'est qu'on a bien un manager
            si manager on vérifie son matricule
            si pas de matricule trouvé, on lance l'exception
        }*/


        employes.add(t);

    }

    /**
     * Méthode générique qui vérifie les champs Matricule, Nom, Prénom, Date d'embauche et Salaire
     * @param ligneEmploye la ligne contenant les infos de l'employé à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processEmploye(String ligneEmploye, Employe employe) throws BatchException {
        //on split la lignes pour séparer chaque colonne. La séparation se fait sur la virgule
        String[] employeFields = ligneEmploye.split(",");

        //vérification format matricule
        if (!employeFields[0].matches(REGEX_MATRICULE)){
            throw new BatchException("La chaîne: " + employeFields[0] + " ne respecte pas l'expression régulière ^[MTC][0-9]{5}$");
        }

        //vérification format nom
        if (!employeFields[1].matches(REGEX_NOM)){
            throw new BatchException("Le nom ne respecte pas les règles");
        }

        //vérification format prenom
        if (!employeFields[2].matches(REGEX_PRENOM)){
            throw new BatchException("Le prénom ne respecte pas les règles");
        }

        //vérification format date
        LocalDate dateEmbauche;
        try{
            dateEmbauche = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(employeFields[3]);
        }
        catch (Exception e) {

            throw new BatchException( employeFields[3] + " ne respecte pas le format de date dd/MM/yyyy");
        }

        //vérification format salaire
        double salaire;
        try {
            salaire = Double.parseDouble(employeFields[4]);
        }
        catch (Exception e){
            throw new BatchException( employeFields[4] + " n'est pas un format valide de salaire");
        }

        //création d'un nouvel employé
        /*employe.setMatricule(employeFields[0]);
        employe.setNom(employeFields[1]);
        employe.setPrenom(employeFields[2]);
        employe.setDateEmbauche(dateEmbauche);
        employe.setSalaire(salaire);*/
    }
}
