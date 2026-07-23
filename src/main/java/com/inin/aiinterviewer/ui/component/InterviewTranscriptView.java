package com.inin.aiinterviewer.ui.component;

import com.inin.aiinterviewer.application.dto.InterviewMessageDto;
import com.inin.aiinterviewer.application.dto.KnowledgeCitationDto;
import com.inin.aiinterviewer.domain.model.Message;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;

public class InterviewTranscriptView extends ScrollPane {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final PseudoClass TARGET = PseudoClass.getPseudoClass("target");
    private static final Image AI_AVATAR = new Image(
            InterviewTranscriptView.class.getResourceAsStream(
                    "/images/interview/interview-ai-avatar-v2.png"));

    private final VBox messageContainer = new VBox();
    private final Map<Integer, Node> questionNodes = new LinkedHashMap<>();

    private List<InterviewMessageDto> messages = List.of();
    private LongConsumer citationHandler;
    private String emptyMessage = "本次面试尚未产生问答记录。";
    private Label streamingContent;
    private boolean streamingHasContent;

    public InterviewTranscriptView() {
        getStyleClass().add("interview-transcript");
        messageContainer.getStyleClass().add("interview-transcript-content");
        messageContainer.setFillWidth(true);
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
                Integer.MAX_VALUE, Message.Role.ASSISTANT, "", LocalDateTime.now(ZoneOffset.UTC), false, List.of()),
                questionNumber, true);
        streamingContent = card.content();
        streamingHasContent = false;
        questionNodes.put(questionNumber, card.root());
        messageContainer.getChildren().add(card.root());
        scrollToBottom();
    }

    public void appendPendingUserAnswer(String answer) {
        if (answer == null || answer.isBlank()) return;
        removeEmptyState();
        MessageCard card = createCard(new InterviewMessageDto(
                Integer.MAX_VALUE - 1, Message.Role.USER, answer.strip(),
                LocalDateTime.now(ZoneOffset.UTC), false, List.of()), questionNodes.size(), false);
        messageContainer.getChildren().add(card.root());
        scrollToBottom();
    }

    public void appendAssistantChunk(String chunk) {
        if (streamingContent == null || chunk == null || chunk.isEmpty()) return;
        if (!streamingHasContent) {
            streamingContent.setText(chunk);
            streamingHasContent = true;
        } else {
            streamingContent.setText(streamingContent.getText() + chunk);
        }
        scrollToBottom();
    }

    public void scrollToBottom() {
        Platform.runLater(() -> {
            applyCss();
            layout();
            Platform.runLater(() -> setVvalue(1.0));
        });
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
        streamingHasContent = false;
        if (messages.isEmpty()) {
            VBox empty = new VBox(8);
            empty.setAlignment(Pos.CENTER);
            empty.getStyleClass().add("transcript-empty");
            FontIcon icon = new FontIcon("mdi2m-message-processing-outline");
            icon.setIconSize(28);
            Label title = new Label("等待第一道面试问题");
            title.getStyleClass().add("transcript-empty-title");
            Label description = new Label(emptyMessage);
            description.setWrapText(true);
            description.getStyleClass().add("transcript-empty-copy");
            empty.getChildren().addAll(icon, title, description);
            messageContainer.getChildren().add(empty);
            return;
        }
        int questionNumber = 0;
        for (InterviewMessageDto message : messages) {
            if (message.role() == Message.Role.ASSISTANT) questionNumber++;
            MessageCard card = createCard(message, questionNumber, false);
            messageContainer.getChildren().add(card.root());
            if (message.role() == Message.Role.ASSISTANT) {
                questionNodes.put(questionNumber, card.root());
            }
        }
    }

    private MessageCard createCard(InterviewMessageDto message, int questionNumber, boolean streaming) {
        boolean assistant = message.role() == Message.Role.ASSISTANT;
        Node avatar = assistant ? assistantAvatar() : userAvatar();

        Label author = new Label(assistant ? "AI 面试官" : "我");
        author.getStyleClass().add("transcript-author");
        Label time = new Label(formatLocalTime(message.createTime()));
        time.getStyleClass().add("transcript-time");
        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);
        Button copyButton = iconButton("mdi2c-content-copy", "复制本条内容");
        copyButton.setOnAction(event -> copyText(message.content()));
        Button moreButton = iconButton("mdi2d-dots-horizontal", "更多操作");
        moreButton.setOnAction(event -> showMessageMenu(moreButton, message.content()));
        HBox header = new HBox(10, author, time, headerSpacer, copyButton, moreButton);
        header.setAlignment(Pos.CENTER_LEFT);
        header.getStyleClass().add("transcript-message-header");
        if (message.partial()) {
            Label partial = new Label("输出中断 · 已保存");
            partial.getStyleClass().add("transcript-partial");
            header.getChildren().add(2, partial);
        }

        Label content = new Label(message.content());
        content.setWrapText(true);
        content.setMaxWidth(Double.MAX_VALUE);
        content.setMinHeight(Label.USE_PREF_SIZE);
        content.getStyleClass().add("transcript-message-text");

        VBox body = new VBox(6, header);
        body.setMaxWidth(Double.MAX_VALUE);
        body.setFocusTraversable(assistant);
        body.setAccessibleText((assistant ? "第 " + questionNumber + " 题：" : "回答：") + message.content());
        body.getStyleClass().addAll("transcript-message-body", "transcript-message-card");
        HBox.setHgrow(body, Priority.ALWAYS);

        if (streaming) {
            ProgressIndicator spinner = new ProgressIndicator();
            spinner.setMinSize(18, 18);
            spinner.setPrefSize(18, 18);
            spinner.setMaxSize(18, 18);
            Label thinking = new Label("正在生成追问");
            thinking.getStyleClass().add("transcript-streaming-title");
            Label dots = new Label("·  ·  ·");
            dots.getStyleClass().add("transcript-streaming-dots");
            HBox thinkingRow = new HBox(10, spinner, thinking, dots);
            thinkingRow.setAlignment(Pos.CENTER_LEFT);
            content.setText("基于你的回答，我正在思考如何深入追问…");
            HBox streamDetail = new HBox(content);
            streamDetail.setAlignment(Pos.CENTER_LEFT);
            HBox.setHgrow(content, Priority.ALWAYS);
            Button collapse = new Button("收起思考过程⌃");
            collapse.getStyleClass().add("transcript-collapse-button");
            collapse.setOnAction(event -> {
                boolean visible = content.isVisible();
                content.setVisible(!visible);
                content.setManaged(!visible);
                collapse.setText(visible ? "展开思考过程⌄" : "收起思考过程⌃");
            });
            Region detailSpacer = new Region();
            HBox.setHgrow(detailSpacer, Priority.ALWAYS);
            streamDetail.getChildren().addAll(detailSpacer, collapse);
            body.getChildren().addAll(thinkingRow, streamDetail);
        } else if (!assistant && looksLikeCode(message.content())) {
            body.getChildren().add(codeBlock(message.content()));
        } else {
            body.getChildren().add(content);
        }

        if (!message.citations().isEmpty()) {
            body.getChildren().add(citationRow(message.citations()));
        }

        HBox row = new HBox(13, avatar, body);
        row.setAlignment(Pos.TOP_LEFT);
        row.setFocusTraversable(assistant);
        row.setAccessibleText(body.getAccessibleText());
        row.setMaxWidth(Double.MAX_VALUE);
        row.getStyleClass().addAll("transcript-message-row",
                assistant ? "assistant-message-row" : "user-message-row");
        if (streaming) row.getStyleClass().add("streaming-message-row");
        return new MessageCard(row, content);
    }

    private Node assistantAvatar() {
        ImageView avatar = new ImageView(AI_AVATAR);
        avatar.setFitWidth(36);
        avatar.setFitHeight(36);
        avatar.setPreserveRatio(true);
        avatar.setSmooth(true);
        avatar.getStyleClass().add("transcript-ai-avatar");
        return avatar;
    }

    private Node userAvatar() {
        Label avatar = new Label("我");
        avatar.getStyleClass().add("transcript-user-avatar");
        return avatar;
    }

    private Button iconButton(String iconLiteral, String tooltip) {
        FontIcon icon = new FontIcon(iconLiteral);
        icon.setIconSize(15);
        Button button = new Button();
        button.setGraphic(icon);
        button.setTooltip(new Tooltip(tooltip));
        button.setAccessibleText(tooltip);
        button.getStyleClass().add("transcript-icon-button");
        return button;
    }

    private void showMessageMenu(Button owner, String text) {
        MenuItem copy = new MenuItem("复制消息");
        copy.setOnAction(event -> copyText(text));
        ContextMenu menu = new ContextMenu(copy);
        menu.show(owner, javafx.geometry.Side.BOTTOM, 0, 2);
    }

    private void copyText(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text == null ? "" : text);
        Clipboard.getSystemClipboard().setContent(content);
    }

    private Node codeBlock(String source) {
        VBox block = new VBox(1);
        block.getStyleClass().add("transcript-code-block");
        String[] lines = source.split("\\R", -1);
        for (int index = 0; index < lines.length; index++) {
            Label number = new Label(Integer.toString(index + 1));
            number.getStyleClass().add("transcript-code-line-number");
            Label code = new Label(lines[index]);
            code.setWrapText(false);
            code.getStyleClass().add("transcript-code-line");
            HBox line = new HBox(12, number, code);
            line.setAlignment(Pos.CENTER_LEFT);
            block.getChildren().add(line);
        }
        return block;
    }

    private boolean looksLikeCode(String content) {
        if (content == null || content.isBlank()) return false;
        long codeLines = content.lines().filter(line -> {
            String value = line.stripLeading();
            return value.startsWith("//") || value.startsWith("/*") || value.startsWith("public ")
                    || value.startsWith("private ") || value.startsWith("if (") || value.startsWith("for (")
                    || value.endsWith(";") || value.equals("{") || value.equals("}");
        }).count();
        return codeLines >= 2;
    }

    private FlowPane citationRow(List<KnowledgeCitationDto> citations) {
        FlowPane row = new FlowPane(7, 5);
        row.getStyleClass().add("transcript-citations");
        Label label = new Label("引用");
        label.getStyleClass().add("transcript-citation-label");
        row.getChildren().add(label);
        for (KnowledgeCitationDto citation : citations) {
            String text = citation.documentName();
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

    private static String formatLocalTime(LocalDateTime value) {
        if (value == null) return "";
        return value.atZone(ZoneOffset.UTC)
                .withZoneSameInstant(ZoneId.systemDefault())
                .format(TIME_FORMAT);
    }

    private record MessageCard(HBox root, Label content) {
    }
}
