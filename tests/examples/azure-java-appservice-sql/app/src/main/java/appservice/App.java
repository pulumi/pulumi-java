package appservice;

import io.pulumi.Config;
import io.pulumi.Pulumi;
import io.pulumi.asset.FileArchive;
import io.pulumi.azurenative.insights.Component;
import io.pulumi.azurenative.insights.ComponentArgs;
import io.pulumi.azurenative.insights.enums.ApplicationType;
import io.pulumi.azurenative.resources.ResourceGroup;
import io.pulumi.azurenative.sql.Database;
import io.pulumi.azurenative.sql.DatabaseArgs;
import io.pulumi.azurenative.sql.Server;
import io.pulumi.azurenative.sql.ServerArgs;
import io.pulumi.azurenative.storage.Blob;
import io.pulumi.azurenative.storage.BlobArgs;
import io.pulumi.azurenative.storage.BlobContainer;
import io.pulumi.azurenative.storage.BlobContainerArgs;
import io.pulumi.azurenative.storage.ListStorageAccountServiceSAS;
import io.pulumi.azurenative.storage.StorageAccount;
import io.pulumi.azurenative.storage.StorageAccountArgs;
import io.pulumi.azurenative.storage.enums.HttpProtocol;
import io.pulumi.azurenative.storage.enums.Kind;
import io.pulumi.azurenative.storage.enums.Permissions;
import io.pulumi.azurenative.storage.enums.PublicAccess;
import io.pulumi.azurenative.storage.enums.SignedResource;
import io.pulumi.azurenative.storage.enums.SkuName;
import io.pulumi.azurenative.storage.inputs.ListStorageAccountServiceSASArgs;
import io.pulumi.azurenative.storage.inputs.SkuArgs;
import io.pulumi.azurenative.storage.outputs.ListStorageAccountServiceSASResult;
import io.pulumi.azurenative.web.AppServicePlan;
import io.pulumi.azurenative.web.AppServicePlanArgs;
import io.pulumi.azurenative.web.WebApp;
import io.pulumi.azurenative.web.WebAppArgs;
import io.pulumi.azurenative.web.enums.ConnectionStringType;
import io.pulumi.azurenative.web.inputs.ConnStringInfoArgs;
import io.pulumi.azurenative.web.inputs.NameValuePairArgs;
import io.pulumi.azurenative.web.inputs.SiteConfigArgs;
import io.pulumi.azurenative.web.inputs.SkuDescriptionArgs;
import io.pulumi.context.ExportContext;
import io.pulumi.context.StackContext;
import io.pulumi.core.Either;
import io.pulumi.core.Output;
import io.pulumi.deployment.InvokeOptions;

public class App {
    public static void main(String[] args) {
        int exitCode = Pulumi.run(App::stack);
        System.exit(exitCode);
    }

