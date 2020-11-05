package com.sinlo.core.prototype.spec;

import com.sinlo.core.prototype.Prop;
import com.sinlo.sponte.util.Typer;

import java.util.Comparator;
import java.util.Map;

/**
 * The detail entry of {@link com.sinlo.core.prototype.Prototype#compare(Object, Object)}
 *
 * @author sinlo
 */
public class Detail {

    public static final Detail SAME = new Detail(
            null, null, null, null, null, 0);

    public final String name;

    public final Property.On on;

    public final Prop prop;

    public final Object one;

    public final Object other;

    public final int difference;

    public Detail(String name, Property.On on, Prop prop, Object one, Object other, int difference) {
        this.name = name;
        this.on = on;
        this.prop = prop;
        this.one = one;
        this.other = other;
        this.difference = difference;
    }

    /**
     * Get the possible label of the property
     */
    public String label() {
        return prop == null ? name : prop.value();
    }

    /**
     * The calculator of {@link Detail}
     */
    public static class Calculator {

        private final String name;
        private final Map.Entry<Property.On, Prop> entry;

        public Calculator(String name, Map.Entry<Property.On, Prop> entry) {
            this.name = name;
            this.entry = entry;
        }

        @SuppressWarnings({"rawtypes", "unchecked"})
        public <V> Detail calculate(V one, V other) {
            Prop prop = entry != null ? entry.getValue() : null;
            Comparator comparator = null;
            if (prop != null && !prop.comparator().isInterface()) {
                comparator = Typer.create(prop.comparator());
            }
            int difference = 0;
            if (comparator != null) {
                difference = comparator.compare(one, other);
            } else if ((one != null && !one.equals(other)
                    || (one == null && other != null))) {
                difference = -1;
            }
            if (difference == 0) return SAME;
            return new Detail(name,
                    entry != null ? entry.getKey() : null,
                    prop, one, other, difference);
        }
    }
}
