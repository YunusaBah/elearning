package com.utg.elearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.util.StringUtils;

@SpringBootApplication
public class ElearningApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ElearningApplication.class);
        if (StringUtils.hasText(System.getenv("RENDER_MYSQL_HOST"))) {
            app.setAdditionalProfiles("render");
        } else if (railwayMysqlLinked()) {
            app.setAdditionalProfiles("railway");
        }
        app.run(args);
    }

    /**
     * Railway sets RAILWAY_ENVIRONMENT when deployed; MySQL plugin exposes MYSQLHOST (and related vars).
     */
    private static boolean railwayMysqlLinked() {
        return StringUtils.hasText(System.getenv("RAILWAY_ENVIRONMENT"))
                && StringUtils.hasText(System.getenv("MYSQLHOST"));
    }
}
