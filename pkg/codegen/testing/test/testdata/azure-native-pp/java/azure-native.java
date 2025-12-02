package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azurenative.network.FrontDoor;
import com.pulumi.azurenative.network.FrontDoorArgs;
import com.pulumi.azurenative.network.inputs.RoutingRuleArgs;
import com.pulumi.azurenative.cdn.Endpoint;
import com.pulumi.azurenative.cdn.EndpointArgs;
import com.pulumi.azurenative.cdn.inputs.EndpointPropertiesUpdateParametersDeliveryPolicyArgs;
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
            .routingRules(RoutingRuleArgs.builder()
                .routeConfiguration(ForwardingConfigurationArgs.builder()
                    .odataType("#Microsoft.Azure.FrontDoor.Models.FrontdoorForwardingConfiguration")
                    .backendPool(SubResourceArgs.builder()
                        .id("/subscriptions/subid/resourceGroups/rg1/providers/Microsoft.Network/frontDoors/frontDoor1/backendPools/backendPool1")
                        .build())
                    .build())
                .build())
            .build());

        var endpoint = new Endpoint("endpoint", EndpointArgs.builder()
            .deliveryPolicy(EndpointPropertiesUpdateParametersDeliveryPolicyArgs.builder()
                .rules(Map.ofEntries(
                    Map.entry("actions",                    
                        DeliveryRuleCacheExpirationActionArgs.builder()
                            .name("CacheExpiration")
                            .parameters(CacheExpirationActionParametersArgs.builder()
                                .cacheBehavior("Override")
                                .cacheDuration("10:10:09")
                                .cacheType("All")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleCacheExpirationActionParameters")
                                .build())
                            .build(),
                        DeliveryRuleResponseHeaderActionArgs.builder()
                            .name("ModifyResponseHeader")
                            .parameters(HeaderActionParametersArgs.builder()
                                .headerAction("Overwrite")
                                .headerName("Access-Control-Allow-Origin")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters")
                                .value("*")
                                .build())
                            .build(),
                        DeliveryRuleRequestHeaderActionArgs.builder()
                            .name("ModifyRequestHeader")
                            .parameters(HeaderActionParametersArgs.builder()
                                .headerAction("Overwrite")
                                .headerName("Accept-Encoding")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters")
                                .value("gzip")
                                .build())
                            .build()),
                    Map.entry("conditions", DeliveryRuleRemoteAddressConditionArgs.builder()
                        .name("RemoteAddress")
                        .parameters(RemoteAddressMatchConditionParametersArgs.builder()
                            .matchValues(                            
                                "192.168.1.0/24",
                                "10.0.0.0/24")
                            .negateCondition(true)
                            .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleRemoteAddressConditionParameters")
                            .operator("IPMatch")
                            .build())
                        .build()),
                    Map.entry("name", "rule1"),
                    Map.entry("order", 1)
                ))
                .build())
            .endpointName("endpoint1")
            .isCompressionEnabled(true)
            .isHttpAllowed(true)
            .isHttpsAllowed(true)
            .location("WestUs")
            .build());

    }
}
