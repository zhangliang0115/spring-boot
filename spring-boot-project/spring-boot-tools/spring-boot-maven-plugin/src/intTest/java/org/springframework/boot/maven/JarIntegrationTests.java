/*
 * Copyright 2012-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.boot.maven;

import java.io.File;

import org.junit.jupiter.api.TestTemplate;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for the Maven plugin's jar support.
 *
 * @author Andy Wilkinson
 * @author Madhura Bhave
 */
@ExtendWith(MavenBuildExtension.class)
class JarIntegrationTests extends AbstractArchiveIntegrationTests {

	@TestTemplate
	void whenJarIsRepackagedInPlaceOnlyRepackagedJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(launchScript(repackaged)).isEmpty();
			assertThat(jar(repackaged)).manifest((manifest) -> {
				manifest.hasMainClass("org.springframework.boot.loader.JarLauncher");
				manifest.hasStartClass("some.random.Main");
				manifest.hasAttribute("Not-Used", "Foo");
			}).hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/jakarta.servlet-api-4")
					.hasEntryWithName("BOOT-INF/classes/org/test/SampleApplication.class")
					.hasEntryWithName("org/springframework/boot/loader/JarLauncher.class");
			assertThat(buildLog(project)).contains("Replacing main artifact with repackaged archive")
					.contains("Installing " + repackaged + " to").doesNotContain("Installing " + original + " to");
		});
	}

	@TestTemplate
	void whenAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original");
			assertThat(original).isFile();
			File main = new File(project, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			assertThat(buildLog(project)).contains("Updating main artifact " + main + " to " + original)
					.contains("Installing " + original + " to").doesNotContain("Installing " + main + " to");
		});
	}

	@TestTemplate
	void whenAClassifierIsConfiguredTheRepackagedJarHasAClassifierAndBothItAndTheOriginalAreInstalled(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-main").goals("install").execute((project) -> {
			assertThat(new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar.original"))
					.doesNotExist();
			File main = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(main).isFile();
			File repackaged = new File(project, "target/jar-classifier-main-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
					.contains("Attaching repackaged archive " + repackaged + " with classifier test")
					.doesNotContain("Creating repackaged archive " + repackaged + " with classifier test")
					.contains("Installing " + main + " to").contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenBothJarsHaveTheSameClassifierRepackagingIsDoneInPlaceAndOnlyRepackagedJarIsInstalled(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-source").goals("install").execute((project) -> {
			File original = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project, "target/jar-classifier-source-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Replacing artifact with classifier test with repackaged archive")
					.doesNotContain("Installing " + original + " to").contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenBothJarsHaveTheSameClassifierAndAttachIsDisabledOnlyTheOriginalJarIsInstalled(MavenBuild mavenBuild) {
		mavenBuild.project("jar-classifier-source-attach-disabled").goals("install").execute((project) -> {
			File original = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar.original");
			assertThat(original).isFile();
			File repackaged = new File(project,
					"target/jar-classifier-source-attach-disabled-0.0.1.BUILD-SNAPSHOT-test.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project))
					.doesNotContain("Attaching repackaged archive " + repackaged + " with classifier test")
					.contains("Updating artifact with classifier test " + repackaged + " to " + original)
					.contains("Installing " + original + " to").doesNotContain("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenAClassifierAndAnOutputDirectoryAreConfiguredTheRepackagedJarHasAClassifierAndIsWrittenToTheOutputDirectory(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-create-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-create-dir-0.0.1.BUILD-SNAPSHOT-foo.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenAnOutputDirectoryIsConfiguredTheRepackagedJarIsWrittenToIt(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-dir").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/foo/jar-custom-dir-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(buildLog(project)).contains("Installing " + repackaged + " to");
		});
	}

	@TestTemplate
	void whenACustomLaunchScriptIsConfiguredItAppearsInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-launcher").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Hello world");
		});
	}

	@TestTemplate
	void whenAnEntryIsExcludedItDoesNotAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-exclude-entry").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-entry-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.doesNotHaveEntryWithName("BOOT-INF/lib/servlet-api-2.5.jar");
		});
	}

	@TestTemplate
	void whenAGroupIsExcludedNoEntriesInThatGroupAppearInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-exclude-group").goals("install").execute((project) -> {
			File repackaged = new File(project, "target/jar-exclude-group-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-core")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-jcl")
					.doesNotHaveEntryWithName("BOOT-INF/lib/log4j-api-2.4.1.jar");
		});
	}

	@TestTemplate
	void whenAJarIsExecutableItBeginsWithTheDefaultLaunchScript(MavenBuild mavenBuild) {
		mavenBuild.project("jar-executable").execute((project) -> {
			File repackaged = new File(project, "target/jar-executable-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/");
			assertThat(launchScript(repackaged)).contains("Spring Boot Startup Script")
					.contains("MyFullyExecutableJarName").contains("MyFullyExecutableJarDesc");
		});
	}

	@TestTemplate
	void whenAJarIsBuiltWithLibrariesWithConflictingNamesTheyAreMadeUniqueUsingTheirGroupIds(MavenBuild mavenBuild) {
		mavenBuild.project("jar-lib-name-conflict").execute((project) -> {
			File repackaged = new File(project, "test-project/target/test-project-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/classes/")
					.hasEntryWithName(
							"BOOT-INF/lib/org.springframework.boot.maven.it-acme-lib-0.0.1.BUILD-SNAPSHOT.jar")
					.hasEntryWithName(
							"BOOT-INF/lib/org.springframework.boot.maven.it.another-acme-lib-0.0.1.BUILD-SNAPSHOT.jar");
		});
	}

	@TestTemplate
	void whenAProjectUsesPomPackagingRepackagingIsSkipped(MavenBuild mavenBuild) {
		mavenBuild.project("jar-pom").execute((project) -> {
			File target = new File(project, "target");
			assertThat(target.listFiles()).containsExactly(new File(target, "build.log"));
		});
	}

	@TestTemplate
	void whenRepackagingIsSkippedTheJarIsNotRepackaged(MavenBuild mavenBuild) {
		mavenBuild.project("jar-skip").execute((project) -> {
			File main = new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("org/springframework/boot");
			assertThat(new File(project, "target/jar-skip-0.0.1.BUILD-SNAPSHOT.jar.original")).doesNotExist();

		});
	}

	@TestTemplate
	void whenADependencyHasSystemScopeAndInclusionOfSystemScopeDependenciesIsEnabledItIsIncludedInTheRepackagedJar(
			MavenBuild mavenBuild) {
		mavenBuild.project("jar-system-scope").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@TestTemplate
	void whenADependencyHasSystemScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-system-scope-default").execute((project) -> {
			File main = new File(project, "target/jar-system-scope-default-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithName("BOOT-INF/lib/sample-1.0.0.jar");

		});
	}

	@TestTemplate
	void whenADependendencyHasTestScopeItIsNotIncludedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-test-scope").execute((project) -> {
			File main = new File(project, "target/jar-test-scope-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).doesNotHaveEntryWithNameStartingWith("BOOT-INF/lib/log4j")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-");
		});
	}

	@TestTemplate
	void whenAProjectUsesKotlinItsModuleMetadataIsRepackagedIntoBootInfClasses(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-kotlin-module").execute((project) -> {
			File main = new File(project, "target/jar-with-kotlin-module-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasEntryWithName("BOOT-INF/classes/META-INF/jar-with-kotlin-module.kotlin_module");
		});
	}

	@TestTemplate
	void whenAProjectIsBuiltWithALayoutPropertyTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-layout-property").goals("package", "-Dspring-boot.repackage.layout=ZIP")
				.execute((project) -> {
					File main = new File(project, "target/jar-with-layout-property-0.0.1.BUILD-SNAPSHOT.jar");
					assertThat(jar(main)).manifest(
							(manifest) -> manifest.hasMainClass("org.springframework.boot.loader.PropertiesLauncher")
									.hasStartClass("org.test.SampleApplication"));
					assertThat(buildLog(project)).contains("Layout: ZIP");
				});
	}

	@TestTemplate
	void whenALayoutIsConfiguredTheSpecifiedLayoutIsUsed(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-zip-layout").execute((project) -> {
			File main = new File(project, "target/jar-with-zip-layout-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main))
					.manifest((manifest) -> manifest.hasMainClass("org.springframework.boot.loader.PropertiesLauncher")
							.hasStartClass("org.test.SampleApplication"));
			assertThat(buildLog(project)).contains("Layout: ZIP");
		});
	}

	@TestTemplate
	void whenRequiresUnpackConfigurationIsProvidedItIsReflectedInTheRepackagedJar(MavenBuild mavenBuild) {
		mavenBuild.project("jar-with-unpack").execute((project) -> {
			File main = new File(project, "target/jar-with-unpack-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(main)).hasUnpackEntryWithNameStartingWith("BOOT-INF/lib/spring-core-")
					.hasEntryWithNameStartingWith("BOOT-INF/lib/spring-context-");
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithACustomLayoutTheJarUsesTheLayout(MavenBuild mavenBuild) {
		mavenBuild.project("jar-custom-layout").execute((project) -> {
			assertThat(jar(new File(project, "custom/target/custom-0.0.1.BUILD-SNAPSHOT.jar")))
					.hasEntryWithName("custom");
			assertThat(jar(new File(project, "default/target/default-0.0.1.BUILD-SNAPSHOT.jar")))
					.hasEntryWithName("sample");
		});
	}

	@TestTemplate
	void whenJarIsRepackagedWithTheLayeredLayoutTheJarContainsLayers(MavenBuild mavenBuild) {
		mavenBuild.project("jar-layered").execute((project) -> {
			File repackaged = new File(project, "jar/target/jar-layered-0.0.1.BUILD-SNAPSHOT.jar");
			assertThat(jar(repackaged)).hasEntryWithNameStartingWith("BOOT-INF/layers/application/classes/")
					.hasEntryWithNameStartingWith("BOOT-INF/layers/dependencies/lib/jar-release")
					.hasEntryWithNameStartingWith("BOOT-INF/layers/snapshot-dependencies/lib/jar-snapshot");
		});
	}

}
