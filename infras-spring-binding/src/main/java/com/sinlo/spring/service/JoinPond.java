package com.sinlo.spring.service;


import com.sinlo.spring.service.core.PondJoiner;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Enable the {@link com.sinlo.core.service.Proxistor} support by importing the
 * {@link PondJoiner}
 *
 * @author sinlo
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Import({PondJoiner.class})
public @interface JoinPond {

}