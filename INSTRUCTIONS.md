# OSGi Demo Project

A demonstration project showcasing OSGi modularity and service-oriented architecture using Apache Felix. This project consists of three loosely-coupled bundles that demonstrate real-world service interaction patterns.

## Project Overview

This demo implements a simple but realistic OSGi architecture with three bundles:

1. **Random Producer** - Generates random strings and publishes them via OSGi service interface
2. **File Writer** - Consumes strings and writes them to individual files
3. **Syslog Sender** - Consumes strings and sends them to local syslog via UDP

## Architecture

```
┌─────────────────────┐    ┌─────────────────────┐
│   Random Producer   │    │   StringProducer    │
│     (Bundle 1)      │───▶│   Service API       │
│                     │    │                     │
└─────────────────────┘    └─────────────────────┘
                                      │
                                      │ Service Registry
                                      │
                    ┌─────────────────┴─────────────────┐
                    │                                   │
                    ▼                                   ▼
    ┌─────────────────────┐                ┌─────────────────────┐
    │    File Writer      │                │   Syslog Sender     │
    │    (Bundle 2)       │                │    (Bundle 3)       │
    │                     │                │                     │
    │ /tmp/osgi-demo/*.txt│                │ localhost:514 (UDP) │
    └─────────────────────┘                └─────────────────────┘
```

## Prerequisites

- **Java 11** (LTS) or higher
- **Maven 3.8+**
- **Apache Felix 7.0.5** (OSGi framework)
- Linux/macOS/Windows (Felix is cross-platform)

## Project Structure

```
osgi-demo/
├── pom.xml                          # Parent POM
├── random-producer/
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/byteliberi/demo/producer/
│   │       ├── api/
│   │       │   └── StringProducer.java          # Service interface
│   │       └── impl/
│   │           ├── RandomStringProducer.java    # Service implementation
│   │           └── Activator.java               # Bundle activator
│   └── src/test/java/
│       └── com/byteliberi/demo/producer/impl/
│           └── RandomStringProducerTest.java
├── file-writer/
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/byteliberi/demo/writer/
│   │       └── impl/
│   │           └── FileStringWriter.java        # Service consumer
│   └── src/test/java/
│       └── com/byteliberi/demo/writer/impl/
│           └── FileStringWriterTest.java
├── syslog-sender/
│   ├── pom.xml
│   ├── src/main/java/
│   │   └── com/byteliberi/demo/syslog/
│   │       └── impl/
│   │           └── SyslogStringSender.java      # Service consumer
│   └── src/test/java/
│       └── com/byteliberi/demo/syslog/impl/
│           └── SyslogStringSenderTest.java
└── README.md
```

## Build Instructions

### Build All Bundles

```bash
# Clean and build all modules
mvn clean install

# Build without running tests (faster)
mvn clean install -DskipTests

# Build with verbose output
mvn clean install -X
```

### Build Individual Bundles

```bash
# Build only random-producer
cd random-producer
mvn clean install

# Build only file-writer
cd file-writer
mvn clean install

# Build only syslog-sender
cd syslog-sender
mvn clean install
```

### Run Tests

```bash
# Run all tests
mvn test

# Run tests with coverage report
mvn clean test jacoco:report

# Run tests for specific bundle
mvn test -pl random-producer
```

## Felix Installation and Setup

### Download and Install Felix

```bash
# Download Apache Felix 7.0.5
wget https://archive.apache.org/dist/felix/org.apache.felix.main.distribution-7.0.5.tar.gz

# Extract
tar -xzf org.apache.felix.main.distribution-7.0.5.tar.gz
cd felix-framework-7.0.5

# Create bundle directory
mkdir bundle
```

### Download Required Bundles

Download these additional bundles for OSGi Declarative Services support:

```bash
cd bundle

# Declarative Services implementation (required)
wget https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.scr/2.2.6/org.apache.felix.scr-2.2.6.jar

# Optional: Better logging support
wget https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.log/1.2.6/org.apache.felix.log-1.2.6.jar
```

### Configure Felix

Create or modify `conf/config.properties`:

```properties
# Felix framework configuration
org.osgi.framework.storage=felix-cache
org.osgi.framework.storage.clean=onFirstInit
felix.auto.deploy.action=install,start
felix.auto.deploy.dir=bundle
felix.log.level=3

# Optional: Enable remote shell (for debugging)
# felix.shell.remote=true
# osgi.shell.telnet.port=6666
```

## Deployment

### Copy Built Bundles to Felix

```bash
# From the osgi-demo project root directory
cp random-producer/target/random-producer-1.0.0-SNAPSHOT.jar /path/to/felix/bundle/
cp file-writer/target/file-writer-1.0.0-SNAPSHOT.jar /path/to/felix/bundle/
cp syslog-sender/target/syslog-sender-1.0.0-SNAPSHOT.jar /path/to/felix/bundle/
```

### Start Felix

```bash
cd /path/to/felix
java -jar bin/felix.jar
```

### Felix Console Commands

Once Felix starts, you'll see the Felix shell prompt `g!`. Use these commands:

```bash
# List all bundles
lb

# List bundles with verbose information
lb -v

# Start a specific bundle
start <bundle-id>

# Stop a specific bundle
stop <bundle-id>

# Inspect services provided by a bundle
inspect capability service <bundle-id>

# Inspect services required by a bundle
inspect requirement service <bundle-id>

# Show bundle headers
headers <bundle-id>

# Display system properties
sysprop

# Show log messages
log display

# Exit Felix
exit
```

## How to Verify It's Working

### 1. Check Bundle Status

