/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 21],
    [platform: 'windows', jdk: 21],
  ],
  // Opt-in to running integration tests with the PCT (plugin compatibility test)
  // More info: https://github.com/jenkinsci/plugin-compat-tester
  forkCount: '1C',
  checkstyle: [run: true, archive: true],
  spotbugs: [run: true, archive: true]
)