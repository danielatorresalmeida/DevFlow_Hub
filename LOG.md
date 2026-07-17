# DevFlow Hub — Registo de Desenvolvimento

## Nota sobre a reorganização

Este LOG foi reconstruído de forma organizada com base no código-fonte existente, nos documentos das sprints, nos registos de desenvolvimento anteriores e nas validações realizadas.

O repositório anterior permanece como histórico do trabalho original. Este novo repositório organiza os principais marcos técnicos através de branches e commits coerentes.

As datas dos novos commits representam a data da reorganização no Git e não substituem as datas reais registadas nas sessões originais.

---

## Marco 0 — 17/07/2026 — Criação do novo repositório organizado

### Objetivo da sessão

Criar uma fundação limpa para o projeto DevFlow Hub, removendo dependências geradas, configurações pessoais e ficheiros desnecessários do novo histórico Git.

### Funcionalidades implementadas

- Criação de um novo repositório Git local.
- Definição da branch inicial `main`.
- Criação de um `.gitignore` adequado para Java, Maven, Node.js, Vite e IDEs.
- Criação de um `.gitattributes` para normalizar os finais de linha.
- Criação de um `.editorconfig` para uniformizar a formatação.
- Criação do README inicial.
- Criação do novo LOG de desenvolvimento.
- Definição da estratégia de branches.
- Preservação do código validado numa pasta-fonte separada.
- Exclusão da configuração local que contém a password do PostgreSQL.

### Problemas encontrados

- O repositório anterior continha ficheiros gerados dentro do histórico Git.
- A pasta `node_modules` tinha sido registada no controlo de versões.
- O projeto estava localizado dentro do OneDrive, causando bloqueios no npm e no Maven.
- O histórico anterior acumulava várias funcionalidades no mesmo branch.
- O README e o LOG já não estavam totalmente sincronizados com o código atual.

### Como resolvi

- Mantive o repositório anterior sem o eliminar.
- Copiei o código para uma pasta-fonte fora do OneDrive.
- Excluí `.git`, `node_modules`, `target`, `dist`, `.idea` e ficheiros locais.
- Criei o novo repositório em `C:\Dev\DevFlow_Hub`.
- Defini uma estratégia baseada em `main`, `develop` e branches de trabalho.
- Decidi adicionar o código progressivamente através de commits coerentes.
- Adicionei uma nota transparente sobre a reconstrução do histórico.

### Ficheiros atualizados

- `.gitignore`
- `.gitattributes`
- `.editorconfig`
- `README.md`
- `LOG.md`

### Estado atual

- Novo repositório inicializado.
- Branch `main` criada.
- Configuração base do Git preparada.
- Código-fonte original preservado em `C:\Dev\DevFlow_Hub_Source`.
- Nenhuma password local adicionada ao novo repositório.
- Nenhum ficheiro gerado adicionado ao Git.

### Validação ainda pendente

- Criar a branch `develop`.
- Adicionar o esquema PostgreSQL.
- Adicionar o backend progressivamente.
- Adicionar a API REST.
- Adicionar o frontend React.
- Adicionar a interface Thymeleaf.
- Adicionar os testes.
- Validar o JAR final.
- Criar o novo repositório remoto no GitHub.

### Próxima etapa

Criar a branch `develop` e iniciar a branch `feature/database-schema` para adicionar o esquema PostgreSQL e os dados de demonstração.

### Linhas de código escritas/alteradas — estimativa

120 linhas de configuração e documentação.

---

## Marco 1 — 17/07/2026 — Estrutura PostgreSQL e migração

### Objetivo da sessão

Adicionar ao novo repositório a estrutura persistente do DevFlow Hub e
documentar a criação e atualização da base de dados PostgreSQL.

### Funcionalidades implementadas

- Adição do script principal `devflow_hub.sql`.
- Criação das tabelas `collaborators`, `projects`, `tasks` e
  `internal_programs`.
- Inclusão das relações entre colaboradores, projetos, tarefas e programas.
- Utilização de identificadores `BIGINT`, compatíveis com `Long` no backend.
- Inclusão de campos de auditoria.
- Inclusão dos campos utilizados pelo temporizador das tarefas.
- Inclusão de constraints, índices e dados de demonstração.
- Adição do script `migrate_existing_database.sql`.
- Criação de um guia específico para preparar e atualizar o PostgreSQL.

### Problemas encontrados

- A base de dados de trabalho já continha tabelas criadas por versões
  anteriores.
- `CREATE TABLE IF NOT EXISTS` não recria tabelas existentes nem adiciona
  constraints que estejam em falta.
- A primeira consulta às foreign keys não apresentou resultados na base
  antiga.

### Como resolvi

- Mantive a base de trabalho sem eliminar os dados existentes.
- Criei uma base vazia chamada `devflow_hub_validation`.
- Executei o script principal numa instalação limpa.
- Validei separadamente as tabelas, os dados de demonstração e as foreign
  keys.
- Mantive um script específico para a migração de bases anteriores.

### Ficheiros atualizados

- `database/devflow_hub.sql`
- `database/migrate_existing_database.sql`
- `database/README.md`
- `README.md`
- `LOG.md`

### Estado atual

- As quatro tabelas principais foram criadas corretamente.
- Foram carregados 6 colaboradores, 4 projetos, 5 tarefas e 4 programas
  internos.
- Foram validadas quatro foreign keys:
  - `internal_programs.manager_id` referencia `collaborators.id`;
  - `projects.manager_id` referencia `collaborators.id`;
  - `tasks.assignee_id` referencia `collaborators.id`;
  - `tasks.project_id` referencia `projects.id`.
- O script principal está preparado para instalações novas.
- O script de migração está disponível para bases antigas.
- Nenhuma password administrativa real foi adicionada ao repositório.

### Validação ainda pendente

- Executar `migrate_existing_database.sql` numa cópia de uma base antiga.
- Confirmar o esquema através do backend com `ddl-auto=validate`.

### Próxima etapa

Adicionar a fundação do backend Spring Boot, incluindo Maven, configuração
segura, entidades e repositories.

### Linhas de código escritas/alteradas — estimativa

354 linhas de SQL e documentação.
