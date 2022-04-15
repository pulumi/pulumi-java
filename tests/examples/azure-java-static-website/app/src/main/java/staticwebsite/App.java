package staticwebsite;

import io.pulumi.Context;
import io.pulumi.Exports;
import io.pulumi.Pulumi;
import io.pulumi.asset.FileAsset;
import io.pulumi.azurenative.cdn.Endpoint;
import io.pulumi.azurenative.cdn.EndpointArgs;
import io.pulumi.azurenative.cdn.Profile;
import io.pulumi.azurenative.cdn.ProfileArgs;
import io.pulumi.azurenative.cdn.enums.QueryStringCachingBehavior;
import io.pulumi.azurenative.cdn.inputs.DeepCreatedOriginArgs;
import io.pulumi.azurenative.resources.ResourceGroup;
import io.pulumi.azurenative.storage.Blob;
import io.pulumi.azurenative.storage.BlobArgs;
import io.pulumi.azurenative.storage.StorageAccount;
import io.pulumi.azurenative.storage.StorageAccountArgs;
import io.pulumi.azurenative.storage.StorageAccountStaticWebsite;
import io.pulumi.azurenative.storage.StorageAccountStaticWebsiteArgs;
import io.pulumi.azurenative.storage.enums.Kind;
import io.pulumi.azurenative.storage.enums.SkuName;
import io.pulumi.azurenative.storage.inputs.SkuArgs;
import io.pulumi.azurenative.storage.outputs.EndpointsResponse;
import io.pulumi.core.Either;
import io.pulumi.core.Output;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static Exports stack(Context ctx) {
        var resourceGroup = new ResourceGroup("resourceGroup");

        var storageAccount = new StorageAccount("storageaccount",
                StorageAccountArgs.builder().kind(Either.ofRight(Kind.StorageV2))
                        .resourceGroupName(resourceGroup.getName())
                        .sku(SkuArgs.builder()
                                .name(Either.ofRight(SkuName.Standard_LRS))
                                .build()).build());

        var staticWebsite = new StorageAccountStaticWebsite("staticWebsite",
                StorageAccountStaticWebsiteArgs.builder().accountName(storageAccount.getName())
                        .resourceGroupName(resourceGroup.getName())
                        .indexDocument("index.html")
                        .error404Document("404.html").build());

        var indexHtml = new Blob("index.html",
                BlobArgs.builder().accountName(storageAccount.getName())
                        .resourceGroupName(resourceGroup.getName())
                        .containerName(staticWebsite.getContainerName())
                        .source(new FileAsset("./wwwroot/index.html"))
                        .contentType("text/html").build());

        var notFoundHtml = new Blob("404.html",
                BlobArgs.builder().accountName(storageAccount.getName())
                        .resourceGroupName(resourceGroup.getName())
                        .containerName(staticWebsite.getContainerName())
                        .source(new FileAsset("./wwwroot/404.html"))
                        .contentType("text/html").build());

        // Web endpoint to the website.
        ctx.export("staticEndpoint", storageAccount.getPrimaryEndpoints()
                .applyValue(EndpointsResponse::getWeb));

        // (Optional) Add a CDN in front of the storage account.
        var profile = new Profile("profile",
                ProfileArgs.builder().resourceGroupName(resourceGroup.getName())
                        .location("global")
                        .sku(io.pulumi.azurenative.cdn.inputs.SkuArgs.builder()
                                .name(Either.ofRight(io.pulumi.azurenative.cdn.enums.SkuName.Standard_Microsoft))
                                .build()).build());

        var endpointOrigin = storageAccount.getPrimaryEndpoints()
                .applyValue(pe -> pe.getWeb().replace("https://", "").replace("/", ""));

        var endpoint = new Endpoint("endpoint",
                EndpointArgs.builder().isHttpAllowed(false)
                        .isHttpsAllowed(true)
                        .originHostHeader(endpointOrigin)
                        .origins(DeepCreatedOriginArgs.builder()
                                .hostName(endpointOrigin)
                                .httpsPort(443)
                                .name("origin-storage-account")
                                .build())
                        .profileName(profile.getName())
                        .queryStringCachingBehavior(QueryStringCachingBehavior.NotSet)
                        .resourceGroupName(resourceGroup.getName())
                        .build()
        );


        // CDN endpoint to the website.
        // Allow it some time after the deployment to get ready.
        ctx.export("cdnEndpoint", Output.format("https://%s", endpoint.getHostName()));
        return ctx.exports();
    }
}
