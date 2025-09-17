package com.emanueleslusarz.it.actor;

import java.util.ArrayList;
import java.util.List;

public class Paziente {

    // Attributi classe
    public static final String TAG_NOME_COGNOME = "00100010";
    public static final String TAG_PATIENT_ID = "00100020";
    public static final String TAG_DATA_NASCITA = "00100030";
    public static final String TAG_SESSO = "00100040";

    // Attributi istanza
    private String generalita = "";
    private String patientId = "";
    private String dataDiNascita = "";
    private String sesso = "";
    private List<Studio> listaStudi;

    {
        listaStudi = new ArrayList<>();
    }

    // fluent API
    public Paziente setGeneralita(String generalita){
        this.generalita = generalita;
        return this;
    }

    public Paziente setPatientId(String patientId){
        this.patientId = patientId;
        return this;
    }

    public Paziente setDataNascita(String dataDiNascita){
        this.dataDiNascita = dataDiNascita;
        return this;
    }

    public Paziente setSesso(String sesso){
        this.sesso = sesso;
        return this;
    }

    public String getGeneralita() {
        return generalita;
    }

    public String getPatientId() {
        return patientId;
    }

    public String getDataDiNascita() {
        return dataDiNascita;
    }

    public String getSesso() {
        return sesso;
    }

    public boolean aggiungiStudio(Studio studio){
        if(!this.listaStudi.contains(studio)){
            this.listaStudi.add(studio);
            return true;
        }
        return false;
    }

    public boolean rimuoviStudio(Studio studio){
        return this.listaStudi.remove(studio);
    }

    @Override
    public String toString(){
        StringBuilder descrizione = new StringBuilder();
        descrizione.append("Nome & Cognome: " + this.generalita);descrizione.append(System.lineSeparator());
        descrizione.append("Patient ID: " + this.patientId);descrizione.append(System.lineSeparator());
        descrizione.append("Data di nascita: " + this.dataDiNascita);descrizione.append(System.lineSeparator());
        descrizione.append("Sesso: " + this.sesso);descrizione.append(System.lineSeparator());
        return descrizione.toString();
    }

}
