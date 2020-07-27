# Releasing

The following steps publish the plugin to both **Maven Central** and **Gradle Plugin Portal**:

1. Change the version in top-level `gradle.properties` to a non-SNAPSHOT version.
2. Update the `CHANGELOG.md` for the impending release.
3. Update the `README.md` with the new version.
4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version).
5. `./gradlew clean uploadArchives --no-daemon --no-parallel`
6. Visit [Sonatype Nexus](https://oss.sonatype.org/) and promote the artifact.
7. `./gradlew publishPlugins`
8. `git tag -a X.Y.X -m "X.Y.Z"` (where X.Y.Z is the new version)
9. Update the top-level `gradle.properties` to the next SNAPSHOT version.
10. `git commit -am "Prepare next development version."`
11. `git push && git push --tags`

If step 5 or 6 fails, drop the Sonatype repo, fix the problem, commit, and start again at step 5.
