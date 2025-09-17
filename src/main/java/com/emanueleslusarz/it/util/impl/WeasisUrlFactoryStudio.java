package com.emanueleslusarz.it.util.impl;

import com.emanueleslusarz.it.util.WeasisUrlFactory;

public class WeasisUrlFactoryStudio extends WeasisUrlFactory {

    // Attr
    private String studyInstanceUID;

    private final String URL;

    public WeasisUrlFactoryStudio (String aetitle,
                                   String ip,
                                   String port,
                                   String studyInstanceUID){
        super(aetitle, ip, port);
        this.studyInstanceUID = studyInstanceUID;
        URL = this.buildURL();
    }

    public String buildURL(){
        StringBuilder URL = new StringBuilder();
        URL.append(this.getURL_PREFIX());
        URL.append(this.getIp());
        URL.append(this.getFixedPort());
        URL.append(this.getURL_MIDDLEFIX());
        URL.append(this.getAetitle());
        URL.append("%2Frs%22%20-r%20%22studyUID%3D");
        URL.append(studyInstanceUID);
        URL.append("%22%20--query-ext%20%22%26includedefaults%3Dfalse%22");

        return URL.toString();
    }

    public String getURL() {
        return URL;
    }

}
