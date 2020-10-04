package com.sinlo.test.configuration

import com.sinlo.spring.service.SpringProxistor
import org.springframework.context.annotation.ComponentScan

@ComponentScan("com.sinlo.test")
@SpringProxistor.Enable
class GeneralConfiguration {
}
