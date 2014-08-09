package com.bluetrainsoftware.maven

import groovy.transform.CompileStatic
import org.apache.maven.artifact.Artifact
import org.apache.maven.plugin.AbstractMojo
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.LifecyclePhase
import org.apache.maven.plugins.annotations.Mojo
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.plugins.annotations.ResolutionScope
import org.apache.maven.project.MavenProject
import org.eclipse.jetty.util.resource.Resource
import org.eclipse.jetty.util.resource.ResourceCollection

/**
 *
 * @author: Richard Vowles - https://plus.google.com/+RichardVowles
 */
@CompileStatic
@Mojo(name="compile", requiresProject = true, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME, defaultPhase = LifecyclePhase.GENERATE_RESOURCES)
class JawrStaticizerMojo extends AbstractMojo {
	@Parameter(required = true, readonly = true, property = "project")
	protected MavenProject project

	@Override
	void execute() throws MojoExecutionException, MojoFailureException {
		if (project.packaging == "pom" || project.packaging == "tile") {
			return
		}

		File jawrFile = new File(project.basedir, "src/main/resources/META-INF/jawr.properties")

		if (jawrFile.exists()) {
			log.info("Processing resources in ${jawrFile.absolutePath}")
			processJawrProperties(jawrFile)
		} else {
			log.info("Project ${project} has no jawr resource in ${jawrFile.absolutePath} - skipping")
		}
	}

	ResourceCollection findDependencyResources() {
		List<Artifact> runtimeArtifacts = (List<Artifact>)project.getRuntimeArtifacts()
		List<Resource> foundResources = []

		File ourResources = new File(project.basedir, "src/main/resources/META-INF/resources")

		if (ourResources.exists()) {
			foundResources.add(Resource.newResource(ourResources))
		}

		runtimeArtifacts.each { Artifact artifact ->
			Resource resource = dependencyIsResource(artifact)

			if (resource) {
				foundResources.add(resource)
				log.info("JAWR-Staticizer: Found ${artifact} with resources.")
			}
		}

		if (foundResources.size() > 0) {
			Resource[] resources = new Resource[foundResources.size()]
			return new ResourceCollection(foundResources.toArray(resources))
		} else {
			return null
		}
	}

	protected void processJawrProperties(File jawrFile) {
		Properties jawr = new Properties()

		jawrFile.withReader { Reader reader ->
			jawr.load(reader)
		}

		if (jawr.size() == 0) {
			log.info("No properties to process - skipping")
			return
		}

		ResourceCollection resources = findDependencyResources()

		if (resources == null) {
			throw new MojoFailureException("JAWR Properties exist but no resources to process them with.")
		}

		List<Bundle> bundles = []

		jawrFile.eachLine { String line ->
			line = line.trim()
			if (line.startsWith("#")) { return }

			String[] parts = line.tokenize('.')

			if (line.startsWith("jawr.js.bundle.")) {
				if (parts[4] == 'id') {
					bundles.add(new Bundle(id:parts[3], properties: jawr))
				}
			} else if (line.startsWith("jawr.css.bundle.")) {

			}
		}
	}

	protected Resource dependencyIsResource(Artifact artifact) {
		if (artifact.type != "jar") { return null }

		Resource resource = Resource.newResource("jar:file:${artifact.file.absolutePath}!/META-INF/resources")

		if (resource && resource.exists()) {
			return resource
		} else {
			return null
		}
	}
}
