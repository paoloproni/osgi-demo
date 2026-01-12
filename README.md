# OSGi Demo Project

A demonstration project showcasing OSGi modularity and service-oriented architecture using Apache Felix. This project demonstrates real-world service interaction patterns with three loosely-coupled bundles.

## 🚀 Quick Start

```bash
# Build the project
mvn clean install

# Deploy to Apache Felix 7.0.5
cp */target/*.jar /path/to/felix/bundle/
java -jar /path/to/felix/bin/felix.jar
```

## 📋 What This Demo Shows

- **OSGi Service Registry** - Service publication and discovery
- **Dynamic Service Binding** - Hot-swappable components
- **Loose Coupling** - Interface-based communication
- **Real-World Architecture** - Beyond "Hello World" examples

## 🏗️ Architecture

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

### Bundle 1: Random Producer
- Generates random alphanumeric strings (8-16 chars)
- Publishes at random intervals (1-5 seconds)
- Uses OSGi Declarative Services
- **Exports**: `com.byteliberi.demo.producer.api`

### Bundle 2: File Writer
- Consumes strings from Producer
- Writes to `/tmp/osgi-demo/string_YYYYMMDD_HHmmss_SSS.txt`
- Can be started/stopped independently
- **Imports**: `com.byteliberi.demo.producer.api`

### Bundle 3: Syslog Sender
- Consumes strings from Producer
- Sends to localhost:514 via UDP in syslog format
- Coexists with File Writer
- **Imports**: `com.byteliberi.demo.producer.api`

## 🛠️ Prerequisites

- **Java 11+** (LTS)
- **Maven 3.8+**
- **Apache Felix 7.0.5** ([Download](https://felix.apache.org/))

## 🔧 Installation

### 1. Build Project
```bash
git clone <this-repo>
cd osgi-demo
mvn clean install
```

### 2. Setup Felix
```bash
# Download and extract Felix
wget https://archive.apache.org/dist/felix/org.apache.felix.main.distribution-7.0.5.tar.gz
tar -xzf org.apache.felix.main.distribution-7.0.5.tar.gz

# Install Declarative Services (required)
cd felix-framework-7.0.5/bundle
wget https://repo1.maven.org/maven2/org/apache/felix/org.apache.felix.scr/2.2.6/org.apache.felix.scr-2.2.6.jar
```

### 3. Deploy Bundles
```bash
# Copy project bundles to Felix
cp /path/to/osgi-demo/*/target/*.jar ./bundle/

# Start Felix
cd ..
java -jar bin/felix.jar
```

## 🎮 Demo Usage

### Felix Console Commands
```bash
g! lb                                    # List bundles
g! inspect capability service 2          # Show producer services
g! stop 3                               # Stop file writer (producer continues)
g! start 3                              # Restart file writer (immediately resumes)
```

### Verify It's Working
```bash
# Check file output
ls -la /tmp/osgi-demo/
cat /tmp/osgi-demo/string_*.txt

# Check syslog output (Linux)
journalctl -f | grep osgi-demo

# Check Felix console for generation logs
```

## ✨ Key Features

- **✅ Service-Oriented Architecture** - Clean separation of concerns
- **✅ Dynamic Service Management** - Start/stop bundles at runtime
- **✅ Loose Coupling** - Bundles interact only through interfaces
- **✅ Hot Swapping** - Update bundles without framework restart
- **✅ Real-World Patterns** - File I/O, network communication, threading
- **✅ Production Ready** - Error handling, logging, resource cleanup
- **✅ Comprehensive Tests** - JUnit 5 + Mockito (29 tests, 100% pass rate)

## 📊 Technology Stack

- **OSGi Framework**: Apache Felix 7.x
- **Java**: 11 (LTS)
- **OSGi Spec**: R7 with Declarative Services
- **Build**: Maven (multi-module)
- **Testing**: JUnit 5 + Mockito
- **Code Quality**: >80% test coverage

## 📚 Documentation

- **[INSTRUCTIONS.md](./INSTRUCTIONS.md)** - Complete setup and deployment guide
- **Felix Console Commands** - Full reference in INSTRUCTIONS.md
- **Troubleshooting** - Common issues and solutions
- **Architecture Details** - Technical specifications and patterns

## 🤝 Use Cases

Perfect for:
- **Learning OSGi** - Practical, working example beyond tutorials
- **Technical Demonstrations** - Show dynamic modularity in action
- **Architecture Evaluation** - Assess OSGi for enterprise projects
- **Job Interviews** - Demonstrate OSGi knowledge with real code

## 🎯 Next Steps

1. **Run the demo** - Follow Quick Start above
2. **Experiment** - Try stopping/starting different bundles
3. **Extend** - Add new consumers or modify existing ones
4. **Learn** - Read INSTRUCTIONS.md for deep dive

## 🔗 Resources

- [OSGi Alliance](https://www.osgi.org/) - Official OSGi website
- [Apache Felix](https://felix.apache.org/) - OSGi framework documentation
- [OSGi Declarative Services](https://docs.osgi.org/specification/osgi.cmpn/7.0.0/service.component.html) - Component model

---

**Built with ❤️ to demonstrate OSGi's power and simplicity**
