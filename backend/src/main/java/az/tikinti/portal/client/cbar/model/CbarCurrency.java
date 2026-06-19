package az.tikinti.portal.client.cbar.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CbarCurrency {

    @JacksonXmlProperty(localName = "Code", isAttribute = true)
    private String code;

    @JacksonXmlProperty(localName = "Nominal")
    private String nominal;

    @JacksonXmlProperty(localName = "Name")
    private String name;

    @JacksonXmlProperty(localName = "Value")
    private String value;
}
