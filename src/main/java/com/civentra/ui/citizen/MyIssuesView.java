package com.civentra.ui.citizen;

import com.civentra.model.CommunityIssue;
import com.civentra.model.IssueStatusHistory;
import com.civentra.security.AuthenticatedUser;
import com.civentra.service.IssueService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Route(value = "citizen/my-issues", layout = CitizenLayout.class)
@PageTitle("My Issues | CIVENTRA")
@RolesAllowed("CITIZEN")
public class MyIssuesView extends VerticalLayout {

    private final IssueService issueService;
    private final AuthenticatedUser authenticatedUser;
    private final Grid<CommunityIssue> grid = new Grid<>(CommunityIssue.class, false);
    private final TextField search = new TextField();
    private final ComboBox<String> statusFilter = new ComboBox<>();
    private final ComboBox<String> categoryFilter = new ComboBox<>();
    private final DatePicker fromDate = new DatePicker("Reported from");
    private final DatePicker toDate = new DatePicker("Reported to");
    private final Paragraph emptyState = new Paragraph("No reports match these filters.");
    private List<CommunityIssue> allIssues = List.of();

    public MyIssuesView(IssueService issueService, AuthenticatedUser authenticatedUser) {
        this.issueService = issueService;
        this.authenticatedUser = authenticatedUser;

        setSizeFull();
        H2 header = new H2("My Reported Issues");
        header.getStyle().set("color", "#3B0764");

        search.setPlaceholder("Search title, category, or location");
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setValueChangeTimeout(250);
        statusFilter.setPlaceholder("All statuses");
        statusFilter.setClearButtonVisible(true);
        categoryFilter.setPlaceholder("All categories");
        categoryFilter.setClearButtonVisible(true);
        fromDate.setClearButtonVisible(true);
        toDate.setClearButtonVisible(true);
        search.addValueChangeListener(event -> applyFilters());
        statusFilter.addValueChangeListener(event -> applyFilters());
        categoryFilter.addValueChangeListener(event -> applyFilters());
        fromDate.addValueChangeListener(event -> applyFilters());
        toDate.addValueChangeListener(event -> applyFilters());
        Div filters = new Div(search, statusFilter, categoryFilter, fromDate, toDate);
        filters.addClassName("civ-issue-filters");
        emptyState.addClassName("civ-empty-state");
        filters.getStyle().set("display", "flex").set("gap", "12px").set("flex-wrap", "wrap");

        configureGrid();
        add(header, filters, grid, emptyState);
        loadIssues();
    }

    private void configureGrid() {
        grid.addColumn(CommunityIssue::getTitle).setHeader("Title").setAutoWidth(true).setSortable(true)
                .setComparator(issue -> safe(issue.getTitle()));
        grid.addColumn(issue -> categoryLabel(issue.getCategoryName())).setHeader("Category").setAutoWidth(true).setSortable(true)
                .setComparator(issue -> categoryLabel(issue.getCategoryName()));
        grid.addComponentColumn(this::createStatusBadge).setHeader("Status").setAutoWidth(true).setSortable(true)
                .setComparator(issue -> issue.getStatusAsString());

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
        grid.addColumn(issue -> issue.getReportedAt() != null ? issue.getReportedAt().format(formatter) : "N/A")
                .setHeader("Reported On").setAutoWidth(true).setSortable(true)
                .setComparator((a, b) -> Comparator.nullsLast(Comparator.<LocalDateTime>naturalOrder())
                        .compare(a.getReportedAt(), b.getReportedAt()));
        grid.addComponentColumn(issue -> new Button("Details & history", event -> showDetails(issue)))
                .setHeader("Details").setAutoWidth(true);

        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
    }

    private Span createStatusBadge(CommunityIssue issue) {
        Span badge = new Span(issue.getStatusAsString());
        badge.getStyle().set("padding", "3px 8px").set("border-radius", "12px")
                .set("font-size", "0.85em").set("color", "white").set("font-weight", "bold");

        switch (issue.getStatusAsString()) {
            case "REPORTED":
                badge.getStyle().set("background-color", "#6D28D9");
                break;
            case "RESOLVED":
                badge.getStyle().set("background-color", "#16A34A");
                break;
            case "IN_PROGRESS":
                badge.getStyle().set("background-color", "#FACC15").set("color", "#000");
                break;
            default:
                badge.getStyle().set("background-color", "#6B7280");
                break;
        }
        return badge;
    }

