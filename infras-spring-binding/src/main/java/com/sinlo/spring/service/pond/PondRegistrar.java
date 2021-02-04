package com.sinlo.spring.service.pond;

import com.sinlo.core.common.util.Strine;
import com.sinlo.core.prototype.Prototype;
import com.sinlo.sponte.util.Typer;
import com.sinlo.spring.service.JoinPond;
import com.sinlo.spring.service.core.Registrar;
import org.springframework.core.annotation.MergedAnnotation;

/**
 * The pond registrar
 *
 * @author sinlo
 */
public class PondRegistrar extends Registrar {

    @Override
    protected void registering() {
        MergedAnnotation<JoinPond> ma = this.metadata.getAnnotations().get(JoinPond.class);
        // if not intended to automatically register beans, then return
        if (!ma.getBoolean("auto")) return;
        PondJoiner.keepers.keySet().stream().map(Typer::forSure)
                .filter(Prototype::instantiable)
                .forEach(c -> register(Strine.uncapInitial(c.getSimpleName()), c));
    }
}
