package rastreia.lti.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;

@SpringBootApplication
public class WebApplication {
	
	
	public static void main(String[] args) {
		SpringApplication.run(WebApplication.class, args);
	}

}
