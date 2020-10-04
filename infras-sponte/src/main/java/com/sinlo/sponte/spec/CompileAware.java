package com.sinlo.sponte.spec;

import com.sinlo.sponte.core.Pri;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

/**
 * Prepare things at compile time
 *
 * @author sinlo
 */
@FunctionalInterface
public interface CompileAware {

    com.sinlo.sponte.core.Pri<CompileAware> Pri = new Pri<>();

    void onCompile(ProcessingEnvironment env, Class<?> c, Element element);
}
