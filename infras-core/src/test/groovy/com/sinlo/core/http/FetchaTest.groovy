package com.sinlo.core.http

import com.sinlo.core.http.spec.Next
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class FetchaTest extends Specification {

    def "should fetcha properly fetch"() {
        given:
        def first = new AtomicBoolean(true)
        def course = Fetcha.Course.identity("https://www.bing.com/").precept(Fetcha.Stage.OPEN,
                { c, f ->
                    println(" Precept - $c")
                    return c
                }).intercept({ c ->
            if (first.compareAndSet(true, false)) {
                println("   Retry - $c")
                return Next.RETRY
            }
            println("Continue - $c")
            return Next.CONTINUE
        })

        expect:
        course.get("").fetch()
                .thenApply({ it -> it.text() })
                .join().isPresent()
    }
}
