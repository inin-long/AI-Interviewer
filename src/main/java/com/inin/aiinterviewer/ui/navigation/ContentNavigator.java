package com.inin.aiinterviewer.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

@Component
public class ContentNavigator {

    private final ApplicationContext applicationContext;
    private final Deque<PageDescriptor> history = new ArrayDeque<>();

    private StackPane contentHost;
    private Label titleLabel;
    private Consumer<Route> routeListener = ignored -> { };
    private PageDescriptor currentPage;

    public ContentNavigator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void attach(StackPane contentHost, Label titleLabel) {
        attach(contentHost, titleLabel, ignored -> { });
    }

    public void attach(StackPane contentHost, Label titleLabel, Consumer<Route> routeListener) {
        this.contentHost = contentHost;
        this.titleLabel = titleLabel;
        this.routeListener = Objects.requireNonNull(routeListener, "routeListener");
        this.currentPage = null;
        history.clear();
    }

    public void showRoute(Route route) {
        requireAttached();
        history.clear();
        PageDescriptor descriptor = new PageDescriptor(route.contentPath(), route.title(), null);
        show(descriptor, false);
        routeListener.accept(route);
    }

    public void showSubPage(String fxmlPath, String title, Object context) {
        requireAttached();
        if (currentPage != null) {
            history.push(currentPage);
        }
        show(new PageDescriptor(fxmlPath, title, context), false);
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    public void back() {
        requireAttached();
        if (!history.isEmpty()) {
            show(history.pop(), false);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void show(PageDescriptor descriptor, boolean rememberCurrent) {
        if (rememberCurrent && currentPage != null) {
            history.push(currentPage);
        }
        titleLabel.setText(descriptor.title());
        if (descriptor.fxmlPath() == null) {
            contentHost.getChildren().setAll(placeholder(descriptor.title()));
            currentPage = descriptor;
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(descriptor.fxmlPath()));
            loader.setControllerFactory(applicationContext::getBean);
            Parent content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ContextAwareController contextAwareController) {
                contextAwareController.initializeContext(descriptor.context());
            }
            contentHost.getChildren().setAll(content);
            currentPage = descriptor;
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot load content page: " + descriptor.fxmlPath(), exception);
        }
    }

    private VBox placeholder(String titleText) {
        Label title = new Label("“" + titleText + "”将在后续里程碑中实现");
        title.getStyleClass().add("page-title");
        Label description = new Label("当前版本不会填充演示数据或伪造业务结果。");
        description.getStyleClass().add("secondary-text");
        VBox placeholder = new VBox(12, title, description);
        placeholder.setAlignment(Pos.CENTER);
        return placeholder;
    }

    private void requireAttached() {
        if (contentHost == null || titleLabel == null) {
            throw new IllegalStateException("Content navigator is not attached to the main window");
        }
    }

    private record PageDescriptor(String fxmlPath, String title, Object context) {
    }
}
