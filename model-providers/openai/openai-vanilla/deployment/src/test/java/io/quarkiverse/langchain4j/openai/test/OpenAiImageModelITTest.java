package io.quarkiverse.langchain4j.openai.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.inject.Inject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.RegisterExtension;

import dev.langchain4j.data.image.Image;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.openai.OpenAiImageModelName;
import dev.langchain4j.model.output.Response;
import io.quarkiverse.langchain4j.ModelName;
import io.quarkus.test.QuarkusUnitTest;

/**
 * End-to-end image generation smoke test against the real OpenAI API. Runs only when {@code OPENAI_API_KEY} is set, and
 * exercises both the newer {@code gpt-image-1} model (which rejects {@code style} and {@code response_format}) and the
 * older {@code dall-e-3} model.
 */
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
public class OpenAiImageModelITTest {

    static final String API_KEY = System.getenv("OPENAI_API_KEY");

    @RegisterExtension
    static QuarkusUnitTest unitTest = new QuarkusUnitTest()
            .setArchiveProducer(() -> ShrinkWrap.create(JavaArchive.class))
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.api-key", API_KEY)
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.timeout", "120s")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.model-name", "gpt-image-1")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.size", "1024x1024")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.quality", "low")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.image-model.background", "transparent")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".api-key", API_KEY)
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".timeout", "120s")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".image-model.model-name",
                    OpenAiImageModelName.DALL_E_3.toString())
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".image-model.size", "1024x1024")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".image-model.quality", "standard")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".image-model.style", "vivid")
            .overrideRuntimeConfigKey("quarkus.langchain4j.openai.\"dalle3\".image-model.response-format", "url");

    @Inject
    ImageModel gptImage1;

    @Inject
    @ModelName("dalle3")
    ImageModel dalle3;

    @Test
    void generatesWithGptImage1() {
        Response<Image> response = gptImage1.generate("a minimalist cube icon on a plain background");
        Image image = response.content();
        assertThat(image).isNotNull();
        assertThat(image.base64Data()).as("gpt-image-1 returns base64 data").isNotBlank();
    }

    @Test
    void generatesWithDalle3() {
        Response<Image> response = dalle3.generate("a minimalist cube icon on a plain background");
        Image image = response.content();
        assertThat(image).isNotNull();
        assertThat(image.url()).as("dall-e-3 returns a URL when response_format=url").isNotNull();
    }

    @Test
    void generatesMultipleWithDalle3() {
        Response<List<Image>> response = dalle3.generate("a minimalist cube icon", 1);
        assertThat(response.content()).isNotEmpty();
    }
}
