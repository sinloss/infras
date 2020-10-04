package com.sinlo.core.jdbc.shapers;

import com.sinlo.core.common.util.Funny;
import com.sinlo.core.jdbc.Shape;
import com.sinlo.core.jdbc.spec.Shaper;

import java.sql.Timestamp;
import java.util.Date;

/**
 * Targeting the {@link Date} type
 *
 * @author sinlo
 */
@Shape
public class DateShaper implements Shaper<Date, Timestamp> {

    @Override
    public Timestamp unshape(Date date) {
        return Funny.maybe(date, t -> new Timestamp(t.getTime()));
    }

    @Override
    public Date shape(Timestamp timestamp, Class<Date> c) {
        return Funny.maybe(timestamp, t -> new Date(t.getTime()));
    }

}
