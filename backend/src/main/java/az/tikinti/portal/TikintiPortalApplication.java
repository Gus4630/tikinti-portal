package az.tikinti.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableFeignClients
@EnableAsync
public class TikintiPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(TikintiPortalApplication.class, args);
    }
}
