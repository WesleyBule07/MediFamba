CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TYPE user_role AS ENUM ('ADMIN', 'DOCTOR', 'PATIENT', 'USER');
CREATE TYPE genero_type AS ENUM ('MASCULINO', 'FEMININO', 'OUTRO');
CREATE TYPE consulta_estado AS ENUM ('PENDENTE', 'CONFIRMADA', 'CONCLUIDA', 'CANCELADA', 'REMARCADA');
CREATE TYPE notificacao_tipo AS ENUM ('NOVA_CONSULTA', 'ALTERACAO_HORARIO', 'CANCELAMENTO', 'ATUALIZACAO');

CREATE TABLE utilizadores (
                              id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              nome        VARCHAR(150) NOT NULL,
                              email       VARCHAR(150) NOT NULL UNIQUE,
                              senha       VARCHAR(255) NOT NULL,
                              role        user_role NOT NULL DEFAULT 'USER',
                              ativo       BOOLEAN NOT NULL DEFAULT TRUE,
                              foto_url    VARCHAR(500),
                              created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                              updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE pacientes (
                           id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           utilizador_id        UUID UNIQUE REFERENCES utilizadores(id) ON DELETE SET NULL,
                           nome_completo        VARCHAR(150) NOT NULL,
                           data_nascimento      DATE NOT NULL,
                           genero               genero_type NOT NULL,
                           telefone             VARCHAR(20),
                           endereco             VARCHAR(300),
                           contacto_emergencia  VARCHAR(100),
                           numero_identificacao VARCHAR(50) UNIQUE,
                           historico_basico     TEXT,
                           foto_url             VARCHAR(500),
                           ativo                BOOLEAN NOT NULL DEFAULT TRUE,
                           created_at           TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at           TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE medicos (
                         id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         utilizador_id       UUID UNIQUE REFERENCES utilizadores(id) ON DELETE SET NULL,
                         nome                VARCHAR(150) NOT NULL,
                         especialidade       VARCHAR(100) NOT NULL,
                         contacto            VARCHAR(20),
                         numero_profissional VARCHAR(50) UNIQUE NOT NULL,
                         horario_inicio      TIME NOT NULL DEFAULT '08:00',
                         horario_fim         TIME NOT NULL DEFAULT '17:00',
                         disponivel          BOOLEAN NOT NULL DEFAULT TRUE,
                         foto_url            VARCHAR(500),
                         ativo               BOOLEAN NOT NULL DEFAULT TRUE,
                         created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
                         updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE consultas (
                           id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           paciente_id UUID NOT NULL REFERENCES pacientes(id),
                           medico_id   UUID NOT NULL REFERENCES medicos(id),
                           data_hora   TIMESTAMP NOT NULL,
                           motivo      VARCHAR(500) NOT NULL,
                           sala        VARCHAR(50),
                           estado      consulta_estado NOT NULL DEFAULT 'PENDENTE',
                           observacoes TEXT,
                           created_by  UUID REFERENCES utilizadores(id),
                           created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                           updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
                           CONSTRAINT no_conflict UNIQUE (medico_id, data_hora)
);

CREATE TABLE historico_medico (
                                  id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  consulta_id UUID NOT NULL REFERENCES consultas(id),
                                  paciente_id UUID NOT NULL REFERENCES pacientes(id),
                                  medico_id   UUID NOT NULL REFERENCES medicos(id),
                                  diagnostico TEXT,
                                  observacoes TEXT,
                                  prescricao  TEXT,
                                  created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE notificacoes (
                              id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                              utilizador_id UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
                              tipo          notificacao_tipo NOT NULL,
                              titulo        VARCHAR(200) NOT NULL,
                              mensagem      TEXT NOT NULL,
                              lida          BOOLEAN NOT NULL DEFAULT FALSE,
                              consulta_id   UUID REFERENCES consultas(id) ON DELETE SET NULL,
                              created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE audit_logs (
                            id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            utilizador_id UUID REFERENCES utilizadores(id) ON DELETE SET NULL,
                            acao          VARCHAR(100) NOT NULL,
                            entidade      VARCHAR(100) NOT NULL,
                            entidade_id   UUID,
                            detalhes      TEXT,
                            ip_address    VARCHAR(45),
                            created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE password_reset_tokens (
                                       id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                       token         VARCHAR(255) NOT NULL UNIQUE,
                                       utilizador_id UUID NOT NULL REFERENCES utilizadores(id) ON DELETE CASCADE,
                                       expira_em     TIMESTAMP NOT NULL,
                                       usado         BOOLEAN NOT NULL DEFAULT FALSE,
                                       created_at    TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_consultas_medico_data ON consultas(medico_id, data_hora);
CREATE INDEX idx_consultas_paciente    ON consultas(paciente_id);
CREATE INDEX idx_consultas_estado      ON consultas(estado);
CREATE INDEX idx_consultas_data        ON consultas(data_hora);
CREATE INDEX idx_notificacoes_user     ON notificacoes(utilizador_id, lida);
CREATE INDEX idx_audit_user            ON audit_logs(utilizador_id);
CREATE INDEX idx_pacientes_nome        ON pacientes(nome_completo);
CREATE INDEX idx_medicos_especialidade ON medicos(especialidade);

INSERT INTO utilizadores (id, nome, email, senha, role)
VALUES (
           gen_random_uuid(),
           'Administrador',
           'adminsystem@medifamba.local',
           'medifamba1234#',
           'ADMIN'
       );
