# java-utils

A collection of lightweight Java utilities, event dispatching systems and experimental APIs.

> [!WARNING]
> Most components in this repository are under active development, highly experimental and subject to breaking changes.

## Components

### EventBus
Status: Experimental (Usable)

A polymorphic event dispatching system supporting priority based routing

> [!NOTE]
> Currently usable but the public API is experimental and subject to change.

### Reflection API
Status: Experimental (Partially usable)

An experimental reflection wrapper to align with the Java Language Specification (JLS).

> [!WARNING]
> Currently rough around the edges and heavily subject to breaking changes and not recommended for production use in its current state.

### Services
Status: WIP

A multi layered and priority service loading framework.

### Registry
Status: WIP

A thread safe, freezable object registry model (`Registry<T>`).
Has a mutate and freeze lifecycle. Allows registration during bootstrap then turns read only on freeze.

### Kaleido Config
Status: WIP

Helper utilities designed to simplify configuration serialization

## Requirements
Java 21+

## License
This project is licensed under the [Apache License 2.0](LICENSE).
