package staticwebsite;

import java.util.List;

import io.pulumi.Config;
import io.pulumi.Stack;
import io.pulumi.azurenative.cdn.Endpoint;
import io.pulumi.azurenative.cdn.EndpointArgs;
import io.pulumi.azurenative.cdn.Profile;
import io.pulumi.azurenative.cdn.ProfileArgs;
import io.pulumi.azurenative.cdn.enums.QueryStringCachingBehavior;
import io.pulumi.azurenative.cdn.inputs.DeepCreatedOriginArgs;
import io.pulumi.azurenative.resources.ResourceGroup;
import io.pulumi.azurenative.resources.ResourceGroupArgs;
import io.pulumi.azurenative.storage.Blob;
import io.pulumi.azurenative.storage.BlobArgs;
import io.pulumi.azurenative.storage.StorageAccount;
import io.pulumi.azurenative.storage.StorageAccountArgs;
import io.pulumi.azurenative.storage.StorageAccountStaticWebsite;
import io.pulumi.azurenative.storage.StorageAccountStaticWebsiteArgs;
import io.pulumi.azurenative.storage.enums.Kind;
import io.pulumi.azurenative.storage.enums.SkuName;
import io.pulumi.azurenative.storage.inputs.SkuArgs;
import io.pulumi.core.Asset;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.resources.CustomResourceOptions;

public final class MyStack extends Stack {

    @OutputExport(type = String.class)
    private Output<String> staticEndpoint;

    @OutputExport(type = String.class)
    private Output<String> cdnEndpoint;

    public MyStack() {
        var resourceGroup = new ResourceGroup("resourceGroup",
                ResourceGroupArgs.builder().build(),
                CustomResourceOptions.Empty);

        var storageAccount = new StorageAccount("storageaccount",
                StorageAccountArgs.builder()
                        .setKind(Either.ofRight(Kind.StorageV2))
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setSku(SkuArgs.builder()
                                .setName(Either.ofRight(SkuName.Standard_LRS))
                                .build())
                        .build(),
                CustomResourceOptions.Empty);

        var staticWebsite = new StorageAccountStaticWebsite("staticWebsite",
                StorageAccountStaticWebsiteArgs.builder()
                        .setAccountName(storageAccount.getName().toInput())
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setIndexDocument("index.html")
                        .setError404Document("404.html")
                        .build(),
                CustomResourceOptions.Empty);

        var indexHtml = new Blob("index.html",
                BlobArgs.builder()
                        .setAccountName(storageAccount.getName().toInput())
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setContainerName(staticWebsite.getContainerName().toInput())
                        .setSource(new Asset.FileAsset("./wwwroot/index.html"))
                        .setContentType("text/html")
                        .build(),
                CustomResourceOptions.Empty);

        var notFoundHtml = new Blob("404.html",
                BlobArgs.builder()
                        .setAccountName(storageAccount.getName().toInput())
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setContainerName(staticWebsite.getContainerName().toInput())
                        .setSource(new Asset.FileAsset("./wwwroot/404.html"))
                        .setContentType("text/html")
                        .build(),
                CustomResourceOptions.Empty);

        // Web endpoint to the website.
        this.staticEndpoint = storageAccount.getPrimaryEndpoints()
                .applyValue(primaryEndpoints -> primaryEndpoints.getWeb());

        // (Optional) Add a CDN in front of the storage account.
        var profile = new Profile("profile", ProfileArgs.builder()
                .setResourceGroupName(resourceGroup.getName().toInput())
                .setLocation("global")
                .setSku(io.pulumi.azurenative.cdn.inputs.SkuArgs.builder()
                        .setName(Either.ofRight(io.pulumi.azurenative.cdn.enums.SkuName.Standard_Microsoft))
                        .build())
                .build(),
                CustomResourceOptions.Empty);

        var endpointOrigin = storageAccount.getPrimaryEndpoints()
                .applyValue(pe -> pe.getWeb().replace("https://", "").replace("/", ""));

        var endpoint = new Endpoint("endpoint",
                EndpointArgs.builder()
                        .setIsHttpAllowed(false)
                        .setIsHttpsAllowed(true)
                        .setOriginHostHeader(endpointOrigin.toInput())
                        .setOrigins(List.of(DeepCreatedOriginArgs.builder()
                                .setHostName(endpointOrigin.toInput())
                                .setHttpsPort(443)
                                .setName("origin-storage-account")
                                .build()))
                        .setProfileName(profile.getName().toInput())
                        .setQueryStringCachingBehavior(QueryStringCachingBehavior.NotSet)
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .build(),
                CustomResourceOptions.Empty);


        // CDN endpoint to the website.
        // Allow it some time after the deployment to get ready.
        this.cdnEndpoint = endpoint.getHostName()
                .applyValue(hostName -> String.format("https://%s", hostName));
    }
}