```bash
g! lb
START LEVEL 1
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (7.0.5)
    1|Active     |    1|Apache Felix Declarative Services (2.2.6)
    2|Active     |    1|random-producer (1.0.0.SNAPSHOT)
    3|Active     |    1|file-writer (1.0.0.SNAPSHOT)
    4|Active     |    1|syslog-sender (1.0.0.SNAPSHOT)
```

### 2. Check Service Registry

```bash
g! inspect capability service 2
random-producer [2] provides:
  service.id = 45
  objectClass = com.byteliberi.demo.producer.api.StringProducer
  service.bundleid = 2
  service.scope = singleton
```

### 3. Monitor String Generation

Check Felix console output for log messages:

```
INFO: Generated string: Kx9mP2qR4tY7
INFO: Successfully wrote string 'Kx9mP2qR4tY7' to file: string_20240112_143052_123.txt
INFO: Successfully sent string 'Kx9mP2qR4tY7' to syslog
```

### 4. Check File Output

```bash
# Check if files are being created
ls -la /tmp/osgi-demo/
total 24
drwxr-xr-x  5 user  wheel  160 Jan 12 14:30 .
drwxr-xr-x 15 root  wheel  480 Jan 12 14:30 ..
-rw-r--r--  1 user  wheel   12 Jan 12 14:30 string_20240112_143052_123.txt
-rw-r--r--  1 user  wheel   14 Jan 12 14:31 string_20240112_143105_456.txt

# Check file content
cat /tmp/osgi-demo/string_20240112_143052_123.txt
Kx9mP2qR4tY7
```

### 5. Check Syslog Output

```bash
# On Linux systems
journalctl -f | grep osgi-demo

# Or check syslog files directly
tail -f /var/log/syslog | grep osgi-demo

# Example output:
Jan 12 14:30:52 localhost osgi-demo: Kx9mP2qR4tY7
```

## Dynamic Service Management

Demonstrate OSGi's dynamic capabilities:

### Stop and Start Consumer Bundles

```bash
# Stop file writer - producer continues, syslog still works
g! stop 3

# Check that files stop being created but syslog continues
ls /tmp/osgi-demo/

# Restart file writer - it resumes operation
g! start 3

# Stop syslog sender - producer continues, file writer still works
g! stop 4

# Restart syslog sender
g! start 4
```

### Bundle Installation Order

The bundles can be installed in any order, but:
- **Producer must be active** for consumers to receive strings
- **Consumers are resilient** to producer restarts
- **No dependencies** between file-writer and syslog-sender

## Troubleshooting

### Common Issues

#### Bundle doesn't start
```bash
g! diag <bundle-id>
```
Check for:
- Missing dependencies
- Import/Export package mismatches
- Java version compatibility

#### Service not found
```bash
g! inspect capability service <producer-bundle-id>
g! inspect requirement service <consumer-bundle-id>
```

#### No files being created
- Check `/tmp/osgi-demo/` directory permissions
- Verify file-writer bundle is active: `lb`
- Check logs for IOException messages

#### No syslog messages
- Verify syslog daemon is running: `systemctl status rsyslog`
- Check if UDP port 514 is accessible
- Look for network errors in Felix console

#### Permission Issues on /tmp/osgi-demo
```bash
# Create directory manually if needed
sudo mkdir -p /tmp/osgi-demo
sudo chmod 755 /tmp/osgi-demo

# Or modify FileStringWriter.java to use user home directory
```

### Debug Logging

Enable debug logging in Felix:

```bash
# In config.properties
felix.log.level=1

# Or at runtime
g! log debug
```

### Bundle Refresh

If you update bundle JARs:

```bash
# Refresh specific bundle
g! refresh <bundle-id>

# Refresh all bundles
g! refresh
```

## Development Tips

### IDE Setup (IntelliJ IDEA)

1. Import as Maven project
2. Enable auto-import for Maven dependencies
3. Set Project SDK to Java 11
4. Install OSGi plugin for better support

### Adding New Features

To extend this demo:

1. **Configuration Admin**: Make intervals and paths configurable
2. **Event Admin**: Use OSGi events instead of direct listener pattern
3. **HTTP Service**: Add web interface to monitor bundles
4. **JMX**: Expose bundle statistics via JMX
5. **Metatype**: Add configuration UI in Felix Web Console

### Testing

```bash
# Run specific test class
mvn test -Dtest=RandomStringProducerTest

# Run tests with JaCoCo coverage
mvn clean test jacoco:report
open target/site/jacoco/index.html
```

## Performance Characteristics

- **String Generation**: 1-5 second intervals, configurable
- **File I/O**: Buffered writes, minimal overhead
- **UDP Syslog**: Non-blocking, fire-and-forget
- **Thread Safety**: CopyOnWriteArrayList for listeners
- **Resource Usage**: Minimal memory footprint, single background thread

## Security Considerations

- **File writes**: Limited to `/tmp/osgi-demo/` directory
- **Syslog**: Localhost only (no remote servers)
- **No sensitive data**: Generated strings are random alphanumeric
- **Resource cleanup**: Proper thread termination and socket closure

## License

This project is a demonstration/educational project. Feel free to use and modify as needed.

## Further Reading

- [OSGi Specification R7](https://docs.osgi.org/specification/)
- [Apache Felix Documentation](https://felix.apache.org/documentation.html)
- [OSGi Declarative Services](https://felix.apache.org/documentation/subprojects/apache-felix-service-component-runtime.html)
- [Maven Bundle Plugin](https://felix.apache.org/documentation/subprojects/apache-felix-maven-bundle-plugin-bnd.html)
