CREATE TABLE categoria (
    id UUID PRIMARY KEY,
    nome VARCHAR(120) NOT NULL CHECK (btrim(nome) <> ''),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE produto (
    id UUID PRIMARY KEY,
    categoria_id UUID NOT NULL REFERENCES categoria(id),
    nome VARCHAR(160) NOT NULL CHECK (btrim(nome) <> ''),
    descricao VARCHAR(2000),
    imagem_url VARCHAR(2048),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL,
    atualizado_em TIMESTAMPTZ NOT NULL
);

CREATE TABLE produto_variacao (
    id UUID PRIMARY KEY,
    produto_id UUID NOT NULL REFERENCES produto(id),
    nome VARCHAR(120) CHECK (nome IS NULL OR btrim(nome) <> ''),
    preco NUMERIC(12,2) NOT NULL CHECK (preco > 0),
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE comanda (
    id UUID PRIMARY KEY,
    nome_cliente VARCHAR(160) NOT NULL CHECK (btrim(nome_cliente) <> ''),
    status VARCHAR(20) NOT NULL CHECK (status IN ('ABERTA', 'FINALIZADA')),
    aberta_em TIMESTAMPTZ NOT NULL,
    finalizada_em TIMESTAMPTZ,
    total NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (total >= 0),
    CONSTRAINT ck_comanda_finalizacao CHECK (
        (status = 'ABERTA' AND finalizada_em IS NULL)
        OR (status = 'FINALIZADA' AND finalizada_em IS NOT NULL AND finalizada_em >= aberta_em)
    )
);

CREATE TABLE comanda_item (
    id UUID PRIMARY KEY,
    comanda_id UUID NOT NULL REFERENCES comanda(id),
    variacao_id UUID NOT NULL REFERENCES produto_variacao(id),
    nome_produto VARCHAR(160) NOT NULL CHECK (btrim(nome_produto) <> ''),
    nome_variacao VARCHAR(120),
    preco_unitario NUMERIC(12,2) NOT NULL CHECK (preco_unitario > 0),
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    subtotal NUMERIC(12,2) NOT NULL CHECK (subtotal > 0),
    CONSTRAINT ck_item_subtotal CHECK (subtotal = preco_unitario * quantidade)
);

CREATE TABLE pagamento (
    id UUID PRIMARY KEY,
    comanda_id UUID NOT NULL REFERENCES comanda(id),
    valor_pago NUMERIC(12,2) NOT NULL CHECK (valor_pago > 0),
    recebido_em TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_pagamento_comanda UNIQUE (comanda_id)
);

CREATE INDEX idx_categoria_ativo_nome ON categoria(ativo, nome, id);
CREATE INDEX idx_produto_categoria_ativo ON produto(categoria_id, ativo);
CREATE INDEX idx_produto_ativo_nome ON produto(ativo, nome, id);
CREATE INDEX idx_variacao_produto_ativo ON produto_variacao(produto_id, ativo);
CREATE INDEX idx_comanda_status_abertura ON comanda(status, aberta_em, id);
CREATE INDEX idx_comanda_status_finalizacao ON comanda(status, finalizada_em DESC, id DESC);
CREATE INDEX idx_item_comanda ON comanda_item(comanda_id);
CREATE INDEX idx_item_variacao ON comanda_item(variacao_id);
CREATE INDEX idx_pagamento_recebimento ON pagamento(recebido_em);

