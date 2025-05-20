package generated_program;

import com.pulumi.Context;
import com.pulumi.Pulumi;
import com.pulumi.core.Output;
import com.pulumi.assetarchive.AssetResource;
import com.pulumi.assetarchive.AssetResourceArgs;
import com.pulumi.assetarchive.ArchiveResource;
import com.pulumi.assetarchive.ArchiveResourceArgs;
import com.pulumi.asset.AssetArchive;
import com.pulumi.asset.FileArchive;
import com.pulumi.asset.FileAsset;
import com.pulumi.asset.RemoteAsset;
import com.pulumi.asset.StringAsset;
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
        var ass = new AssetResource("ass", AssetResourceArgs.builder()
            .value(new FileAsset("../test.txt"))
            .build());

        var arc = new ArchiveResource("arc", ArchiveResourceArgs.builder()
            .value(new FileArchive("../archive.tar"))
            .build());

        var dir = new ArchiveResource("dir", ArchiveResourceArgs.builder()
            .value(new FileArchive("../folder"))
            .build());

        var assarc = new ArchiveResource("assarc", ArchiveResourceArgs.builder()
            .value(new AssetArchive(Map.ofEntries(
                Map.entry("string", new StringAsset("file contents")),
                Map.entry("file", new FileAsset("../test.txt")),
                Map.entry("folder", new FileArchive("../folder")),
                Map.entry("archive", new FileArchive("../archive.tar"))
            )))
            .build());

        var remoteass = new AssetResource("remoteass", AssetResourceArgs.builder()
            .value(new RemoteAsset("https://raw.githubusercontent.com/pulumi/pulumi/7b0eb7fb10694da2f31c0d15edf671df843e0d4c/cmd/pulumi-test-language/tests/testdata/l2-resource-asset-archive/test.txt"))
            .build());

    }
}
