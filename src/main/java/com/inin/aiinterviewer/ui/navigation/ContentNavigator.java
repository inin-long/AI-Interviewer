package com.inin.aiinterviewer.ui.navigation;

import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.function.Consumer;

@Component
public class ContentNavigator {

    private static final Logger log = LoggerFactory.getLogger(ContentNavigator.class);

    private final ApplicationContext applicationContext;
    private final Deque<PageDescriptor> history = new ArrayDeque<>();

    private StackPane contentHost;
    private Label titleLabel;
    private Consumer<Route> routeListener = ignored -> { };
    private Consumer<String> pageListener = ignored -> { };
    private PageDescriptor currentPage;
    private Object currentController;

    public ContentNavigator(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public void attach(StackPane contentHost, Label titleLabel) {
        attach(contentHost, titleLabel, ignored -> { });
    }

    public void attach(StackPane contentHost, Label titleLabel, Consumer<Route> routeListener) {
        attach(contentHost, titleLabel, routeListener, ignored -> { });
    }

    public void attach(
            StackPane contentHost,
            Label titleLabel,
            Consumer<Route> routeListener,
            Consumer<String> pageListener
    ) {
        this.contentHost = contentHost;
        this.titleLabel = titleLabel;
        this.routeListener = Objects.requireNonNull(routeListener, "routeListener");
        this.pageListener = Objects.requireNonNull(pageListener, "pageListener");
        this.currentPage = null;
        this.currentController = null;
        history.clear();
    }

    public void showRoute(Route route) {
        requireAttached();
        if (!allowNavigationAway()) return;
        history.clear();
        PageDescriptor descriptor = new PageDescriptor(route.contentPath(), route.title(), null);
        show(descriptor, false);
        routeListener.accept(route);
    }

    public void showSubPage(String fxmlPath, String title, Object context) {
        requireAttached();
        if (!allowNavigationAway()) return;
        if (currentPage != null) {
            history.push(currentPage);
        }
        show(new PageDescriptor(fxmlPath, title, context), false);
    }

    public void showSubRoute(Route route, Object context) {
        showSubPage(route.contentPath(), route.title(), context);
    }

    public boolean canGoBack() {
        return !history.isEmpty();
    }

    public boolean prepareForExternalNavigation() {
        requireAttached();
        return allowNavigationAway();
    }

    public void back() {
        requireAttached();
        if (!history.isEmpty() && allowNavigationAway()) {
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
            currentController = null;
            pageListener.accept(null);
            return;
        }
        try {
            URL location = classLoaderResource(descriptor.fxmlPath());
            if (location == null) {
                throw new IllegalStateException("FXML resource not found on classpath: " + descriptor.fxmlPath());
            }
            FXMLLoader loader = new FXMLLoader(location);
            loader.setControllerFactory(applicationContext::getBean);
            Parent content = loader.load();
            Object controller = loader.getController();
            if (controller instanceof ContextAwareController contextAwareController) {
                contextAwareController.initializeContext(descriptor.context());
            }
            contentHost.getChildren().setAll(content);
            currentPage = descriptor;
            currentController = controller;
            pageListener.accept(descriptor.fxmlPath());
        } catch (Exception exception) {
            log.error("Failed to load content page: {} (context={})", descriptor.fxmlPath(), descriptor.context(), exception);
            throw new IllegalStateException("Cannot load content page: " + descriptor.fxmlPath(), exception);
        }
    }

    private URL classLoaderResource(String path) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = getClass().getClassLoader();
        }
        String normalized = path.startsWith("/") ? path.substring(1) : path;
        URL resource = classLoader.getResource(normalized);
        if (resource == null) {
            resource = getClass().getResource(path);
        }
        return resource;
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

    private boolean allowNavigationAway() {
        return !(currentController instanceof NavigationGuard guard) || guard.allowNavigationAway();
    }

    private record PageDescriptor(String fxmlPath, String title, Object context) {
    }
}
