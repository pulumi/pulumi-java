package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azure.core.CoreFunctions;
import com.pulumi.azure.core.inputs.GetResourceGroupArgs;
import com.pulumi.azure.storage.Account;
import com.pulumi.azure.storage.AccountArgs;
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
        final var config = ctx.config();
        final var storageAccountNameParam = config.get("storageAccountNameParam");
        final var resourceGroupNameParam = config.get("resourceGroupNameParam");
        final var resourceGroupVar = CoreFunctions.getResourceGroup(GetResourceGroupArgs.builder()
            .name(resourceGroupNameParam)
            .build());

        final var locationParam = config.get("locationParam").orElse(resourceGroupVar.applyValue(getResourceGroupResult -> getResourceGroupResult.location()));
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
