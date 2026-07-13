# Release checklist

- [ ] Check that tests have passed on CI.
- [ ] Change version name from SNAPSHOT to an actual name.
- [ ] Update readme with the new version’s maven address and document new APIs, if any.
- [ ] Commit `Prepare to release vX.X.X`. Do not push yet.
- [ ] Upload archives to maven.
  `./gradlew clean publish --no-parallel --no-daemon`
- [ ] Wait for artifacts to be available.
  `dependency-watch await me.saket.touchrobot:touchrobot-paparazzi:{version}`
- [ ] Ensure that the release is available on maven by using it in `:sample`
- [ ] Check that the library sources are correctly available.
- [ ] Push commit.
- [ ] Generate a sample APK.
- [ ] Draft a changelog.
- [ ] Make a release on Github.
- [ ] Push a new commit `Prepare next development version` by bumping version and changing library version to SNAPSHOT.
