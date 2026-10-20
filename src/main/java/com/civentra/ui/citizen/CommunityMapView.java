package com.civentra.ui.citizen;

import com.civentra.dao.IssueDAO;
import com.civentra.model.CommunityIssue;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route(value = "citizen/community-map", layout = CitizenLayout.class)
@PageTitle("Community Map | Civentra")
public class CommunityMapView extends VerticalLayout {

    private final IssueDAO issueDAO;
    private final Div mapContainer;
    private final ComboBox<String> statusFilter;
    private List<CommunityIssue> allIssues = new ArrayList<>();

    private final Span totalCountBadge = new Span("0 Issues");
    private final Span reportedBadge = new Span("0 Reported");
    private final Span assignedBadge = new Span("0 Assigned");
    private final Span solvedBadge = new Span("0 Solved");

    @Autowired
    public CommunityMapView(IssueDAO issueDAO) {
        this.issueDAO = issueDAO;

        setSizeFull();
        setPadding(true);
        setSpacing(true);
        getStyle().set("background-color", "#f8fafc");

        // Header Section
        H2 title = new H2("Community Issue Map");
        title.getStyle().set("margin", "0").set("color", "#0f172a").set("font-weight", "700");

        Paragraph description = new Paragraph("View real-time citizen-reported issues and resolution status across your area.");
        description.getStyle().set("margin-top", "4px").set("color", "#64748b");

        VerticalLayout headerText = new VerticalLayout(title, description);
        headerText.setPadding(false);
        headerText.setSpacing(false);

        // Status badges bar
        setupBadges();
        HorizontalLayout badgesLayout = new HorizontalLayout(totalCountBadge, reportedBadge, assignedBadge, solvedBadge);
        badgesLayout.setSpacing(true);

        // Filter Bar
        statusFilter = new ComboBox<>("Filter by Status");
        statusFilter.setItems("All", "REPORTED", "ASSIGNED", "SOLVED");
        statusFilter.setValue("All");
        statusFilter.setWidth("220px");
        statusFilter.addValueChangeListener(e -> refreshMapMarkers());

        HorizontalLayout topControls = new HorizontalLayout(headerText, statusFilter);
        topControls.setWidthFull();
        topControls.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        topControls.setAlignItems(FlexComponent.Alignment.END);

        // Map Div Container
        mapContainer = new Div();
        mapContainer.setId("community-map-container");
        mapContainer.setWidthFull();
        mapContainer.setHeight("600px");
        mapContainer.getStyle()
                .set("border-radius", "12px")
                .set("box-shadow", "0 4px 12px rgba(0,0,0,0.08)")
                .set("border", "1px solid #e2e8f0")
                .set("overflow", "hidden");

        add(topControls, badgesLayout, mapContainer);
        loadIssues();
    }

    private void setupBadges() {
        styleBadge(totalCountBadge, "#3b82f6", "#eff6ff");
        styleBadge(reportedBadge, "#ef4444", "#fef2f2");
        styleBadge(assignedBadge, "#f59e0b", "#fffbeb");
        styleBadge(solvedBadge, "#10b981", "#ecfdf5");
    }

    private void styleBadge(Span badge, String color, String bgColor) {
        badge.getStyle()
                .set("background-color", bgColor)
                .set("color", color)
                .set("font-weight", "600")
                .set("font-size", "0.875rem")
                .set("padding", "6px 14px")
                .set("border-radius", "20px")
                .set("border", "1px solid " + color + "33");
    }

    private void loadIssues() {
        try {
            allIssues = issueDAO.findAll();
        } catch (Exception e) {
            allIssues = new ArrayList<>();
        }

        if (allIssues == null || allIssues.isEmpty() || allIssues.stream().noneMatch(CommunityIssue::hasCoordinates)) {
            allIssues = generateDynamicDemoIssues();
        }

        updateSummaryBadges();
    }

    private List<CommunityIssue> generateDynamicDemoIssues() {
        List<CommunityIssue> list = new ArrayList<>();

        CommunityIssue issue1 = new CommunityIssue();
        issue1.setTitle("Pothole on Main Street");
        issue1.setCategoryName("Road Repair");
        issue1.setStatus("REPORTED");
        issue1.setLatitude(28.6139);
        issue1.setLongitude(77.2090);
        issue1.setLocationAddress("Connaught Place, New Delhi");
        list.add(issue1);

        CommunityIssue issue2 = new CommunityIssue();
        issue2.setTitle("Broken Streetlight near Park");
        issue2.setCategoryName("Electrical");
        issue2.setStatus("ASSIGNED");
        issue2.setLatitude(19.0760);
        issue2.setLongitude(72.8777);
        issue2.setLocationAddress("Bandra West, Mumbai");
        list.add(issue2);

        CommunityIssue issue3 = new CommunityIssue();
        issue3.setTitle("Water Pipeline Leak Fixed");
        issue3.setCategoryName("Water Supply");
        issue3.setStatus("SOLVED");
        issue3.setLatitude(22.5726);
        issue3.setLongitude(88.3639);
        issue3.setLocationAddress("Park Street, Kolkata");
        list.add(issue3);

        return list;
    }

