# OSGi Demo Roadmap

This project is a teaching and demonstration codebase: three Declarative Services bundles
on Apache Felix showing the service registry, dynamic binding and loose coupling.
The roadmap keeps that purpose. Every change should make the demo **easier to run** or
**more idiomatic OSGi** to learn from, not just bigger.

Sizes are rough effort estimates: **S** ≈ 1–3 days, **M** ≈ 1–2 weeks, **L** ≈ 3+ weeks.

---

## Phase 1 — Correctness and idiomatic OSGi

These items fix real issues or patterns a reviewer with OSGi experience would flag.

| Item | Size | Notes |
|---|---|---|
| Adopt the **whiteboard pattern** | M | Today consumers call `StringProducer.addListener(this)`, the classic listener pattern that OSGi discourages. Consumers should instead register `StringListener` as a service, and the producer should track them with a dynamic, multiple, greedy `@Reference`. This also removes the manual listener bookkeeping and the cleanup in `deactivate()`. Keep the current version in a branch or tag as a "before/after" lesson. |
| Split the API into its own bundle | S | `com.byteliberi.demo.producer.api` lives inside `random-producer`, and both consumers depend on that artifact. Move it to an `osgi-demo-api` bundle so the producer can be replaced or updated without refreshing the consumers. This is what makes the README's "hot swapping" claim actually true. |
| Package versioning with `package-info.java` | S | Use `@org.osgi.annotation.versioning.Version` and `@ProviderType`/`@ConsumerType` instead of hand-written `Export-Package` versions; add bnd baselining to catch breaking API changes. |
| Remove the no-op `Activator` | S | It only logs and mixes a `BundleActivator` with DS for no reason. Remove it, or keep it explicitly as a side-by-side "raw API vs DS" example. |
| Consistent activation failure handling | S | `SyslogStringSender` throws on activation failure, `FileStringWriter` swallows the error and stays active while unable to write. Pick one behavior and document it. |
| Locale-independent syslog timestamp | S | `DateTimeFormatter.ofPattern("MMM dd …")` uses the JVM default locale, so on an Italian system it emits `ott` instead of `Oct`, which is invalid RFC 3164. Use `Locale.ROOT` and the space-padded day (`MMM d` with padding). |
| Unique output filenames | S | Two strings in the same millisecond overwrite each other's file. Add a sequence number or use `CREATE_NEW`. |
| Use the OSGi Log Service (R7 `Logger`) instead of `java.util.logging` | S | Shows another standard service and integrates with `log display` in the shell. |

## Phase 2 — Build, run and test experience

| Item | Size | Notes |
|---|---|---|
| Java 17 baseline (Java 21 tested) | S | Java 11 is near end of free support for most vendors. |
| Move to **bnd-maven-plugin** with a `.bndrun` file | M | Replaces `maven-bundle-plugin` and the manual `wget` list in INSTRUCTIONS.md. The resolver computes the runtime set (Felix, SCR, Promise, Function, Gogo) automatically. |
| One-command run: executable JAR | S | `bnd-export-maven-plugin` produces `osgi-demo.jar`; the whole demo starts with `java -jar osgi-demo.jar`. |
| Upgrade runtime | S | Latest Felix framework, SCR and Gogo; OSGi R8 annotations where available. |
| Fix the file-writer tests | S | Several tests override `onStringGenerated` in an anonymous subclass, so they test a copy of the logic, not the production code. Make the output directory configurable (see Phase 3) and test the real class. |
| Constructor injection instead of reflection in tests | S | DS 1.4 supports `@Activate` constructors with `@Reference` parameters; tests then need no `setAccessible`. |
| OSGi integration tests | M | Use the OSGi Testing project (`org.osgi.test.junit5`) with `bnd-testing-maven-plugin` to run tests inside a real framework: services appear, bind and rebind when a bundle stops. |
| Coverage that matches the README | S | The README claims >80% coverage, but JaCoCo is not configured in the POM. Add the plugin or drop the claim. |
| CI with GitHub Actions | S | Build, unit and integration tests on JDK 17/21; attach the executable JAR to releases. |
| Container image | S | A small Dockerfile so the demo runs without installing Felix; include a syslog receiver in a `docker-compose.yml` for the Syslog Sender. |

## Phase 3 — Show more of the OSGi platform

Each item is a small, self-contained lesson. The first five are already listed as ideas in INSTRUCTIONS.md.

| Item | Size | Notes |
|---|---|---|
| **Configuration Admin + Metatype** | M | Make interval, string length, output directory and syslog host/port configurable; change them at runtime and watch components reconfigure. |
| **Felix Web Console** | S | Visual bundle, service and configuration management; pairs well with Metatype. |
| **Event Admin** | M | A variant where the producer posts events on a topic instead of calling services, to compare the two communication styles. |
| **HTTP / JAX-RS Whiteboard** | M | A small REST endpoint and page showing the latest strings and live service bindings. |
| **JMX or Health Checks** | S | Expose counters (strings produced, written, sent) and component state. |
| Multiple producer implementations | S | A second producer (e.g. dictionary words) with `service.ranking` and target filters, to demonstrate selection between providers. |
| Optional and dynamic references | S | A consumer with `cardinality = OPTIONAL` and `policy = DYNAMIC` that keeps running when the producer disappears, compared with the current static, mandatory references. |
| Remote Services | L | Run producer and consumers in separate JVMs with Apache Aries RSA, to show OSGi services across processes. |

## Phase 4 — Real-world integrations (optional)

| Item | Size | Notes |
|---|---|---|
| AWS consumer bundles | M | A CloudWatch Logs or SQS sender built with the AWS SDK v2, showing how to wrap a large non-OSGi library (embedding vs. a wrapped bundle). Credentials only through Configuration Admin, never hardcoded. |
| Kafka or MQTT consumer | M | Same pattern with a message broker; good for comparing modular plugins with microservices. |
| Apache Karaf feature | S | Package the demo as a Karaf feature for those who use Karaf rather than plain Felix. |

## Phase 5 — Documentation

| Item | Size | Notes |
|---|---|---|
| Merge README and INSTRUCTIONS | S | Today they duplicate the architecture section and diverge on the required bundles. Keep a short README and a step-by-step guide. |
| Guided scenarios | M | Short exercises: "stop the producer and observe", "change config at runtime", "replace the producer". Each with expected console output. |
| Architecture diagrams | S | Replace the ASCII art with diagrams that show bundles, packages and service wiring, before and after the whiteboard refactoring. |
| Comparison notes | S | When to choose OSGi versus JPMS, Spring Boot plugins or microservices: the question readers of this repository usually have. |
| License section | S | INSTRUCTIONS.md says "feel free to use"; point to the MIT `LICENSE` instead. |

---

## Proposed milestones

| Release | Content |
|---|---|
| **1.1.0** | Phase 1: whiteboard pattern, API bundle, versioned packages, bug fixes |
| **1.2.0** | Phase 2: bnd build, executable JAR, real tests, CI, container image |
| **1.3.0** | Configuration Admin, Metatype, Web Console, optional/dynamic references |
| **1.4.0** | Event Admin, HTTP/JAX-RS Whiteboard, health checks, second producer |
| **2.0.0** | Remote Services and integration bundles (AWS, Kafka/MQTT), Karaf feature |
