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
- criação dos campos de auditoria `tasks.created_at` e `tasks.updated_at`;
- alinhamento dos estados e prioridades;
- atualização das relações entre tabelas.

### `migrations/20260723_add_task_audit_fields.sql`

Migração idempotente dedicada aos campos de auditoria das tarefas. O script:

- adiciona `created_at` e `updated_at` quando não existem;
- preenche valores em falta nas linhas existentes;
- aplica valores por defeito;
- torna ambos os campos obrigatórios;
- valida que não ficaram tarefas sem timestamps.

## Criar uma instalação nova

1. Criar uma base de dados PostgreSQL chamada:

devflow_hub