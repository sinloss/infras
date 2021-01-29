package com.sinlo.jdbc;

import com.sinlo.jdbc.spec.SqlFunction;
import com.sinlo.jdbc.util.Jype;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.sponte.SponteInitializer;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Jadebee the jdbc executor
 *
 * @author sinlo
 */
public class Jadebee extends SponteInitializer {

    private final DataSource ds;

    public Jadebee(DataSource ds) {
        this.ds = ds;
    }

    /**
     * Apply the {@link SqlFunction then} on a newly got connection
     *
     * @param then the given sql function
     * @param <T>  the return type of the given {@link SqlFunction then}
     * @return pass on the return value of the given {@link SqlFunction then}
     */
    public <T> T connected(SqlFunction<Connection, T> then) {
        try (Connection c = ds.getConnection()) {
            if (then != null)
                return then.employ(c);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Apply a general transactional procedure approach around the given {@link Function then}
     *
     * @see #connected(SqlFunction)
     */
    public <T> T transactional(SqlFunction<Connection, T> then) {
        return connected(c -> {
            if (then == null) return null;
            c.setSavepoint();
            try {
                T t = then.employ(c);
                c.commit();
                return t;
            } catch (RuntimeException e) {
                c.rollback();
            }
            return null;
        });
    }

    public Sql cast(String sql) {
        return connected(c -> {
            PreparedStatement s = c.prepareStatement(sql);
            if (s != null)
                return new Sql(s);
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    @Override
    public Class<? extends Annotation>[] subjects() {
        return new Class[]{Shape.class};
    }

    public static class Sql {

        private final PreparedStatement s;

        public Sql(PreparedStatement s) {
            this.s = s;
        }

        public Sql set(Object... args) {
            if (args == null) return this;
            for (int i = 0; i < args.length; i++) {
                Jype.set(s, i, args[i]);
            }
            return this;
        }

        public Rs execute() {
            try {
                if (s.execute()) return new Rs(s);
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return Rs.FAILED;
        }

    }

    public static class Rs {

        public static final Rs FAILED = new Rs(null) {
        };

        private final PreparedStatement s;

        public Rs(PreparedStatement s) {
            this.s = s;
        }

        @SuppressWarnings("SameReturnValue")
        public <T> T single(Class<T> t) {
            try {
                final ResultSet rs = s.getResultSet();
                Prototype.of(t).from((name, type, originValue)
                        -> Jype.get(rs, name, type));
            } catch (SQLException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}
