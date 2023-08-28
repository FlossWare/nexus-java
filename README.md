# nexus
Command line tool to interact with Sonatype Nexus

## Example

java -jar target/nexus-1.0.jar list [repo-name]
java -jar target/nexus-1.0.jar list [repo-name] '.+6\.2.+'

java -jar target/nexus-1.0.jar delete [repo-name] '.+6\.2.+'