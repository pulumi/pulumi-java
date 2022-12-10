package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azurenative.resources.ResourceGroup;
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
        var staticsitegroup = new ResourceGroup("staticsitegroup");

        var storageaccount = new StorageAccount("storageaccount", StorageAccountArgs.builder()        
            .resourceGroupName(staticsitegroup.name())
            .kind("StorageV2")
            .sku(SkuArgs.builder()
                .name("Standard_LRS")
                .build())
            .build());

        var staticwebsite = new StorageAccountStaticWebsite("staticwebsite", StorageAccountStaticWebsiteArgs.builder()        
            .resourceGroupName(staticsitegroup.name())
            .accountName(storageaccount.name())
            .indexDocument("index.html")
            .error404Document("404.html")
            .build());

        var indexHtml = new Blob("indexHtml", BlobArgs.builder()        
            .resourceGroupName(staticsitegroup.name())
            .accountName(storageaccount.name())
            .containerName(staticwebsite.containerName())
            .contentType("text/html")
            .type("Block")
            .source(new FileAsset("./www/index.html"))
            .build());

        var faviconPng = new Blob("faviconPng", BlobArgs.builder()        
            .resourceGroupName(staticsitegroup.name())
            .accountName(storageaccount.name())
            .containerName(staticwebsite.containerName())
            .contentType("image/png")
            .type("Block")
            .source(new FileAsset("./www/favicon.png"))
            .build());

        var _404Html = new Blob("404Html", BlobArgs.builder()        
            .resourceGroupName(staticsitegroup.name())
            .accountName(storageaccount.name())
            .containerName(staticwebsite.containerName())
            .contentType("text/html")
            .type("Block")
            .source(new FileAsset("./www/404.html"))
            .build());

        ctx.export("endpoint", storageaccount.primaryEndpoints().applyValue(primaryEndpoints -> primaryEndpoints.web()));
    }
}
