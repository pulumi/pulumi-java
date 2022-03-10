package appservice;

import java.util.ArrayList;
import java.util.List;

import io.pulumi.Config;
import io.pulumi.Stack;
import io.pulumi.azurenative.insights.Component;
import io.pulumi.azurenative.insights.ComponentArgs;
import io.pulumi.azurenative.insights.enums.ApplicationType;
import io.pulumi.azurenative.resources.ResourceGroup;
import io.pulumi.azurenative.resources.ResourceGroupArgs;
import io.pulumi.azurenative.sql.Database;
import io.pulumi.azurenative.sql.DatabaseArgs;
import io.pulumi.azurenative.sql.Server;
import io.pulumi.azurenative.sql.ServerArgs;
import io.pulumi.azurenative.storage.*;
import io.pulumi.azurenative.storage.enums.*;
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
import io.pulumi.core.Archive;
import io.pulumi.core.Either;
import io.pulumi.core.Input;
import io.pulumi.core.Output;
import io.pulumi.core.annotations.OutputExport;
import io.pulumi.deployment.InvokeOptions;
import io.pulumi.resources.CustomResourceOptions;

public final class MyStack extends Stack {

    @OutputExport(name="endpoint", type=String.class)
    private final Output<String> endpoint;

    public MyStack() {
        var resourceGroup = new ResourceGroup("resourceGroup");

        var storageAccount = new StorageAccount("sa",
                StorageAccountArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setKind(Either.ofRight(Kind.StorageV2))
                        .setSku(SkuArgs.builder().setName(Either.ofRight(SkuName.Standard_LRS)).build())
                        .build());

        var storageContainer = new BlobContainer("container",
                BlobContainerArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setAccountName(storageAccount.getName().toInput())
                        .setPublicAccess(PublicAccess.None)
                        .build());

        var blob = new Blob("blob",
                BlobArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setAccountName(storageAccount.getName().toInput())
                        .setContainerName(storageContainer.getName().toInput())
                        .setSource(new Archive.FileArchive("wwwroot"))
                        .build());

        var codeBlobUrl = getSASToken(storageAccount.getName(), storageContainer.getName(), blob.getName(), resourceGroup.getName());

        var appInsights = new Component("ai",
                ComponentArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setKind("web")
                        .setApplicationType(Either.ofRight(ApplicationType.Web))
                        .build());

        var username = "pulumi";

        // Get the password to use for SQL from config.
        var config = Config.of();
        var pwd = config.require("sqlPassword");

        var sqlServer = new Server("sqlserver",
                ServerArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setAdministratorLogin(username)
                        .setAdministratorLoginPassword(pwd)
                        .setVersion("12.0")
                        .build());

       var database = new Database("db",
                DatabaseArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setServerName(sqlServer.getName().toInput())
                        .setSku(io.pulumi.azurenative.sql.inputs.SkuArgs.builder().setName("S0").build())
                        .build());

        var appServicePlan = new AppServicePlan("asp",
                AppServicePlanArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setKind("App")
                        .setSku(SkuDescriptionArgs.builder().setName("B1").setTier("Basic").build())
                        .build());

        var app = new WebApp("webapp",
                WebAppArgs.builder()
                        .setResourceGroupName(resourceGroup.getName().toInput())
                        .setServerFarmId(appServicePlan.getId().toInput())
                        .setSiteConfig(SiteConfigArgs.builder()
                                .setAppSettings(List.of(
                                        NameValuePairArgs.builder()
                                            .setName("APPINSIGHTS_INSTRUMENTATIONKEY")
                                            .setValue(appInsights.getInstrumentationKey().toInput())
                                            .build(),
                                        NameValuePairArgs.builder()
                                            .setName("APPLICATIONINSIGHTS_CONNECTION_STRING")
                                            .setValue(Output.format("InstrumentationKey=%s", appInsights.getInstrumentationKey()).toInput())
                                            .build(),
                                        NameValuePairArgs.builder()
                                            .setName("ApplicationInsightsAgent_EXTENSION_VERSION")
                                            .setValue("~2")
                                            .build(),
                                        NameValuePairArgs.builder()
                                            .setName("WEBSITE_RUN_FROM_PACKAGE")
                                            .setValue(codeBlobUrl.toInput())
                                            .build()))
                                .setConnectionStrings(List.of(
                                        ConnStringInfoArgs.builder()
                                            .setName("db")
                                            .setConnectionString(
                                                Output.format(
                                    "Server=tcp:%s.database.windows.net;initial catalog=%s;user ID=%s;password=%s;Min Pool Size=0;Max Pool Size=30;Persist Security Info=true;",
                                                   sqlServer.getName(), database.getName(), Input.of(username), Input.of(pwd)).toInput())
                                            .setType(ConnectionStringType.SQLAzure)
                                            .build()))
                                .build())
                        .setHttpsOnly(true)
                        .build());

        this.endpoint = Output.format("https://%s", app.getDefaultHostName());
    }

    private Output<String> getSASToken(Output<String> storageAccountName, Output<String> storageContainerName,
                                       Output<String> blobName, Output<String> resourceGroupName) {
        var blobSAS = Output.tuple(resourceGroupName, storageAccountName, storageContainerName).applyFuture(t ->
            ListStorageAccountServiceSAS.invokeAsync(
                ListStorageAccountServiceSASArgs.builder()
                        .setResourceGroupName(t.t1)
                        .setAccountName(t.t2)
                        .setProtocols(HttpProtocol.Https)
                        .setSharedAccessStartTime("2022-01-01")
                        .setSharedAccessExpiryTime("2030-01-01")
                        .setResource(Either.ofRight(SignedResource.C))
                        .setPermissions(Either.ofRight(Permissions.R))
                        .setCanonicalizedResource(String.format("/blob/%s/%s", t.t2, t.t3))
                        .setContentType("application/json")
                        .setCacheControl("max-age=5")
                        .setContentDisposition("inline")
                        .setContentEncoding("deflate")
                        .build(),
                InvokeOptions.Empty));
        var token = blobSAS.applyValue(ListStorageAccountServiceSASResult::getServiceSasToken);
        return Output.format("https://%s.blob.core.windows.net/%s/%s?%s", storageAccountName, storageContainerName, blobName, token);
    }
}
