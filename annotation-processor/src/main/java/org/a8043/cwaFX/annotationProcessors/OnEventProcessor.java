package org.a8043.cwaFX.annotationProcessors;

import com.google.auto.service.AutoService;
import org.a8043.cwaFX.annotations.bean.OnEvent;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.util.Set;

@AutoService(Processor.class)
@SupportedAnnotationTypes("org.a8043.cwaFX.annotations.bean.OnEvent")
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class OnEventProcessor extends AbstractProcessor {
    private Types typeUtils;
    private Messager messager;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element e : roundEnv.getElementsAnnotatedWith(OnEvent.class)) {
            ExecutableElement method = (ExecutableElement) e;

            OnEvent annotation = method.getAnnotation(OnEvent.class);
            if (annotation == null) {
                continue;
            }

            TypeMirror targetMirror = getTargetType(annotation);
            if (targetMirror == null) {
                continue;
            }

            if (!check(method, targetMirror)) {
                messager.printMessage(Diagnostic.Kind.ERROR,
                    "@OnEvent method must have exactly one " + targetMirror + " parameter",
                    method);
            }
        }
        return true;
    }

    private TypeMirror getTargetType(OnEvent annotation) {
        try {
            annotation.value();
            throw new IllegalStateException("unreachable");
        } catch (MirroredTypeException ex) {
            return ex.getTypeMirror();
        }
    }

    private boolean check(ExecutableElement method, TypeMirror targetMirror) {
        if (method.getParameters().size() != 1) {
            return false;
        }
        return typeUtils.isSameType(method.getParameters().getFirst().asType(), targetMirror);
    }
}
