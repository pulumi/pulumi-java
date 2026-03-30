package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azurenative.network.FrontDoor;
import com.pulumi.azurenative.network.FrontDoorArgs;
import com.pulumi.azurenative.cdn.Endpoint;
import com.pulumi.azurenative.cdn.EndpointArgs;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var frontDoor = new FrontDoor("frontDoor", FrontDoorArgs.builder()
            .routingRules(Arrays.asList(Map.of("routeConfiguration", Map.ofEntries(
                Map.entry("odataType", "#Microsoft.Azure.FrontDoor.Models.FrontdoorForwardingConfiguration"),
                Map.entry("backendPool", Map.of("id", "/subscriptions/subid/resourceGroups/rg1/providers/Microsoft.Network/frontDoors/frontDoor1/backendPools/backendPool1"))
            ))))
            .build());

        var endpoint = new Endpoint("endpoint", EndpointArgs.builder()
            .deliveryPolicy(Map.of("rules", Arrays.asList(Map.ofEntries(
                Map.entry("actions", Arrays.asList(                
                    Map.ofEntries(
                        Map.entry("name", "CacheExpiration"),
                        Map.entry("parameters", Map.ofEntries(
                            Map.entry("cacheBehavior", "Override"),
                            Map.entry("cacheDuration", "10:10:09"),
                            Map.entry("cacheType", "All"),
                            Map.entry("odataType", "#Microsoft.Azure.Cdn.Models.DeliveryRuleCacheExpirationActionParameters")
                        ))
                    ),
                    Map.ofEntries(
                        Map.entry("name", "ModifyResponseHeader"),
                        Map.entry("parameters", Map.ofEntries(
                            Map.entry("headerAction", "Overwrite"),
                            Map.entry("headerName", "Access-Control-Allow-Origin"),
                            Map.entry("odataType", "#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters"),
                            Map.entry("value", "*")
                        ))
                    ),
                    Map.ofEntries(
                        Map.entry("name", "ModifyRequestHeader"),
                        Map.entry("parameters", Map.ofEntries(
                            Map.entry("headerAction", "Overwrite"),
                            Map.entry("headerName", "Accept-Encoding"),
                            Map.entry("odataType", "#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters"),
                            Map.entry("value", "gzip")
                        ))
                    ))),
                Map.entry("conditions", Arrays.asList(Map.ofEntries(
                    Map.entry("name", "RemoteAddress"),
                    Map.entry("parameters", Map.ofEntries(
                        Map.entry("matchValues", Arrays.asList(                        
                            "192.168.1.0/24",
                            "10.0.0.0/24")),
                        Map.entry("negateCondition", true),
                        Map.entry("odataType", "#Microsoft.Azure.Cdn.Models.DeliveryRuleRemoteAddressConditionParameters"),
                        Map.entry("operator", "IPMatch")
                    ))
                ))),
                Map.entry("name", "rule1"),
                Map.entry("order", 1)
            ))))
            .endpointName("endpoint1")
            .isCompressionEnabled(true)
            .isHttpAllowed(true)
            .isHttpsAllowed(true)
            .location("WestUs")
            .build());

    }
}
