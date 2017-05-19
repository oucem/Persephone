package re.vianneyfaiv.persephone.bootstrap;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.support.PassThroughItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.PathResource;

import re.vianneyfaiv.persephone.domain.Application;
import re.vianneyfaiv.persephone.service.ApplicationService;
import re.vianneyfaiv.persephone.service.HealthService;

@Configuration
@EnableBatchProcessing
public class CsvBatchConfiguration {

	@Value("${persephone.applications.csv}")
	private String csvPath;

	@Autowired
	public JobBuilderFactory jobBuilderFactory;

	@Autowired
	public StepBuilderFactory stepBuilderFactory;

	@Autowired
	private ApplicationService applicationService;

	@Autowired
	private HealthService healthService;

	@Bean
	public FlatFileItemReader<Application> reader() {
		FlatFileItemReader<Application> reader = new FlatFileItemReader<Application>();
		reader.setResource(new PathResource(this.csvPath));
		reader.setLinesToSkip(1);

		final AtomicInteger counter = new AtomicInteger(1);
		reader.setLineMapper(new DefaultLineMapper<Application>() {
			{
				this.setLineTokenizer(new DelimitedLineTokenizer() {
					{
						this.setNames(new String[] { "name", "environment", "url" });
					}
				});
				this.setFieldSetMapper(line -> {
					Application app = new Application(counter.get(), line.readString("name"), line.readString("environment"), line.readString("url"));
					counter.incrementAndGet();
					return app;
				});
			}
		});
		return reader;
	}

	@Bean
	public ItemWriter<Application> writer() {
		return items -> this.applicationService.setApplications((List<Application>) items);
	}

	@Bean
	public Job importAppsJob() {
		return this.jobBuilderFactory
					.get("importAppsJob")
					.incrementer(new RunIdIncrementer())
					.flow(this.step1())
					.end()
					.listener(new JobExecutionListener() {
						@Override
						public void beforeJob(JobExecution jobExecution) {
						}

						@Override
						public void afterJob(JobExecution jobExecution) {
							CsvBatchConfiguration.this.applicationService
									.findAll()
									.stream()
									.forEach(app -> app.setUp(CsvBatchConfiguration.this.healthService.isUp(app)));
						}
					})
					.build();
	}

	@Bean
	public Step step1() {
		return this.stepBuilderFactory
				.get("step1")
				.<Application, Application>chunk(10)
				.reader(this.reader())
				.processor(new PassThroughItemProcessor<>())
				.writer(this.writer())
				.build();
	}
}