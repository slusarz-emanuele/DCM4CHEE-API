package com.emanueleslusarz.it.query;

import com.emanueleslusarz.it.actor.Paziente;
import com.emanueleslusarz.it.actor.Studio;

import java.util.List;

public interface Query {

    // Raw JSON get
    public String getFullPatientsJSON(int limit,
                                      int offest) throws Exception;

    public String getFullStudiesFromPatientJSON(String patientId,
                                                String issuerOfPatientId,
                                                int limit,
                                                int offset,
                                                String orderBy) throws Exception;

    // Clear get
    public List<Paziente> getPatientList(int limit,
                                         int offest) throws Exception;

    public List<Studio> getStudiesFromPatient(String patientId,
                                                  String issuerOfPatientId,
                                                  int limit,
                                                  int offset,
                                                  String orderBy) throws Exception;

}
