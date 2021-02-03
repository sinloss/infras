package com.sinlo.security.tkn;

import com.sinlo.security.tkn.spec.Knowledge;
import com.sinlo.security.tkn.spec.State;
import com.sinlo.security.tkn.spec.Tkn;
import com.sinlo.security.tkn.spec.TknException;

/**
 * Tkn keeper
 *
 * @param <T> the type of the raw token
 * @param <K> the type of the parsed token
 * @param <A> the type of the subject
 * @author sinlo
 */
public class TknKeeper<T, K, A> {

    /**
     * A lifespan token containing the lifespan data of all tokens kept by
     * this keeper
     */
    private final Tkn<Long> lifespan;
    /**
     * The transition period before the next longevous token has taken place
     */
    private final long transition;

    /**
     * The {@link Knowledge} that can stat the state of a given token, and
     * create a new token base on the given lifespan and subject
     */
    private final Knowledge<T, K, A> knowledge;

    TknKeeper(Tkn<Long> lifespan,
              long transition,
              Knowledge<T, K, A> knowledge) {
        this.lifespan = lifespan;
        this.transition = transition;
        this.knowledge = knowledge;
    }

    /**
     * Create a new tkn from the given subject
     */
    public Tkn<T> create(A subject) {
        return lifespan.map(t -> knowledge.create(t, subject));
    }

    /**
     * Get state information of the given {@link Tkn}
     */
    public Tkn<State<T, K, A>> stat(Tkn<T> tkn) {
        if (tkn == null) throw new TknException.Null();
        return tkn.map(knowledge::stat);
    }

    /**
     * Renew the given tkn
     */
    public Tkn<T> renew(Tkn<T> tkn) {
        if (tkn == null || tkn.longevous == null)
            throw new TknException.Null();

        State<T, K, A> state = knowledge.stat(tkn.longevous);
        if (state == null) throw new TknException.NoState();

        // time left before expiring
        long t = state.expire - System.currentTimeMillis();
        if (t > transition) {
            // not yet enter transition period, only renew the ephemeral
            // token
            return Tkn.of(knowledge.create(lifespan.ephemeral, state.subject),
                    tkn.longevous);
        } else if (t > 0 && t < transition) {
            // not expired and in the transition period, renew the entire
            // tkn
            return this.lifespan.map(ls -> knowledge.create(ls, state.subject));
        } else {
            // expired
            throw new TknException.Expired();
        }
    }
}
