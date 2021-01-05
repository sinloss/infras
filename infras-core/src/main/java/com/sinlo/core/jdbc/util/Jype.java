package com.sinlo.core.jdbc.util;

import com.sinlo.core.common.util.Strine;
import com.sinlo.core.jdbc.spec.ResultSetGetter;
import com.sinlo.core.jdbc.spec.Shaper;
import com.sinlo.core.jdbc.spec.StatementSetter;
import com.sinlo.sponte.util.Typer;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Jadebee types
 *
 * @author sinlo
 */
public enum Jype {

    // primitive
    BOOLEAN(Boolean.class, PreparedStatement::setBoolean, ResultSet::getBoolean),
    BYTE(Byte.class, PreparedStatement::setByte, ResultSet::getByte),
    SHORT(Short.class, PreparedStatement::setShort, ResultSet::getShort),
    INT(Integer.class, PreparedStatement::setInt, ResultSet::getInt),
    LONG(Long.class, PreparedStatement::setLong, ResultSet::getLong),
    DOUBLE(Double.class, PreparedStatement::setDouble, ResultSet::getDouble),
    FLOAT(Float.class, PreparedStatement::setFloat, ResultSet::getFloat),

    // basic
    BIG_DECIMAL(BigDecimal.class, PreparedStatement::setBigDecimal, ResultSet::getBigDecimal),
    STRING(String.class, PreparedStatement::setString, ResultSet::getString),
    BYTES(byte[].class, PreparedStatement::setBytes, ResultSet::getBytes),
    OBJECT(Object.class, PreparedStatement::setObject, ResultSet::getObject),

    // date and time
    DATE(Date.class, PreparedStatement::setDate, ResultSet::getDate),
    TIME(Time.class, PreparedStatement::setTime, ResultSet::getTime),
    TIMESTAMP(Timestamp.class, PreparedStatement::setTimestamp, ResultSet::getTimestamp),

    // lob
    BLOB(Blob.class, PreparedStatement::setBlob, ResultSet::getBlob),
    CLOB(Clob.class, PreparedStatement::setClob, ResultSet::getClob),
    STREAMED_BLOB(new Wrapper<>(StreamedBlob.class), PreparedStatement::setBlob,
            (rs, i) -> rs.getBlob(i).getBinaryStream()),
    STREAMED_CLOB(new Wrapper<>(StreamedClob.class), PreparedStatement::setClob,
            (rs, i) -> rs.getClob(i).getCharacterStream()),
    ASCII_STREAM(new Wrapper<>(AsciiStream.class),
            PreparedStatement::setAsciiStream, ResultSet::getAsciiStream),
    BINARY_STREAM(new Wrapper<>(BinaryStream.class),
            PreparedStatement::setBinaryStream, ResultSet::getBinaryStream),
    CHARACTER_STREAM(new Wrapper<>(CharacterStream.class),
            PreparedStatement::setCharacterStream, ResultSet::getCharacterStream),

    // complex
    CSV(new Wrapper<>(Csv.class), PreparedStatement::setString, ResultSet::getString),
    /**
     * MySQL didn't implement the ARRAY type, so be careful with this
     */
    @Deprecated
    ARRAY(Array.class, PreparedStatement::setArray, ResultSet::getArray),
    SQL_XML(SQLXML.class, PreparedStatement::setSQLXML, ResultSet::getSQLXML),

    // infrastructure
    ROW_ID(RowId.class, PreparedStatement::setRowId, ResultSet::getRowId),
    REF(Ref.class, PreparedStatement::setRef, ResultSet::getRef),
    NULL(new Wrapper<>(Null.class), PreparedStatement::setNull, (rs, i) -> null),

    // N type
    N_STRING(new Wrapper<>(NString.class), PreparedStatement::setNString,
            ResultSet::getNString),
    N_CLOB(NClob.class, PreparedStatement::setNClob, ResultSet::getNClob),
    STREAMED_N_CLOB(new Wrapper<>(StreamedNClob.class),
            PreparedStatement::setNClob, (rs, i) -> rs.getNClob(i).
            getCharacterStream()),
    N_CHARACTER_STREAM(new Wrapper<>(NCharacterStream.class),
            PreparedStatement::setNCharacterStream, ResultSet::getNCharacterStream),
    ;

    public final Class<?> c;
    @SuppressWarnings("rawtypes")
    private final Function conv;
    @SuppressWarnings("rawtypes")
    private final StatementSetter setter;
    @SuppressWarnings("rawtypes")
    private final ResultSetGetter getter;

    /**
     * Constructor
     *
     * @param c      the targeting class
     * @param conv   the type converter
     * @param setter the {@link StatementSetter setter}
     * @param getter the {@link ResultSetGetter getter}
     * @param <A>    the type of the targeting class
     * @param <T>    the type of underlying sql type
     */
    <A, T> Jype(Class<A> c, Function<A, T> conv, StatementSetter<T> setter, ResultSetGetter<T> getter) {
        this.c = c;
        this.conv = conv;
        this.setter = setter;
        this.getter = getter;
    }

    <T> Jype(Class<T> c, StatementSetter<T> setter, ResultSetGetter<T> getter) {
        this(c, null, setter, getter);
    }

