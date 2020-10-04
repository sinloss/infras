package com.sinlo.core.jdbc.spec;

import com.sinlo.core.common.spec.ImpatientBiFunction;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A result set getter who represent any getter of {@link ResultSet}
 * and throws {@link SQLException}
 *
 * @param <R>
 * @author sinlo
 */
@FunctionalInterface
public interface ResultSetGetter<R> extends ImpatientBiFunction<ResultSet, String, R, SQLException> {
}
