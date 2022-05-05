package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import com.pulumi.codegen.internal.KeyedValue;

public class App {
    public static void main(String[] args) {
        Pulumi.run(App::stack);
    }

    public static void stack(Context ctx) {
        final var zones = Output.of(AwsFunctions.getAvailabilityZones());

        final var vpcSubnet = zones.apply(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<Subnet>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names()) {
                var resource = new Subnet("vpcSubnet-" + range.key(), SubnetArgs.builder()                
                    .assignIpv6AddressOnCreation(false)
                    .mapPublicIpOnLaunch(true)
                    .cidrBlock(String.format("10.100.%s.0/24", range.key()))
                    .availabilityZone(range.value())
                    .tags(Map.of("Name", String.format("pulumi-sn-%s", range.value())))
                    .build());

                resources.add(resource);
            }

            return resources;
        });

    }
}
