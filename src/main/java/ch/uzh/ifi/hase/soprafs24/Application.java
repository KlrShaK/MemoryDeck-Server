package ch.uzh.ifi.hase.soprafs24;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@RestController
@SpringBootApplication
public class Application {

    private static final Logger logger = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping(value = "/", produces = MediaType.TEXT_PLAIN_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public String helloWorld() {
        return "The application is running.";
    }

    @Bean
    public ApplicationRunner checkEnv() {
        return args -> {
            String key = System.getenv("OPENAI_API_KEY");
            if (key == null || key.isBlank()) {
                logger.error("🚨 OPENAI_API_KEY is MISSING in the environment!");
                // Optionally, fail fast:
                // throw new IllegalStateException("OPENAI_API_KEY must be set");
            } else {
                logger.info("✅ OPENAI_API_KEY is present (length {}).", key.length());
            }
        };
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins("http://localhost:3000", "https://sopra-fs25-group-40-client.vercel.app")
                        .allowedMethods("GET", "POST", "PUT", "DELETE");
            }
        };
    }
}