    private void updateSummaryBadges() {
        long reported = allIssues.stream().filter(i -> i.getStatusAsString().equalsIgnoreCase("REPORTED")).count();
        long assigned = allIssues.stream().filter(i -> i.getStatusAsString().equalsIgnoreCase("ASSIGNED") || i.getStatusAsString().equalsIgnoreCase("IN_PROGRESS")).count();
        long solved = allIssues.stream().filter(i -> i.getStatusAsString().equalsIgnoreCase("SOLVED") || i.getStatusAsString().equalsIgnoreCase("RESOLVED")).count();

        totalCountBadge.setText(allIssues.size() + " Total Issues");
        reportedBadge.setText(reported + " Reported");
        assignedBadge.setText(assigned + " Assigned");
        solvedBadge.setText(solved + " Solved");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        loadLeafletAndRenderMap();
    }

    private void loadLeafletAndRenderMap() {
        UI ui = getUI().orElse(UI.getCurrent());
        if (ui == null) return;

        ui.getPage().addStyleSheet("https://unpkg.com/leaflet@1.9.4/dist/leaflet.css");
        ui.getPage().addJavaScript("https://unpkg.com/leaflet@1.9.4/dist/leaflet.js");

        ui.getPage().executeJs(
                "setTimeout(function() {" +
                "  var container = document.getElementById('community-map-container');" +
                "  if (!container) return;" +
                "  if (window.civentraMap) { window.civentraMap.remove(); }" +
                "  var map = L.map('community-map-container').setView([20.5937, 78.9629], 5);" +
                "  window.civentraMap = map;" +
                "  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {" +
                "    maxZoom: 19," +
                "    attribution: '&copy; Civentra Community Map'" +
                "  }).addTo(map);" +
                "  window.civentraMarkers = [];" +
                "  $0.$server.renderMapMarkers();" +
                "}, 400);",
                getElement()
        );
    }

    private void refreshMapMarkers() {
        getUI().ifPresent(ui -> ui.getPage().executeJs("$0.$server.renderMapMarkers();", getElement()));
    }

    @com.vaadin.flow.component.ClientCallable
    public void renderMapMarkers() {
        UI ui = getUI().orElse(UI.getCurrent());
        if (ui == null) return;

        String selectedFilter = statusFilter.getValue();

        StringBuilder js = new StringBuilder();
        js.append("if (window.civentraMarkers) { window.civentraMarkers.forEach(m => m.remove()); } ");
        js.append("window.civentraMarkers = []; ");

        for (CommunityIssue issue : allIssues) {
            if (!issue.hasCoordinates()) continue;

            String status = issue.getStatusAsString().toUpperCase();
            if (!"All".equals(selectedFilter) && !status.contains(selectedFilter)) {
                continue;
            }

            String color = "#ef4444"; // Red for Reported
            if (status.contains("ASSIGN") || status.contains("PROGRESS")) {
                color = "#f59e0b"; // Orange for Assigned
            } else if (status.contains("SOLV") || status.contains("RESOLV")) {
                color = "#10b981"; // Green for Solved
            }

            String popupHtml = String.format(
                    "<div style='font-family: sans-serif; padding: 4px;'>" +
                    "<strong style='font-size: 14px; color: #0f172a;'>%s</strong><br/>" +
                    "<span style='color: #64748b; font-size: 12px;'>Category: %s</span><br/>" +
                    "<span style='display: inline-block; margin-top: 6px; padding: 2px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; background-color: %s22; color: %s;'>%s</span>" +
                    "</div>",
                    escapeJs(issue.getTitle()),
                    escapeJs(issue.getCategoryName() != null ? issue.getCategoryName() : "General"),
                    color, color, status
            );

            js.append(String.format(
                    "var circle = L.circleMarker([%f, %f], {" +
                    "  color: '%s'," +
                    "  fillColor: '%s'," +
                    "  fillOpacity: 0.8," +
                    "  radius: 10" +
                    "}).bindPopup(\"%s\").addTo(window.civentraMap); ",
                    issue.getLatitudeAsDouble(), issue.getLongitudeAsDouble(), color, color, popupHtml
            ));
            js.append("window.civentraMarkers.push(circle); ");
        }

        ui.getPage().executeJs(js.toString());
    }

    private String escapeJs(String input) {
        if (input == null) return "";
        return input.replace("'", "\\'").replace("\"", "\\\"");
    }
}