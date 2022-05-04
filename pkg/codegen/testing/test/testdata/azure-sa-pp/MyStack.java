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
        final var config = ctx.config();
        final var storageAccountNameParam = config.get("storageAccountNameParam");
        final var resourceGroupNameParam = config.get("resourceGroupNameParam");
        final var resourceGroupVar = Output.of(CoreFunctions.getResourceGroup(GetResourceGroupArgs.builder()
            .name(resourceGroupNameParam)
            .build()));

        final var locationParam = config.get("locationParam").orElse(resourceGroupVar.apply(getResourceGroupResult -> getResourceGroupResult.location()));
        final var storageAccountTierParam = config.get("storageAccountTierParam").orElse("Standard");
        final var storageAccountTypeReplicationParam = config.get("storageAccountTypeReplicationParam").orElse("LRS");
        var storageAccountResource = new Account("storageAccountResource", AccountArgs.builder()        
            .name(storageAccountNameParam)
            .accountKind("StorageV2")
            .location(locationParam)
            .resourceGroupName(resourceGroupNameParam)
            .accountTier(storageAccountTierParam)
            .accountReplicationType(storageAccountTypeReplicationParam)
            .build());

        ctx.export("storageAccountNameOut", storageAccountResource.name());
    }
}
