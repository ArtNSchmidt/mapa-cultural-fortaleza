CREATE TABLE usuario (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP DEFAULT now()
);

CREATE TABLE atividade_cultural (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(100) NOT NULL,
    descricao TEXT,
    data_hora TIMESTAMP NOT NULL,
    local VARCHAR(200) NOT NULL,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    organizador_id BIGINT NOT NULL REFERENCES usuario(id),
    criado_em TIMESTAMP DEFAULT now()
);

-- (Opcional para otimizar buscas por coordenada)
CREATE INDEX idx_atividade_coords ON atividade_cultural (latitude, longitude);
