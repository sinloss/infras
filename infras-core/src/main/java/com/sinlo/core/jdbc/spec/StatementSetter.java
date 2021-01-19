package com.sinlo.core.jdbc.spec;

import com.sinlo.core.common.functional.ImpatientTriConsumer;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * A statement setter who represent any parameter setter of {@link PreparedStatement}
 * and throws {@link SQLException}
 *
 * @param <T>
 * @author sinlo
 */
@FunctionalInterface
public interface StatementSetter<T> extends
        ImpatientTriConsumer<PreparedStatement, Integer, T, SQLException> {

}
