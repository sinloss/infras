package com.sinlo.test.configuration

import com.sinlo.spring.service.EnableProxervice
import org.springframework.context.annotation.ComponentScan

@ComponentScan("com.sinlo.test")
@EnableProxervice(fallback = true)
class GeneralConfiguration {
}
