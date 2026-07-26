# Base de dados — DevFlow Hub

O DevFlow Hub utiliza PostgreSQL para armazenar colaboradores, projetos, tarefas, programas internos, documentos e metadata de anexos.

## Ficheiros

### `devflow_hub.sql`

Cria uma instalação nova da base de dados, incluindo:

- tabela `collaborators`;
- tabela `projects`;
- tabela `tasks`;
- tabela `internal_programs`;
- tabela `documents`;
- tabela `attachments`;
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

### `migrations/20260724_restore_core_foreign_keys.sql`

Migração idempotente que restaura as relações principais que podem estar
ausentes em bases criadas por versões anteriores. O script:

- valida previamente que não existem referências órfãs;
- restaura `projects.manager_id`;
- restaura `tasks.project_id`;
- restaura `tasks.assignee_id`;
- restaura `internal_programs.manager_id`;
- aplica `ON DELETE SET NULL` às quatro relações;
- valida que todas as chaves estrangeiras foram instaladas corretamente.

### `migrations/20260724_add_documents_foundation.sql`

Migração idempotente que cria a fundação para documentos e anexos. O modelo aplica as seguintes regras:

- um documento pertence exclusivamente a um projeto ou a uma tarefa;
- documentos de tarefas armazenam apenas `task_id`;
- o projeto de um documento de tarefa é derivado através da respetiva tarefa;
- um documento pode conter texto e vários anexos;
- a tabela `attachments` guarda apenas metadata e a chave de armazenamento;
- os conteúdos físicos dos ficheiros não são guardados como BLOB no PostgreSQL;
- eliminar um projeto ou uma tarefa elimina os respetivos documentos;
- eliminar um documento elimina a metadata dos respetivos anexos.

A implementação do armazenamento físico e dos endpoints multipart será realizada numa etapa posterior.

## Atualizar uma instalação existente

Antes de executar qualquer migração, criar uma cópia de segurança da base de
dados.

As migrações são manuais porque o projeto ainda não utiliza Flyway ou
Liquibase. Devem ser executadas pela seguinte ordem:

1. `migrations/20260724_restore_core_foreign_keys.sql`
2. `migrations/20260724_add_documents_foundation.sql`

A partir da raiz do projeto, com `psql` disponível no `PATH`:

```powershell
psql -U postgres -d devflow_hub -v ON_ERROR_STOP=1 -f database/migrations/20260724_restore_core_foreign_keys.sql

psql -U postgres -d devflow_hub -v ON_ERROR_STOP=1 -f database/migrations/20260724_add_documents_foundation.sql
```

Os dois scripts são idempotentes e podem ser executados novamente para
verificação. `ON_ERROR_STOP=1` impede que a execução continue depois de um
erro SQL.

Após as migrações, iniciar o backend. O Hibernate utiliza
`spring.jpa.hibernate.ddl-auto=validate` e interrompe o arranque quando o
esquema não corresponde às entidades Java.

## Criar uma instalação nova

1. Criar uma base de dados PostgreSQL chamada:

devflow_hub