# Base de dados — DevFlow Hub

O DevFlow Hub utiliza PostgreSQL para armazenar colaboradores, projetos, tarefas e programas internos.

## Ficheiros

### `devflow_hub.sql`

Cria uma instalação nova da base de dados, incluindo:

- tabela `collaborators`;
- tabela `projects`;
- tabela `tasks`;
- tabela `internal_programs`;
- chaves estrangeiras;
- restrições de domínio;
- índices;
- dados de demonstração.

### `migrate_existing_database.sql`

Atualiza uma base de dados criada por versões anteriores do projeto.

O script pode incluir alterações como:

- conversão de identificadores para `BIGINT`;
- criação de campos de auditoria;
- alinhamento dos estados e prioridades;
- atualização das relações entre tabelas.

## Criar uma instalação nova

1. Criar uma base de dados PostgreSQL chamada:

devflow_hub