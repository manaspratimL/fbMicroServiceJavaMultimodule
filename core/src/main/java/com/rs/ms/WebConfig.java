/*******************************************************************************
 *  Copyright - Talentica Software Pvt. Ltd. 2015. 
 *  All rights reserved.
 *******************************************************************************/
package com.rs.ms;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.web.servlet.config.annotation.DefaultServletHandlerConfigurer;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

/**
 * @author Manas
 *
 */
@Configuration
@ComponentScan(basePackages = {"com.rs.ms"})
@PropertySource(value={"classpath:application.properties"})
@EnableWebMvc
public class WebConfig extends WebMvcConfigurerAdapter{
		
	@Override
    public void configureDefaultServletHandling(DefaultServletHandlerConfigurer configurer) {
        configurer.enable();
    }
	
	
	
}
