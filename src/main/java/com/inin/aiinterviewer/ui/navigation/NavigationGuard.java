package com.inin.aiinterviewer.ui.navigation;

/** Allows a page to save state or veto navigation before it is replaced. */
public interface NavigationGuard {

    boolean allowNavigationAway();
}
