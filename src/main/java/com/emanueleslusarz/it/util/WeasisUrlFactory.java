package com.emanueleslusarz.it.util;

import java.net.URL;

public abstract class WeasisUrlFactory {

    // Attr
    private final String URL_PREFIX;
    private final String URL_MIDDLEFIX;

    private String aetitle;
    private String ip;
    private String port;

    {
        URL_PREFIX = "weasis://$dicom%3Ars%20--url%20%22http%3A%2F%2F";
        URL_MIDDLEFIX = "dcm4chee-arc%2Faets%2F";
    }

    public WeasisUrlFactory(String aetitle,
                            String ip,
                            String port){
        this.aetitle = aetitle;
        this.ip = ip;
        this.port = port;
    }

    public String getAetitle() {
        return aetitle;
    }

    public String getIp() {
        return ip;
    }

    public String getPort() {
        return port;
    }

    public String getFixedPort(){
        return "%3A" + this.port + "%2F";
    }

    public String getURL_PREFIX() {
        return URL_PREFIX;
    }

    public String getURL_MIDDLEFIX() {
        return URL_MIDDLEFIX;
    }

    public void setAetitle(String aetitle) {
        this.aetitle = aetitle;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public abstract String buildURL();

}
