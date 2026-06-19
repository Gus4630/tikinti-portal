package az.tikinti.portal.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String INVOICE_QUEUE = "invoice.processing.q";
    public static final String INVOICE_DLQ = "invoice.processing.dlq";
    public static final String INVOICE_EXCHANGE = "invoice.exchange";

    @Bean
    Queue invoiceQueue() {
        return QueueBuilder.durable(INVOICE_QUEUE)
                .withArgument("x-dead-letter-exchange", "")
                .withArgument("x-dead-letter-routing-key", INVOICE_DLQ)
                .build();
    }

    @Bean
    Queue invoiceDlq() {
        return QueueBuilder.durable(INVOICE_DLQ).build();
    }

    @Bean
    DirectExchange invoiceExchange() {
        return new DirectExchange(INVOICE_EXCHANGE);
    }

    @Bean
    Binding invoiceBinding() {
        return BindingBuilder.bind(invoiceQueue()).to(invoiceExchange()).with(INVOICE_QUEUE);
    }

    @Bean
    Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
