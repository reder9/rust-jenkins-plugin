/*
 See the documentation for more options:
 https://github.com/jenkins-infra/pipeline-library/
*/
buildPlugin(
  useContainerAgent: true,
  configurations: [
    [platform: 'linux', jdk: 17],
    [platform: 'windows', jdk: 17],
  ],
  // Opt-in to running integration tests with the PCT (plugin compatibility test)
  // More info: https://github.com/jenkinsci/plugin-compat-tester
  forkCount: '1C',
  checkstyle: [run: true, archive: true],
  spotbugs: [run: true, archive: true]
)