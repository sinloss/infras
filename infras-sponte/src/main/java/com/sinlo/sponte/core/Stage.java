package com.sinlo.sponte.core;

import com.sinlo.sponte.Sponte;

import java.io.IOException;

import static com.sinlo.sponte.Sponte.*;

/**
 * Annotation processing stages
 *
 * @author sinlo
 */
public enum Stage {
    /**
     * The initial stage which is not consumable
     */
    INITIAL {
        @Override
        public void process(Context.Subject cs) {
            if (!Fo.INITIALIZED.exists()) {
                try {
                    Fo.clear();
                    Fo.INITIALIZED.create();
                    Runtime.getRuntime().addShutdownHook(new Thread(FIN::fin));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    },

    /**
     * Stage 1: process {@link com.sinlo.sponte.Sponte} annotated classes
     * including annotation types
     */
    SPONTE(Context.Subject::currentSponte,
            Procedure::killUsurpers,
            Procedure::trace,
            Procedure::validate,
            Procedure::compiling,
            Procedure::agent,
            Context.Subject::sponted),

    /**
     * Stage 2: process classes annotated by annotations that annotated by
     * {@link com.sinlo.sponte.Sponte}
     */
    MANIFEST(Context.Subject::contextSponte,
            Procedure::trace,
            Procedure::must,
            Procedure::compiling,
            Procedure::agent,
            Context.Subject::inheritance,
            Procedure::adapter),

    /**
     * Stage 3: process classes that are annotated by annotations that are
     * processed in stage 2 and having the {@link Sponte#inheritable()}
     * flagged as true
     */
    INHERIT(Context.Subject::contextSponte,
            Procedure::trace,
            Procedure::must,
            Procedure::compiling,
            Procedure::agent),

    /**
     * The final unadvanceable state
     */
    FIN {
        @Override
        public Stage advance() {
            return this;
        }

        @Override
        public void process(Context.Subject cs) {
            try {
                Fo.INITIALIZED.delete();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fin() {
            for (Stage stage : Stage.values()) {
                try {
                    Fo.delete(stage.fn);
                } catch (IOException ignored) {
                }
            }
            try {
                Fo.create(FIN.fn);
                FIN.process(null);
            } catch (IOException ignored) {
            }
        }
    };

    private final String fn = this.name().concat(".stage");
    private final Procedure[] procs;

    Stage(Procedure... procs) {
        this.procs = procs == null ? new Procedure[0] : procs;
    }

    /**
     * Tells in which stage the current process is
     */
    public static Stage which() {
        for (Stage stage : values()) {
            if (stage.is()) {
                return stage;
            }
        }
        return INITIAL; // if not valid, start from the initial stage
    }

    /**
     * Process the given subject of a specific context
     */
    public void process(Context.Subject cs) {
        for (Procedure proc : procs) {
            try {
                proc.perform(cs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Is it this stage?
     */
    public boolean is() {
        return Fo.exists(fn);
    }

    /**
     * Advance to the next stage
     */
    public Stage advance() {
        Stage next = values()[ordinal() + 1];
        try {
            Fo.create(next.fn);
            Fo.delete(fn);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return next;
    }

    /**
     * Finale
     */
    public void fin() {
        try {
            Fo.delete(fn);
            Fo.create(FIN.fn);
            FIN.process(null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
