package org.jboss.shrinkwrap.plugins.maven.testproject;

import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.junit.Assert;

import java.util.Map;

/**
 * Defines the artifact assembly that will be used
 */
public class ShrinkWrapArtifact {

    /**
     * Will be invoked by the shrinkwrap-maven-plugin to
     * determine what contents should compose this module's artifact.
     * The archive defined here effectively replaces whatever the JAR plugin
     * would have created in the package phase.
     * @return
     */
    //TODO BELOW
    /*
      Define here some way (annotation, likely)
      that the plugin knows what class and method to call
      to get this archive.
     */
    public static JavaArchive build() {
        return ShrinkWrap.create(JavaArchive.class).
                // Add a class from src/main/java
                addClass(TestClass.class).
                // Add a class from the JDK
                addClass(Map.class).
                // Add a class from the test classpath
                addClass(Assert.class);
    }

}
