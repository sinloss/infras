package com.sinlo.core.service.spec;

import java.lang.annotation.*;

/**
 * Tell proxervice created proxies to ignore the annotated method
 *
 * @author sinlo
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProxerviceIgnore {
}
