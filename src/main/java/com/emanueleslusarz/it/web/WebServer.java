package com.emanueleslusarz.it.web;

import com.emanueleslusarz.it.actor.Paziente;
import com.emanueleslusarz.it.actor.Studio;
import com.emanueleslusarz.it.query.Query;
import com.emanueleslusarz.it.query.impl.QueryImpl;
import com.emanueleslusarz.it.util.impl.WeasisUrlFactoryPatient;
import com.emanueleslusarz.it.util.impl.WeasisUrlFactoryStudio;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class WebServer {
    private final Query query;               // usa la tua Query/QueryImpl, non la modifichiamo
    private final ObjectMapper mapper = new ObjectMapper();
    private final String rsBase;             // per costruire link Weasis con la tua factory
    private final String aet;                // AET dell'ARC (es. DCM4CHEE)
    private final String host;               // host REST (es. 127.0.0.1)
    private final String portStr;            // porta REST (es. 8080)

    public WebServer(String rsBase, String aet, String host, String portStr) {
        this.rsBase = rsBase;
        this.aet = aet;
        this.host = host;
        this.portStr = portStr;
        this.query = new QueryImpl(rsBase);
    }

    public void start(int port) throws IOException {
        HttpServer http = HttpServer.create(new InetSocketAddress(port), 0);
        http.createContext("/api/patients", new PatientsApiHandler());
        http.createContext("/api/studies", new StudiesApiHandler());
        http.createContext("/ui/patients", ex -> sendHtml(ex, htmlPatients()));
        http.createContext("/ui/studies", ex -> sendHtml(ex, htmlStudies()));
        http.createContext("/", ex -> sendHtml(ex, "<h1>OK</h1><p>Try <a href='ui/patients'>/ui/patients</a></p>"));
        http.setExecutor(null);
        http.start();
        System.out.println("WebServer up on http://localhost:" + port);
    }

    // ========================= API =========================
    class PatientsApiHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            if (corsPreflight(ex)) return;
            try {
                Map<String, String> q = queryParams(ex.getRequestURI().getRawQuery());
                int limit = parseIntOr(q.getOrDefault("limit", "5000"), 5000);
                int offset = parseIntOr(q.getOrDefault("offset", "0"), 0);

                List<Paziente> list = query.getPatientList(limit, offset);
                List<Map<String, Object>> out = new ArrayList<>();
                for (Paziente p : list) {
                    String pid = p.getPatientId();
                    // usa la tua factory per i link weasis
                    String w = new WeasisUrlFactoryPatient(aet, host, portStr, pid).getURL();
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("patientName", p.getGeneralita());
                    row.put("patientId", pid);
                    row.put("birthDate", p.getDataDiNascita());
                    row.put("sex", p.getSesso());
                    row.put("weasis", w);
                    out.add(row);
                }
                sendJson(ex, out);
            } catch (Exception e) {
                sendError(ex, 500, e.getMessage());
            }
        }
    }

    class StudiesApiHandler implements HttpHandler {
        @Override public void handle(HttpExchange ex) throws IOException {
            if (corsPreflight(ex)) return;
            try {
                Map<String, String> q = queryParams(ex.getRequestURI().getRawQuery());
                String patientId = q.get("patientId");
                if (patientId == null || patientId.isBlank()) {
                    sendError(ex, 400, "Missing patientId");
                    return;
                }
                int limit = parseIntOr(q.getOrDefault("limit", "500"), 500);
                int offset = parseIntOr(q.getOrDefault("offset", "0"), 0);
                String orderBy = q.getOrDefault("orderby", "-StudyDate,-StudyTime");

                // filtri opzionali
                String dateFrom = q.get("dateFrom"); // YYYY-MM-DD
                String dateTo   = q.get("dateTo");   // YYYY-MM-DD
                String timeFrom = q.get("timeFrom"); // HH:mm[:ss]
                String timeTo   = q.get("timeTo");   // HH:mm[:ss]

                String fromKey = buildDTKey(dateFrom, timeFrom, true);   // es. 20240101000000
                String toKey   = buildDTKey(dateTo,   timeTo,   false);  // es. 20241231235959

                List<Studio> list = query.getStudiesFromPatient(patientId, null, limit, offset, orderBy);

                // 1) Filtro
                List<Studio> filtered = new ArrayList<>();
                for (Studio s : list) {
                    String d = safe(s.getStudyDate());                 // atteso YYYYMMDD
                    String t = padTime(safe(s.getStudyTime()));        // HHmmss
                    String key = (d.length()==8? d : "") + (t.length()==6? t : "000000");

                    if (fromKey != null && !key.isEmpty() && key.compareTo(fromKey) < 0) continue;
                    if (toKey   != null && !key.isEmpty() && key.compareTo(toKey)   > 0) continue;

                    filtered.add(s);
                }

                // 2) Ordina con l'ordinamento naturale (usa il tuo compareTo su Studio)
                // "-StudyDate,-StudyTime" => discendente; qualsiasi altra cosa => ascendente
                boolean desc = orderBy.strip().startsWith("-");
                filtered.sort(desc ? Comparator.reverseOrder() : Comparator.naturalOrder());

                // 3) Mappa in output
                List<Map<String, Object>> out = new ArrayList<>(filtered.size());
                for (Studio s : filtered) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("studyDate", s.getStudyDate());
                    row.put("studyTime", s.getStudyTime());
                    row.put("studyDescription", s.getStudyDescription());
                    row.put("modalitiesInStudy", s.getModalitiesInStudy());
                    row.put("studyInstanceUID", s.getStudyInstanceUID());
                    row.put("studyID", s.getStudyID());
                    row.put("numSeries", s.getNumberStudyRelatedSeries());
                    row.put("numInstances", s.getNumberStudyRelatedInstances());
                    row.put("weasis", new WeasisUrlFactoryStudio(aet, host, portStr, s.getStudyInstanceUID()).getURL());
                    out.add(row);
                }

                sendJson(ex, out);
            } catch (Exception e) {
                sendError(ex, 500, e.getMessage());
            }
        }

        private String safe(String s){ return s==null? "": s.trim(); }

        private String buildDTKey(String dateISO, String timeHMx, boolean isFrom){
            if ((dateISO==null || dateISO.isBlank()) && (timeHMx==null || timeHMx.isBlank())) return null;
            String d = (dateISO==null || dateISO.isBlank()) ? null : dateISO.replaceAll("-", "");
            String t = padTime(timeHMx);

            if (d == null) {
                // se non c'è la data, usiamo “00000000”/“99999999” per coprire tutto
                d = isFrom ? "00000000" : "99999999";
            }
            if (t.isEmpty()) t = isFrom ? "000000" : "235959";
            return d + t; // YYYYMMDDHHmmss
        }

        private String padTime(String time){
            if (time==null || time.isBlank()) return "";
            // accetta HH:mm[:ss] o HHmm[ss]
            String t = time.contains(":") ? time.replace(":", "") : time;
            if (t.length()==2)  t = t + "0000";      // HH -> HH0000
            if (t.length()==4)  t = t + "00";        // HHmm -> HHmm00
            if (t.length()>6)   t = t.substring(0,6);
            if (t.length()<6)   t = String.format("%-6s", t).replace(' ', '0');
            return t;
        }
    }


    // ========================= UI =========================
    private String htmlPatients() {
        return """
<!doctype html><meta charset='utf-8'>
<title>Patients</title>
<style>
 body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;margin:20px}
 table{border-collapse:collapse;width:100%;max-width:1000px}
 th,td{border:1px solid #e6e6e6;padding:8px}
 th{background:#fafafa;text-align:left}
 .btn{padding:6px 10px;border:1px solid #ddd;border-radius:8px;cursor:pointer;background:#fff;text-decoration:none}
 tr:hover{background:#fcfcfc}
</style>
<h1>Patients</h1>
<table id="tbl"><thead>
<tr><th>Name</th><th>Patient ID</th><th>Birth</th><th>Sex</th><th>Azioni</th></tr>
</thead><tbody></tbody></table>
<script>
const TBL = document.querySelector('#tbl tbody');
fetch('/api/patients?limit=2000&offset=0')
  .then(r => {
    if(!r.ok) throw new Error('HTTP '+r.status);
    return r.json();
  })
  .then(rows => {
    for(const r of rows){
      const tr=document.createElement('tr');
      const hrefStudies = `/ui/studies?patientId=${encodeURIComponent(r.patientId)}`;
      tr.innerHTML = `
        <td>${esc(r.patientName||'')}</td>
        <td>${esc(r.patientId||'')}</td>
        <td>${esc(r.birthDate||'')}</td>
        <td>${esc(r.sex||'')}</td>
        <td>
          <a class='btn' href='${hrefStudies}' target='_top'>Apri studi</a>
          <a class='btn' href='${r.weasis}' target='_blank' rel='noopener'>Apri in Weasis</a>
        </td>`;
      TBL.appendChild(tr);
    }
  })
  .catch(err => console.error('patients fetch error', err));

function esc(s){return (s+"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\\"":"&quot;","'":"&#39;"}[c]))}
</script>
""";
    }


    private String htmlStudies() {
        return """
<!doctype html><meta charset='utf-8'>
<title>Studies</title>
<style>
 body{font-family:system-ui,-apple-system,Segoe UI,Roboto,Arial,sans-serif;margin:20px}
 .row{display:flex;gap:8px;flex-wrap:wrap;align-items:end;margin-bottom:12px}
 label{display:flex;flex-direction:column;font-size:12px;color:#555}
 input{padding:6px 8px;border:1px solid #ddd;border-radius:8px}
 button,.btn{padding:6px 10px;border:1px solid #ddd;border-radius:8px;cursor:pointer;background:#fff}
 table{border-collapse:collapse;width:100%;max-width:1200px}
 th,td{border:1px solid #e6e6e6;padding:8px}
 th{background:#fafafa;text-align:left}
</style>
<h1>Studies</h1>
<div id="meta"></div>

<div class="row">
  <label>Date from
    <input type="date" id="dateFrom">
  </label>
  <label>Time from
    <input type="time" id="timeFrom" step="1">
  </label>
  <label>Date to
    <input type="date" id="dateTo">
  </label>
  <label>Time to
    <input type="time" id="timeTo" step="1">
  </label>
  <button id="btnApply">Filtra</button>
  <button id="btnReset" type="button">Reset</button>
</div>

<table id="tbl"><thead>
<tr><th>Date</th><th>Time</th><th>Description</th><th>Modalities</th><th>#Series</th><th>#Instances</th><th>Azioni</th></tr>
</thead><tbody></tbody></table>

<script>
const q = new URLSearchParams(location.search);
const pid = q.get('patientId') || '';
const META = document.querySelector('#meta');
META.innerHTML = `<p><b>PatientID:</b> ${esc(pid)}</p>`;

const el = (id)=>document.getElementById(id);

initFromQuery(); // ripristina eventuali filtri da URL
load();

el('btnApply').addEventListener('click', ()=>{
  const url = new URL(location.href);
  url.searchParams.set('patientId', pid);
  setOrDelete(url.searchParams,'dateFrom', el('dateFrom').value);
  setOrDelete(url.searchParams,'dateTo',   el('dateTo').value);
  setOrDelete(url.searchParams,'timeFrom', el('timeFrom').value);
  setOrDelete(url.searchParams,'timeTo',   el('timeTo').value);
  history.replaceState(null,'', url.toString());
  load();
});
el('btnReset').addEventListener('click', ()=>{
  el('dateFrom').value = '';
  el('dateTo').value   = '';
  el('timeFrom').value = '';
  el('timeTo').value   = '';
  const url = new URL(location.href);
  ['dateFrom','dateTo','timeFrom','timeTo'].forEach(k=>url.searchParams.delete(k));
  history.replaceState(null,'', url.toString());
  load();
});

function setOrDelete(sp,k,v){ if(v) sp.set(k,v); else sp.delete(k); }

function initFromQuery(){
  ['dateFrom','dateTo','timeFrom','timeTo'].forEach(k=>{
    const v = q.get(k); if(v) el(k).value = v;
  });
}

function load(){
  if(!pid) return;
  const sp = new URLSearchParams({ patientId: pid, limit: '500', orderby: '-StudyDate,-StudyTime' });
  const dateFrom = el('dateFrom').value, dateTo = el('dateTo').value;
  const timeFrom = el('timeFrom').value, timeTo = el('timeTo').value;
  if(dateFrom) sp.set('dateFrom', dateFrom);
  if(dateTo)   sp.set('dateTo',   dateTo);
  if(timeFrom) sp.set('timeFrom', timeFrom);
  if(timeTo)   sp.set('timeTo',   timeTo);

  fetch(`/api/studies?${sp.toString()}`)
    .then(r => { if(!r.ok) throw new Error('HTTP '+r.status); return r.json(); })
    .then(rows => {
      const TBL = document.querySelector('#tbl tbody');
      TBL.innerHTML = '';
      for(const r of rows){
        const tr=document.createElement('tr');
        tr.innerHTML = `
          <td>${fmtDate(r.studyDate)}</td>
          <td>${fmtTime(r.studyTime)}</td>
          <td>${esc(r.studyDescription||'')}</td>
          <td>${esc(r.modalitiesInStudy||'')}</td>
          <td>${esc(r.numSeries||'')}</td>
          <td>${esc(r.numInstances||'')}</td>
          <td><a class='btn' href='${r.weasis}' target='_blank' rel='noopener'>Weasis</a></td>`;
        TBL.appendChild(tr);
      }
    })
    .catch(err => console.error('studies fetch error', err));
}

function fmtDate(d){ if(!d) return ''; return `${d.slice(0,4)}-${d.slice(4,6)}-${d.slice(6,8)}`; }
function fmtTime(t){ if(!t) return ''; const hh=t.slice(0,2),mm=t.slice(2,4),ss=t.slice(4,6); return `${hh}:${mm}:${ss}`; }
function esc(s){return (s+"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;","\\"":"&quot;","'":"&#39;"}[c]))}
</script>
""";
    }



    // ========================= Helpers =========================
    private boolean corsPreflight(HttpExchange ex) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(ex.getRequestMethod())) {
            Headers h = ex.getResponseHeaders();
            h.set("Access-Control-Allow-Origin", "*");
            h.set("Access-Control-Allow-Methods", "GET, OPTIONS");
            h.set("Access-Control-Allow-Headers", "Content-Type");
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return true;
        }
        return false;
    }

    private void sendJson(HttpExchange ex, Object obj) throws IOException {
        byte[] body = mapper.writeValueAsBytes(obj);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private void sendHtml(HttpExchange ex, String html) throws IOException {
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "text/html; charset=utf-8");
        // niente X-Frame-Options così puoi inglobare via <iframe> nel sito PHP
        ex.sendResponseHeaders(200, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = ("{\"error\":\"" + (msg==null?"":msg.replace("\"","'")) + "\"}").getBytes(StandardCharsets.UTF_8);
        Headers h = ex.getResponseHeaders();
        h.set("Content-Type", "application/json; charset=utf-8");
        h.set("Access-Control-Allow-Origin", "*");
        ex.sendResponseHeaders(code, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }

    private Map<String, String> queryParams(String raw) {
        Map<String, String> m = new LinkedHashMap<>();
        if (raw == null || raw.isBlank()) return m;
        for (String kv : raw.split("&")) {
            int i = kv.indexOf('=');
            if (i >= 0) {
                String k = URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8);
                String v = URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8);
                m.put(k, v);
            } else {
                m.put(URLDecoder.decode(kv, StandardCharsets.UTF_8), "");
            }
        }
        return m;
    }

    private int parseIntOr(String s, int def){ try { return Integer.parseInt(s); } catch(Exception e){ return def; } }

    public static void main(String[] args) throws Exception {
        //String rs = System.getProperty("rs", "http://127.0.0.1:8080/dcm4chee-arc/aets/DCM4CHEE/rs");
        String rs = System.getProperty("rs", "http://95.231.220.172:8080/dcm4chee-arc/aets/DCM4CHEE/rs");
        String aet = System.getProperty("aet", "DCM4CHEE");
        //String host = System.getProperty("host", "127.0.0.1");
        String host = System.getProperty("host", "95.231.220.172");
        String portStr = System.getProperty("rport", "8080");
        int httpPort = Integer.parseInt(System.getProperty("port", "9090"));
        new WebServer(rs, aet, host, portStr).start(httpPort);
    }
}
