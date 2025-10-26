package com.carbonmarketplace.analyticsservice.batch;

import com.carbonmarketplace.analyticsservice.entity.TransactionMetric;
import com.carbonmarketplace.analyticsservice.model.Transaction;
import com.carbonmarketplace.analyticsservice.service.MetricsCalculationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.Order;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class AnalyticsETLConfig {

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final JobLauncher jobLauncher;
    private final MetricsCalculationService metricsCalculationService;
    private final DataSource dataSource;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    @Value("${analytics.batch.chunk-size:1000}")
    private int chunkSize;

    @Bean
    public Job analyticsJob() {
        return jobBuilderFactory.get("analyticsJob")
                .incrementer(new RunIdIncrementer())
                .listener(jobExecutionListener())
                .flow(extractTransactionDataStep())
                .next(calculateMetricsStep())
                .next(updateMaterializedViewsStep())
                .next(generateKPIReportsStep())
                .end()
                .build();
    }

    @Bean
    public Step extractTransactionDataStep() {
        return stepBuilderFactory.get("extractTransactionDataStep")
                .<Transaction, TransactionMetric>chunk(chunkSize)
                .reader(transactionReader())
                .processor(transactionProcessor())
                .writer(metricsWriter())
                .listener(stepExecutionListener())
                .build();
    }

    @Bean
    public Step calculateMetricsStep() {
        return stepBuilderFactory.get("calculateMetricsStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("Calculating aggregated metrics...");
                    metricsCalculationService.calculateDailyMetrics();
                    metricsCalculationService.calculateWeeklyMetrics();
                    metricsCalculationService.calculateMonthlyMetrics();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step updateMaterializedViewsStep() {
        return stepBuilderFactory.get("updateMaterializedViewsStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("Refreshing materialized views...");
                    metricsCalculationService.refreshMaterializedViews();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    public Step generateKPIReportsStep() {
        return stepBuilderFactory.get("generateKPIReportsStep")
                .tasklet((contribution, chunkContext) -> {
                    log.info("Generating KPI reports...");
                    metricsCalculationService.generateKPIReports();
                    return RepeatStatus.FINISHED;
                })
                .build();
    }

    @Bean
    @StepScope
    public JdbcPagingItemReader<Transaction> transactionReader() {
        Map<String, Order> sortKeys = new HashMap<>();
        sortKeys.put("id", Order.ASCENDING);

        return new JdbcPagingItemReaderBuilder<Transaction>()
                .name("transactionReader")
                .dataSource(dataSource)
                .fetchSize(chunkSize)
                .rowMapper(new BeanPropertyRowMapper<>(Transaction.class))
                .selectClause("SELECT id, user_id, amount, credit_amount, transaction_type, status, created_at")
                .fromClause("FROM transactions")
                .whereClause("WHERE created_at >= :startDate AND processed_for_analytics = false")
                .sortKeys(sortKeys)
                .parameterValues(Map.of("startDate", LocalDateTime.now().minusHours(1)))
                .build();
    }

    @Bean
    public ItemProcessor<Transaction, TransactionMetric> transactionProcessor() {
        return transaction -> {
            log.debug("Processing transaction: {}", transaction.getId());
            TransactionMetric metric = new TransactionMetric();
            metric.setTransactionId(transaction.getId());
            metric.setUserId(transaction.getUserId());
            metric.setAmount(transaction.getAmount());
            metric.setCreditAmount(transaction.getCreditAmount());
            metric.setTransactionType(transaction.getTransactionType());
            metric.setStatus(transaction.getStatus());
            metric.setProcessedAt(LocalDateTime.now());
            metric.setTimestamp(transaction.getCreatedAt());
            
            // Calculate additional metrics
            metric.setCo2Offset(calculateCO2Offset(transaction));
            metric.setRevenueGenerated(calculateRevenue(transaction));
            
            return metric;
        };
    }

    @Bean
    public ItemWriter<TransactionMetric> metricsWriter() {
        return new JdbcBatchItemWriterBuilder<TransactionMetric>()
                .dataSource(dataSource)
                .sql("INSERT INTO transaction_metrics (transaction_id, user_id, amount, credit_amount, " +
                     "transaction_type, status, processed_at, timestamp, co2_offset, revenue_generated) " +
                     "VALUES (:transactionId, :userId, :amount, :creditAmount, :transactionType, :status, " +
                     ":processedAt, :timestamp, :co2Offset, :revenueGenerated)")
                .beanMapped()
                .build();
    }

    @Bean
    public JobExecutionListener jobExecutionListener() {
        return new JobExecutionListener() {
            @Override
            public void beforeJob(JobExecution jobExecution) {
                log.info("Analytics ETL Job starting at: {}", jobExecution.getStartTime());
            }

            @Override
            public void afterJob(JobExecution jobExecution) {
                log.info("Analytics ETL Job completed with status: {} at: {}", 
                        jobExecution.getStatus(), jobExecution.getEndTime());
                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    log.info("Job completed successfully. Processed records: {}", 
                            jobExecution.getStepExecutions().stream()
                                    .mapToLong(StepExecution::getWriteCount)
                                    .sum());
                } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
                    log.error("Job failed with exceptions: {}", 
                            jobExecution.getAllFailureExceptions());
                }
            }
        };
    }

    @Bean
    public StepExecutionListener stepExecutionListener() {
        return new StepExecutionListener() {
            @Override
            public void beforeStep(StepExecution stepExecution) {
                log.info("Step {} starting", stepExecution.getStepName());
            }

            @Override
            public ExitStatus afterStep(StepExecution stepExecution) {
                log.info("Step {} completed. Read: {}, Written: {}, Skipped: {}", 
                        stepExecution.getStepName(),
                        stepExecution.getReadCount(),
                        stepExecution.getWriteCount(),
                        stepExecution.getSkipCount());
                return stepExecution.getExitStatus();
            }
        };
    }

    @Scheduled(cron = "0 */15 * * * *") // Every 15 minutes
    public void runAnalyticsJob() {
        try {
            log.info("Starting scheduled analytics ETL job");
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("startTime", new Date())
                    .addString("jobType", "scheduled")
                    .toJobParameters();
            
            JobExecution jobExecution = jobLauncher.run(analyticsJob(), jobParameters);
            log.info("Job execution ID: {}, Status: {}", 
                    jobExecution.getId(), jobExecution.getStatus());
        } catch (Exception e) {
            log.error("Failed to run analytics ETL job", e);
        }
    }

    private Double calculateCO2Offset(Transaction transaction) {
        // Calculate CO2 offset based on credit amount and type
        // This is a simplified calculation - actual logic would be more complex
        if (transaction.getCreditAmount() != null && transaction.getCreditAmount() > 0) {
            return transaction.getCreditAmount() * 1.0; // 1 credit = 1 ton CO2
        }
        return 0.0;
    }

    private Double calculateRevenue(Transaction transaction) {
        // Calculate revenue based on transaction amount and fees
        // Platform fee is typically 2-5%
        if (transaction.getAmount() != null && transaction.getAmount().doubleValue() > 0) {
            return transaction.getAmount().doubleValue() * 0.03; // 3% platform fee
        }
        return 0.0;
    }
}
