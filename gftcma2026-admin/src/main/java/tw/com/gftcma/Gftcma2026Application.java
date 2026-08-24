package tw.com.gftcma;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@ComponentScan("tw.com.gftcma")
@EnableCaching
@EnableScheduling
@SpringBootApplication
public class Gftcma2026Application {
	public static void main(String[] args) {
		SpringApplication.run(Gftcma2026Application.class, args);
	}
}
