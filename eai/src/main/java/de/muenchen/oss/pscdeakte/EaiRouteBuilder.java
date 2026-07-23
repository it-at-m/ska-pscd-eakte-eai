package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.data.PscdData;
import lombok.RequiredArgsConstructor;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.BindyType;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EaiRouteBuilder extends RouteBuilder {

    public static final String DIRECT_ROUTE = "direct:eai-route";

    @Override
    public void configure() {
        onException(Exception.class).handled(true).log(LoggingLevel.ERROR, "${exception}");

        from("file://testdata?noop=true")
                .routeId("eai-route")
                .unmarshal().bindy(BindyType.Csv, PscdData.class)
                .log(LoggingLevel.INFO, "de.muenchen",
                        "${body.toString()}")
                .split(body())
                .process("eAkteConnector")
                .end()
                .process(exchange -> log.info("end"))
                .to("mock:example");
    }

}
