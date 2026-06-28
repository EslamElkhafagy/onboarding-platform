-- V2__documents_and_chunks.sql
-- Document storage + chunked embeddings for RAG.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE documents (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id    UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    uploaded_by   UUID REFERENCES users(id),
    filename      TEXT NOT NULL,
    storage_key   TEXT NOT NULL,
    mime_type     TEXT,
    status        TEXT NOT NULL DEFAULT 'UPLOADED'
                  CHECK (status IN ('UPLOADED','PROCESSING','READY','FAILED')),
    error_message TEXT,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_documents_company_status ON documents(company_id, status);

CREATE TABLE document_chunks (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id  UUID NOT NULL REFERENCES documents(id) ON DELETE CASCADE,
    company_id   UUID NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    chunk_index  INT NOT NULL,
    content      TEXT NOT NULL,
    token_count  INT,
    embedding    vector(1536),  -- adjust to your embedding model's dimension
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_chunks_company_id ON document_chunks(company_id);
-- Approximate nearest-neighbour index for fast similarity search:
CREATE INDEX idx_chunks_embedding ON document_chunks
    USING hnsw (embedding vector_cosine_ops);
