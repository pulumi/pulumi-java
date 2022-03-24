package random;

import com.google.common.base.MoreObjects;
import io.pulumi.deployment.Deployment;

import java.io.File;
import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class App {
    public static void main(String[] args) {
        // simple
        var list2 = getResources2("io/pulumi/random/version.txt");
        for (var name : list2) {
            System.out.println(name);
        }
//        System.exit(0);

        // complex
        var list = getResources(Pattern.compile(".*/version.txt"));
        for (var name : list) {
            System.out.println(name);
        }
        System.exit(0);

        Integer exitCode = Deployment.runAsyncStack(MyStack.class).join();
        System.exit(exitCode);
    }

    // simple -----

    public static Collection<String> getResources2(String name) {
        ClassLoader loader = MoreObjects.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                App.class.getClassLoader()
        );
        final Enumeration<URL> urls;
        try {
            urls = loader.getResources(name);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var resources = new ArrayList<String>();
        while (urls.hasMoreElements()) {
            var url = urls.nextElement();
            resources.addAll(getResources2(url));
        }
        return resources;
    }

    public static Collection<String> getResources2(URL url) {
        final JarURLConnection conn;
        try {
            conn = (JarURLConnection) url.openConnection();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final JarFile jarfile;
        try {
            jarfile = conn.getJarFile();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var resources = new ArrayList<String>();
        var es = jarfile.entries();
        while (es.hasMoreElements()) {
            var ze = (ZipEntry) es.nextElement();
            var fileName = ze.getName();
            if (url.getPath().endsWith(fileName)) {
                resources.add(fileName);
            }
        }
        try {
            jarfile.close();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
        return resources;
    }

    // complex -----

    public static Collection<String> getResources(Pattern pattern) {
        // this is hacky
        var classPath = System.getProperty("java.class.path", ".");
        var classPathElements = classPath.split(System.getProperty("path.separator"));
        var resources = new ArrayList<String>();
        for (var element : classPathElements) {
            resources.addAll(getResources(element, pattern));
        }
        return resources;
    }

    private static Collection<String> getResources(
            final String element,
            final Pattern pattern
    ) {
        var resources = new ArrayList<String>();
        var file = new File(element);
        if (!file.exists()) {
            return resources;
        }
        if (file.isDirectory()) {
            resources.addAll(getResourcesFromDirectory(file, pattern));
        } else {
            resources.addAll(getResourcesFromJarFile(file, pattern));
        }
        return resources;
    }

    private static Collection<String> getResourcesFromDirectory(
            final File directory,
            final Pattern pattern
    ) {
        var resources = new ArrayList<String>();
        var fileList = Objects.requireNonNull(directory.listFiles());
        for (var file : fileList) {
            if (file.isDirectory()) {
                resources.addAll(getResourcesFromDirectory(file, pattern));
            } else {
                try {
                    var fileName = file.getCanonicalPath();
                    var accept = pattern.matcher(fileName).matches();
                    if (accept) {
                        resources.add(fileName);
                    }
                } catch (final IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return resources;
    }

    private static Collection<String> getResourcesFromJarFile(
            File file,
            Pattern pattern
    ) {
        final ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        var resources = new ArrayList<String>();
        var es = zf.entries();
        while (es.hasMoreElements()) {
            var ze = (ZipEntry) es.nextElement();
            var fileName = ze.getName();
            var accept = pattern.matcher(fileName).matches();
            if (accept) {
                resources.add(fileName);
            }
        }
        try {
            zf.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return resources;
    }
}
