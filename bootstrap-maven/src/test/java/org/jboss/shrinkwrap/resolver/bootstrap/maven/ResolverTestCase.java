package org.jboss.shrinkwrap.resolver.bootstrap.maven;

import org.junit.Test;

public class ResolverTestCase {

    @Test
    public void test() throws Exception {

        ClassLoader cl = Resolver.resolveAsClassLoader(
                "org.jboss.arquillian.junit:arquillian-junit-container:1.1.8.Final",
                "junit:junit:4.12");

        System.out.println(cl.loadClass("org.jboss.arquillian.junit.Arquillian"));
    }
}
