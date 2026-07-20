# DevFlow Hub - Registo de Desenvolvimento

## Nota sobre a reorganização

Este LOG foi reconstruído de forma organizada com base no código-fonte existente, nos documentos das sprints, nos registos de desenvolvimento anteriores e nas validações realizadas.

O repositório anterior permanece como histórico do trabalho original. Este novo repositório organiza os principais marcos técnicos através de branches e commits coerentes.

As datas dos novos commits representam a data da reorganização no Git e não substituem as datas reais registadas nas sessões originais.

---

## Marco 0 - 17/07/2026 - Criação do novo repositório organizado

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

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**259 linhas**.

A contagem inclui os ficheiros de configuração e documentação utilizados para
criar a fundação do novo repositório.

---

## Marco 1 - 17/07/2026 - Estrutura PostgreSQL e migração

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

### Estatísticas do Git

Segundo as estatísticas do Git, este marco adicionou **5 ficheiros** e
**465 linhas**.

A contagem inclui os scripts SQL, a documentação da base de dados e as
atualizações associadas no README e no LOG.

---

## Atualização - Guia de instalação local da base de dados

### Objetivo da sessão

Criar um documento simples com as instruções necessárias para preparar a base
de dados PostgreSQL numa máquina local.

### Funcionalidades implementadas

