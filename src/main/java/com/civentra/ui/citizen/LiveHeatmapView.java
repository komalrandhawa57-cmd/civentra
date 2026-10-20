package com.civentra.ui.citizen;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

// NOTE: Route changed to "heatmap" to prevent server crashes
@Route(value = "citizen/heatmap", layout = CitizenLayout.class)
@PageTitle("Live Heatmap | CIVENTRA")
@RolesAllowed("CITIZEN")
public class LiveHeatmapView extends VerticalLayout {

    public LiveHeatmapView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 header = new H2("Live Heatmap Dashboard");
        header.getStyle().set("margin-top", "0").set("color", "var(--civ-text-main)");
        
        Paragraph info = new Paragraph("Map data is actively being routed through the Community Map.");
        
        add(header, info);
    }
}