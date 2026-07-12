package com.inin.aiinterviewer.ui.navigation;

public interface ContextAwareController<T> {
    void initializeContext(T context);
}

