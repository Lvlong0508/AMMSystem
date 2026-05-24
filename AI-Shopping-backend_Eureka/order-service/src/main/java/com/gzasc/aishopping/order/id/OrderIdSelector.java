package com.gzasc.aishopping.order.id;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OrderIdSelector {

    private OrderIdGenerator orderIdGenerator;

    public OrderIdSelector(List<OrderIdGenerator> generators) {
        if (generators.isEmpty()) {
            throw new IllegalStateException("No OrderIdGenerator found");
        }
        this.orderIdGenerator = generators.get(0);
    }

    public OrderIdSelector switchTo(OrderIdGenerator generator) {
        this.orderIdGenerator = generator;
        return this;
    }

    public String generate() {
        return orderIdGenerator.generate();
    }
}
