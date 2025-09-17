package com.emanueleslusarz.it;

import com.emanueleslusarz.it.actor.Paziente;
import com.emanueleslusarz.it.actor.Studio;
import com.emanueleslusarz.it.query.Query;
import com.emanueleslusarz.it.query.impl.QueryImpl;
import com.emanueleslusarz.it.util.impl.WeasisUrlFactoryPatient;
import com.emanueleslusarz.it.util.impl.WeasisUrlFactoryStudio;

import java.util.List;

public class Main {

    public static void main(String [] A) throws Exception {
        Query q = new QueryImpl("http://127.0.0.1:8080/dcm4chee-arc/aets/DCM4CHEE/rs");
        List<Paziente> paz = q.getPatientList(200, 0);

        for (Paziente p: paz){
            System.out.println(p.toString());
            WeasisUrlFactoryPatient url = new WeasisUrlFactoryPatient("DCM4CHEE", "127.0.0.1", "8080", p.getPatientId());
            System.out.println(url.getURL());
        }

        System.out.println("------------------");

        List<Studio> studi = q.getStudiesFromPatient("MLTCLS49R22Z600L", null, 200, 0, "-StudyDate,-StudyTime");

        for (Studio s: studi){
            System.out.println(s.toString());
            WeasisUrlFactoryStudio url = new WeasisUrlFactoryStudio("DCM4CHEE", "127.0.0.1", "8080", s.getStudyInstanceUID());
            System.out.println(url.getURL());
        }
    }
}