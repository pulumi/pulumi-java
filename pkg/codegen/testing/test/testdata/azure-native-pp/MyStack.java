package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        var frontDoor = new FrontDoor("frontDoor", FrontDoorArgs.builder()        
            .routingRules(RoutingRule.builder()
                .routeConfiguration(ForwardingConfiguration.builder()
                    .odataType("#Microsoft.Azure.FrontDoor.Models.FrontdoorForwardingConfiguration")
                    .backendPool(SubResource.builder()
                        .id("/subscriptions/subid/resourceGroups/rg1/providers/Microsoft.Network/frontDoors/frontDoor1/backendPools/backendPool1")
                        .build())
                    .build())
                .build())
            .build());

        var endpoint = new Endpoint("endpoint", EndpointArgs.builder()        
            .deliveryPolicy(EndpointPropertiesUpdateParametersDeliveryPolicy.builder()
                .rules(DeliveryRule.builder()
                    .actions(                    
                        DeliveryRuleCacheExpirationAction.builder()
                            .name("CacheExpiration")
                            .parameters(CacheExpirationActionParameters.builder()
                                .cacheBehavior("Override")
                                .cacheDuration("10:10:09")
                                .cacheType("All")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleCacheExpirationActionParameters")
                                .build())
                            .build(),
                        DeliveryRuleCacheExpirationAction.builder()
                            .name("ModifyResponseHeader")
                            .parameters(CacheExpirationActionParameters.builder()
                                .headerAction("Overwrite")
                                .headerName("Access-Control-Allow-Origin")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters")
                                .value("*")
                                .build())
                            .build(),
                        DeliveryRuleCacheExpirationAction.builder()
                            .name("ModifyRequestHeader")
                            .parameters(CacheExpirationActionParameters.builder()
                                .headerAction("Overwrite")
                                .headerName("Accept-Encoding")
                                .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleHeaderActionParameters")
                                .value("gzip")
                                .build())
                            .build())
                    .conditions(DeliveryRuleRemoteAddressCondition.builder()
                        .name("RemoteAddress")
                        .parameters(RemoteAddressMatchConditionParameters.builder()
                            .matchValues(                            
                                "192.168.1.0/24",
                                "10.0.0.0/24")
                            .negateCondition(true)
                            .odataType("#Microsoft.Azure.Cdn.Models.DeliveryRuleRemoteAddressConditionParameters")
                            .operator("IPMatch")
                            .build())
                        .build())
                    .name("rule1")
                    .order(1)
                    .build())
                .build())
            .endpointName("endpoint1")
            .isCompressionEnabled(true)
            .isHttpAllowed(true)
            .isHttpsAllowed(true)
            .location("WestUs")
            .build());

        }
}
