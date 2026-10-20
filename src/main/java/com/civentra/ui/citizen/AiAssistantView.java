package com.civentra.ui.citizen;

import com.civentra.ui.MainLayout;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Route(value = "citizen/ai-assistant", layout = CitizenLayout.class)
@PageTitle("AI Assistant | CIVENTRA")
public class AiAssistantView extends VerticalLayout {

    private final VerticalLayout chatMessagesContainer;
    private final TextField inputField;

    public AiAssistantView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Header Title
        HorizontalLayout header = new HorizontalLayout();
        header.setVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.setSpacing(true);
        
        // FIXED COMPILE ERROR: Changed ROBOT to CHAT
        Icon botIcon = VaadinIcon.CHAT.create();
        botIcon.setSize("32px");
        botIcon.setColor("#7C3AED");

        H2 title = new H2("CIVENTRA AI Assistant");
        title.getStyle().set("margin", "0").set("font-size", "1.6rem").set("font-weight", "700");

        header.add(botIcon, title);

        Paragraph subtitle = new Paragraph("Ask me about reporting issues, tracking complaints, repair estimates, or finding local services.");
        subtitle.getStyle().set("color", "var(--lumo-secondary-text-color)").set("margin-top", "0");

        // Quick Suggestion Chips
        HorizontalLayout suggestionChips = new HorizontalLayout();
        suggestionChips.setSpacing(true);
        suggestionChips.setWidthFull();
        suggestionChips.getStyle().set("overflow-x", "auto").set("padding-bottom", "4px");

        suggestionChips.add(
            createChip("Report a pothole", "How do I report a pothole?"),
            createChip("Track my issues", "How can I track my reported issues?"),
            createChip("Book a plumber", "How do I book a plumber?"),
            createChip("Emergency numbers", "What are the emergency numbers?")
        );

        // Chat Container
        chatMessagesContainer = new VerticalLayout();
        chatMessagesContainer.setWidthFull();
        chatMessagesContainer.getStyle()
                .set("flex-grow", "1")
                .set("overflow-y", "auto")
                .set("background", "var(--lumo-contrast-5pct)")
                .set("border-radius", "12px")
                .set("padding", "16px")
                .set("min-height", "300px");

        // Welcome Message from AI
        addAiMessage("Hello! I am your CIVENTRA Smart Assistant. I can help you navigate the portal, report issues, and book services. What can I help you with today?");

        // Input Area
        inputField = new TextField();
        inputField.setPlaceholder("Type your message here...");
        inputField.setWidthFull();
        inputField.addKeyDownListener(Key.ENTER, e -> sendMessage());

        Button sendBtn = new Button("Send", new Icon(VaadinIcon.PAPERPLANE));
        sendBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        sendBtn.getStyle().set("background-color", "#7C3AED");
        sendBtn.addClickListener(e -> sendMessage());

        HorizontalLayout inputLayout = new HorizontalLayout(inputField, sendBtn);
        inputLayout.setWidthFull();
        inputLayout.setFlexGrow(1, inputField);

        add(header, subtitle, suggestionChips, chatMessagesContainer, inputLayout);
        setFlexGrow(1, chatMessagesContainer);
    }

    private Button createChip(String label, String fullQuery) {
        Button chip = new Button(label);
        chip.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_SMALL);
        chip.getStyle()
                .set("background", "var(--lumo-contrast-10pct)")
                .set("border-radius", "16px")
                .set("padding", "6px 14px");
        chip.addClickListener(e -> {
            inputField.setValue(fullQuery);
            sendMessage();
        });
        return chip;
    }

    private void sendMessage() {
        String query = inputField.getValue();
        if (query == null || query.trim().isEmpty()) return;

        // Display User Message
        addUserMessage(query.trim());
        inputField.clear();

        // Generate Offline Smart AI Response
        String response = generateOfflineResponse(query.trim());
        addAiMessage(response);
    }

    private void addUserMessage(String messageText) {
        Div msgCard = new Div();
        msgCard.getStyle()
                .set("align-self", "flex-end")
                .set("background", "#7C3AED")
                .set("color", "white")
                .set("padding", "10px 16px")
                .set("border-radius", "16px 16px 2px 16px")
                .set("max-width", "75%")
                .set("margin-bottom", "8px");

        Span text = new Span(messageText);
        Span timestamp = new Span(LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.getStyle().set("display", "block").set("font-size", "0.7rem").set("opacity", "0.8").set("text-align", "right").set("margin-top", "4px");

        msgCard.add(text, timestamp);
        chatMessagesContainer.add(msgCard);
    }

    private void addAiMessage(String messageText) {
        Div msgCard = new Div();
        msgCard.getStyle()
                .set("align-self", "flex-start")
                .set("background", "var(--lumo-base-color)")
                .set("border", "1px solid var(--lumo-contrast-15pct)")
                .set("color", "var(--lumo-body-text-color)")
                .set("padding", "12px 18px")
                .set("border-radius", "16px 16px 16px 2px")
                .set("max-width", "80%")
                .set("margin-bottom", "8px");

        Span text = new Span(messageText);
        text.getStyle().set("white-space", "pre-line");

        Span timestamp = new Span("CIVENTRA AI • " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
        timestamp.getStyle().set("display", "block").set("font-size", "0.7rem").set("color", "var(--lumo-secondary-text-color)").set("margin-top", "6px");

        msgCard.add(text, timestamp);
        chatMessagesContainer.add(msgCard);
    }

    private String generateOfflineResponse(String input) {
        String text = input.toLowerCase();

        if (text.contains("pothole") || text.contains("road") || text.contains("report")) {
            return "📌 To report an issue like a pothole:\n1. Click 'Report Issue' in the side menu.\n2. Fill in the location and category.\n3. Upload a photo and click Submit. Our officers will review it within 24 hours.";
        } else if (text.contains("track") || text.contains("status")) {
            return "📋 You can track your reports in the 'My Issues' tab. The statuses are:\n• REPORTED: Received by system\n• IN_PROGRESS: Dispatched to crew\n• RESOLVED: Issue fixed";
        } else if (text.contains("book") || text.contains("plumb") || text.contains("electric") || text.contains("service")) {
            return "🛠️ Need a local technician?\n1. Go to 'Find Services'.\n2. Browse verified plumbers, electricians, etc.\n3. Click 'Book Service' and enter your requirement.";
        } else if (text.contains("emergency") || text.contains("number")) {
            return "🚨 Municipal Emergency Numbers:\n• Control Room: 1800-11-2026\n• Water Leak: 1916\n• Power Outage: 1912";
        } else {
            return "I am your offline virtual assistant. I can help you with:\n- How to report civic issues\n- Booking service providers\n- Tracking complaints\n- Emergency contacts\nTry asking me one of these topics!";
        }
    }
}