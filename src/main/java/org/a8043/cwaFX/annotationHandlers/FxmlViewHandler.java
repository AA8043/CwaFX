package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.io.resource.ResourceUtil;
import com.google.auto.service.AutoService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.annotations.view.FxmlView;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.NewBeanEvent;
import org.a8043.cwaFX.navigation.PageDefinition;
import org.a8043.cwaFX.navigation.PageRegistry;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@AutoService(AnnotationHandler.class)
public class FxmlViewHandler implements AnnotationHandler<FxmlView> {
    @Override
    public void onEvent(Class<?> clazz, FxmlView annotation, Event event, AppContext context) {
        if (!(event instanceof NewBeanEvent beanEvent) || !beanEvent.getKey().getClazz().equals(clazz)) return;

        URL url = annotation.fxml().isEmpty() ? clazz.getResource(clazz.getSimpleName() + ".fxml")
            : ResourceUtil.getResource(annotation.fxml());
        String name = annotation.value().isEmpty()
            ? (beanEvent.getKey().getName() == null || beanEvent.getKey().getName().isEmpty()
                ? clazz.getSimpleName() : beanEvent.getKey().getName()) + "Node"
            : annotation.value();
        PageRegistry registry = context.getBean(PageRegistry.class, "PageRegistry");
        Object templateController = beanEvent.getObject();
        PageDefinition template = onFxThread(() -> loadPage(name, url, templateController));

        registry.registerFactory(name, () -> onFxThread(() -> {
            Object controller = context.createPageBean(clazz);
            try {
                BeanHandler.preparePageBean(clazz, controller, context);
                PageDefinition page = loadPage(name, url, controller);
                return new PageDefinition(page.getName(), page.getNode(), page.getController(),
                    () -> BeanHandler.destroyPageBean(clazz, controller));
            } catch (RuntimeException | Error failure) {
                BeanHandler.destroyPageBean(clazz, controller);
                throw failure;
            }
        }));
        context.addBean(new BeanKey(javafx.scene.Node.class, name), template.getNode());
    }

    private static <T> T onFxThread(java.util.function.Supplier<T> supplier) {
        if (Platform.isFxApplicationThread()) return supplier.get();
        CompletableFuture<T> result = new CompletableFuture<>();
        try {
            Platform.runLater(() -> {
                try {
                    result.complete(supplier.get());
                } catch (Throwable failure) {
                    result.completeExceptionally(failure);
                }
            });
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while loading FXML", exception);
        } catch (ExecutionException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof RuntimeException runtimeException) throw runtimeException;
            if (cause instanceof Error error) throw error;
            throw new IllegalStateException("Unable to load FXML", cause);
        }
    }

    private static PageDefinition loadPage(String name, URL url, Object controller) {
        try {
            FXMLLoader loader = new FXMLLoader(url);
            loader.setResources(I18n.getBundle());
            loader.setControllerFactory(type -> controller);
            return new PageDefinition(name, loader.load(), controller);
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to load FXML page '" + name + "'", exception);
        }
    }

    @Override
    public Class<FxmlView> getType() {
        return FxmlView.class;
    }
}
