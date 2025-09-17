package com.emanueleslusarz.it.util.impl;

import com.emanueleslusarz.it.util.WeasisUrlFactory;

public class WeasisUrlFactoryPatient extends WeasisUrlFactory {

    // Attr
    private String patientId;

    private final String URL;

    public WeasisUrlFactoryPatient (String aetitle,
                                    String ip,
                                    String port,
                                    String patientId){
        super(aetitle, ip, port);
        this.patientId = patientId;
        URL = this.buildURL();
    }

    @Override
    public String buildURL(){
        StringBuilder URL = new StringBuilder();
        URL.append(this.getURL_PREFIX());
        URL.append(this.getIp());
        URL.append(this.getFixedPort());
        URL.append(this.getURL_MIDDLEFIX());
        URL.append(this.getAetitle());
        URL.append("%2Frs%22%20-r%20%22patientID%3D");
        URL.append(patientId);
        URL.append("%22");

        return URL.toString();
    }

    public String getURL() {
        return URL;
    }
}
