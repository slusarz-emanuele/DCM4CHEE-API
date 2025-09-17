package com.emanueleslusarz.it.actor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Studio implements Comparable<Studio> {

    // Tag DICOM per Studio
    public static final String TAG_STUDY_DATE = "00080020";
    public static final String TAG_STUDY_TIME = "00080030";
    public static final String TAG_RETRIEVE_AETITLE = "00080054";
    public static final String TAG_MODALITIES_IN_STUDY = "00080061";
    public static final String TAG_SOP_CLASSES_IN_STUDY = "00080062";
    public static final String TAG_REFERRING_PHYSICIAN_NAME = "00080090";
    public static final String TAG_STUDY_DESCRIPTION = "00081030";
    public static final String TAG_STUDY_INSTANCE_UID = "0020000D";
    public static final String TAG_STUDY_ID = "00200010";
    public static final String TAG_NUMBER_PATIENT_RELATED_STUDIES = "00201200";
    public static final String TAG_NUMBER_STUDY_RELATED_SERIES = "00201206";
    public static final String TAG_NUMBER_STUDY_RELATED_INSTANCES = "00201208";

    // Attributi istanza
    private String studyDate = "";
    private String studyTime = "";
    private String retrieveAETitle = "";
    private String modalitiesInStudy = "";
    private String sopClassesInStudy = "";
    private String referringPhysicianName = "";
    private String studyDescription = "";
    private String studyInstanceUID = "";
    private String studyID = "";
    private String numberPatientRelatedStudies = "";
    private String numberStudyRelatedSeries = "";
    private String numberStudyRelatedInstances = "";

    // opzionale: lista serie
    private List<Serie> listaSerie;

    {
        listaSerie = new ArrayList<>();
    }

    // Fluent API
    public Studio setStudyDate(String studyDate) {
        this.studyDate = studyDate;
        return this;
    }

    public Studio setStudyTime(String studyTime) {
        this.studyTime = studyTime;
        return this;
    }

    public Studio setRetrieveAETitle(String retrieveAETitle) {
        this.retrieveAETitle = retrieveAETitle;
        return this;
    }

    public Studio setModalitiesInStudy(String modalitiesInStudy) {
        this.modalitiesInStudy = modalitiesInStudy;
        return this;
    }

    public Studio setSopClassesInStudy(String sopClassesInStudy) {
        this.sopClassesInStudy = sopClassesInStudy;
        return this;
    }

    public Studio setReferringPhysicianName(String referringPhysicianName) {
        this.referringPhysicianName = referringPhysicianName;
        return this;
    }

    public Studio setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
        return this;
    }

    public Studio setStudyInstanceUID(String studyInstanceUID) {
        this.studyInstanceUID = studyInstanceUID;
        return this;
    }

    public Studio setStudyID(String studyID) {
        this.studyID = studyID;
        return this;
    }

    public Studio setNumberPatientRelatedStudies(String numberPatientRelatedStudies) {
        this.numberPatientRelatedStudies = numberPatientRelatedStudies;
        return this;
    }

    public Studio setNumberStudyRelatedSeries(String numberStudyRelatedSeries) {
        this.numberStudyRelatedSeries = numberStudyRelatedSeries;
        return this;
    }

    public Studio setNumberStudyRelatedInstances(String numberStudyRelatedInstances) {
        this.numberStudyRelatedInstances = numberStudyRelatedInstances;
        return this;
    }

    // Getter
    public String getStudyDate() {
        return studyDate;
    }

    public String getStudyTime() {
        return studyTime;
    }

    public String getRetrieveAETitle() {
        return retrieveAETitle;
    }

    public String getModalitiesInStudy() {
        return modalitiesInStudy;
    }

    public String getSopClassesInStudy() {
        return sopClassesInStudy;
    }

    public String getReferringPhysicianName() {
        return referringPhysicianName;
    }

    public String getStudyDescription() {
        return studyDescription;
    }

    public String getStudyInstanceUID() {
        return studyInstanceUID;
    }

    public String getStudyID() {
        return studyID;
    }

    public String getNumberPatientRelatedStudies() {
        return numberPatientRelatedStudies;
    }

    public String getNumberStudyRelatedSeries() {
        return numberStudyRelatedSeries;
    }

    public String getNumberStudyRelatedInstances() {
        return numberStudyRelatedInstances;
    }

    // Gestione serie
    public boolean aggiungiSerie(Serie serie) {
        if (!this.listaSerie.contains(serie)) {
            this.listaSerie.add(serie);
            return true;
        }
        return false;
    }

    public boolean rimuoviSerie(Serie serie) {
        return this.listaSerie.remove(serie);
    }

    @Override
    public String toString() {
        StringBuilder descrizione = new StringBuilder();
        descrizione.append("Study Date: ").append(this.studyDate).append(System.lineSeparator());
        descrizione.append("Study Time: ").append(this.studyTime).append(System.lineSeparator());
        descrizione.append("Retrieve AE Title: ").append(this.retrieveAETitle).append(System.lineSeparator());
        descrizione.append("Modalities In Study: ").append(this.modalitiesInStudy).append(System.lineSeparator());
        descrizione.append("SOP Classes In Study: ").append(this.sopClassesInStudy).append(System.lineSeparator());
        descrizione.append("Referring Physician Name: ").append(this.referringPhysicianName).append(System.lineSeparator());
        descrizione.append("Study Description: ").append(this.studyDescription).append(System.lineSeparator());
        descrizione.append("Study Instance UID: ").append(this.studyInstanceUID).append(System.lineSeparator());
        descrizione.append("Study ID: ").append(this.studyID).append(System.lineSeparator());
        descrizione.append("Number Patient Related Studies: ").append(this.numberPatientRelatedStudies).append(System.lineSeparator());
        descrizione.append("Number Study Related Series: ").append(this.numberStudyRelatedSeries).append(System.lineSeparator());
        descrizione.append("Number Study Related Instances: ").append(this.numberStudyRelatedInstances).append(System.lineSeparator());
        return descrizione.toString();
    }

    @Override
    public int compareTo(Studio studio){
        try{
            if (Integer.parseInt(this.studyDate) > Integer.parseInt(studio.studyDate)) return 1;
            if (Integer.parseInt(this.studyDate) < Integer.parseInt(studio.studyDate)) return -1;
            return 0;
        }catch (ArithmeticException e){
            return 0;
        }
    }

}
