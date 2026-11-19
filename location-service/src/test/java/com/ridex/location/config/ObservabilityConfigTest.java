package com.ridex.location.config;
import com.ridex.observability.CorrelationIdFilter; import org.junit.jupiter.api.Test; import org.springframework.boot.web.servlet.FilterRegistrationBean; import static org.junit.jupiter.api.Assertions.*;
class ObservabilityConfigTest { @Test void registersCorrelationFilterForAllPaths(){FilterRegistrationBean<CorrelationIdFilter> bean=new ObservabilityConfig().correlationIdFilter();assertInstanceOf(CorrelationIdFilter.class,bean.getFilter());assertEquals(-100,bean.getOrder());assertTrue(bean.getUrlPatterns().contains("/*"));} }
