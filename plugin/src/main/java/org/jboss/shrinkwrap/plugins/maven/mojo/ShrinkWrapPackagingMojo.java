package org.jboss.shrinkwrap.plugins.maven.mojo;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.Assignable;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;

/**
 * An execution which defines an artifact as assembled by
 * a ShrinkWrap {@link Archive}
 */
@Mojo( name = "shrinkwrap", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.TEST)
public final class ShrinkWrapPackagingMojo
    extends AbstractMojo
{
	@Component
	private MavenProject project;

    @Parameter( defaultValue = "${project.build.directory}", property = "shrinkwrap.outputDir", required = true )
    private File outputDirectory;

    @Parameter(property = "project.build.finalName", readonly = true)
    private String finalName;

    @Parameter(defaultValue = "jar", property = "shrinkwrap.fileExtension", readonly = true)
    private String fileExtension;

    /**
     * @throws IllegalStateException If the {@link ShrinkWrapPackagingMojo#finalName},
     * {@link ShrinkWrapPackagingMojo#project}, {@link ShrinkWrapPackagingMojo#fileExtension},
     * or {@link ShrinkWrapPackagingMojo#outputDirectory) are not set
     */
    @Override
	public void execute() throws MojoExecutionException, MojoFailureException, IllegalStateException {

        // Precondition checks
        if (project == null) {
            throw new IllegalStateException("project is not set");
        }
        if (outputDirectory == null) {
            throw new IllegalStateException("outputDirectory is not set");
        }
        if (finalName == null || finalName.length() == 0) {
            throw new IllegalStateException("finalName is not set");
        }
        if (fileExtension == null || fileExtension.length() == 0) {
            throw new IllegalStateException("fileExtension is not set");
        }

        final String artifactName = this.outputDirectory.getAbsolutePath() +
                '/' +
                this.finalName +
                '.' +
                this.fileExtension;
        final File artifact = new File(artifactName);

        this.getLog().info("Output Directory: " + this.outputDirectory);
        this.getLog().info("Final Name: " + this.finalName);
        this.getLog().info("File Extension: " + this.fileExtension);
        this.getLog().info("Artifact to be created: " + artifactName);


        // Get at the ClassPath URLs of the module running the Plugin
        final List<String> classPathElements;
        try {
            classPathElements = this.project.getTestClasspathElements();
        } catch (final DependencyResolutionRequiredException drre) {
            throw new RuntimeException(drre);
        }
        final int numElements = classPathElements.size();
        final URL[] urls = new URL[numElements];
        for (int i = 0; i < numElements; i++) {
            final String element = classPathElements.get(i);
            final URL url;
            try {
                url = new File(element).toURI().toURL();
            } catch (final MalformedURLException murle) {
                throw new RuntimeException(
                        "Could not construct URL reference from classpath element in build: " +
                                element, murle);
            }
            urls[i] = url;
            this.getLog().info("ClassPath: " + element);
        }

        // Construct a new isolated ClassLoader that can search the classpath of the module
        final URLClassLoader cl = new URLClassLoader(urls, null);
        final ClassLoader tccl = Thread.currentThread().getContextClassLoader();

        //TODO Below
        /*
         * Don't hardcode the class and method containing the archive
         */
        try {
            //TODO Make configurable/discoverable
            final String className = "org.jboss.shrinkwrap.plugins.maven.testproject.ShrinkWrapArtifact";
            final Class<?> clazz = cl.loadClass(className);
            this.getLog().info("GOT CLASS: " + clazz);
            //TODO Put the TCCL nonsense behind priv actions so they work in a security context
            Thread.currentThread().setContextClassLoader(cl);
            //TODO Make configurable/discoverable
            final String methodName = "build";
            final Method method;
            try {
                method = clazz.getMethod(methodName, null);
            } catch (final NoSuchMethodException nsme) {
                throw new RuntimeException("Could not find method defining the artifact archive", nsme);
            }
            final Object o;
            try {
                o = method.invoke(null, null);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Could not invoke method to get the archive ", e);
            }

            // This object should be an "Assignable" type capable of the operation "as"
            // so represent it as a ZipExporter and export
            final String asMethodName = "as";
            final Class<?> assignableClass = cl.loadClass(Assignable.class.getName());
            final Method asMethod;
            try {
                asMethod = assignableClass.getMethod(asMethodName, Class.class);
            } catch (final NoSuchMethodException nsme) {
                throw new RuntimeException("Plugin error, likely not user related", nsme);
            }
            final Class<?> zipExporterClass = cl.loadClass(ZipExporter.class.getName());
            final Object zipExporter;
            try{
                zipExporter = asMethod.invoke(o,zipExporterClass);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Could not represent as a " +
                        ZipExporter.class.getSimpleName(), e);
            }
            final String exportToMethodName = "exportTo";
            final Method exportTo;
            try {
                exportTo = zipExporterClass.getMethod(exportToMethodName, File.class);
            } catch (final NoSuchMethodException nsme) {
                throw new RuntimeException("Plugin error, likely not user related", nsme);
            }

            // Serialize the mother
            try{
                exportTo.invoke(zipExporter,artifact);
            } catch (final IllegalAccessException | InvocationTargetException e) {
                throw new RuntimeException("Could not export as serialized ZIP file " +
                        artifact.getAbsolutePath(),e);
            }

        } catch (final ClassNotFoundException cnfe) {
            throw new RuntimeException(cnfe);
        }
        finally {
            Thread.currentThread().setContextClassLoader(tccl);
        }

        // Set the artifact with our serialized ShrinkWrap archive
        this.project.getArtifact().setFile(artifact);
    }

    /*
     * Test-only methods below this line.
     * 
     * Keep access to package-level (default).
     * 
     * Because of the way tests are set up, we don't do precondition checking
     * here; we check state is enforced in the {@link ShrinkWrapPackagingMojo#execute}
     * method when things are ready to be run.
     */

    void setProject(final MavenProject project) {
		this.project = project;
	}
	void setOutputDirectory(File outputDirectory) {
		this.outputDirectory = outputDirectory;
	}
	void setFinalName(String finalName) {
		this.finalName = finalName;
	}
    void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }

}