    private void loadIssues() {
        if (authenticatedUser.getUserId() != null) {
            allIssues = issueService.getIssuesByCitizen(authenticatedUser.getUserId());
            statusFilter.setItems(allIssues.stream().map(CommunityIssue::getStatusAsString).distinct().sorted().toList());
            categoryFilter.setItems(allIssues.stream().map(issue -> categoryLabel(issue.getCategoryName())).distinct().sorted().toList());
            Object savedSearch = VaadinSession.getCurrent().getAttribute("issueSearchQuery");
            if (savedSearch instanceof String query) {
                search.setValue(query);
                VaadinSession.getCurrent().setAttribute("issueSearchQuery", null);
            }
            applyFilters();
        } else {
            allIssues = List.of();
            grid.setItems(allIssues);
        }
    }

    private void applyFilters() {
        String query = search.getValue() == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        String status = statusFilter.getValue();
        String category = categoryFilter.getValue();
        LocalDate from = fromDate.getValue();
        LocalDate to = toDate.getValue();
        if (from != null && to != null && from.isAfter(to)) {
            Notification.show("The start date must be on or before the end date.")
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        List<CommunityIssue> filtered = filterIssues(allIssues, query, status, category, from, to);
        grid.setItems(filtered);
        emptyState.setVisible(filtered.isEmpty());
    }

    static List<CommunityIssue> filterIssues(List<CommunityIssue> issues, String query, String status,
                                             String category, LocalDate from, LocalDate to) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        LocalDateTime fromInclusive = from == null ? null : from.atStartOfDay();
        LocalDateTime toExclusive = to == null ? null : to.plusDays(1).atStartOfDay();
        return issues.stream()
                .filter(issue -> status == null || status.equalsIgnoreCase(issue.getStatusAsString()))
                .filter(issue -> category == null || category.equalsIgnoreCase(categoryLabel(issue.getCategoryName())))
                .filter(issue -> normalizedQuery.isBlank()
                        || contains(issue.getTitle(), normalizedQuery)
                        || contains(issue.getCategoryName(), normalizedQuery)
                        || contains(issue.getLocationAddress(), normalizedQuery))
                .filter(issue -> issue.getReportedAt() == null
                        ? fromInclusive == null && toExclusive == null
                        : (fromInclusive == null || !issue.getReportedAt().isBefore(fromInclusive))
                        && (toExclusive == null || issue.getReportedAt().isBefore(toExclusive)))
                .sorted(Comparator.comparing(CommunityIssue::getReportedAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private static String categoryLabel(String value) { return value == null || value.isBlank() ? "Other" : value; }

    private static String safe(String value) { return value == null ? "" : value; }

    private static boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private void showDetails(CommunityIssue issue) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(issue.getTitle());
        dialog.setWidth("min(680px, 95vw)");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.add(new Paragraph(issue.getDescription() == null ? "No description provided." : issue.getDescription()));
        content.add(new Paragraph("Category: " + (issue.getCategoryName() == null ? "Uncategorized" : issue.getCategoryName())));
        content.add(new Paragraph("Status: " + issue.getStatusAsString()));
        content.add(new Paragraph("Location: " + (issue.getLocationAddress() == null ? "Not provided" : issue.getLocationAddress())));
        if (issue.getEvidencePath() != null && !issue.getEvidencePath().isBlank()) {
            Image evidence = new Image(issue.getEvidencePath(), "Issue evidence");
            evidence.setMaxWidth("100%");
            evidence.setMaxHeight("320px");
            content.add(evidence);
        }

        content.add(new H3("Status history"));
        List<IssueStatusHistory> history = issueService.getStatusHistoryForCitizen(issue.getIssueId(), authenticatedUser.getUserId());
        if (history.isEmpty()) {
            content.add(new Paragraph("No status changes have been recorded yet."));
        } else {
            DateTimeFormatter format = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            for (IssueStatusHistory change : history) {
                String date = change.getChangedAt() == null ? "" : " · " + change.getChangedAt().format(format);
                String actor = change.getChangedByName() == null ? "" : " · by " + change.getChangedByName();
                String notes = change.getNotes() == null || change.getNotes().isBlank() ? "" : " — " + change.getNotes();
                content.add(new Paragraph(change.getFromStatus() + " → " + change.getToStatus() + date + actor + notes));
            }
        }
        Button close = new Button("Close", event -> dialog.close());
        dialog.add(content);
        dialog.getFooter().add(close);
        dialog.open();
    }
}
