package com.inin.aiinterviewer.ui.component;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.domain.model.Message;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public class InterviewTranscriptView extends ScrollPane {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final PseudoClass TARGET = PseudoClass.getPseudoClass("target");

    private final VBox messageContainer = new VBox(14);
    private final Map<Integer, Node> questionNodes = new LinkedHashMap<>();

    private List<InterviewMessageDto> messages = List.of();
    private LongConsumer citationHandler;
    private String emptyMessage = "本次面试尚未产生问答记录。";
    private Label streamingContent;

    public InterviewTranscriptView() {
        getStyleClass().add("interview-transcript");
        messageContainer.getStyleClass().add("interview-transcript-content");
        setContent(messageContainer);
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
        setFocusTraversable(true);
        render();
    }

    public void setMessages(List<InterviewMessageDto> messages) {
        this.messages = messages == null ? List.of() : List.copyOf(messages);
        render();
        scrollToBottom();
    }

    public List<InterviewMessageDto> getMessages() {
        return messages;
    }

    public void setEmptyMessage(String emptyMessage) {
        this.emptyMessage = emptyMessage == null || emptyMessage.isBlank()
                ? "本次面试尚未产生问答记录。" : emptyMessage.strip();
        if (messages.isEmpty() && streamingContent == null) render();
    }

    public void setCitationHandler(LongConsumer citationHandler) {
        this.citationHandler = citationHandler;
        render();
    }

    public void beginAssistantStream(int questionNumber) {
        if (questionNumber <= 0) throw new IllegalArgumentException("Question number must be positive");
        removeEmptyState();
        MessageCard card = createCard(new InterviewMessageDto(
                Integer.MAX_VALUE, Message.Role.ASSISTANT, "", LocalDateTime.now(), false, List.of()),
                questionNumber);
        streamingContent = card.content();
        questionNodes.put(questionNumber, card.root());
        messageContainer.getChildren().add(card.root());
        scrollToBottom();
    }

    public void appendPendingUserAnswer(String answer) {
        if (answer == null || answer.isBlank()) return;
        removeEmptyState();
        MessageCard card = createCard(new InterviewMessageDto(
                Integer.MAX_VALUE - 1, Message.Role.USER, answer.strip(),
                LocalDateTime.now(), false, List.of()), questionNodes.size());
        messageContainer.getChildren().add(card.root());
        scrollToBottom();
    }

    public void appendAssistantChunk(String chunk) {
        if (streamingContent == null || chunk == null || chunk.isEmpty()) return;
        streamingContent.setText(streamingContent.getText() + chunk);
        scrollToBottom();
    }

    public void scrollToBottom() {
        Platform.runLater(() -> setVvalue(1));
    }

    public void scrollToQuestion(int questionNumber) {
        Platform.runLater(() -> {
            applyCss();
            layout();
            Platform.runLater(() -> positionAtQuestion(questionNumber));
        });
    }

    private void positionAtQuestion(int questionNumber) {
        Node target = questionNodes.get(questionNumber);
        if (target == null) return;
        questionNodes.values().forEach(node -> node.pseudoClassStateChanged(TARGET, false));
        target.pseudoClassStateChanged(TARGET, true);
        double contentHeight = messageContainer.getBoundsInLocal().getHeight();
        double viewportHeight = getViewportBounds().getHeight();
        double scrollable = Math.max(1, contentHeight - viewportHeight);
        setVvalue(Math.max(0, Math.min(1, target.getBoundsInParent().getMinY() / scrollable)));
        target.requestFocus();
    }

    public int getQuestionCount() {
        return questionNodes.size();
    }

    private void render() {
        messageContainer.getChildren().clear();
        questionNodes.clear();
        streamingContent = null;
        if (messages.isEmpty()) {
            Label empty = new Label(emptyMessage);
            empty.setWrapText(true);
            empty.getStyleClass().add("transcript-empty");
            messageContainer.getChildren().add(empty);
            return;
        }
        int questionNumber = 0;
        for (InterviewMessageDto message : messages) {
            if (message.role() == Message.Role.ASSISTANT) questionNumber++;
            MessageCard card = createCard(message, questionNumber);
            messageContainer.getChildren().add(card.root());
            if (message.role() == Message.Role.ASSISTANT) {
                questionNodes.put(questionNumber, card.root());
            }
        }
    }

    private MessageCard createCard(InterviewMessageDto message, int questionNumber) {
        boolean assistant = message.role() == Message.Role.ASSISTANT;
        Label marker = new Label(assistant ? "Q" + questionNumber : "A");
        marker.getStyleClass().addAll("transcript-marker",
                assistant ? "question-marker" : "answer-marker");

        Label author = new Label(assistant ? "AI 面试官" : "你的回答");
        author.getStyleClass().add("transcript-author");
        Label time = new Label(message.createTime() == null ? "" : TIME_FORMAT.format(message.createTime()));
        time.getStyleClass().add("transcript-time");
        HBox header = new HBox(8, author, time);
        header.setAlignment(Pos.CENTER_LEFT);
        if (message.partial()) {
            Label partial = new Label("输出中断 · 已保存");
            partial.getStyleClass().add("transcript-partial");
            header.getChildren().add(partial);
        }

        Label content = new Label(message.content());
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMinHeight(Label.USE_PREF_SIZE);
        content.getStyleClass().add("transcript-message-text");

        VBox body = new VBox(7, header, content);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setFocusTraversable(assistant);
        body.setAccessibleText((assistant ? "第 " + questionNumber + " 题：" : "回答：") + message.content());
        body.getStyleClass().addAll("transcript-message-card",
                assistant ? "question-message-card" : "answer-message-card");
        HBox.setHgrow(body, Priority.ALWAYS);
        if (!message.citations().isEmpty()) {
            body.getChildren().add(citationRow(message.citations()));
        }

        HBox row = new HBox(12, marker, body);
        row.setAlignment(Pos.TOP_LEFT);
        row.setFocusTraversable(assistant);
        row.setAccessibleText(body.getAccessibleText());
        row.getStyleClass().add("transcript-message-row");
        return new MessageCard(row, content);
    }

    private FlowPane citationRow(List<KnowledgeCitationDto> citations) {
        FlowPane row = new FlowPane(7, 6);
        row.getStyleClass().add("transcript-citations");
        Label label = new Label("依据");
        label.getStyleClass().add("transcript-citation-label");
        row.getChildren().add(label);
        for (KnowledgeCitationDto citation : citations) {
            String text = citation.documentName() + " · 片段 " + (citation.chunkIndex() + 1);
            if (citationHandler == null) {
                Label source = new Label(text);
                source.getStyleClass().add("transcript-citation-chip");
                row.getChildren().add(source);
            } else {
                Button source = new Button(text);
                source.getStyleClass().add("transcript-citation-chip");
                source.setAccessibleText("查看来源文档 " + citation.documentName());
                source.setTooltip(new Tooltip(citation.excerpt()));
                source.setOnAction(event -> citationHandler.accept(citation.documentId()));
                row.getChildren().add(source);
            }
        }
        return row;
    }

    private void removeEmptyState() {
        if (messageContainer.getChildren().size() == 1
                && messageContainer.getChildren().getFirst().getStyleClass().contains("transcript-empty")) {
            messageContainer.getChildren().clear();
        }
    }

    private record MessageCard(HBox root, Label content) {
    }
}
