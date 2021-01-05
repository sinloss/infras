package com.sinlo.sponte.util;

import com.sinlo.sponte.spec.Perch;

import javax.lang.model.element.*;
import java.util.List;
import java.util.function.BiFunction;

import static com.sinlo.sponte.spec.Profile.*;

/**
 * The element profiler
 *
 * @author sinlo
 */
public class Profiler {


    private final StringBuilder builder = new StringBuilder();

    private Element held;

    /**
     * {@link #builder#toString()}
     */
    @Override
    public String toString() {
        return builder.toString();
    }

    private static void everyParameter(ExecutableElement e,
                                       BiFunction<VariableElement, Integer, Boolean> action) {
        if (action == null) return;

        List<? extends VariableElement> parameters = e.getParameters();
        for (int i = 0; i < parameters.size(); i++) {
            if (!action.apply(parameters.get(i), i)) break;
        }
    }

    @SuppressWarnings("SameReturnValue")
    private boolean describe(VariableElement v, int i) {
        if (i != 0) {
            builder.append(METHOD_ARG_DELIMITER);
        }
        builder.append(Typer.descriptor(v.asType()))
                .append(METHOD_ARG_NAME_DELIMITER)
                .append(v.getSimpleName());
        return true;
    }

    /**
     * Trace the declared type of the given {@link Element}
     */
    public TypeElement trace(Element e) {
        return Typer.trace(e, this::accept);
    }

    /**
     * Accept an element and fill the {@link #builder} base on it
     */
    public <T extends Element> T accept(T e) {
        Perch perch = Perch.regard(e.getKind());
        if (perch != null) {
            perch.pend(e, builder);
            switch (perch) {
                case METHOD:
                case CONSTRUCTOR:
                    builder.append(METHOD_OPEN);
                    everyParameter((ExecutableElement) e, this::describe);
                    builder.append(METHOD_CLOSE);
                    break;
                case PARAMETER:
                    everyParameter((ExecutableElement) held, (v, i) -> {
                        if (e.equals(v)) {
                            builder.append(i);
                            return false;
                        }
                        return true;
                    });
                    break;
            }
            held = e;
        }
        return e;
    }
}
