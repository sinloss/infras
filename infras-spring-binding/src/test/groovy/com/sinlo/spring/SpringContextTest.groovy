package com.sinlo.spring


import com.sinlo.spring.configuration.GeneralConfiguration
import com.sinlo.spring.service.FooService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.support.AnnotationConfigContextLoader
import spock.lang.Specification
import spock.lang.Subject

@ContextConfiguration(classes = GeneralConfiguration,
        loader = AnnotationConfigContextLoader)
@Subject(FooService)
class SpringContextTest extends Specification {

    @Autowired
    private FooService fooService

    def "should proxy method commit"() {
        when:
        fooService.foo()
        def e = fooService.get()

        then:
        "foo" == e.getFoo()
    }

    def "should ignored method not commit"() {
        when:
        fooService.foo()
        fooService.feintBar()
        def e = fooService.get()

        then:
        "bar" != e.getFoo()
    }
}
