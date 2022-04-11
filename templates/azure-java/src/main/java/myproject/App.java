package myproject;

import io.pulumi.Pulumi;
import io.pulumi.azurenative.resources.ResourceGroup;
import io.pulumi.azurenative.storage.ListStorageAccountKeys;
import io.pulumi.azurenative.storage.StorageAccount;
import io.pulumi.azurenative.storage.StorageAccountArgs;
import io.pulumi.azurenative.storage.enums.Kind;
import io.pulumi.azurenative.storage.enums.SkuName;
import io.pulumi.azurenative.storage.inputs.ListStorageAccountKeysArgs;
import io.pulumi.azurenative.storage.inputs.SkuArgs;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.deployment.InvokeOptions;

import java.util.Map;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(() -> {
            var resourceGroup = new ResourceGroup("resourceGroup");
            var storageAccount = new StorageAccount("sa", StorageAccountArgs.builder()
                    .resourceGroupName(resourceGroup.getName())
                    .sku(SkuArgs.builder()
                            .name(Either.ofRight(SkuName.Standard_LRS))
                            .build())
                    .kind(Either.ofRight(Kind.StorageV2))
                    .build());

            var primaryStorageKey = getStorageAccountPrimaryKey(
                    resourceGroup.getName(),
                    storageAccount.getName());

            return Map.of("primaryStorageKey", primaryStorageKey);
        });
        System.exit(exitCode);
    }

    private static Output<String> getStorageAccountPrimaryKey(Output<String> resourceGroupName,
                                                              Output<String> accountName) {
        return Output.tuple(resourceGroupName, accountName).apply(tuple -> {
            var actualResourceGroupName = tuple.t1;
            var actualAccountName = tuple.t2;
            var invokeResult = ListStorageAccountKeys.invokeAsync(ListStorageAccountKeysArgs.builder()
                    .resourceGroupName(actualResourceGroupName)
                    .accountName(actualAccountName)
                    .build(), InvokeOptions.Empty);
            return Output.of(invokeResult)
                    .applyValue(r -> r.getKeys().get(0).getValue())
                    .asSecret();
        });
    }
}
