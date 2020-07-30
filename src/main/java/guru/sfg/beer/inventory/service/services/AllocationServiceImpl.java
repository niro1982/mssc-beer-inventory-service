package guru.sfg.beer.inventory.service.services;

import guru.sfg.beer.inventory.service.domain.BeerInventory;
import guru.sfg.beer.inventory.service.repositories.BeerInventoryRepository;
import guru.sfg.brewery.model.BeerOrderDto;
import guru.sfg.brewery.model.BeerOrderLineDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RequiredArgsConstructor
@Service
public class AllocationServiceImpl implements AllocationService {

    private final BeerInventoryRepository beerInventoryRepository;

    @Override
    public Boolean allocateOrder(BeerOrderDto beerOrderDto) {
        log.debug("Allocating orderId: " + beerOrderDto.getId());

        AtomicInteger totalOrdered = new AtomicInteger();
        AtomicInteger totalAllocated = new AtomicInteger();

        beerOrderDto.getBeerOrderLines().forEach(beerOrderLine -> {
            //for each order line, if there is more supply(OrderQuantity) than demand (QuantityAllocated)
            //call to allocate
           if((beerOrderLine.getOrderQuantity() != null ? beerOrderLine.getOrderQuantity() : 0)
                    - (beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0) > 0){
               allocateBeerOrderLine(beerOrderLine);
           }

           totalOrdered.set(totalOrdered.get() + beerOrderLine.getOrderQuantity());
           totalAllocated.set(totalAllocated.get() + (beerOrderLine.getQuantityAllocated() != null ? beerOrderLine.getQuantityAllocated() : 0));

        });

        log.debug("Total ordered: " + totalOrdered.get() + " total allocated: " + totalAllocated);

        return totalOrdered.get() == totalAllocated.get();
    }

    //we could just locate the inventory in DB and deallocate it according to the dto we receive
    //here we just implemented creating a new record for simplicity
    @Override
    public void deallocateOrder(BeerOrderDto beerOrderDto) {
        beerOrderDto.getBeerOrderLines().forEach(beerOrderLineDto -> {
            BeerInventory beerInventory = BeerInventory.builder()
                    .beerId(beerOrderLineDto.getBeerId())
                    .upc(beerOrderLineDto.getUpc())
                    .quantityOnHand(beerOrderLineDto.getQuantityAllocated())
                    .build();
            BeerInventory savedInventory = beerInventoryRepository.save(beerInventory);
            log.debug("Saved Inventory for beer upc: " + savedInventory.getUpc() + " Inventory id: " + savedInventory.getId());
        });
    }

    private void allocateBeerOrderLine(BeerOrderLineDto beerOrderLine) {
        List<BeerInventory> beerInventoryList = beerInventoryRepository.findAllByUpc(beerOrderLine.getUpc());

        beerInventoryList.forEach(beerInventory -> {
            int inventory = beerInventory.getQuantityOnHand() == null ? 0 : beerInventory.getQuantityOnHand();
            int orderQty = beerOrderLine.getOrderQuantity() == null ? 0 : beerOrderLine.getOrderQuantity();
            int allocatedQty = beerOrderLine.getQuantityAllocated() == null ? 0 : beerOrderLine.getQuantityAllocated();
            int quantityToAllocate = orderQty - allocatedQty;

            if (inventory >= quantityToAllocate) {//full allocation
                inventory = inventory - quantityToAllocate;
                beerOrderLine.setQuantityAllocated(orderQty);
                beerInventory.setQuantityOnHand(inventory);

                beerInventoryRepository.save(beerInventory);
            } else if (inventory > 0) {//partial allocation
                beerOrderLine.setQuantityAllocated(allocatedQty + inventory);
                beerInventory.setQuantityOnHand(0);

                beerInventoryRepository.save(beerInventory);
            }
        });

    }
}
