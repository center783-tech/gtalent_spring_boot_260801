package charlie.gtalent_spring_boot_260801.scheduler;

import java.time.LocalDateTime;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DemoScheduler {

    //
    //  Cron                         執行時間
    //  秒   分  時  日  月  星期
    //  */10 *   *   *   *   *       每 10 秒
    //  0    *   *   *   *   *       每分鐘
    //  0    */5 *   *   *   *       每 5 分鐘
    //  0    0   *   *   *   *       每小時
    //  0    0   2   *   *   *       每天凌晨 2 點
    //  0    30  8   *   *  MON-FRI  週一至週五 08:30
    //  0    0   0   1   *   *       每月 1 日凌晨
    //  0    0   0   *   *   SUN     每週日凌晨
    //

    // 每 10 秒執行一次
    // @Scheduled(fixedRate = 10000)
    // public void task1() {
    //     System.out.println("每10秒執行：" + LocalDateTime.now());
    // }

    // 每 5 分鐘執行一次
    @Scheduled(cron = "0 */5 * * * *")
    public void task2() {
        System.out.println("每5分鐘執行：" + LocalDateTime.now());
    }

    // 每天凌晨 2 點執行
    @Scheduled(
        cron = "0 0 2 * * *",
        zone = "Asia/Taipei"
    )
    public void task3() {
        System.out.println("每天凌晨2點執行");
    }
}