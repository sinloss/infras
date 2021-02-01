package com.sinlo.core.common.util;


import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Xeger is the shadow brother of regex
 *
 * @author sinlo
 */
public class Xeger {

    public static Pattern zip(String delim, String... expr) {
        final Jason.Thingama.Bob m = Jason.map();
        Arrays.stream(expr).map(Strine::split).map(s -> s.by(delim))
                .map(s -> s.raw().toArray(String[]::new))
                .forEach(splits -> {
                    int last = splits.length - 1;
                    if (last == 0) {
                        m.merge(splits[0], "");
                        return;
                    }
                    m.plant(splits[last], true).into(
                            (Object[]) Arrays.copyOfRange(splits, 0, last));
                });
        // TODO Compose the requested regex Pattern from the well organized Thingamabob
        return null;
    }

}
