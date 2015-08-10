package org.jboss.shrinkwrap.resolver.bootstrap.maven;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

public final class Resolver {

    private static final String CL_RESOURCE_PATH = "META-INF/resolver/";
    private static final String CL_INDEX = CL_RESOURCE_PATH + "INDEX";

    private static Resolver instance = null;

    public static ClassLoader resolveAsClassLoader(String... artifacts) {
        return getInstance().resolveClassloader(null, artifacts);
    }

    public static ClassLoader resolveAsClassLoader(ClassLoader parent, String... artifacts) {
        return getInstance().resolveClassloader(parent, artifacts);
    }

    public static File[] resolveAsFiles(String... artifacts) {
        return getInstance().resolveFiles(artifacts);
    }

    private static synchronized Resolver getInstance() {
        if (instance == null) {
            instance = new Resolver();
        }
        return instance;
    }

    private ClassLoader cl;

    private Resolver() {
        cl = createInternalClassLoader();
    }

    private ClassLoader createInternalClassLoader() {
        URL[] resources = loadResourceIndexFile();
        return new URLClassLoader(resources, null);
/*
        return new URLClassLoader(resources, null, new URLStreamHandlerFactory() {

            @Override
            public URLStreamHandler createURLStreamHandler(String protocol) {
                System.out.println(protocol);
                if ("jar".equals(protocol)) {
                    return new sun.net.www.protocol.jar.Handler();
                }
                return null;
            }

        });
*/
    }

    private URL[] loadResourceIndexFile() {
        List<URL> uris = new ArrayList<URL>();
        ClassLoader targetClassLoader = Resolver.class.getClassLoader();

        ByteArrayOutputStream byteContent = new ByteArrayOutputStream();
        try {
            IOUtil.copy(targetClassLoader.getResourceAsStream(CL_INDEX), byteContent);
        } catch (Exception e) {
            throw new IllegalStateException("Could not find index file on classpath, " + CL_INDEX, e);
        }

        String content = byteContent.toString();
        String[] files = content.split("\\:");

        for (String path : files) {
            String file = path.substring(path.lastIndexOf("/") + 1, path.length());
            try {
                uris.add(FileUtil.INSTANCE.fileFromClassLoaderResource(CL_RESOURCE_PATH + file, targetClassLoader).toURL());
            } catch (MalformedURLException e) {
                throw new RuntimeException("Could not resolve URL from file: " + file, e);
            }
        }

        return uris.toArray(new URL[] {});
    }

    public ClassLoader resolveClassloader(ClassLoader parent, String[] artifacts) {
        return new URLClassLoader(toURL(resolveFiles(artifacts)), parent);
    }

    private URL[] toURL(File[] resolveFiles) {
        URL[] urls = new URL[resolveFiles.length];
        try {
            for (int i = 0; i < resolveFiles.length; i++) {
                urls[i] = resolveFiles[i].toURL();
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("Could not resolve URL from file: " + resolveFiles, e);
        }
        return urls;
    }

    /*
     * Maven.resolver().resolve("").withTransitivity().asFile()
     */
    public File[] resolveFiles(String[] artifacts) {

        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);

        String mavenClassString = "org.jboss.shrinkwrap.resolver.api.maven.Maven";
        String mavenMethodResolverString = "resolver";
        String mavenMethodResolveString = "resolve";
        String mavenMethodWithTransString = "withTransitivity";
        String mavenMethodAsFileString = "asFile";
        try {
            Class<?> mavenClass = cl.loadClass(mavenClassString);
            Method mavenMethodResolver = mavenClass.getMethod(mavenMethodResolverString);

            Object resolver = mavenMethodResolver.invoke(null);
            Method mavenMethodResolve = resolver.getClass().getMethod(mavenMethodResolveString, String[].class);

            Object resolve = mavenMethodResolve.invoke(resolver, new Object[] { artifacts });
            Method mavenMethodWithTrans = resolve.getClass().getMethod(mavenMethodWithTransString);

            Object withTrans = mavenMethodWithTrans.invoke(resolve);
            Method asFile = withTrans.getClass().getMethod(mavenMethodAsFileString);

            return (File[]) asFile.invoke(withTrans);

        } catch (Exception e) {
            throw new RuntimeException("Could not invoke resolver", e);
        } finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }
    }
}
