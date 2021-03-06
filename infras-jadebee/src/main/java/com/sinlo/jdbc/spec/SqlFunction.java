package com.sinlo.jdbc.spec;

import com.sinlo.core.common.functional.ImpatientFunction;

import java.sql.SQLException;

/**
 * An impatient function who throws {@link SQLException}
 *
 * @author sinlo
 * @see ImpatientFunction
 */
@FunctionalInterface
public interface SqlFunction<T, R> extends ImpatientFunction<T, R, SQLException> {

}
