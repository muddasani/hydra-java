package de.escalon.hypermedia.sample.beans;

import de.escalon.hypermedia.hydra.mapping.Expose;
import org.springframework.hateoas.ResourceSupport;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Currency;
import java.util.List;

/**
 * Created by Dietrich on 17.02.2015.
 */
@Expose("CafeOrCoffeeShop")
public class Store extends ResourceSupport {

    public String name = "Kaffeehaus Hagen";

    public List<Offer> getMakesOffer() {
        return Arrays.asList(
                createOffer("Latte Macchiato", 2.80, createOffer("Shot", 0.20)),
                createOffer("Caffè Macchiato", 1.40, createOffer("Shot", 0.20)),
                createOffer("Caffè Espresso", 1.10, createOffer("Shot", 0.20)),
                createOffer("Cup of El Salvador Finca El Carmen Bourbon", 1.50),
                createOffer("Cappuccino", 2.20, createOffer("Shot", 0.20))
        );
    }

    private Offer createOffer(String productName, double val, Offer... addOns) {
        Product latteMacchiato = new Product(productName);

        Offer offer = new Offer();
        offer.setItemOffered(latteMacchiato);
        BigDecimal price = BigDecimal.valueOf(val)
                .setScale(2);
        offer.setPrice(price);
        offer.setPriceCurrency(Currency.getInstance("EUR"));
        for (Offer addOn : addOns) {
            offer.addOn(addOn);
        }

        return offer;
    }


}
