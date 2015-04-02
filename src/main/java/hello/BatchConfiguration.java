package hello;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadPoolExecutor;

import javax.sql.DataSource;

import org.springframework.batch.core.ItemProcessListener;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableBatchProcessing
public class BatchConfiguration {

    @Bean
    public ItemReader<Person> reader() {
        FlatFileItemReader<Person> reader = new FlatFileItemReader<Person>();
        reader.setResource(new ClassPathResource("sample-data.csv"));
        reader.setLineMapper(new DefaultLineMapper<Person>() {{
            setLineTokenizer(new DelimitedLineTokenizer() {{
                setNames(new String[] { "firstName", "lastName" });
            }});
            setFieldSetMapper(new BeanWrapperFieldSetMapper<Person>() {{
                setTargetType(Person.class);
            }});
        }});
        return reader;
    }

    @Bean
    public ItemProcessor<Person, Person> processor() {
    	AsyncItemProcessor asyncItemProcessor = new AsyncItemProcessor();
    	asyncItemProcessor.setDelegate(new PersonItemProcessor());
    	asyncItemProcessor.setTaskExecutor(getAsyncExecutor());
        return asyncItemProcessor;
    }
    
    @Bean
    public ItemProcessListener itemProcessListener() {
    	return new PersonItemProcessListener();
    }

	private TaskExecutor getAsyncExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(10);
		executor.setMaxPoolSize(10);
		executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
		executor.setThreadNamePrefix("MyExecutor-");
		executor.afterPropertiesSet();
		return executor;
	}
    
    @Bean
    public ItemWriter<Person> writer(DataSource dataSource) {
    	
    	ItemWriter<Person> noOpWriter = new ItemWriter<Person>() {
    		@Override
    		public void write(List<? extends Person> p) throws Exception {
//    			System.out.println("write: "+p);
    		}
		};
        
        AsyncItemWriter asyncItemWriter = new AsyncItemWriter();
        asyncItemWriter.setDelegate(noOpWriter);
        return asyncItemWriter;
    }

    @Bean
    public Job importUserJob(JobBuilderFactory jobs, Step s1) {
        return jobs.get("importUserJob")
                .incrementer(new RunIdIncrementer())
                .flow(s1)
                .end()
                .build();
    }

    @Bean
    public Step step1(StepBuilderFactory stepBuilderFactory, ItemReader<Person> reader,
            ItemWriter<Person> writer, ItemProcessor<Person, Person> processor, ItemProcessListener<Person, Person> itemProcessListener) {
        return stepBuilderFactory.get("step1")
                .<Person, Person> chunk(10)
                .reader(reader)
                .faultTolerant()
					.noRollback(PersonProcessingException.class)
						.noRetry(PersonProcessingException.class)
						.skip(PersonProcessingException.class)
						.noRollback(ExecutionException.class)
						.noRetry(ExecutionException.class)
						.skip(ExecutionException.class)
						.skipLimit(Integer.MAX_VALUE)
                .processor(processor)
                .writer(writer)
                .listener(itemProcessListener)
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

}
