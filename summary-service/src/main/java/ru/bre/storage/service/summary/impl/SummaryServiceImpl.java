package ru.bre.storage.service.summary.impl;

import com.github.tjake.jlama.model.AbstractModel;
import com.github.tjake.jlama.model.ModelSupport;
import com.github.tjake.jlama.model.functions.Generator;
import com.github.tjake.jlama.safetensors.DType;
import com.github.tjake.jlama.safetensors.prompt.PromptContext;
import com.github.tjake.jlama.util.Downloader;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.bre.kafka.KafkaProducer;
import ru.bre.kafka.message.SummaryMessage;
import ru.bre.storage.entity.Feedback;
import ru.bre.storage.service.summary.SummaryService;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SummaryServiceImpl implements SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryServiceImpl.class);

    private final KafkaProducer producer;

    private AbstractModel model;
    private  String modelId = "tjake/Qwen2.5-0.5B-Instruct-JQ4";
    private String workingDirectory = "./models";

    public SummaryServiceImpl(KafkaProducer producer) {
        this.producer = producer;
    }

    @PostConstruct
    public void initModel() {
        try {
            log.info("Initializing Jlama model from resources inside jar...");

            // Создаём временную директорию
            File tempDir = new File(System.getProperty("java.io.tmpdir"), "jlama-model");
            tempDir.mkdirs();

            // Копируем ресурсы модели из jar в tempDir
            String[] modelFiles = {"config.json", "model.safetensors", "tokenizer.json", "tokenizer_config.json"};
            for (String fileName : modelFiles) {
                try (var in = getClass().getClassLoader()
                        .getResourceAsStream("models/Qwen2.5-0.5B-Instruct-JQ4/" + fileName)) {
                    if (in == null) throw new IllegalStateException(fileName + " not found in resources");
                    File outFile = new File(tempDir, fileName);
                    try (var out = new java.io.FileOutputStream(outFile)) {
                        in.transferTo(out);
                    }
                }
            }

            this.model = ModelSupport.loadModel(tempDir, DType.F32, DType.I8);

            log.info("Jlama model initialized successfully from resources.");
        } catch (Exception e) {
            log.error("Failed to initialize Jlama model", e);
            throw new IllegalStateException("Model initialization failed", e);
        }
    }


    @PreDestroy
    public void closeModel() {
        if (model != null) {
            try {
                model.close();
                log.info("Jlama model closed successfully.");
            } catch (Exception e) {
                log.warn("Failed to close Jlama model cleanly", e);
            }
        }
    }

    @Override
    public void createSummary(List<Feedback> feedbackList) {
        if (feedbackList == null || feedbackList.isEmpty()) {
            log.warn("No feedback to summarize — skipping summary generation.");
            return;
        }

        String combinedFeedback = feedbackList.stream()
                .map(f -> "- " + f.getText())
                .collect(Collectors.joining("\n"));

        String prompt = """
                Summarize the following user feedback in Russian in a concise but meaningful paragraph.
                Keep technical and emotional aspects if relevant.
                
                Feedback list:
                %s
                """.formatted(combinedFeedback);

        try {
            PromptContext ctx = model.promptSupport()
                    .map(support -> support.builder()
                            .addSystemMessage("You are a helpful summarizer who writes concise summaries in Russian.")
                            .addUserMessage(prompt)
                            .build())
                    .orElse(PromptContext.of(prompt));

            log.info("Generating feedback summary using preloaded model...");

            Generator.Response response = model.generate(
                    UUID.randomUUID(),
                    ctx,
                    0.7f,
                    256,
                    (step, text) -> {}
            );

            String summaryText = response.responseText.trim();
            log.info("Generated summary:\n{}", summaryText);

            SummaryMessage summaryMessage = new SummaryMessage(
                    "Weekly Feedback Summary",
                    summaryText,
                    new Date()
            );

            producer.sendSummaryMessage(summaryMessage);
            log.info("Summary message sent to Kafka successfully.");

        } catch (Exception e) {
            log.error("Error generating summary with Jlama", e);
        }
    }
}
