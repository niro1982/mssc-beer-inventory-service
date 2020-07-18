package guru.sfg.beer.inventory.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * spring does component scan from this package(where @SpringBootApplication is).
 * it would not pick up any spring components outside of this package (in this case all
 * we have outside of this package is pojos in common.events package so its not a problem.
 * if we do have components we can tell spring to do a component scan)
 */

@SpringBootApplication
public class BeerInventoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(BeerInventoryApplication.class, args);
    }

}
