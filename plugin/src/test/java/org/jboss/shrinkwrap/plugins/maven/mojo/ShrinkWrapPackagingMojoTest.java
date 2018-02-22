package org.jboss.shrinkwrap.plugins.maven.mojo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.artifact.metadata.ArtifactMetadata;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.OverConstrainedVersionException;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import junit.framework.Assert;

/**
 * Unit test enforcing general function of the 
 * {@link ShrinkWrapPackagingMojo}.
 * 
 * @author Andrew Lee Rubinger <alr@redhat.com>
 *
 */
@SuppressWarnings("deprecation") // ArtifactMetadata is required as an import reference here
//TODO Instead of manual setup as we do here, maybe use a Plexus setup:
// https://maven.apache.org/plugin-developers/plugin-testing.html
public class ShrinkWrapPackagingMojoTest {
	
	private static final String FINAL_NAME = "test-artifact";
    private static final String FILE_EXTENSION = "jar";
	private static final File OUTPUT_DIRECTORY = new File("target");
	
	private ShrinkWrapPackagingMojo mojo;
	private MavenProject project;

	@Before 
	public void createMojo() {
		final MavenProject project = MavenProjectFactory.createTemporaryProject();
		final ShrinkWrapPackagingMojo mojo = new ShrinkWrapPackagingMojo();
		this.mojo = mojo;
		this.project = project;
	}
	
	@Test(expected=IllegalStateException.class)
	public void mustHaveFinalName() {
		this.executeMojo(this.project, OUTPUT_DIRECTORY, null, FILE_EXTENSION);
	}
	
	@Test(expected=IllegalStateException.class)
	public void mustHaveProject() {
		this.executeMojo(null, OUTPUT_DIRECTORY, FINAL_NAME, FILE_EXTENSION);
	}

    @Test(expected=IllegalStateException.class)
    public void mustHaveFileExtension() {
        this.executeMojo(this.project, OUTPUT_DIRECTORY, FINAL_NAME, null);
    }

    @Test(expected=IllegalStateException.class)
	public void mustHaveOutputDirectory() {
		this.executeMojo(this.project, null, FINAL_NAME, FILE_EXTENSION);
	}

	//TODO Below
    /*
     * We turn this one off until we implement
     * discovery of the archive method and class.
     * We get coverage through the testproject tests at the moment
     * while the POC matures.
     */
	@Ignore
	@Test
	public void testPackaging() {
		this.executeMojo(this.project, OUTPUT_DIRECTORY, FINAL_NAME, FILE_EXTENSION);
		final File expectedLocation = new File(OUTPUT_DIRECTORY, FINAL_NAME + ".jar");
		Assert.assertTrue(
				"Could not find artifact at " + expectedLocation.getAbsolutePath(), 
				expectedLocation.exists());
	}
	
	void executeMojo(final MavenProject project,
                     final File outputDirectory,
                     final String finalName,
                     final String fileExtension) {
		this.mojo.setProject(project);
		this.mojo.setOutputDirectory(outputDirectory);
		this.mojo.setFinalName(finalName);
		this.mojo.setFileExtension(fileExtension);
		try{
			mojo.execute();
	
		}catch(final MojoExecutionException | MojoFailureException me) {
			throw new RuntimeException("Error in Mojo execution",me);
		}
	}
	
}

class MavenProjectFactory {
	
	private static final String SYSPROP_IO_TEMPDIR = "java.io.tmpdir";
	private static final String NAME_TMP_PROJECT = "shrinkwrap-test-tmp-project";
	private static final String BUILD_OUTPUT_LOCATION = "target";
	
	/**
	 * Creates a project in the temporary directory; will destroy 
	 * previously-created material under 
	 * {@link MavenProjectFactory#NAME_TMP_PROJECT}.
	 * 
	 * @return the project.
	 */
	public static MavenProject createTemporaryProject() {

		// Make the project
		final MavenProject project = new MavenProject();
		project.setGroupId("org.jboss.shrinkwrap.plugins.maven.mojo.test");
		project.setArtifactId(NAME_TMP_PROJECT);
		project.setVersion("1.0.0-SNAPSHOT");

		// Set up the file reference to the project folder
		final File projectFolder = new File(
				System.getProperty(
						SYSPROP_IO_TEMPDIR),
						NAME_TMP_PROJECT);

		// Scrap out anything already in there.
		if (projectFolder.exists()) {
			try {
				FileUtils.deleteDirectory(projectFolder);
			} catch (final IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}

		// Create the physical structure to the build output
		projectFolder.mkdir();
		project.setFile(projectFolder);
		project.setArtifact(new NoopArtifact());
		final File targetFolder = new File(projectFolder, BUILD_OUTPUT_LOCATION);
		targetFolder.mkdir();
		final Build build = new Build();
		build.setDirectory(projectFolder.getAbsolutePath());
		build.setOutputDirectory(targetFolder.getAbsolutePath());
		build.setFinalName(NAME_TMP_PROJECT + '-' + project.getVersion());
		project.setBuild(build);
		
		// Return
		return project;
	}
	
}

/**
 * No-op implementation implementing the {@link Artifact}
 * interface; used in testing where the object is required but true 
 * support is not.
 *
 */
final class NoopArtifact implements Artifact{

	@Override
	public int compareTo(Artifact arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void addMetadata(ArtifactMetadata arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArtifactHandler getArtifactHandler() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getArtifactId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ArtifactVersion> getAvailableVersions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getBaseVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getClassifier() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDependencyConflictId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArtifactFilter getDependencyFilter() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<String> getDependencyTrail() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDownloadUrl() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public File getFile() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getGroupId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<ArtifactMetadata> getMetadataList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArtifactRepository getRepository() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getScope() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArtifactVersion getSelectedVersion() throws OverConstrainedVersionException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getVersion() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VersionRange getVersionRange() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasClassifier() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isOptional() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isRelease() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isResolved() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSelectedVersionKnown() throws OverConstrainedVersionException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isSnapshot() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void selectVersion(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setArtifactHandler(ArtifactHandler arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setArtifactId(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setAvailableVersions(List<ArtifactVersion> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setBaseVersion(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDependencyFilter(ArtifactFilter arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDependencyTrail(List<String> arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setDownloadUrl(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setFile(File arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setGroupId(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setOptional(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRelease(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setRepository(ArtifactRepository arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResolved(boolean arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setResolvedVersion(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setScope(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVersion(String arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setVersionRange(VersionRange arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void updateVersion(String arg0, ArtifactRepository arg1) {
		// TODO Auto-generated method stub
		
	}
	
}
