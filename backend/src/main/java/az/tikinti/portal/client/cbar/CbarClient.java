package az.tikinti.portal.client.cbar;

import az.tikinti.portal.client.cbar.model.CbarCurrencyResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "cbar", url = "${application.client.cbar.base-url}")
public interface CbarClient {

    @GetMapping(value = "/currencies/{date}.xml", consumes = MediaType.APPLICATION_XML_VALUE)
    CbarCurrencyResponse getCurrencies(@PathVariable String date);
}
