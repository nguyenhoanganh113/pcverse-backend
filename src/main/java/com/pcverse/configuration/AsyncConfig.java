package com.pcverse.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2); // số lượng thread cơ bản của pool
        executor.setMaxPoolSize(4); // Pool sẽ tăng tối đa lên 4 thread khi lượng công việc vượt quá khả năng xử lý
        executor.setQueueCapacity(100); // Cho phép tối đa 100 tác vụ chờ trong bộ nhớ
        executor.setKeepAliveSeconds(60); // Thread tạo thêm ngoài core pool sẽ bị đóng nếu không có việc trong 60 giây
        executor.setThreadNamePrefix("email-async-"); // đặt tên cho các thread

        // Khi ứng dụng shutdown, Spring sẽ cố chờ các email đang xử lý hoặc đang nằm trong queue hoàn thành
        // thay vì đóng executor ngay lập tức.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30); // Server chỉ chờ tối đa 30 giây khi shutdown.

        // Khi thread pool và queue đều đầy,
        // chạy task trên thread gọi để tạo backpressure,
        // tránh tạo thêm thread hoặc làm mất task.
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());

        executor.initialize();
        return executor;
    }

}
