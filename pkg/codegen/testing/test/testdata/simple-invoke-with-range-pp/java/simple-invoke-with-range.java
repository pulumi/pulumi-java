package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.aws.AwsFunctions;
import com.pulumi.aws.inputs.GetAvailabilityZonesArgs;
import com.pulumi.aws.ec2.Subnet;
import com.pulumi.aws.ec2.SubnetArgs;
import com.pulumi.codegen.internal.KeyedValue;
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
        final var zones = AwsFunctions.getAvailabilityZones(GetAvailabilityZonesArgs.builder()
            .build());

        final var vpcSubnet = zones.applyValue(getAvailabilityZonesResult -> {
            final var resources = new ArrayList<Subnet>();
            for (var range : KeyedValue.of(getAvailabilityZonesResult.names())) {
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
