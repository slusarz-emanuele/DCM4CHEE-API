package com.emanueleslusarz.it.query.impl;

import com.emanueleslusarz.it.actor.Paziente;
import com.emanueleslusarz.it.actor.Studio;
import com.emanueleslusarz.it.query.Query;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryImpl implements Query {
    private final String baseRsUrl;
    private final HttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();

    public QueryImpl(String baseRsUrl) {
        this.baseRsUrl = baseRsUrl.endsWith("/") ? baseRsUrl.substring(0, baseRsUrl.length() - 1) : baseRsUrl;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    // -------------------------------------------------
    // RAW
    // -------------------------------------------------
    @Override
    public String getFullPatientsJSON(int limit, int offset) throws Exception {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("includefield", "PatientID");
        q.put("includefield", "PatientName");
        q.put("includefield", "IssuerOfPatientID");
        q.put("includefield", "PatientBirthDate");
        q.put("includefield", "PatientSex");
        q.put("limit", String.valueOf(limit));
        q.put("offset", String.valueOf(offset));
        String url = buildUrl(baseRsUrl + "/patients", q);
        return httpGet(url);
    }

    @Override
    public String getFullStudiesFromPatientJSON(String patientId, String issuerOfPatientId, int limit, int offset, String orderBy) throws Exception {
        Map<String, String> q = new LinkedHashMap<>();
        q.put("PatientID", patientId);
        if (issuerOfPatientId != null && !issuerOfPatientId.isBlank()) {
            q.put("IssuerOfPatientID", issuerOfPatientId);
        }
        // includi esattamente i tag che ti servono
        q.put("includefield", "StudyDate"); // 00080020
        q.put("includefield", "StudyTime"); // 00080030
        q.put("includefield", "RetrieveAETitle"); // 00080054
        q.put("includefield", "ModalitiesInStudy"); // 00080061
        q.put("includefield", "SOPClassesInStudy"); // 00080062
        q.put("includefield", "ReferringPhysicianName"); // 00080090
        q.put("includefield", "StudyDescription"); // 00081030
        q.put("includefield", "StudyInstanceUID"); // 0020000D
        q.put("includefield", "StudyID"); // 00200010
        q.put("includefield", "NumberOfPatientRelatedStudies"); // 00201200
        q.put("includefield", "NumberOfStudyRelatedSeries"); // 00201206
        q.put("includefield", "NumberOfStudyRelatedInstances"); // 00201208
        q.put("limit", String.valueOf(limit));
        q.put("offset", String.valueOf(offset));
        if (orderBy != null && !orderBy.isBlank()) {
            q.put("orderby", orderBy); // es. -StudyDate,-StudyTime
        }
        String url = buildUrl(baseRsUrl + "/studies", q);
        return httpGet(url);
    }

    // -------------------------------------------------
    // Tipizzati
    // -------------------------------------------------
    @Override
    public List<Paziente> getPatientList(int limit, int offset) throws Exception {
        String json = getFullPatientsJSON(limit, offset);
        JsonNode arr = mapper.readTree(json);
        List<Paziente> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode obj : arr) {
                Paziente p = new Paziente()
                        .setGeneralita(readPatientName(obj))
                        .setPatientId(readTagValue(obj, "00100020")) // PatientID
                        .setDataNascita(readTagValue(obj, "00100030")) // BirthDate
                        .setSesso(readTagValue(obj, "00100040")); // Sex
                out.add(p);
            }
        }
        return out;
    }

    @Override
    public List<Studio> getStudiesFromPatient(String patientId, String issuerOfPatientId, int limit, int offset, String orderBy) throws Exception {
        String json = getFullStudiesFromPatientJSON(patientId, issuerOfPatientId, limit, offset, orderBy);
        JsonNode arr = mapper.readTree(json);
        List<Studio> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode obj : arr) {
                Studio s = new Studio()
                        .setStudyDate(readTagValue(obj, "00080020"))
                        .setStudyTime(readTagValue(obj, "00080030"))
                        .setRetrieveAETitle(readTagValue(obj, "00080054"))
                        .setModalitiesInStudy(readMultiCS(obj, "00080061"))
                        .setSopClassesInStudy(readMultiUI(obj, "00080062"))
                        .setReferringPhysicianName(readPN(obj, "00080090"))
                        .setStudyDescription(readTagValue(obj, "00081030"))
                        .setStudyInstanceUID(readTagValue(obj, "0020000D"))
                        .setStudyID(readTagValue(obj, "00200010"))
                        .setNumberPatientRelatedStudies(readTagValue(obj, "00201200"))
                        .setNumberStudyRelatedSeries(readTagValue(obj, "00201206"))
                        .setNumberStudyRelatedInstances(readTagValue(obj, "00201208"));
                out.add(s);
            }
        }
        return out;
    }

    // -------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------
    private String httpGet(String url) throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/dicom+json")
                .GET()
                .build();
        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 200 && resp.statusCode() < 300) {
            return resp.body();
        }
        throw new IllegalStateException("HTTP " + resp.statusCode() + " on GET " + url + " " +
                "Body: " + resp.body());
    }

    private String buildUrl(String base, Map<String, String> q) {
        StringBuilder sb = new StringBuilder(base);
        if (q != null && !q.isEmpty()) {
            boolean first = true;
            for (Map.Entry<String, String> e : q.entrySet()) {
                sb.append(first ? '?' : '&');
                first = false;
                // NB: la QIDO-RS di dcm4chee accetta ripetizioni di includefield.
                sb.append(URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8));
                sb.append('=');
                sb.append(URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    // -------------------------------------------------
    // JSON parsing helpers (DICOM JSON)
    // -------------------------------------------------
    private String readTagValue(JsonNode obj, String tag) {
        JsonNode n = obj.get(tag);
        if (n == null) return "";
        JsonNode v = n.get("Value");
        if (v == null || !v.isArray() || v.isEmpty()) return "";
        JsonNode first = v.get(0);
        return first.isValueNode() ? first.asText("") : first.toString();
    }

    private String readPatientName(JsonNode obj) {
        // 00100010 può essere un oggetto { Alphabetic: "A^B" }
        JsonNode n = obj.get("00100010");
        if (n == null) return "";
        JsonNode v = n.get("Value");
        if (v == null || !v.isArray() || v.isEmpty()) return "";
        JsonNode first = v.get(0);
        if (first.isObject()) {
            return first.path("Alphabetic").asText("");
        }
        return first.asText("");
    }

    private String readPN(JsonNode obj, String tag) {
        JsonNode n = obj.get(tag);
        if (n == null) return "";
        JsonNode v = n.get("Value");
        if (v == null || !v.isArray() || v.isEmpty()) return "";
        JsonNode first = v.get(0);
        if (first.isObject()) {
            return first.path("Alphabetic").asText("");
        }
        return first.asText("");
    }

    private String readMultiCS(JsonNode obj, String tag) {
        // restituisce valori CS uniti da "," (es. ModalitiesInStudy)
        JsonNode n = obj.get(tag);
        if (n == null) return "";
        JsonNode v = n.get("Value");
        if (v == null || !v.isArray() || v.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(v.get(i).asText(""));
        }
        return sb.toString();
    }

    private String readMultiUI(JsonNode obj, String tag) {
        // unisci le UI con "|" (solo rappresentazione umana)
        JsonNode n = obj.get(tag);
        if (n == null) return "";
        JsonNode v = n.get("Value");
        if (v == null || !v.isArray() || v.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < v.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(v.get(i).asText(""));
        }
        return sb.toString();
    }
}