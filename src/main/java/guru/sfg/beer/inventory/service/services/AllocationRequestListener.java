package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.config.JmsConfig;
import guru.sfg.brewery.model.events.AllocateOrderRequest;
import guru.sfg.brewery.model.events.AllocationOrderResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class AllocationRequestListener {

    private final AllocationService allocationService;
    private final JmsTemplate jmsTemplate;

    @JmsListener(destination = JmsConfig.ALLOCATE_ORDER_QUEUE)
    public void listen(AllocateOrderRequest orderRequest){
        Boolean isAllocated = false;
        boolean isAllocationError = false;

        try {
            isAllocated = allocationService.allocateOrder(orderRequest.getBeerOrderDto());
        } catch (Exception e) {
            log.error(" Allocation failed for order id: " +orderRequest.getBeerOrderDto().getId());
            isAllocationError = true;
        }

        jmsTemplate.convertAndSend(JmsConfig.ALLOCATE_ORDER_RESPONSE_QUEUE,
                AllocationOrderResponse.builder()
                .beerOrderDto(orderRequest.getBeerOrderDto())
                .allocationError(isAllocationError)
                .pendingInventory(!isAllocated).build());

    }
}
