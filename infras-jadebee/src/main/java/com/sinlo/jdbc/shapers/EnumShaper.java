package com.sinlo.jdbc.shapers;

import com.sinlo.core.common.util.Funny;
import com.sinlo.jdbc.Shape;
import com.sinlo.jdbc.spec.Shaper;

@Shape
public class EnumShaper<T extends Enum<T>> implements Shaper<Enum<T>, String> {

    @Override
    public String unshape(Enum<T> e) {
        return Funny.maybe(e, Enum::toString);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Enum<T> shape(String s, Class<Enum<T>> c) {
        return Funny.maybe(s, t -> Enum.valueOf((Class<T>) c, t));
    }
}
