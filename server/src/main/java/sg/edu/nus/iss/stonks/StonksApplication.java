package sg.edu.nus.iss.stonks;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.StreamUtils;

import sg.edu.nus.iss.stonks.repo.ScrapRepo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableScheduling
public class StonksApplication implements CommandLineRunner {

	@Autowired
	private ScrapRepo scrapRepo;
	
	@Autowired
	private JdbcTemplate jdbcTemplate;

	public static void main(String[] args) {
		SpringApplication.run(StonksApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		updateDatabaseSchema();
	}
	
	/**
	 * Checks and updates the database schema if necessary
	 */
	private void updateDatabaseSchema() {
		try {
			System.out.println("Checking database schema...");
			
			// Check if price_alerts column exists
			boolean hasNeededColumns = true;
			try {
				jdbcTemplate.queryForObject("SELECT price_alerts FROM users LIMIT 1", String.class);
				System.out.println("price_alerts column exists");
			} catch (Exception e) {
				System.out.println("price_alerts column missing: " + e.getMessage());
				hasNeededColumns = false;
			}
			
			// Run the update script if needed
			if (!hasNeededColumns) {
				System.out.println("Running database schema update script...");
				
				try {
					// Load the SQL script
					ClassPathResource resource = new ClassPathResource("db_update.sql");
					String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
					
					// Execute each statement separately
					String[] statements = sql.split(";");
					for (String statement : statements) {
						if (!statement.trim().isEmpty()) {
							try {
								jdbcTemplate.execute(statement);
								System.out.println("Executed SQL: " + statement);
							} catch (Exception e) {
								System.err.println("Error executing statement: " + statement);
								System.err.println("Error details: " + e.getMessage());
							}
						}
					}
					
					System.out.println("Database schema update completed successfully");
				} catch (IOException e) {
					System.err.println("Error reading db_update.sql: " + e.getMessage());
				}
			} else {
				System.out.println("Database schema is up to date");
			}
		} catch (Exception e) {
			System.err.println("Error checking/updating database schema: " + e.getMessage());
			e.printStackTrace();
		}
	}

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
