package org.a8043.cwaFX.annotationHandlers;

import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.events.Event;

import java.lang.annotation.Annotation;

public interface AnnotationHandler<T extends Annotation> {
    void onEvent(Class<?> clazz, T annotation, Event event, AppContext context);

    Class<T> getType();
}
