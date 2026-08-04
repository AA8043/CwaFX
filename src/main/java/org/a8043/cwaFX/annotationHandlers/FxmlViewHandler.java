package org.a8043.cwaFX.annotationHandlers;

import cn.hutool.core.io.resource.ResourceUtil;
import com.google.auto.service.AutoService;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.a8043.cwaFX.AppContext;
import org.a8043.cwaFX.BeanKey;
import org.a8043.cwaFX.I18n;
import org.a8043.cwaFX.annotations.view.FxmlView;
import org.a8043.cwaFX.events.Event;
import org.a8043.cwaFX.events.NewBeanEvent;

import java.io.IOException;
import java.net.URL;
import java.util.concurrent.CountDownLatch;

@Slf4j
@AutoService(AnnotationHandler.class)
public class FxmlViewHandler implements AnnotationHandler<FxmlView> {
    @SneakyThrows
    @Override
    public void onEvent(Class<?> clazz, FxmlView annotation, Event event, AppContext context) {
        if (event instanceof NewBeanEvent e && e.getKey().getClazz().equals(clazz)) {
            CountDownLatch latch = new CountDownLatch(1);
            Platform.runLater(() -> {
                URL url = annotation.fxml().isEmpty() ? clazz.getResource(clazz.getSimpleName() + ".fxml") :
                    ResourceUtil.getResource(annotation.fxml());
                FXMLLoader loader = new FXMLLoader(url);
                loader.setResources(I18n.getLangBundle());
                loader.setControllerFactory(c -> e.getObject());
                String name = annotation.value().isEmpty() ? clazz.getSimpleName() + "Node" : annotation.value();
                try {
                    context.addBean(new BeanKey(Node.class, name), loader.<Node>load());
                } catch (IOException ex) {
                    log.error("Error loading FXML for class: {}", clazz.getName(), ex);
                } finally {
                    latch.countDown();
                }
            });
            latch.await();
        }
    }

    @Override
    public Class<FxmlView> getType() {
        return FxmlView.class;
    }
}
