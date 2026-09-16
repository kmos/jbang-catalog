# Retrieval Augmented Generation with Debezium

This demo shows how Debezium Server can be used to implement RAG in an application.

## Use case

The application manages arxiv documents for RAG.
Debezium Server calculates embeddings from downloaded papers using [FieldToEmbedding SMT](https://debezium.io/blog/2025/04/02/debezium-3-1-final-released/#new-features-and-improvements-ai) and stores them in a Milvus vector database.
A locally served LLM provides chat responses augmented with knowledge retrieved from Milvus.

## Prerequisites

- [JBang](https://www.jbang.dev/download/) installed
- [Docker](https://docs.docker.com/get-docker/) running (for managed mode)
- A chat model server (e.g. `llama-server` or any OpenAI-compatible endpoint)
- An embedding model server

## Quick start

### 1. Start the LLM servers

Start a chat model:

```
$ llama-server -m ~/models/granite-4.2-3b-Q3_K_L.gguf --alias granite4.2 -ngl 99 --port 1144 -n 400 -e -sm layer --load-mode auto --reasoning-budget -1 --host 0.0.0.0
```

Start an embedding model:

```
$ llama-server -m ~/models/granite-embedding-small-english-r2.Q8_0.gguf --alias granite -ngl 99 -n 400 -e -sm layer --load-mode auto --port 1145 --embeddings --host 0.0.0.0
```

### 2. Run the RAG CLI

```
$ jbang rag@kmos
```

This will:
1. Build and start Docker containers (PostgreSQL, Milvus, Debezium Server) via Testcontainers
2. Run health checks against all services
3. Drop you into an interactive REPL

## Running modes

### Managed mode (default)

```
$ jbang rag@kmos
```

Docker containers are automatically started and managed by the application using Testcontainers. On exit, all containers are stopped and cleaned up.

### Manual mode

```
$ jbang rag@kmos -Ddocker=false
```

The application skips container management. You must start the infrastructure yourself before running the CLI:

```
$ docker-compose up --build
```

In this mode, health checks run once and report which services are missing with fix hints.

## CLI usage

The CLI works as an interactive shell. Text typed without a `/` prefix is sent to the chat model with RAG augmentation. Commands are prefixed with `/`.

### Available commands

| Command | Description |
|---|---|
| `/scenario init` | Initialize Milvus collection and truncate PostgreSQL documents table |
| `/scenario milvus` | List all vectors stored in Milvus |
| `/papers available` | List all bundled papers |
| `/papers add <paper-id>` | Add a paper to PostgreSQL |
| `/papers delete <paper-id>` | Remove a paper from PostgreSQL |
| `/exit` | Stop all containers (if managed) and exit |
| `/help` | Show available commands |

You can also press `Ctrl-D` to exit.

## Demo walkthrough

For testing we use the paper [IterQR: An Iterative Framework for LLM-based Query Rewrite in e-Commercial Search System](https://arxiv.org/abs/2504.05309) since `IterQR` is not known by the model.

Initialize the scenario (creates Milvus collection and cleans up PostgreSQL):

```
> /scenario init
```

Verify Milvus is empty:

```
> /scenario milvus
```

Ask about `IterQR` — the model has no knowledge of it yet, so the reply will be hallucinated:

```
> Describe IterQR framework
```

List available papers and add the IterQR paper:

```
> /papers available
> /papers add 2504.05309v1
```

Debezium picks up the change from PostgreSQL, computes embeddings, and stores them in Milvus. Verify the vectors appeared:

```
> /scenario milvus
```

Ask again — this time the answer is augmented with the paper content:

```
> Describe IterQR framework
```

Remove the paper to forget the knowledge:

```
> /papers delete 2504.05309v1
```

Asking again produces a hallucinated reply, demonstrating the full RAG lifecycle.

## Infrastructure

The managed mode starts three containers:

| Service | Image | Ports |
|---|---|---|
| PostgreSQL | `quay.io/debezium/example-postgres:3.2` | 5432 |
| Milvus | `milvusdb/milvus:v2.5.4` | 19530, 9091 |
| Debezium Server | Custom build with OpenAI embeddings | 8080 |

## Stopping the demo

In managed mode, use `/exit` or `Ctrl-D` — containers are stopped automatically.

In manual mode, stop the infrastructure separately:

```
$ docker-compose down
```
