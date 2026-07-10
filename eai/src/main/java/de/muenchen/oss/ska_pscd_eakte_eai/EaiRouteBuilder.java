package de.muenchen.oss.ska_pscd_eakte_eai;

import de.muenchen.oss.ska_pscd_eakte_eai.data.PscdData;
import lombok.RequiredArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EaiRouteBuilder extends RouteBuilder {

    public static final String DIRECT_ROUTE = "direct:eai-route";
    @Value("/n")
    protected String lineBreak;





    @Override
    public void configure() {
        onException(Exception.class).handled(true).log(LoggingLevel.ERROR, "${exception}");

        from("file://testdaten?noop=true")
                .routeId("eai-route")
                .split(body().tokenize(lineBreak))
                .unmarshal().bindy(BindyType.Csv, PscdData.class)
                .log(LoggingLevel.WARN, "de.muenchen",
                        "${body.toString()}")

                .process("eAkteConnector")
//                .log(LoggingLevel.DEBUG, "de.muenchen",
//                        "Add camel routing... (https://camel.apache.org/components/latest/eips/enterprise-integration-patterns.html).")
                .to("mock:example");
    }

}
