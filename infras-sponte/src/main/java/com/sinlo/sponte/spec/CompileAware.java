package com.sinlo.sponte.spec;

import com.sinlo.sponte.core.Context;
import com.sinlo.sponte.core.Pri;

/**
 * Prepare things at compile time
 *
 * @author sinlo
 */
@FunctionalInterface
public interface CompileAware {

    Pri<CompileAware> pri = new Pri<>();

    void onCompile(Context.Subject cs);

}
