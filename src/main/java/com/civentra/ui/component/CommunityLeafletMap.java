package com.civentra.ui.component;

import com.civentra.model.CommunityIssue;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.ClientCallable;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.UUID;

@StyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css")
@JavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js")
public class CommunityLeafletMap extends Div {

    private final String mapId;
    private final String tileUrl;
    private final String attribution;
    private final Map<Long, CommunityIssue> visibleIssues = new LinkedHashMap<>();
    private Consumer<CommunityIssue> markerClickListener;

    public CommunityLeafletMap() {
        this("https://tile.openstreetmap.org/{z}/{x}/{y}.png", "© OpenStreetMap contributors");
    }

    public CommunityLeafletMap(String tileUrl, String attribution) {
        this.tileUrl = tileUrl;
        this.attribution = attribution;
        this.mapId = "map-" + UUID.randomUUID().toString().replace("-", "");
        setId(mapId);
        setWidthFull();
        setHeight("400px");
        getStyle()
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(46,16,101,0.12)")
                .set("overflow", "hidden")
                .set("z-index", "0");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        initMap();
    }

    private void initMap() {
        String js = "setTimeout(function() {"
                + "  try {"
                + "    if (window['mymap_' + $0]) { window['mymap_' + $0].remove(); }"
                + "    var el = document.getElementById($0);"
                + "    if (!el) return;"
                + "    var map = L.map($0).setView([20.5937, 78.9629], 5);"
                + "    L.tileLayer($1, {"
                + "      attribution: $2"
                + "    }).addTo(map);"
                + "    window['mymap_' + $0] = map;"
                + "    setTimeout(function(){ map.invalidateSize(); }, 300);"
                + "  } catch(e) { console.error(e); }"
                + "}, 400);";
        UI.getCurrent().getPage().executeJs(js, mapId, tileUrl, attribution);
    }

    public void addMarkers(List<CommunityIssue> issues) {
        if (issues == null || issues.isEmpty()) {
            UI.getCurrent().getPage().executeJs("setTimeout(function(){ var map = window['mymap_' + $0]; if(!map) return; "
                    + "if(window['markers_' + $0]) window['markers_' + $0].forEach(function(m){map.removeLayer(m);}); "
                    + "window['markers_' + $0]=[]; }, 700);", mapId);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("setTimeout(function() {");
        sb.append("  var map = window['mymap_' + $0];");
        sb.append("  if (!map) return;");
        sb.append(
                "  if (window['markers_' + $0]) { window['markers_' + $0].forEach(function(m){ map.removeLayer(m); }); }");
        sb.append("  window['markers_' + $0] = [];");

        boolean first = true;
        for (CommunityIssue issue : issues) {
            double lat = issue.getLatitudeAsDouble();
            double lng = issue.getLongitudeAsDouble();
            if (!issue.hasCoordinates()) {
                continue;
            }
            if (issue.getIssueId() == null) continue;
            visibleIssues.put(issue.getIssueId(), issue);
            String title = safe(issue.getTitle());
            String status = safe(issue.getStatusAsString());
            String priority = safe(issue.getPriorityAsString());

            sb.append("  (function(){");
            sb.append("    var mk = L.marker([").append(lat).append(",").append(lng).append("]).addTo(map);");
            sb.append("    mk.bindPopup('<b>").append(title).append("</b><br>Status: ")
                    .append(status).append("<br>Priority: ").append(priority).append("');");
            sb.append("    mk.on('click', function(){ var el = document.getElementById($0); if (el && el.$server) el.$server.markerClicked(")
                    .append(issue.getIssueId()).append("); });");
            sb.append("    window['markers_' + $0].push(mk);");
            sb.append("  })();");

            if (first) {
                sb.append("  map.setView([").append(lat).append(",").append(lng).append("], 12);");
                first = false;
            }
        }
        sb.append("  setTimeout(function(){ map.invalidateSize(); }, 200);");
        sb.append("}, 700);");

        UI.getCurrent().getPage().executeJs(sb.toString(), mapId);
    }

    public void setIssues(List<CommunityIssue> issues) {
        visibleIssues.clear();
        addMarkers(issues);
    }

    public void setMarkerClickListener(Consumer<CommunityIssue> listener) {
        this.markerClickListener = listener;
    }

    @ClientCallable
    private void markerClicked(Long issueId) {
        CommunityIssue issue = visibleIssues.get(issueId);
        if (issue != null && markerClickListener != null) markerClickListener.accept(issue);
    }

    private static String safe(String s) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;")
                .replace("\\", "\\\\")
                .replace("'", "\\'")
                .replace("\"", "&quot;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\n", " ")
                .replace("\r", " ");
    }
}