    /**
     * Specifically designed for {@link Wrapper}
     */
    <T, A extends Wrapper<T>> Jype(Wrapper<Class<A>> tc, StatementSetter<T> setter, ResultSetGetter<T> getter) {
        this(tc.t, a -> a.t, setter, getter);
    }

    private static final Map<String, Jype> jypes = new HashMap<>();

    static {
        // populate the jypes map
        for (Jype jype : values()) {
            jypes.put(jype.c.getName(), jype);
        }
    }

    /**
     * Set the parameter value for the statement via a chosen setter
     *
     * @param statement the statement
     * @param i         the parameter index
     * @param val       the parameter value
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static void set(PreparedStatement statement, int i, Object val) {
        if (statement == null) return;

        Jype jype = val == null ? Jype.NULL : jypes.get(val.getClass().getName());
        if (jype == null) {
            Shaper shaper = Shapeherder.get().shaper(val.getClass());
            if (shaper != null) {
                // unshape and set again
                set(statement, i, shaper.unshape(val));
                return;
            }
            jype = Jype.OBJECT;
        }

        try {
            if (jype.conv != null) {
                val = jype.conv.apply(val);
            }
            jype.setter.accept(statement, i, val);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings({"unchecked"})
    public static <T> T get(ResultSet rs, String name, Class<T> c) {
        if (rs == null) return null;

        Jype jype = c == null ? Jype.OBJECT : jypes.get(c.getName());
        if (jype == null) {
            Shaper<T, Object> shaper = (Shaper<T, Object>) Shapeherder.get().shaper(c);
            if (shaper != null) {
                try {
                    // get again to get a proper typed value to be shaped
                    return shaper.shape(get(rs, name, Typer.forName(shaper.bw())), c);
                } catch (ClassNotFoundException ignored) {
                }
            }
            jype = Jype.OBJECT;
        }

        try {
            Object val = jype.getter.apply(rs, name);
            if (c != null && Wrapper.class.isAssignableFrom(c)) {
                return c.getDeclaredConstructor(Object.class)
                        .newInstance(val);
            }
            return (T) val;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * The general wrapper
     *
     * @param <T> the wrapped type
     */
    public static class Wrapper<T> {

        public final T t;

        public Wrapper(T t) {
            this.t = t;
        }

    }

    /**
     * The comma separated value
     */
    public static class Csv extends Wrapper<String> {

        public static final String[] ZERO_VALUE = new String[0];

        private final String[] csv;

        public Csv(String s) {
            super(s);
            this.csv = Strine.isEmpty(s) ? ZERO_VALUE : s.split(",");
        }

        public Stream<String> stream() {
            return Stream.of(csv);
        }

        public List<String> list() {
            return stream().collect(Collectors.toList());
        }

        public String get(int i) {
            return csv[i];
        }

        public int size() {
            return csv.length;
        }
    }

    /**
     * The ascii stream value
     *
     * @see PreparedStatement#setAsciiStream(int, InputStream)
     * @see ResultSet#getAsciiStream(int)
     */
    public static class AsciiStream extends Wrapper<InputStream> {

        public AsciiStream(InputStream inputStream) {
            super(inputStream);
        }
    }

    /**
     * The binary stream value
     *
     * @see PreparedStatement#setBinaryStream(int, InputStream)
     * @see ResultSet#getBinaryStream(int)
     */
    public static class BinaryStream extends Wrapper<InputStream> {

        public BinaryStream(InputStream inputStream) {
            super(inputStream);
        }
    }

    /**
     * The character stream value
     *
     * @see PreparedStatement#setCharacterStream(int, Reader)
     * @see ResultSet#getCharacterStream(int)
     */
    public static class CharacterStream extends Wrapper<Reader> {

        public CharacterStream(Reader reader) {
            super(reader);
        }
    }

    /**
     * The N character stream value
     *
     * @see PreparedStatement#setNCharacterStream(int, Reader)
     * @see ResultSet#getNCharacterStream(int)
     */
    public static class NCharacterStream extends Wrapper<Reader> {

        public NCharacterStream(Reader reader) {
            super(reader);
        }
    }

    /**
     * The stream based blob
     *
     * @see PreparedStatement#setBlob(int, InputStream)
     */
    public static class StreamedBlob extends Wrapper<InputStream> {

        public StreamedBlob(InputStream inputStream) {
            super(inputStream);
        }
    }

    /**
     * The stream based clob
     *
     * @see PreparedStatement#setClob(int, Reader)
     */
    public static class StreamedClob extends Wrapper<Reader> {

        public StreamedClob(Reader reader) {
            super(reader);
        }
    }

    /**
     * The stream based N clob
     *
     * @see PreparedStatement#setNClob(int, Reader)
     */
    public static class StreamedNClob extends Wrapper<Reader> {

        public StreamedNClob(Reader reader) {
            super(reader);
        }
    }

    /**
     * The null value
     */
    public static class Null extends Wrapper<Integer> {

        public Null(Integer integer) {
            super(integer);
        }
    }

    /**
     * The NString value
     */
    public static class NString extends Wrapper<String> {

        public NString(String s) {
            super(s);
        }
    }

}