- Criação de um guia TXT para instalação local do PostgreSQL.
- Documentação da criação da base `devflow_hub`.
- Instruções para executar o script `devflow_hub.sql`.
- Instruções para configurar `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.
- Inclusão de consultas para validar tabelas, dados e chaves estrangeiras.
- Documentação de erros frequentes e respetivas soluções.

### Problemas encontrados

- Era necessário fornecer instruções independentes do README principal.
- As credenciais do PostgreSQL variam entre computadores.

### Como resolvi

- Criei um ficheiro TXT dedicado à instalação local.
- Mantive as passwords reais fora do repositório.
- Adicionei uma referência ao guia no README principal.

### Ficheiros atualizados

- `database/INSTALACAO_BASE_DADOS_LOCAL.txt`
- `README.md`
- `LOG.md`

### Estado atual

- O projeto possui instruções para criar a base de dados localmente.
- O utilizador pode configurar as suas próprias credenciais.
- O guia inclui validação das quatro tabelas e das foreign keys.

### Validação ainda pendente

- Testar o guia completo numa segunda máquina ou instalação PostgreSQL limpa.

### Próxima etapa

Continuar a implementação da branch `feature/backend-core`.

### Estatísticas do Git

Segundo as estatísticas do Git, esta atualização alterou **3 ficheiros**, com
**371 linhas adicionadas** e **1 linha removida**.

A contagem inclui o guia de instalação local e as atualizações correspondentes
no README e no LOG.

---

## Marco 2 - 20/07/2026 - Fundação e serviços do backend

### Objetivo da sessão

Adicionar ao novo repositório a fundação Spring Boot e organizar as principais
regras de negócio do DevFlow Hub através de entidades, repositories e services.

### Funcionalidades implementadas

- Configuração do backend com Java 21 e Spring Boot.
- Adição do Maven Wrapper.
- Configuração segura da ligação PostgreSQL através de variáveis de ambiente.
- Criação das entidades `Collaborator`, `Project`, `Task` e
  `InternalProgram`.
- Criação dos repositories Spring Data JPA.
- Centralização dos estados e prioridades permitidos.
- Criação de uma função comum para normalização de texto.
- Adição de exceções reutilizáveis.
- Implementação das regras de negócio dos colaboradores.
- Implementação da lógica de autenticação simples de colaboradores.
- Implementação das regras de negócio dos projetos.
- Implementação das regras de negócio das tarefas.
- Implementação das ações de iniciar, pausar, retomar e concluir o timer.
- Implementação das regras de negócio dos programas internos.
- Injeção de `Clock` para tornar a lógica temporal testável.

### Problemas encontrados

- O Java configurado anteriormente dependia de uma pasta interna de uma
  extensão do VS Code.
- A pasta utilizada pela extensão deixou de existir.
- O Maven Wrapper não conseguia encontrar uma instalação válida através de
  `JAVA_HOME`.
- Os valores de estados e prioridades estavam distribuídos por diferentes
  partes da aplicação.
- A lógica temporal precisava de ser testável sem depender diretamente da hora
  real do computador.

### Como resolvi

- Instalei o Eclipse Temurin JDK 21 através do WinGet.
- Configurei permanentemente `JAVA_HOME`.
- Atualizei o `PATH` e as configurações Java do VS Code.
- Removi dependências de caminhos temporários da extensão Pleiades.
- Centralizei estados e prioridades na camada de domínio.
- Mantive os repositories responsáveis apenas pelo acesso aos dados.
- Mantive as regras da aplicação dentro dos services.
- Injetei um `Clock` configurável no service de tarefas.
- Validei a compilação através do Maven Wrapper.

### Ficheiros atualizados

- `backend/.mvn/`
- `backend/mvnw`
- `backend/mvnw.cmd`
- `backend/pom.xml`
- `backend/src/main/java/com/devflowhub/backend/BackendApplication.java`
- `backend/src/main/java/com/devflowhub/backend/config/`
- `backend/src/main/java/com/devflowhub/backend/domain/`
- `backend/src/main/java/com/devflowhub/backend/entity/`
- `backend/src/main/java/com/devflowhub/backend/exception/`
- `backend/src/main/java/com/devflowhub/backend/repository/`
- `backend/src/main/java/com/devflowhub/backend/service/`
- `backend/src/main/java/com/devflowhub/backend/util/`
- `backend/src/main/resources/application.properties`
- `backend/src/main/resources/application-local.example.properties`
- `README.md`
- `LOG.md`

### Estado atual

- A fundação Spring Boot está presente.
- Java 21 está instalado e configurado.
- As entidades estão alinhadas com o esquema PostgreSQL.
- Os repositories estão disponíveis.
- As principais regras de negócio estão implementadas.
- O temporizador das tarefas está implementado.
- O backend compila com sucesso através do Maven Wrapper.
- Nenhuma password administrativa foi adicionada ao repositório.

### Validação ainda pendente

- Adicionar testes unitários dos services.
- Executar o backend ligado à base PostgreSQL.
- Validar o esquema com `ddl-auto=validate`.
- Adicionar os controllers REST.
- Adicionar o tratamento global dos erros da API.
- Adicionar os controllers MVC e a interface Thymeleaf.

### Próxima etapa

Criar a branch `feature/rest-api` e implementar os endpoints REST da aplicação.

### Estatísticas do Git

Segundo as estatísticas do Git, a pasta `backend` adicionou **24 ficheiros** e
**1 855 linhas**.

A contagem inclui código Java, Maven Wrapper, configuração Spring Boot e
ficheiros de propriedades.

---

## Marco 3 - 20/07/2026 - Implementação da API REST

### Objetivo da sessão

Adicionar uma API REST ao DevFlow Hub para expor as funcionalidades já
implementadas na camada de serviços.

### Funcionalidades implementadas

- Criação dos DTOs utilizados pelo dashboard.
- Criação do serviço de consulta e agregação do dashboard.
- Criação do controller REST dos colaboradores.
- Criação do controller REST dos projetos.
- Criação do controller REST das tarefas.
- Criação do controller REST dos programas internos.
- Criação do controller REST do dashboard.
- Implementação de endpoints CRUD.
- Implementação dos endpoints do temporizador das tarefas.
- Implementação do endpoint para concluir uma tarefa.
- Criação de uma estrutura comum para erros da API.
- Implementação do tratamento global das exceções.

### Endpoints adicionados

- `/api/collaborators`
- `/api/projects`
- `/api/tasks`
- `/api/internal-programs`
- `/api/dashboard`
- `/api/tasks/{id}/start-timer`
- `/api/tasks/{id}/pause-timer`
- `/api/tasks/{id}/resume-timer`
- `/api/tasks/{id}/total-time`
- `/api/tasks/{id}/timer`
- `/api/tasks/{id}/complete`

### Problemas encontrados

- O `DashboardController` dependia de um `DashboardService` que ainda não
  estava presente no novo repositório.
- A primeira compilação dos controllers terminou com erro porque essa classe
  não podia ser encontrada.
- Era necessário manter os controllers REST separados dos controllers MVC da
  interface Thymeleaf.

### Como resolvi

- Localizei o `DashboardService` no código-fonte original.
- Adicionei o serviço num commit separado.
- Validei que o serviço utiliza os repositories e DTOs já presentes.
- Mantive os controllers REST na pasta `controller`.
- Não adicionei ainda os controllers MVC existentes em `web/controller`.
- Compilei novamente o backend depois de adicionar a dependência em falta.
- Mantive o tratamento global de erros num commit independente.

### Ficheiros atualizados

- `backend/src/main/java/com/devflowhub/backend/controller/CollaboratorController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/DashboardController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/InternalProgramController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/ProjectController.java`
- `backend/src/main/java/com/devflowhub/backend/controller/TaskController.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardProjectItem.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardSummary.java`
- `backend/src/main/java/com/devflowhub/backend/dto/DashboardTaskItem.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiError.java`
- `backend/src/main/java/com/devflowhub/backend/exception/ApiExceptionHandler.java`
- `backend/src/main/java/com/devflowhub/backend/service/DashboardService.java`
- `README.md`
- `LOG.md`

### Estado atual

- Os principais recursos possuem controllers REST.
- As operações CRUD estão expostas através da API.
- As operações do temporizador estão disponíveis através de endpoints
  específicos.
- O dashboard possui DTOs e serviço próprios.
- As exceções da aplicação são tratadas globalmente.
- O backend compila e pode ser empacotado através do Maven Wrapper.
- O ficheiro `backend/target/devflow-hub.jar` foi gerado.
- Na validação realizada em 20/07/2026, o JAR gerado possuía
  `57 700 076` bytes.
- A pasta `target` permaneceu fora do controlo de versões.

### Validação ainda pendente

- Executar a aplicação ligada ao PostgreSQL.
- Testar os endpoints com Postman ou ferramenta equivalente.
- Adicionar testes unitários dos services.
- Adicionar testes dos controllers com MockMvc.
- Validar os pedidos com Jakarta Validation.
- Adicionar os controllers MVC e a interface Thymeleaf.
- Validar o JAR numa máquina diferente.

### Próxima etapa

Criar a branch `test/backend-api` para adicionar testes unitários dos services
e testes dos controllers REST com MockMvc. Depois, executar a aplicação ligada
ao PostgreSQL e validar os endpoints através do Postman.

### Estatísticas do Git

Segundo as estatísticas do Git, a implementação da API REST adicionou
**11 ficheiros** e **528 linhas**.

A contagem inclui controllers REST, DTOs, tratamento de exceções e o serviço do
dashboard. As alterações no README e no LOG são contabilizadas separadamente
no commit documental.
