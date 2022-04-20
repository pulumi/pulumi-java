package generated_program;

import java.util.*;
import java.io.*;
import java.nio.*;
import com.pulumi.*;
import com.pulumi.codegen.internal.KeyedValue;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    public static Exports stack(Context ctx) {
        final var zones = Output.of(AwsFunctions.getAvailabilityZones());

        final var vpcSubnet = zones.apply(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<Subnet>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.getNames()) {
                var resource = new Subnet("vpcSubnet-" + range.getKey(), SubnetArgs.builder()                
                    .assignIpv6AddressOnCreation(false)
                    .mapPublicIpOnLaunch(true)
                    .cidrBlock(String.format("10.100.%s.0/24", range.getKey()))
                    .availabilityZone(range.getValue())
                    .tags(Map.of("Name", String.format("pulumi-sn-%s", range.getValue())))
                    .build());

                resources.add(resource);
            }

            return resources;
        });

        return ctx.exports();
    }
}
