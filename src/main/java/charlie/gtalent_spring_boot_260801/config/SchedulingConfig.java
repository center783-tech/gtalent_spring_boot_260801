package charlie.gtalent_spring_boot_260801.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

// 啟用 @Scheduled 定時任務（OrderExpirationService 需要）。
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
