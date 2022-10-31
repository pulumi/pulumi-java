package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.azurenative.resources.ResourceGroup;
import com.pulumi.azurenative.storage.StorageAccount;
import com.pulumi.azurenative.storage.StorageAccountArgs;
import com.pulumi.azurenative.storage.inputs.SkuArgs;
import com.pulumi.azurenative.storage.BlobContainer;
import com.pulumi.azurenative.storage.BlobContainerArgs;
import com.pulumi.azurenative.web.AppServicePlan;
import com.pulumi.azurenative.web.AppServicePlanArgs;
import com.pulumi.azurenative.web.inputs.SkuDescriptionArgs;
import com.pulumi.azurenative.storage.Blob;
import com.pulumi.azurenative.storage.BlobArgs;
import com.pulumi.azurenative.insights.Component;
import com.pulumi.azurenative.insights.ComponentArgs;
import com.pulumi.random.RandomPassword;
import com.pulumi.random.RandomPasswordArgs;
import com.pulumi.azurenative.sql.Server;
import com.pulumi.azurenative.sql.ServerArgs;
import com.pulumi.azurenative.sql.Database;
import com.pulumi.azurenative.sql.DatabaseArgs;
import com.pulumi.azurenative.sql.inputs.SkuArgs;
import com.pulumi.azurenative.web.WebApp;
import com.pulumi.azurenative.web.WebAppArgs;
import com.pulumi.azurenative.web.inputs.SiteConfigArgs;
import com.pulumi.asset.FileArchive;
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
        final var sqlAdmin = config.get("sqlAdmin").orElse("pulumi");
        var appservicegroup = new ResourceGroup("appservicegroup");

        var sa = new StorageAccount("sa", StorageAccountArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .kind("StorageV2")
            .sku(SkuArgs.builder()
                .name("Standard_LRS")
                .build())
            .build());

        var container = new BlobContainer("container", BlobContainerArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .accountName(sa.name())
            .publicAccess("None")
            .build());

        final var blobAccessToken = Output.ofSecret(Output.tuple(sa.name(), appservicegroup.name(), sa.name(), container.name()).applyValue(values -> {
            var saName = values.t1;
            var appservicegroupName = values.t2;
            var saName1 = values.t3;
            var containerName = values.t4;
            return StorageFunctions.listStorageAccountServiceSAS();
        }).applyValue(invoke -> invoke.serviceSasToken()));

        var appserviceplan = new AppServicePlan("appserviceplan", AppServicePlanArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .kind("App")
            .sku(SkuDescriptionArgs.builder()
                .name("B1")
                .tier("Basic")
                .build())
            .build());

        var blob = new Blob("blob", BlobArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .accountName(sa.name())
            .containerName(container.name())
            .type("Block")
            .source(new FileArchive("./www"))
            .build());

        var appInsights = new Component("appInsights", ComponentArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .applicationType("web")
            .kind("web")
            .build());

        var sqlPassword = new RandomPassword("sqlPassword", RandomPasswordArgs.builder()        
            .length(16)
            .special(true)
            .build());

        var sqlServer = new Server("sqlServer", ServerArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .administratorLogin(sqlAdmin)
            .administratorLoginPassword(sqlPassword.result())
            .version("12.0")
            .build());

        var db = new Database("db", DatabaseArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .serverName(sqlServer.name())
            .sku(SkuArgs.builder()
                .name("S0")
                .build())
            .build());

        var app = new WebApp("app", WebAppArgs.builder()        
            .resourceGroupName(appservicegroup.name())
            .serverFarmId(appserviceplan.id())
            .siteConfig(SiteConfigArgs.builder()
                .appSettings(                
                    NameValuePairArgs.builder()
                        .name("WEBSITE_RUN_FROM_PACKAGE")
                        .value(Output.tuple(sa.name(), container.name(), blob.name(), blobAccessToken).applyValue(values -> {
                            var saName = values.t1;
                            var containerName = values.t2;
                            var blobName = values.t3;
                            var blobAccessToken = values.t4;
                            return String.format("https://%s.blob.core.windows.net/%s/%s?%s", saName,containerName,blobName,blobAccessToken);
                        }))
                        .build(),
                    NameValuePairArgs.builder()
                        .name("APPINSIGHTS_INSTRUMENTATIONKEY")
                        .value(appInsights.instrumentationKey())
                        .build(),
                    NameValuePairArgs.builder()
                        .name("APPLICATIONINSIGHTS_CONNECTION_STRING")
                        .value(appInsights.instrumentationKey().applyValue(instrumentationKey -> String.format("InstrumentationKey=%s", instrumentationKey)))
                        .build(),
                    NameValuePairArgs.builder()
                        .name("ApplicationInsightsAgent_EXTENSION_VERSION")
                        .value("~2")
                        .build())
                .connectionStrings(ConnStringInfoArgs.builder()
                    .name("db")
                    .type("SQLAzure")
                    .connectionString(Output.tuple(sqlServer.name(), db.name(), sqlPassword.result()).applyValue(values -> {
                        var sqlServerName = values.t1;
                        var dbName = values.t2;
                        var result = values.t3;
                        return String.format("Server= tcp:%s.database.windows.net;initial catalog=%s;userID=%s;password=%s;Min Pool Size=0;Max Pool Size=30;Persist Security Info=true;", sqlServerName,dbName,sqlAdmin,result);
                    }))
                    .build())
                .build())
            .build());

        ctx.export("endpoint", app.defaultHostName());
    }
}
