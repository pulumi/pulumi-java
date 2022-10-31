package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azurenative.resources.ResourceGroup;
import com.pulumi.azurenative.resources.ResourceGroupArgs;
import com.pulumi.azurenative.storage.StorageAccount;
import com.pulumi.azurenative.storage.StorageAccountArgs;
import com.pulumi.azurenative.storage.inputs.SkuArgs;
import com.pulumi.azurenative.storage.StorageAccountStaticWebsite;
import com.pulumi.azurenative.storage.StorageAccountStaticWebsiteArgs;
import com.pulumi.azurenative.storage.Blob;
import com.pulumi.azurenative.storage.BlobArgs;
import com.pulumi.asset.FileAsset;
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
        var rawkodeGroup = new ResourceGroup("rawkodeGroup", ResourceGroupArgs.builder()        
            .location("WestUs")
            .build());

        var rawkodeStorage = new StorageAccount("rawkodeStorage", StorageAccountArgs.builder()        
            .resourceGroupName(rawkodeGroup.name())
            .kind("StorageV2")
            .sku(SkuArgs.builder()
                .name("Standard_LRS")
                .build())
            .build());

        var rawkodeWebsite = new StorageAccountStaticWebsite("rawkodeWebsite", StorageAccountStaticWebsiteArgs.builder()        
            .resourceGroupName(rawkodeGroup.name())
            .accountName(rawkodeStorage.name())
            .indexDocument("index.html")
            .error404Document("404.html")
            .build());

        var rawkodeIndexHtml = new Blob("rawkodeIndexHtml", BlobArgs.builder()        
            .resourceGroupName(rawkodeGroup.name())
            .accountName(rawkodeStorage.name())
            .containerName(rawkodeWebsite.containerName())
            .contentType("text/html")
            .type("Block")
            .source(new FileAsset("./website/index.html"))
            .build());

        var stack72Group = new ResourceGroup("stack72Group", ResourceGroupArgs.builder()        
            .location("WestUs")
            .build());

        var stack72Storage = new StorageAccount("stack72Storage", StorageAccountArgs.builder()        
            .resourceGroupName(stack72Group.name())
            .kind("StorageV2")
            .sku(SkuArgs.builder()
                .name("Standard_LRS")
                .build())
            .build());

        var stack72Website = new StorageAccountStaticWebsite("stack72Website", StorageAccountStaticWebsiteArgs.builder()        
            .resourceGroupName(stack72Group.name())
            .accountName(stack72Storage.name())
            .indexDocument("index.html")
            .error404Document("404.html")
            .build());

        var stack72IndexHtml = new Blob("stack72IndexHtml", BlobArgs.builder()        
            .resourceGroupName(stack72Group.name())
            .accountName(stack72Storage.name())
            .containerName(stack72Website.containerName())
            .contentType("text/html")
            .type("Block")
            .source(new FileAsset("./website/index.html"))
            .build());

    }
}