    private static ExportContext stack(StackContext ctx) {
        var resourceGroup = new ResourceGroup("resourceGroup");

        var storageAccount = new StorageAccount("sa",
                StorageAccountArgs.builder().resourceGroupName(resourceGroup.getName())
                        .kind(Either.ofRight(Kind.StorageV2))
                        .sku(SkuArgs.builder().name(Either.ofRight(SkuName.Standard_LRS)).build())
                        .build());

        var storageContainer = new BlobContainer("container",
                BlobContainerArgs.builder().resourceGroupName(resourceGroup.getName())
                        .accountName(storageAccount.getName())
                        .publicAccess(PublicAccess.None)
                        .build());

        var blob = new Blob("blob",
                BlobArgs.builder().resourceGroupName(resourceGroup.getName())
                        .accountName(storageAccount.getName())
                        .containerName(storageContainer.getName())
                        .source(new FileArchive("wwwroot"))
                        .build());

        var codeBlobUrl = getSASToken(storageAccount.getName(), storageContainer.getName(), blob.getName(), resourceGroup.getName());

        var appInsights = new Component("ai",
                ComponentArgs.builder().resourceGroupName(resourceGroup.getName())
                        .kind("web")
                        .applicationType(Either.ofRight(ApplicationType.Web))
                        .build());

        var username = "pulumi";

        // Get the password to use for SQL from config.
        var config = Config.of();
        var pwd = config.require("sqlPassword");

        var sqlServer = new Server("sqlserver",
                ServerArgs.builder().resourceGroupName(resourceGroup.getName())
                        .administratorLogin(username)
                        .administratorLoginPassword(pwd)
                        .version("12.0")
                        .build());

        var database = new Database("db",
                DatabaseArgs.builder().resourceGroupName(resourceGroup.getName())
                        .serverName(sqlServer.getName())
                        .sku(io.pulumi.azurenative.sql.inputs.SkuArgs.builder().name("S0").build())
                        .build());

        var appServicePlan = new AppServicePlan("asp",
                AppServicePlanArgs.builder().resourceGroupName(resourceGroup.getName())
                        .kind("App")
                        .sku(SkuDescriptionArgs.builder().name("B1").tier("Basic").build())
                        .build());

        var app = new WebApp("webapp",
                WebAppArgs.builder().resourceGroupName(resourceGroup.getName())
                        .serverFarmId(appServicePlan.getId())
                        .siteConfig(SiteConfigArgs.builder()
                                .appSettings(
                                        NameValuePairArgs.builder()
                                                .name("APPINSIGHTS_INSTRUMENTATIONKEY")
                                                .value(appInsights.getInstrumentationKey())
                                                .build(),
                                        NameValuePairArgs.builder()
                                                .name("APPLICATIONINSIGHTS_CONNECTION_STRING")
                                                .value(Output.format("InstrumentationKey=%s", appInsights.getInstrumentationKey()))
                                                .build(),
                                        NameValuePairArgs.builder()
                                                .name("ApplicationInsightsAgent_EXTENSION_VERSION")
                                                .value("~2")
                                                .build(),
                                        NameValuePairArgs.builder()
                                                .name("WEBSITE_RUN_FROM_PACKAGE")
                                                .value(codeBlobUrl)
                                                .build())
                                .connectionStrings(
                                        ConnStringInfoArgs.builder()
                                                .name("db")
                                                .connectionString(
                                                        Output.format(
                                                                "Server=tcp:%s.database.windows.net;initial catalog=%s;user ID=%s;password=%s;Min Pool Size=0;Max Pool Size=30;Persist Security Info=true;",
                                                                sqlServer.getName(), database.getName(), Output.of(username), Output.of(pwd)))
                                                .type(ConnectionStringType.SQLAzure)
                                                .build())
                                .build())
                        .httpsOnly(true)
                        .build());

        return ctx.export("endpoint", Output.format("https://%s", app.getDefaultHostName()));
    }

    private static Output<String> getSASToken(Output<String> storageAccountName, Output<String> storageContainerName,
                                              Output<String> blobName, Output<String> resourceGroupName) {
        var blobSAS = Output.tuple(resourceGroupName, storageAccountName, storageContainerName).applyFuture(t ->
                ListStorageAccountServiceSAS.invokeAsync(
                        ListStorageAccountServiceSASArgs.builder().resourceGroupName(t.t1)
                                .accountName(t.t2)
                                .protocols(HttpProtocol.Https)
                                .sharedAccessStartTime("2022-01-01")
                                .sharedAccessExpiryTime("2030-01-01")
                                .resource(Either.ofRight(SignedResource.C))
                                .permissions(Either.ofRight(Permissions.R))
                                .canonicalizedResource(String.format("/blob/%s/%s", t.t2, t.t3))
                                .contentType("application/json")
                                .cacheControl("max-age=5")
                                .contentDisposition("inline")
                                .contentEncoding("deflate")
                                .build(),
                        InvokeOptions.Empty));
        var token = blobSAS.applyValue(ListStorageAccountServiceSASResult::getServiceSasToken);
        return Output.format("https://%s.blob.core.windows.net/%s/%s?%s", storageAccountName, storageContainerName, blobName, token);
    }
}
