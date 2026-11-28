package org.a8043.cwaFX.navigation;

/**
 * Receives the parameter supplied when a page becomes visible.
 *
 * @param <T> the type of the page parameter
 */
public interface Page<T> {
    void onNavigate(T parameter);
}
