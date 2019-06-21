package spring;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by KamranMa on 28.12.2017.
 */
@Component
public class ObjectMapperCustomizer implements BeanPostProcessor {

    private static class DateTimeSerializer extends JsonSerializer<DateTime> {
        private final DateTimeFormatter formatter = DateTimeFormat.forPattern("dd.MM.yyyy HH:mm");

        @Override
        public void serialize(DateTime date, JsonGenerator generator, SerializerProvider provider) throws IOException {
            String dateString = formatter.print(date);
            generator.writeString(dateString);
        }
    }

    private static class DateSerializer extends JsonSerializer<Date> {
        private final SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyy HH:mm");

        @Override
        public void serialize(Date date, JsonGenerator generator, SerializerProvider provider) throws IOException {
            String dateString = formatter.format(date);
            generator.writeString(dateString);
        }
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (!(bean instanceof ObjectMapper)) {
            return bean;
        }

        ObjectMapper mapper = (ObjectMapper) bean;
        //mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        SimpleModule module = new SimpleModule();
        module.addSerializer(DateTime.class, new DateTimeSerializer());
        module.addSerializer(Date.class, new DateSerializer());
        mapper.registerModule(module);
        return mapper;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
