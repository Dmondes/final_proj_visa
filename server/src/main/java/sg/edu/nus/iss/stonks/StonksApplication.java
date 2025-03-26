package sg.edu.nus.iss.stonks;

import org.springframework.beans.factory.annotation.Autowired;
// import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import sg.edu.nus.iss.stonks.repo.ScrapRepo;

@SpringBootApplication
@EnableScheduling
public class StonksApplication {

	@Autowired
	private ScrapRepo scrapRepo;

	public static void main(String[] args) {
		SpringApplication.run(StonksApplication.class, args);
	}

	// @Override
	// public void run(String... args) throws Exception {
	// 	scrapRepo.scrapeRisingPosts();
	// }

	@Scheduled(cron = "${scheduler.ticker_counts_daily.cron}") // Run hourly
	public void updateDailyTickerCountsSchedule() {
		scrapRepo.scrapeRisingPosts();
		scrapRepo.updateDailyTickerCounts();
		scrapRepo.updateWeeklyTickerCounts();
		System.out.println("Hourly scrape");
	}

	// @Scheduled(cron = "${scheduler.ticker_counts_weekly.cron}") // Run daily at midnight
	// public void updateWeeklyTickerCountsSchedule() {
	// 	scrapRepo.updateWeeklyTickerCounts();
	// 	System.out.println("Weekly update");
	// }

}
